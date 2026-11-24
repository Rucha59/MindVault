# MindVault — Search Provider

Current provider: **Free Dictionary API** (`https://api.dictionaryapi.dev/api/v2/`), English words, no API key.

> **Interim version.** This is the primitive first Search, built and tested. It is planned to be replaced by Search v2 (Wiktionary for words, Wikipedia for concepts); see `SEARCH_V2_REQUIREMENTS.md`. Search v2 will need the shared contract below to change (a second source, concept results, extra fields, partial failures), so the "Files that must NOT change" rule applies only to swapping one dictionary provider for another.

## Known limits of the current provider
- Covers ordinary words well; specialist and academic concepts often return nothing. Search shows "No definition found" and points to manual Add. It never invents a definition.
- Community-run and free, with no service guarantee. Decide before a public release whether to keep it, switch, or put a backend in front of it.
- Unknown words answer HTTP 404, which the data source maps to "no result".
- Observed 2026-10-24: HTTP 530 (provider unreachable) on the emulator; the app showed its error state without crashing.
- Licensing is unverified: the data is believed to derive from Wiktionary (CC BY-SA). Confirm the current terms and attribution requirements before any public release.

## How Search is layered

```text
SearchScreen → SearchViewModel → SearchDefinitionsUseCase → SearchRepository (domain)
                                                                   ↑
                                                         SearchRepositoryImpl
                                                                   ↓
                                                      DefinitionDataSource   ← the swap point
                                                                   ↓
                                            data/remote/freedictionary/* (provider-specific)
```

Everything above `DefinitionDataSource` knows nothing about the provider. Provider models (DTOs) never leave `data/remote/<provider>/`.

## The contract (`data/remote/DefinitionDataSource.kt`)
`suspend fun lookup(query: String): List<SearchResult>`

- No match: return an **empty list**. Never invent a result.
- Cannot reach the provider (offline, timeout): throw `java.io.IOException`. The UI shows the offline state.
- Anything else wrong (bad body, server error, rate limit): throw any other exception. The UI shows the error state.
- Return **one `SearchResult` per word**, mapped to domain models and trimmed. If the provider returns several entries for a word, merge them (see `toSearchResults()`).
- Set `SearchResult.sourceName` to the provider's display name; the card shows it as attribution.

## Switching providers: steps

1. **Create a package** `data/remote/<provider>/` with:
   - `<Provider>Api.kt`: the Retrofit interface (or any HTTP client).
   - `<Provider>Dto.kt`: response models. Keep them separate from domain models.
   - `<Provider>Mapper.kt`: DTO → `SearchResult`. Drop blank text; merge duplicates by `toTitleKey()`.
   - `<Provider>DataSource.kt`: implements `DefinitionDataSource` and follows the contract above. Map the provider's "not found" signal to `emptyList()`.
2. **Edit only `core/di/SearchProviderModule.kt`**:
   - Change `bindDefinitionDataSource(impl: ...)` to the new data source class.
   - Replace `BASE_URL` and `provideFreeDictionaryApi` with the new provider's API provider. Reuse `provideOkHttpClient` or adjust timeouts.
3. **Dependencies** (`gradle/libs.versions.toml`, `app/build.gradle.kts`): add only what the new provider needs. Keep the version-catalog convention.
4. **Secrets**: never put a private API key in the app. If the provider needs one, call it through a backend and point `BASE_URL` at that backend.
5. **Tests**: copy `FreeDictionaryMapperTest` for the new mapper (sample JSON, blank fields, merged entries, no definitions). `SearchRepositoryImplTest` already covers the error mapping and needs no change.
6. **Remove the old provider**: delete `data/remote/freedictionary/` and its tests once nothing references it.
7. **Docs**: update "Current provider" and "Known limits" above, add a `CODE_CHANGES.md` entry, and update `IMPLEMENTATION_STATUS.md`.

## Files that must NOT change when switching
`SearchRepository`, `SearchRepositoryImpl`, the Search use cases, `SearchViewModel`, `SearchScreen`, `SearchResultCard`, navigation, and the Room schema. If a switch seems to need one of these, the new provider's shape probably doesn't fit `SearchResult`. Flag that before changing the shared model.

## Switching checklist
- [ ] New data source returns `emptyList()` for unknown words (no exception).
- [ ] Offline and timeout surface as `IOException`.
- [ ] `sourceName` is set.
- [ ] `SearchProviderModule` binds the new data source; no other file names the old one.
- [ ] `gradlew.bat assembleDebug` and the unit tests pass (record the real output).
- [ ] Manually test: known word, unknown word, airplane mode, save, "Edit first", already-saved state.
- [ ] Release build checked (R8 is on for release; test that parsing still works).

