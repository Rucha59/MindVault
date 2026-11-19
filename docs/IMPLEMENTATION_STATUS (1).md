# MindVault — Implementation Status

> **Status accuracy rule:** This file is a checkpoint for the human developer and AI tools. Update it from actual repository inspection, supplied code, and build/test output. Do not infer that a feature is implemented just because it appears in the roadmap. "Developer-reported" means Rucha ran the build, tests or manual checks and reported the result without pasting output.

## 1. Current Project Snapshot

- **Product:** MindVault — Android personal knowledge vault.
- **Core features:** Search, Add, Vault, Flashcards, plus Home dashboard and Profile & Settings.
- **Technology:** Kotlin, Jetpack Compose, Material 3, Navigation Compose, MVVM, Room, Hilt, Coroutines/Flow, Retrofit/OkHttp (external Search), DataStore Preferences (theme setting).
- **Environment:** Android Studio Rabbit 1 | 2026.2.1, Windows, emulator API 35 (x86_64).
- **Build status:** Verified (developer-reported) through Phase 7a. Phase 7b (Entry 12) is written, not built.
- **Test status:** Unit tests for use cases, mappers, Vault query, flashcards and Home (`HomeSummaryBuilderTest`) run by developer (developer-reported). `ThemePreferenceTest` (Phase 7b) written, not run. No ViewModel or UI tests.
- **Scope change (2026-10-29):** user accounts (Supabase email + Google sign-in), guest mode, per-user sessions and cloud sync are now **in scope** for launch. Planned, not started. See `docs/AUTH_CLOUD_PHASE_PROMPT.md` and §7. `PROJECT_BRIEF.md` and `ARCHITECTURE.md` still describe these as out of scope and must be updated first.
- **Last verified commit:** Not tracked in Git yet.

## 2. Implementation Checklist

