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