# MindVault — Search Providers

Current (V2): **English Wiktionary** for words and **English Wikipedia** for concepts. No API key, no server, no AI.
Previous (V1): Free Dictionary API (`https://api.dictionaryapi.dev/api/v2/`), words only. Kept in the code so the app can switch back (see "Switching back to V1").

> Status: Phase 1 is built and working (developer-reported). Autocomplete, recent searches (Phase 2) and the R8 rules (Phase 3) are written and unverified until a build and a release (R8) check are reported. Statements about Wikimedia's rate limits come from Wikimedia's published pages and are not verified against the live service for this app.

## What each source gives

| | Words (Wiktionary) | Concepts (Wikipedia) |
|---|---|---|
| Search | `GET https://en.wiktionary.org/api/rest_v1/page/definition/{term}` | `GET https://en.wikipedia.org/w/rest.php/v1/search/page?q=…&limit=5` |
| Detail | (same response) | `GET https://en.wikipedia.org/api/rest_v1/page/summary/{key}` |
| Autocomplete | `GET https://en.wiktionary.org/w/api.php?action=opensearch&format=json&search=…&limit=6` | same on en.wikipedia.org (shape assumed identical, see below) |
| Shown | definitions by part of speech, examples | short description, summary, link to the article |
| Not available | pronunciation, synonyms, etymology (not in this response) | — |
| Licence | CC BY-SA 4.0 (shown on the card) | CC BY-SA 4.0 (shown on the card) |

Real response shapes (from samples):
- **Wiktionary definition:** `{"en":[{"partOfSpeech","language","definitions":[{"definition" (HTML), "examples" (HTML list), …}]}]}`. The `"en"` list also holds other languages and symbols, so only `language == "English"` is kept. A definition with sub-senses embeds them in an `<ol>` and they also appear as separate entries, so the text is cut at `<ol>` and the parent's example list (the union of its children's) is ignored. Empty definitions are dropped; sections with the same part of speech are merged. Terms are case-sensitive: a miss on a capitalised query is retried once in lower case. An unknown term is HTTP 404 (`{"status":404,"type":"Internal error"}`) → "no result".
- **Wikipedia search:** `{"pages":[{"key","title","description","excerpt" (HTML, unused),…}]}`. Hits whose title ends "(disambiguation)" or whose description is "Topics referred to by the same term" are dropped.
- **Wikipedia summary:** `type` ("standard", "disambiguation", "no-extract", "mainpage"), `title`, `description`, `extract` (plain text), `content_urls.desktop.page`. Only `type == "standard"` with text is kept, so a disambiguation page such as "Mercury" never becomes a concept.
- **Opensearch (autocomplete):** `[query, [titles], [descriptions], [urls]]` (verified for Wiktionary; assumed identical for Wikipedia, as it is the standard MediaWiki format). Only the titles are used. Any other shape yields no suggestions instead of an error. Wiktionary's title search also suggests words of other languages; searching one may end in "Nothing found".

## How Search is layered

```text
SearchScreen → SearchViewModel → SearchDefinitionsUseCase / SuggestSearchTermsUseCase / LoadConceptSummaryUseCase
                                  RankSearchResultsUseCase (pure)
                                          ↓
                                 SearchRepository (domain)
                                          ↑
                              WikimediaSearchRepository   ← the V2 / V1 switch (RepositoryModule)
                                  ↓                 ↓
                  DefinitionDataSource       ConceptDataSource, SuggestionDataSource
                  (WiktionaryDataSource)     (WikipediaDataSource, WiktionaryDataSource)
                                  ↓                 ↓
                         data/remote/wikimedia/* (APIs, DTOs, mappers; provider-specific)
```

- Everything above the data sources knows nothing about Wikimedia. DTOs never leave `data/remote/wikimedia/`.
- **All** asks both sources at the same time; each source has its own outcome (`Success`, `NoResults`, `Offline`, `Busy`, `Failure`), so one failing source never hides the other's results. The screen shows a notice with Retry for the failed source.
- Request budget per search in **All**: 1 Wiktionary request, 1 Wikipedia search, and summaries for the first 2 Wikipedia hits = 4 requests. The other concept cards show title and description and load their summary when opened, saved or edited first. Autocomplete adds 1–2 requests per pause in typing.
- A concept without its summary is never saved.

## Wikimedia rules the app follows