| Area                                        | Status                                                     | Evidence / notes                                                                                                                             |
|---------------------------------------------|------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| Android project and Gradle setup            | Verified                                                   | Builds and installs on an emulator (API 35, x86_64).                                                                                         |
| App theme and reusable UI components        | Verified                                                   | Light/dark theme follows the system setting on-device.                                                                                       |
| Main navigation shell                       | Verified                                                   | Search, Vault, Add sheet, Flashcards, Home and Profile (from Home's top bar) navigable on-device.                                            |
| Architecture and dependency injection       | Verified (developer-reported)                              | Hilt wiring, repositories, use cases and ViewModels in use across features.                                                                  |
| Knowledge data model and Room database      | Verified (developer-reported)                              | Room schema v2 (exported to `app/schemas`). No migrations yet.                                                                               |
| Manual add / view / edit / delete           | Verified (developer-reported)                              | Add form, detail view, edit and delete with confirmation.                                                                                    |
| Categories                                  | Verified (developer-reported)                              | Create and pick in the Add form; used for Vault filtering and Home counts. No category management (rename/delete) UI yet.                    |
| Vault search, filter, sort                  | Verified (developer-reported)                              | Search within saved items, type and category filters, 4 sort orders (Entry 8).                                                               |
| External Search                             | Verified (developer-reported) — interim                    | Free Dictionary API behind `DefinitionDataSource`. To be replaced by Search v2 (`docs/SEARCH_V2_REQUIREMENTS.md`).                           |
| Save Search result to Vault                 | Verified (developer-reported)                              | Save, "Edit first", duplicate handling.                                                                                                      |
| Default and custom flashcard decks          | Verified (developer-reported)                              | Create, rename, delete (default deck cannot be deleted), add/remove cards.                                                                   |
| Flashcard study and review state            | Verified (developer-reported)                              | Flip, previous/next, shuffle, progress, deck completion. A card is "reviewed" on first reveal. No scheduling logic or due counts yet.        |
| Home dashboard                              | Verified (developer-reported)                              | Entry 11: Continue studying, decks completed today (approximation), category pills, recent items. No due count.                              |
| Profile and Settings                        | Written — unverified                                       | Entry 12: theme preference (System/Light/Dark) via DataStore. No other settings.                                                             |
| Authentication, sessions, guest mode        | Partial — written, unverified                              | Entry 13: email auth, encrypted session, deep link, tests.                                                                                   |Google sign-in, guest mode, in-app account deletion; email hidden by flag|
| Cloud sync and per-user data                | Not started                                                | In scope since 2026-10-29.                                                                                                                   |
| Spaced repetition (self-rating, due counts) | Not started                                                | `ReviewState` already stores `nextReviewAt`, `easeFactor`, `intervalDays`.                                                                   |
| Unit / database / UI tests                  | Partial                                                    | Use-case, mapper, Vault query and Home logic tests; Room database instrumented test. Theme tests written, not run. No ViewModel or UI tests. |
| Debug / release build                       | Verified (debug only)                                      | Release build not attempted. Release signing config exists in `app/build.gradle.kts`.                                                        |
| Per-account local data and user switching   | Verified (developer-reported)                              | Entry 16                                                                                                                                     |
| Cloud sync foundation (C1)                  | Verified (developer-reported)                              | Entry 17                                                                                                                                     |
| Authentication, sessions, guest mode	        | Verified (developer-reported)	                              | Google sign-in, guest mode, account deletion; email hidden by flag.                                                                          |
| Cloud sync engine and triggers (C2+C3)	      | Written, unverified 	                                       | Entry 18.                                                                                                                                    |
| Sync status UI and sign-out with final sync | Written, unverified                                        | Entry 18                                                                                                                                     |
| Unit / database / UI tests	                  | Partial	Add: sync engine, conflict policy, status builder   | (Entry 18).                                                                                                                                  |
## 3. Files Changed

Entry numbers refer to `docs/CODE_CHANGES.md`.

| Date                                     | Task / phase                                            | Files created or modified                                                                                                                                                                                                                                                                                      | Verification                           |
|------------------------------------------|---------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------|
| —                                        | Phase 1: Foundation and UI shell                        | Gradle setup, theme, navigation, reusable components, placeholder screens. Entry 1.                                                                                                                                                                                                                            | Verified (developer-reported)          |
| —                                        | Phase 1: Dark mode                                      | `core/theme/Color.kt`, theme file, `MainActivity.kt`, `values-night/themes.xml`. Entry 2.                                                                                                                                                                                                                      | Verified (developer-reported)          |
| 2026-10-17                               | Phase 1: Build/run debugging                            | Renamed `libs.version.toml` → `libs.versions.toml`; updated Android Studio; fixed empty/misplaced files and the `MindVaultDestination.kt` init-order crash (B1–B8).                                                                                                                                            | Verified — builds and runs on emulator |
| —                                        | Phase 2: Data model and Room                            | Entities, DAOs, database, domain models, repositories, mappers, Hilt modules, tests. Entry 3.                                                                                                                                                                                                                  | Verified (developer-reported)          |
| —                                        | Entry 4                                                 | [Fill in from `docs/CODE_CHANGES.md` — this entry was not provided to the assistant.]                                                                                                                                                                                                                          | [Fill in]                              |
| —                                        | Phase 3: Add form, Vault list, item detail, build fixes | Add form, `VaultScreen`, detail screen, use cases, `MindVaultTextField`. Entries 5–6.                                                                                                                                                                                                                          | Verified (developer-reported)          |
| —                                        | Phase 4a: Edit and delete                               | `KnowledgeValidator`, update/delete use cases, edit route reusing the Add form. Entry 7.                                                                                                                                                                                                                       | Verified (developer-reported)          |
| —                                        | Phase 4b: Vault search, filter, sort                    | `VaultQuery`, `ApplyVaultQueryUseCase`, `MindVaultFilterChip`, Vault UI. Entry 8.                                                                                                                                                                                                                              | Verified (developer-reported)          |
| —                                        | Phase 5: Search and save                                | `feature/search/*`, `DefinitionDataSource`, remote DTOs, Search use cases; `docs/SEARCH_PROVIDER.md`. Entries 9–10 [confirm numbering].                                                                                                                                                                        | Verified (developer-reported)          |
| —                                        | Phase 6: Flashcards and decks                           | `feature/flashcards/*`, deck and card use cases, `DeckSummary`, `DeckProgress`; `docs/FLASHCARDS_TEST_PLAN.md`. Entries 9–10 [confirm numbering].                                                                                                                                                              | Verified (developer-reported)          |
| 2026-10-29                               | Phase 7a: Home dashboard                                | New: `HomeSummary`, `HomeSummaryBuilder`, `ObserveHomeSummaryUseCase`, `HomeUiState`, `HomeViewModel`, `HomeSummaryBuilderTest`. Modified: `HomeScreen.kt`, `MindVaultNavHost.kt`. Entry 11.                                                                                                                   | Verified (developer-reported)          |
| 2026-10-29                               | Phase 7b: Profile & Settings (theme)                    | New: `ThemePreference`, `SettingsRepository`, theme use cases, `SettingsRepositoryImpl`, `SettingsModule`, `AppThemeViewModel`, `ProfileUiState`, `ProfileViewModel`, 2 test files. Modified: `MainActivity.kt`, `ProfileScreen.kt`, `libs.versions.toml`, `app/build.gradle.kts` (DataStore 1.1.1). Entry 12. | Written — unverified                   |
| 2026-10-29                               | Phase A1a: Supabase Auth foundation                     | See Entry 13                                                                                                                                                                                                                                                                                                   | Written — unverified                   |
| Phase A3: Google-only + account deletion | Entry 15                                                | Written — unverified                                                                                                                                                                                                                                                                                           |                                        |
| Phase B: per-account local data          | See Entry 16                                            | Written — unverified|
| Phase C1: Cloud sync foundation          | See Entry 17                                            |
## 4. What's Working (developer-reported)

- App builds, installs and runs on an emulator.
- Add, view, edit and delete knowledge items; items persist across restarts.
- Vault list with search, type/category filters and sorting.
- Search, review a result, save it (or edit first) with duplicate handling.
- Decks (default and custom), adding cards, study mode with flip, previous/next and shuffle, progress and deck completion.
- Home dashboard with real data: Continue studying, decks completed today, category counts, recent items.
- Bottom navigation order Search, Vault, Add, Flashcards, Home; Profile from Home's top bar.

Not yet confirmed: Phase 7b theme setting, behavior on a physical device, accessibility/contrast checks (dark palette is unmeasured), release build.

## 5. Known Bugs and Limitations

| ID | Issue | Status |
|---|---|---|
| B1 | `ClassNotFoundException: MainActivity` on launch (empty file) | **Fixed** |
| B2 | Same exception from a stale APK | **Fixed** — clean, uninstall, rebuild |
| B3 | `Unresolved reference: libs` (misnamed version catalog) | **Fixed** |
| B4 | AGP incompatible with installed Android Studio | **Fixed** — updated to Rabbit 1 |
| B5 | `Expects top level declaration` from a partial paste | **Fixed** |
| B6 | Missing `Color.kt` / `Type.kt` references | **Fixed** |
| B7 | `app/` package folder mismatch | **Fixed** |
| B8 | Crash from eager `bottomBarRoutes` init order | **Fixed** — `by lazy` |

Known limitations (not bugs):
- Search uses an interim provider (Free Dictionary API); coverage of specialist concepts is limited.
- "Decks completed today" is an approximation: a deck counts when every card's last review is today, so a finished deck fully re-studied on a later day counts again. An exact version needs a `completedAt` column (non-destructive Room migration).
- No spaced repetition: no due counts, no self-rating.
- Theme may briefly show the system theme at cold start before the saved choice loads; the launch window background follows the system setting.
- Account data exists only on this device until cloud sync (Phase C).
- UI strings are hardcoded (move to `strings.xml` before release).
- `AddCardsViewModel` assumes `VaultQuery` fields have defaults; some file names have odd casing (e.g. `Searchviewmodel.kt`).
- No `kotlinx-coroutines-test` in the version catalog, so no ViewModel tests yet.

## 6. Decisions and Open Questions

### Confirmed product decisions
- MindVault is an Android personal knowledge app; core features are Search, Add, Vault, Flashcards.
- Saved knowledge is stored once and reused across Home, Vault, Search and Flashcards.
- Room is the local source of truth; flashcards reference knowledge items and never copy them.
- External Search is isolated behind a repository/data-source boundary.
- sessions encrypted; PKCE; guest = signed out; Google via Credential Manager with nonce
- Launch auth = Google + guest; email sign-in hidden behind auth.emailEnabled until custom SMTP exists.
- - **Sign-out keeps the vault (Phase B, 2026-11-06).** Signing out keeps that account's on-device vault; it reopens on the next sign-in to the same account. Cloud sync (Phase C) will add "clear after a confirmed sync".
- **Delete account removes the local vault (Phase B).** Deleting an account also deletes that account's on-device vault, after the server deletion succeeds. The guest vault is never touched.
- **New accounts start empty (Phase B).** Guest items stay in the guest vault. Phase D1 will offer to add them to an account.
- **Account change resets navigation (Phase B).** Any change of active vault returns to Home, clears the back stack and clears saved tab states. Switching accounts is sign-out then sign-in only; there is no direct account switching.
- **Theme and default deck (Phase B).** The theme preference stays global (DataStore), not per account. The default deck ID stays `default-deck` in every vault.
- **No Room migration in Phase B.** Phase B changes no tables or columns, so `MindVaultDatabase` stays at version 1 and `app/schemas/1.json` is unchanged. Isolation between vaults is covered by `ActiveDatabaseTest` (which uses its own test files) and by a manual check that existing guest data is intact after installing over the previous build.
- **Backup exclusions (Phase B).** The encrypted auth session (`mindvault_auth.xml`) and the last-open-vault marker (`mindvault_active_owner.xml`) are excluded from backup. Account vaults live in `no_backup/users/`, which Android never backs up.
-  Conflicts: the newer updatedAt wins for a whole row (client time, clamped to server time + 5 minutes). Local edits are always at least 1 ms newer than the version they replace, so a device clock moved backwards cannot lose edits.
-  A deletion wins over a concurrent edit. A card for a deleted item or deck is not created.
-  Same name on two devices: categories merge (older kept, items re-pointed); the later deck is renamed "Name (2)". Duplicate cards for the same item and deck: the earlier card is kept. The app resolves these after pulling (C2); the cloud has no unique indexes on names or (item, deck).
-  Sign-out: final sync, then delete the account's local vault only if everything is uploaded; otherwise keep it and warn (C3).
-  Sync runs on app open, a few seconds after a change, and about every 6 hours, on any network (C3). Status is shown only in the Profile account card (C3).
-  Sync metadata lives in two new tables, not extra columns on every entity.
- **Scope (updated 2026-10-29):** user accounts, Supabase email and Google sign-in, guest mode, per-user sessions and cloud sync are in scope for launch. AI-generated content, social features, payments and gamification remain out of scope.
- Package / application ID: `com.mindvault`.
- Bottom navigation order: Search, Vault, Add (center action, opens a sheet), Flashcards, Home. Profile opens from Home's top bar.
- Dark mode follows the system by default; users can choose System / Light / Dark in Profile (Phase 7b, unverified).
- IDs are UUID strings; timestamps are UTC epoch millis.
- A card is "reviewed" when its answer is first revealed in a pass; a deck is complete when every card has been reviewed at least once; the default deck (`default-deck`) can be renamed but never deleted; shuffle is not persisted.
- Home shows no due count until scheduling exists; no streaks or other unsupported statistics.
- **Versions (from the supplied catalog):** AGP 8.9.1, Kotlin 2.0.21, KSP 2.0.21-1.0.28, Hilt 2.52, Compose BOM 2024.10.01, Navigation Compose 2.8.4, Room 2.6.1, lifecycle 2.8.7, Retrofit 2.11.0, OkHttp 4.12.0, kotlinx.serialization 1.7.3, DataStore 1.1.1 (Phase 7b), compile/target SDK 36, minSdk 26. Gradle wrapper version unconfirmed.
  supabase-kt 3.0.2, Ktor 3.0.1 (Phase A); Gradle wrapper 8.11.1 (from gradle-wrapper.properties)
### Decisions to record when made
- Auth/sync design: guest data handling, per-user data separation , default-deck ID strategy, email confirmation setting.
- Review scheduling algorithm and exact "completed today" storage.
- Search v2 provider scope (see `docs/SEARCH_V2_REQUIREMENTS.md`).
- Final design tokens for new screens (Sign in, Account, sync status).
- Sessions encrypted at rest; PKCE email links; guest mode = signed out; no gate
- auth session file and last-open-vault marker excluded from backup.
## 7. Next Task
**Recommended next task:** Phase C1 (sync metadata, Postgres schema, RLS).
Postponed: spaced repetition with due counts, Search v2, "Create a flashcard" in the Add menu, remembering the study shuffle choice, exact "completed today".

## 8. Handoff Template for a New AI Conversation

- **Current phase:** Phases 1–7a complete and verified (developer-reported). Phase 7b written, not built.
- **Exact task:** See "Next Task" above.
- **Relevant files to provide:** `PROJECT_BRIEF.md`, `ARCHITECTURE.md`, `DESIGN_SYSTEM.md`, `IMPLEMENTATION_STATUS.md` (this file), `docs/CODE_CHANGES.md`, `docs/SEARCH_PROVIDER.md`, `docs/SEARCH_V2_REQUIREMENTS.md`, `docs/FLASHCARDS_TEST_PLAN.md`, and the source files the task needs.
- **Current build/test result:** [Paste the latest `gradlew.bat assembleDebug` and `testDebugUnitTest` output, or state that they passed.]
- **Files changed in the previous task:** See §3 and `docs/CODE_CHANGES.md`.
- **Known issues to address:** See §5.
- **Acceptance criteria:** Per `PROJECT_BRIEF.md` §9 plus the new account and sync requirements once the brief is updated.
- **Out of scope for the next task:** AI-generated content, social features, payments, gamification, unrelated refactors.

AI instructions:
- Treat this file as a status checkpoint, not proof of implementation.
- Use only files and outputs supplied in the current conversation.
- Ask for missing context rather than guessing.
- Make only the changes needed for the specified task.
- Provide complete changed files or precise patches.
- State which checks were actually run and which remain unverified.
- Do not update a status to `Verified` without supporting evidence.