-- MindVault Phase C1 (+ Phase 2 of category decks): cloud tables, Row Level Security and sync functions.
-- Run once in EACH Supabase project (dev first, then prod): Dashboard -> SQL Editor -> New query -> paste -> Run.
-- Safe to run again: tables use IF NOT EXISTS, columns use ADD COLUMN IF NOT EXISTS, functions use CREATE OR REPLACE,
-- policies are dropped and recreated.
--
-- Rules this file implements (see docs/CODE_CHANGES.md, Entries 17 and 20):
--  * Every row belongs to one user (user_id). Deleting the auth user deletes all their rows (ON DELETE CASCADE),
--    so delete_my_account() also removes the cloud data.
--  * Signed-in clients can only READ their own rows (RLS). All writes go through sync_push(), which uses
--    auth.uid() and never trusts a user_id from the payload.
--  * Conflicts: the newer updated_at wins for a whole row. A deletion always wins over an edit. Client timestamps
--    more than 5 minutes ahead of the server clock are clamped to that limit.
--  * Deletions are kept as tombstones (deleted_at) so other devices learn about them.
--  * server_updated_at is set by the server and is the cursor for sync_pull(). The app must re-read a few minutes
--    before its saved cursor (rows from a slow transaction can commit late) and apply rows idempotently.
--  * No unique indexes on names or on (item, deck): two devices can create the same name or card offline. The app
--    resolves those deterministically after pulling (categories merge, later deck is renamed, earlier card is kept).
--  * sync_push() reports rows it could not apply in "rejected" instead of failing the whole batch.
--  * Phase 2: a deck can belong to a category (category decks): decks.category_id. A category that no longer
--    exists or was deleted becomes "no category", like for items. Deleting a category clears it on its decks.

-- ---------------------------------------------------------------- tables

create table if not exists public.categories (
  user_id           uuid        not null default auth.uid() references auth.users (id) on delete cascade,
  id                text        not null check (char_length(id) between 1 and 100),
  name              text        not null check (char_length(name) between 1 and 500),
  created_at        timestamptz not null,
  updated_at        timestamptz not null,
  deleted_at        timestamptz,
  server_updated_at timestamptz not null default clock_timestamp(),
  primary key (user_id, id)
);

create table if not exists public.decks (
  user_id           uuid        not null default auth.uid() references auth.users (id) on delete cascade,
  id                text        not null check (char_length(id) between 1 and 100),
  name              text        not null check (char_length(name) between 1 and 500),
  is_default        boolean     not null default false,
  category_id       text,
  created_at        timestamptz not null,
  updated_at        timestamptz not null,
  deleted_at        timestamptz,
  server_updated_at timestamptz not null default clock_timestamp(),
  primary key (user_id, id)
);

create table if not exists public.knowledge_items (
  user_id           uuid        not null default auth.uid() references auth.users (id) on delete cascade,
  id                text        not null check (char_length(id) between 1 and 100),
  title             text        not null check (char_length(title) between 1 and 2000),
  type              text        not null,
  definition        text        not null check (char_length(definition) <= 50000),
  personal_notes    text        not null default '' check (char_length(personal_notes) <= 50000),
  example           text        check (char_length(example) <= 20000),
  category_id       text,
  created_at        timestamptz not null,
  updated_at        timestamptz not null,
  deleted_at        timestamptz,
  server_updated_at timestamptz not null default clock_timestamp(),
  primary key (user_id, id),
  foreign key (user_id, category_id) references public.categories (user_id, id)
);

create table if not exists public.flashcards (
  user_id           uuid        not null default auth.uid() references auth.users (id) on delete cascade,
  id                text        not null check (char_length(id) between 1 and 100),
  knowledge_item_id text        not null,
  deck_id           text        not null,
  created_at        timestamptz not null,
  deleted_at        timestamptz,
  server_updated_at timestamptz not null default clock_timestamp(),
  primary key (user_id, id),
  foreign key (user_id, knowledge_item_id) references public.knowledge_items (user_id, id) on delete cascade,
  foreign key (user_id, deck_id) references public.decks (user_id, id) on delete cascade
);