- **User-Agent:** every request carries `MindVault/<version> (Android; contact: <CONTACT>)` (`UserAgentInterceptor`). **`CONTACT` in `core/di/WikimediaModule.kt` must be replaced with a project email address or website before any public release.**
- **Caching:** an HTTP disk cache (10 MB, follows Wikimedia's cache headers) and an in-memory cache (5 minutes, 50 entries each for searches, summaries and suggestions). Failures are never cached.
- **Back-off:** on HTTP 429 or 503 the app waits 1 s, then 2 s (or the server's `Retry-After` if it is 3 s or less) and retries at most twice. After a final 429 it sends no requests for 30 s (or `Retry-After`, 5–120 s) and the card area shows "busy" with a Retry.
- **Concurrency:** at most two requests in flight.
- **Debounce:** autocomplete starts after 2 characters and a 300 ms pause; each keystroke cancels the pending request. Search itself runs only on the keyboard's Search action or a tap on a suggestion, filter or Retry.

### Rate limits (research, 2026-11)
Wikimedia introduced global API limits in March 2026 for its Action API and REST APIs and describes them as experimental and subject to change: roughly 10 requests per minute for unidentified clients and 200 per minute for clients that identify themselves with a compliant User-Agent. It is **not confirmed** whether the identified limit is counted per IP address (one phone) or per User-Agent (all installs of the app together). If it is per User-Agent, all users would share it. **Before launch, contact Wikimedia, describe the app, and ask how the limit applies to a mobile app with many installs; ask about Wikimedia Enterprise if volume requires it.** The caching, debounce and small request budget above are there to keep usage low.

## Licensing and attribution

Wikipedia and Wiktionary text is available under CC BY-SA 4.0 (Wikimedia's current terms apply; re-check before release). Each card shows the source name, the licence and, when expanded, an "Open on Wikipedia/Wiktionary" link. The saved item does **not** contain the source (the definition also appears on flashcards). For public release also: list the sources and licence in an About / Open-source licences screen, and mention in the privacy policy and the Play data-safety form that **the text the user types in Search (including autocomplete) is sent to Wikimedia (Wiktionary and Wikipedia), which can see the device's IP address**. The "Search suggestions" setting turns autocomplete off.

## Known limits
- English only. Other languages (other Wikimedia language editions) are a later phase.
- Wiktionary returns no pronunciation, synonyms or etymology in this endpoint; a separate, clearly labelled source would be needed for them.
- Coverage of very specialised concepts depends on Wikipedia having an article; otherwise "Nothing found" and manual Add.
- Autocomplete is prefix based: a misspelling gets no suggestions.
- Only the top 5 Wikipedia hits are shown for a query.
- Items saved before the source footnote was removed from the saved text still contain a `Source: …` line in their definition; edit them to remove it.

## Switching back to V1 (Free Dictionary, words only)
1. In `core/di/RepositoryModule.kt`, bind `SearchRepositoryImpl` instead of `WikimediaSearchRepository` for `SearchRepository`.
2. Nothing else changes: `SearchProviderModule.kt` and `data/remote/freedictionary/*` are untouched. V1 has no concepts (the Concepts filter shows "Nothing found") and no autocomplete.
3. V1 limits: coverage of specialist concepts is poor; the service is community run with no guarantee; licensing is believed to derive from Wiktionary (CC BY-SA) and was never confirmed.

## Switching to another provider later
- **A different word or concept source:** implement `DefinitionDataSource` / `ConceptDataSource` (and `SuggestionDataSource`) in a new `data/remote/<provider>/` package with its own DTOs and mapper, bind it in a module, and add a repository or change `WikimediaSearchRepository`. Follow the contract: empty list for no match, `IOException` when unreachable, any other exception for a bad response.
- **Secrets:** never put a private API key in the app. If a provider needs one, call it through a backend and point the base URL at that backend.
- **Tests:** copy the mapper tests (`WiktionaryMapperTest`, `WikipediaMapperTest`, `OpenSearchParserTest`) and use real sample responses.
- **Docs:** update this file and add a `CODE_CHANGES.md` entry.

## Release checklist for Search
- [ ] Replace `CONTACT` in `WikimediaModule.kt`.
- [ ] Contact Wikimedia about the rate limit for a mobile app (see above).
- [ ] Run a release (R8) build and test word search, concept search, Save, Edit first and autocomplete; the keep rules in `proguard-rules.pro` are unverified until then.
- [ ] Privacy policy and Play data-safety form mention that search text is sent to Wikimedia.
- [ ] About screen lists Wikipedia/Wiktionary and CC BY-SA 4.0.
- [ ] `gradlew.bat testDebugUnitTest` and `gradlew.bat assembleDebug` pass (record the real output).
- [ ] Manual tests: known word, unknown word, concept, disambiguation term (for example "Mercury"), airplane mode, one source failing, save a word and a concept with the same title, "Edit first" on a concept, suggestions on and off, recent searches.