-- Review state is removed (not tombstoned) when its card is deleted: other devices learn about the deletion
-- from the flashcard tombstone and their local copy of the state goes with the card.
create table if not exists public.review_states (
  user_id           uuid        not null default auth.uid() references auth.users (id) on delete cascade,
  flashcard_id      text        not null,
  last_reviewed_at  timestamptz,
  next_review_at    timestamptz not null,
  review_count      integer     not null check (review_count >= 0),
  ease_factor       double precision not null,
  interval_days     integer     not null check (interval_days >= 0),
  updated_at        timestamptz not null,
  server_updated_at timestamptz not null default clock_timestamp(),
  primary key (user_id, flashcard_id),
  foreign key (user_id, flashcard_id) references public.flashcards (user_id, id) on delete cascade
);

-- Phase 2 upgrade for projects that already have the decks table: the category link and its foreign key.
alter table public.decks add column if not exists category_id text;

do $$
begin
  if not exists (
    select 1 from pg_constraint
    where conname = 'decks_category_fkey' and conrelid = 'public.decks'::regclass
  ) then
    alter table public.decks
      add constraint decks_category_fkey
      foreign key (user_id, category_id) references public.categories (user_id, id);
  end if;
end
$$;

create index if not exists categories_pull_idx      on public.categories      (user_id, server_updated_at, id);
create index if not exists decks_pull_idx           on public.decks           (user_id, server_updated_at, id);
create index if not exists knowledge_items_pull_idx on public.knowledge_items (user_id, server_updated_at, id);
create index if not exists flashcards_pull_idx      on public.flashcards      (user_id, server_updated_at, id);
create index if not exists review_states_pull_idx   on public.review_states   (user_id, server_updated_at, flashcard_id);

-- ---------------------------------------------------------------- Row Level Security: read own rows only

alter table public.categories      enable row level security;
alter table public.decks           enable row level security;
alter table public.knowledge_items enable row level security;
alter table public.flashcards      enable row level security;
alter table public.review_states   enable row level security;

drop policy if exists categories_select_own      on public.categories;
drop policy if exists decks_select_own           on public.decks;
drop policy if exists knowledge_items_select_own on public.knowledge_items;
drop policy if exists flashcards_select_own      on public.flashcards;
drop policy if exists review_states_select_own   on public.review_states;

create policy categories_select_own      on public.categories      for select to authenticated using ((select auth.uid()) = user_id);
create policy decks_select_own           on public.decks           for select to authenticated using ((select auth.uid()) = user_id);
create policy knowledge_items_select_own on public.knowledge_items for select to authenticated using ((select auth.uid()) = user_id);
create policy flashcards_select_own      on public.flashcards      for select to authenticated using ((select auth.uid()) = user_id);
create policy review_states_select_own   on public.review_states   for select to authenticated using ((select auth.uid()) = user_id);

-- No insert/update/delete policies exist, and table privileges are removed: clients cannot write directly.
revoke all on table public.categories, public.decks, public.knowledge_items, public.flashcards, public.review_states
  from public, anon, authenticated;
grant select on table public.categories, public.decks, public.knowledge_items, public.flashcards, public.review_states
  to authenticated;

-- ---------------------------------------------------------------- sync_push

-- p = { "changes": [ { "type": "CATEGORY|DECK|KNOWLEDGE_ITEM|FLASHCARD|REVIEW_STATE",
--                       "op": "UPSERT|DELETE", "id": "<id>", "changed_at": <epoch ms>, "data": { ... } } ] }
-- "data" (UPSERT only), all timestamps are epoch milliseconds:
--   CATEGORY        name, created_at, updated_at
--   DECK            name, is_default, category_id, created_at, updated_at
--   KNOWLEDGE_ITEM  title, type, definition, personal_notes, example, category_id, created_at, updated_at
--   FLASHCARD       knowledge_item_id, deck_id, created_at
--   REVIEW_STATE    (id = flashcard id) last_reviewed_at, next_review_at, review_count, ease_factor,
--                   interval_days, updated_at
-- Returns { "server_time": "<ISO-8601 UTC>", "rejected": [ { "type": ..., "id": ... } ] }.
-- Upserts run parents first, deletions run children first, so one batch can contain a whole tree.
create or replace function public.sync_push(p jsonb)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid      uuid        := auth.uid();
  v_now      timestamptz := clock_timestamp();
  v_max      timestamptz := clock_timestamp() + interval '5 minutes';
  v_changes  jsonb       := coalesce(p -> 'changes', '[]'::jsonb);
  r          record;
  v_cat      text;
  v_del_at   timestamptz;
  v_rejected jsonb       := '[]'::jsonb;
begin
  if v_uid is null then
    raise exception 'Not authenticated' using errcode = '28000';
  end if;
  if jsonb_typeof(v_changes) <> 'array' or jsonb_array_length(v_changes) > 1000 then
    raise exception 'Invalid payload' using errcode = '22023';
  end if;

  -- 1. Categories (upsert)
  for r in select t.value as v from jsonb_array_elements(v_changes) as t(value)
           where t.value ->> 'type' = 'CATEGORY' and t.value ->> 'op' = 'UPSERT' loop
    begin
      insert into public.categories (user_id, id, name, created_at, updated_at, server_updated_at)
      values (v_uid, r.v ->> 'id', r.v #>> '{data,name}',
              to_timestamp((r.v #>> '{data,created_at}')::numeric / 1000.0),
              least(to_timestamp((r.v #>> '{data,updated_at}')::numeric / 1000.0), v_max),
              v_now)
      on conflict (user_id, id) do update
        set name = excluded.name, updated_at = excluded.updated_at, server_updated_at = v_now
        where public.categories.deleted_at is null and excluded.updated_at > public.categories.updated_at;
    exception when others then
      v_rejected := v_rejected || jsonb_build_array(jsonb_build_object('type', r.v ->> 'type', 'id', r.v ->> 'id'));
    end;
  end loop;

  -- 2. Decks (upsert). A category that no longer exists or was deleted becomes "no category".
  for r in select t.value as v from jsonb_array_elements(v_changes) as t(value)
           where t.value ->> 'type' = 'DECK' and t.value ->> 'op' = 'UPSERT' loop
    begin
      v_cat := nullif(r.v #>> '{data,category_id}', '');
      if v_cat is not null and not exists (
        select 1 from public.categories c
        where c.user_id = v_uid and c.id = v_cat and c.deleted_at is null
      ) then
        v_cat := null;
      end if;

      insert into public.decks (user_id, id, name, is_default, category_id, created_at, updated_at, server_updated_at)
      values (v_uid, r.v ->> 'id', r.v #>> '{data,name}',
              coalesce((r.v #>> '{data,is_default}')::boolean, false),
              v_cat,
              to_timestamp((r.v #>> '{data,created_at}')::numeric / 1000.0),
              least(to_timestamp((r.v #>> '{data,updated_at}')::numeric / 1000.0), v_max),
              v_now)
      on conflict (user_id, id) do update
        set name = excluded.name, category_id = excluded.category_id,
            updated_at = excluded.updated_at, server_updated_at = v_now
        where public.decks.deleted_at is null and excluded.updated_at > public.decks.updated_at;
    exception when others then
      v_rejected := v_rejected || jsonb_build_array(jsonb_build_object('type', r.v ->> 'type', 'id', r.v ->> 'id'));
    end;
  end loop;

  -- 3. Knowledge items (upsert). A category that no longer exists or was deleted becomes "no category".
  for r in select t.value as v from jsonb_array_elements(v_changes) as t(value)
           where t.value ->> 'type' = 'KNOWLEDGE_ITEM' and t.value ->> 'op' = 'UPSERT' loop
    begin
      v_cat := nullif(r.v #>> '{data,category_id}', '');
      if v_cat is not null and not exists (
        select 1 from public.categories c
        where c.user_id = v_uid and c.id = v_cat and c.deleted_at is null
      ) then
        v_cat := null;
      end if;

      insert into public.knowledge_items (user_id, id, title, type, definition, personal_notes, example,
                                          category_id, created_at, updated_at, server_updated_at)
      values (v_uid, r.v ->> 'id', r.v #>> '{data,title}', r.v #>> '{data,type}', r.v #>> '{data,definition}',
              coalesce(r.v #>> '{data,personal_notes}', ''), nullif(r.v #>> '{data,example}', ''), v_cat,
              to_timestamp((r.v #>> '{data,created_at}')::numeric / 1000.0),
              least(to_timestamp((r.v #>> '{data,updated_at}')::numeric / 1000.0), v_max),
              v_now)
      on conflict (user_id, id) do update
        set title = excluded.title, type = excluded.type, definition = excluded.definition,
            personal_notes = excluded.personal_notes, example = excluded.example,
            category_id = excluded.category_id, updated_at = excluded.updated_at, server_updated_at = v_now
        where public.knowledge_items.deleted_at is null
          and excluded.updated_at > public.knowledge_items.updated_at;
    exception when others then
      v_rejected := v_rejected || jsonb_build_array(jsonb_build_object('type', r.v ->> 'type', 'id', r.v ->> 'id'));
    end;
  end loop;

  -- 4. Flashcards (upsert). Immutable once created; skipped if the item or deck was deleted (deletion wins).
  for r in select t.value as v from jsonb_array_elements(v_changes) as t(value)
           where t.value ->> 'type' = 'FLASHCARD' and t.value ->> 'op' = 'UPSERT' loop
    begin
      if exists (select 1 from public.knowledge_items i
                 where i.user_id = v_uid and i.id = r.v #>> '{data,knowledge_item_id}' and i.deleted_at is null)
         and exists (select 1 from public.decks d
                     where d.user_id = v_uid and d.id = r.v #>> '{data,deck_id}' and d.deleted_at is null) then
        insert into public.flashcards (user_id, id, knowledge_item_id, deck_id, created_at, server_updated_at)
        values (v_uid, r.v ->> 'id', r.v #>> '{data,knowledge_item_id}', r.v #>> '{data,deck_id}',
                to_timestamp((r.v #>> '{data,created_at}')::numeric / 1000.0), v_now)
        on conflict (user_id, id) do nothing;
      end if;
    exception when others then
      v_rejected := v_rejected || jsonb_build_array(jsonb_build_object('type', r.v ->> 'type', 'id', r.v ->> 'id'));
    end;
  end loop;

  -- 5. Review states (upsert). Skipped if the card is missing or deleted.
  for r in select t.value as v from jsonb_array_elements(v_changes) as t(value)
           where t.value ->> 'type' = 'REVIEW_STATE' and t.value ->> 'op' = 'UPSERT' loop
    begin
      if exists (select 1 from public.flashcards f
                 where f.user_id = v_uid and f.id = r.v ->> 'id' and f.deleted_at is null) then
        insert into public.review_states (user_id, flashcard_id, last_reviewed_at, next_review_at, review_count,
                                          ease_factor, interval_days, updated_at, server_updated_at)
        values (v_uid, r.v ->> 'id',
                to_timestamp(nullif(r.v #>> '{data,last_reviewed_at}', '')::numeric / 1000.0),
                to_timestamp((r.v #>> '{data,next_review_at}')::numeric / 1000.0),
                (r.v #>> '{data,review_count}')::integer,
                (r.v #>> '{data,ease_factor}')::double precision,
                (r.v #>> '{data,interval_days}')::integer,
                least(to_timestamp((r.v #>> '{data,updated_at}')::numeric / 1000.0), v_max),
                v_now)
        on conflict (user_id, flashcard_id) do update
          set last_reviewed_at = excluded.last_reviewed_at, next_review_at = excluded.next_review_at,
              review_count = excluded.review_count, ease_factor = excluded.ease_factor,
              interval_days = excluded.interval_days, updated_at = excluded.updated_at, server_updated_at = v_now
          where excluded.updated_at > public.review_states.updated_at;
      end if;
    exception when others then
      v_rejected := v_rejected || jsonb_build_array(jsonb_build_object('type', r.v ->> 'type', 'id', r.v ->> 'id'));
    end;
  end loop;

  -- 6. Deletions, children first. Each one is a tombstone; dependent rows are tombstoned in the same step.
  for r in select t.value as v from jsonb_array_elements(v_changes) as t(value)
           where t.value ->> 'type' = 'FLASHCARD' and t.value ->> 'op' = 'DELETE' loop
    begin
      v_del_at := least(to_timestamp((r.v ->> 'changed_at')::numeric / 1000.0), v_max);
      update public.flashcards
         set deleted_at = v_del_at, server_updated_at = v_now
       where user_id = v_uid and id = r.v ->> 'id' and deleted_at is null;
      delete from public.review_states where user_id = v_uid and flashcard_id = r.v ->> 'id';
    exception when others then
      v_rejected := v_rejected || jsonb_build_array(jsonb_build_object('type', r.v ->> 'type', 'id', r.v ->> 'id'));
    end;
  end loop;

  for r in select t.value as v from jsonb_array_elements(v_changes) as t(value)
           where t.value ->> 'type' = 'KNOWLEDGE_ITEM' and t.value ->> 'op' = 'DELETE' loop
    begin
      v_del_at := least(to_timestamp((r.v ->> 'changed_at')::numeric / 1000.0), v_max);
      update public.knowledge_items
         set deleted_at = v_del_at, server_updated_at = v_now
       where user_id = v_uid and id = r.v ->> 'id' and deleted_at is null;
      with dead as (
        update public.flashcards
           set deleted_at = v_del_at, server_updated_at = v_now
         where user_id = v_uid and knowledge_item_id = r.v ->> 'id' and deleted_at is null
        returning id
      )
      delete from public.review_states
       where user_id = v_uid and flashcard_id in (select id from dead);
    exception when others then
      v_rejected := v_rejected || jsonb_build_array(jsonb_build_object('type', r.v ->> 'type', 'id', r.v ->> 'id'));
    end;
  end loop;

  for r in select t.value as v from jsonb_array_elements(v_changes) as t(value)
           where t.value ->> 'type' = 'DECK' and t.value ->> 'op' = 'DELETE' loop
    begin
      v_del_at := least(to_timestamp((r.v ->> 'changed_at')::numeric / 1000.0), v_max);
      -- The default deck can never be deleted.
      update public.decks
         set deleted_at = v_del_at, server_updated_at = v_now
       where user_id = v_uid and id = r.v ->> 'id' and deleted_at is null and not is_default;
      if found then
        with dead as (
          update public.flashcards
             set deleted_at = v_del_at, server_updated_at = v_now
           where user_id = v_uid and deck_id = r.v ->> 'id' and deleted_at is null
          returning id
        )
        delete from public.review_states
         where user_id = v_uid and flashcard_id in (select id from dead);
      end if;
    exception when others then
      v_rejected := v_rejected || jsonb_build_array(jsonb_build_object('type', r.v ->> 'type', 'id', r.v ->> 'id'));
    end;
  end loop;

  for r in select t.value as v from jsonb_array_elements(v_changes) as t(value)
           where t.value ->> 'type' = 'CATEGORY' and t.value ->> 'op' = 'DELETE' loop
    begin
      v_del_at := least(to_timestamp((r.v ->> 'changed_at')::numeric / 1000.0), v_max);
      update public.categories
         set deleted_at = v_del_at, server_updated_at = v_now
       where user_id = v_uid and id = r.v ->> 'id' and deleted_at is null;
      -- Items keep existing; they just lose the category. updated_at is left alone so the Vault order does not change.
      update public.knowledge_items
         set category_id = null, server_updated_at = v_now
       where user_id = v_uid and category_id = r.v ->> 'id' and deleted_at is null;
      -- Decks made for the category keep existing as ordinary decks; updated_at is left alone.
      update public.decks
         set category_id = null, server_updated_at = v_now
       where user_id = v_uid and category_id = r.v ->> 'id' and deleted_at is null;
    exception when others then
      v_rejected := v_rejected || jsonb_build_array(jsonb_build_object('type', r.v ->> 'type', 'id', r.v ->> 'id'));
    end;
  end loop;

  return jsonb_build_object(
    'server_time', to_char(v_now at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"'),
    'rejected', v_rejected
  );
end;
$$;

-- ---------------------------------------------------------------- sync_pull

-- Returns one page of rows changed after (p_since, p_after_id), oldest first, as a JSON array.
-- p_table: categories | decks | knowledge_items | flashcards | review_states.
-- First page: p_since = '1970-01-01T00:00:00Z', p_after_id = ''. Next page: the server_updated_at and id (for
-- review_states, flashcard_id) of the last row returned. A page shorter than p_limit is the last one.
-- Timestamps are epoch milliseconds, except server_updated_at (ISO-8601 UTC with microseconds; pass it back as is).
create or replace function public.sync_pull(
  p_table    text,
  p_since    timestamptz,
  p_after_id text    default '',
  p_limit    integer default 500
)
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  v_uid   uuid    := auth.uid();
  v_limit integer := least(greatest(coalesce(p_limit, 500), 1), 1000);
  v_after text    := coalesce(p_after_id, '');
  v_rows  jsonb;
begin
  if v_uid is null then
    raise exception 'Not authenticated' using errcode = '28000';
  end if;

  if p_table = 'categories' then
    select coalesce(jsonb_agg(jsonb_build_object(
             'id', s.id, 'name', s.name,
             'created_at', round(extract(epoch from s.created_at) * 1000)::bigint,
             'updated_at', round(extract(epoch from s.updated_at) * 1000)::bigint,
             'deleted_at', round(extract(epoch from s.deleted_at) * 1000)::bigint,
             'server_updated_at', to_char(s.server_updated_at at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"')
           ) order by s.server_updated_at, s.id), '[]'::jsonb)
      into v_rows
      from (select * from public.categories t
             where t.user_id = v_uid and (t.server_updated_at, t.id) > (p_since, v_after)
             order by t.server_updated_at, t.id limit v_limit) s;

  elsif p_table = 'decks' then
    select coalesce(jsonb_agg(jsonb_build_object(
             'id', s.id, 'name', s.name, 'is_default', s.is_default, 'category_id', s.category_id,
             'created_at', round(extract(epoch from s.created_at) * 1000)::bigint,
             'updated_at', round(extract(epoch from s.updated_at) * 1000)::bigint,
             'deleted_at', round(extract(epoch from s.deleted_at) * 1000)::bigint,
             'server_updated_at', to_char(s.server_updated_at at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"')
           ) order by s.server_updated_at, s.id), '[]'::jsonb)
      into v_rows
      from (select * from public.decks t
             where t.user_id = v_uid and (t.server_updated_at, t.id) > (p_since, v_after)
             order by t.server_updated_at, t.id limit v_limit) s;

  elsif p_table = 'knowledge_items' then
    select coalesce(jsonb_agg(jsonb_build_object(
             'id', s.id, 'title', s.title, 'type', s.type, 'definition', s.definition,
             'personal_notes', s.personal_notes, 'example', s.example, 'category_id', s.category_id,
             'created_at', round(extract(epoch from s.created_at) * 1000)::bigint,
             'updated_at', round(extract(epoch from s.updated_at) * 1000)::bigint,
             'deleted_at', round(extract(epoch from s.deleted_at) * 1000)::bigint,
             'server_updated_at', to_char(s.server_updated_at at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"')
           ) order by s.server_updated_at, s.id), '[]'::jsonb)
      into v_rows
      from (select * from public.knowledge_items t
             where t.user_id = v_uid and (t.server_updated_at, t.id) > (p_since, v_after)
             order by t.server_updated_at, t.id limit v_limit) s;

  elsif p_table = 'flashcards' then
    select coalesce(jsonb_agg(jsonb_build_object(
             'id', s.id, 'knowledge_item_id', s.knowledge_item_id, 'deck_id', s.deck_id,
             'created_at', round(extract(epoch from s.created_at) * 1000)::bigint,
             'deleted_at', round(extract(epoch from s.deleted_at) * 1000)::bigint,
             'server_updated_at', to_char(s.server_updated_at at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"')
           ) order by s.server_updated_at, s.id), '[]'::jsonb)
      into v_rows
      from (select * from public.flashcards t
             where t.user_id = v_uid and (t.server_updated_at, t.id) > (p_since, v_after)
             order by t.server_updated_at, t.id limit v_limit) s;

  elsif p_table = 'review_states' then
    select coalesce(jsonb_agg(jsonb_build_object(
             'id', s.flashcard_id,
             'last_reviewed_at', round(extract(epoch from s.last_reviewed_at) * 1000)::bigint,
             'next_review_at', round(extract(epoch from s.next_review_at) * 1000)::bigint,
             'review_count', s.review_count, 'ease_factor', s.ease_factor, 'interval_days', s.interval_days,
             'updated_at', round(extract(epoch from s.updated_at) * 1000)::bigint,
             'server_updated_at', to_char(s.server_updated_at at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"')
           ) order by s.server_updated_at, s.flashcard_id), '[]'::jsonb)
      into v_rows
      from (select * from public.review_states t
             where t.user_id = v_uid and (t.server_updated_at, t.flashcard_id) > (p_since, v_after)
             order by t.server_updated_at, t.flashcard_id limit v_limit) s;

  else
    raise exception 'Unknown table' using errcode = '22023';
  end if;

  return v_rows;
end;
$$;

-- Only signed-in users may call the sync functions; never anonymous or public callers.
revoke all on function public.sync_push(jsonb) from public, anon;
revoke all on function public.sync_pull(text, timestamptz, text, integer) from public, anon;
grant execute on function public.sync_push(jsonb) to authenticated;
grant execute on function public.sync_pull(text, timestamptz, text, integer) to authenticated;