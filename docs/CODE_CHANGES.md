# MindVault — Code Change Log

Purpose: a running record of every code change, written so a new AI conversation (or a future you) can continue without guessing. Add a new entry at the **bottom** after each change. Pair it with `IMPLEMENTATION_STATUS.md` (which tracks verified status); this file tracks **what was changed and where**.

Status words used below: **Written** = files exist but nothing has been compiled or run. **Verified** = built/run with real output. Nothing in this log is Verified until a note says so.

Paths are relative to the project root (the folder containing `settings.gradle.kts`). Kotlin sources live under `app/src/main/java/com/mindvault/`, abbreviated `…/mindvault/` below.

---

## Entry 1 — Phase 1: Foundation and UI shell

**Status:** Written, not built.

**Goal:** Hilt app entry point, Material 3 theme from `DESIGN_SYSTEM.md`, Navigation Compose shell, shared components, and six placeholder screens. No Room, repositories, networking, auth, AI or real knowledge/flashcard logic.

### Files created

| File | Purpose |
|---|---|
| `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties` | Project build setup |
| `gradle/libs.versions.toml` | Version catalog (AGP 8.7.3, Kotlin 2.0.21, KSP 2.0.21-1.0.28, Hilt 2.52, Compose BOM 2024.10.01, Navigation 2.8.4) |
| `gradle/wrapper/gradle-wrapper.properties` | Pins Gradle 8.10.2 |
| `app/build.gradle.kts` | App module: namespace/applicationId `com.mindvault`, minSdk 26, compile/target 35, Compose, Hilt via KSP |
| `app/src/main/AndroidManifest.xml` | Registers `MindVaultApp` and `MainActivity` |
| `app/src/main/res/values/strings.xml`, `values/themes.xml` | App name; launch window theme |
| `…/mindvault/app/MindVaultApp.kt` | `@HiltAndroidApp` Application |
| `…/mindvault/app/MainActivity.kt` | `@AndroidEntryPoint` activity, sets `MindVaultTheme { MindVaultNavHost() }` |
| `…/mindvault/core/theme/Color.kt` | Design-system color tokens |
| `…/mindvault/core/theme/Type.kt` | Type scale |
| `…/mindvault/core/theme/Shape.kt` | Shape tokens (`ShapeSmall` … `ShapePill`) |
| `…/mindvault/core/theme/Spacing.kt` | `MindVaultSpacing` (space1–space8, screenHorizontal, cardPadding, touchTargetMin, …) |
| `…/mindvault/core/theme/Theme.kt` | `MindVaultTheme` |
| `…/mindvault/core/navigation/MindVaultDestination.kt` | All route strings in one place; `bottomBarRoutes` |
| `…/mindvault/core/navigation/MindVaultNavHost.kt` | Scaffold + bottom bar + NavHost + Add sheet state |
| `…/mindvault/core/ui/components/PrimaryButton.kt` | Forest button with pressed state |
| `…/mindvault/core/ui/components/MindVaultCard.kt` | Standard card; `containerColor = SageLight` for the highlight card |
| `…/mindvault/core/ui/components/SectionHeader.kt` | Section title |
| `…/mindvault/core/ui/components/EmptyState.kt` | Icon + title + message + optional action |
| `…/mindvault/core/ui/components/MindVaultTopBar.kt` | Screen title bar, optional back button and actions |
| `…/mindvault/core/ui/components/MindVaultSearchField.kt` | Search input (supports `enabled = false`) |
| `…/mindvault/core/ui/components/MindVaultBottomBar.kt` | Shared bottom nav: Search, Vault, **Add**, Flashcards, Home |
| `…/mindvault/core/ui/components/AddMenuSheet.kt` | Modal sheet: "Add a concept", "Search and save" |
| `…/mindvault/feature/home/HomeScreen.kt` | Search shortcut, welcome card, quick actions, empty "Recently saved" |
| `…/mindvault/feature/search/SearchScreen.kt` | Disabled field + "Search isn't available yet" |
| `…/mindvault/feature/vault/VaultScreen.kt` | "My Vault" empty state with Add action |
| `…/mindvault/feature/flashcards/FlashcardsScreen.kt` | "No cards yet" empty state |
| `…/mindvault/feature/profile/ProfileScreen.kt` | Back button, "No settings yet" |
| `…/mindvault/feature/knowledge/AddKnowledgeScreen.kt` | **Placeholder**; real form comes later |

### Key decisions
- **Bottom nav order** follows `DESIGN_SYSTEM.md` (Search, Vault, Add, Flashcards, Home), not the destination list in the brief. Profile is opened from the Home top bar. Profile and Add concept hide the bottom bar and use a top bar with back.
- **Tab switching** uses `popUpTo(start) { saveState }` + `launchSingleTop` + `restoreState`, so each tab keeps its state.
- **No fake data:** no stats, streaks, filters, sort, deck tabs, or settings toggles are shown until they work. Search is disabled and says so.
- **No ViewModels yet** (screens are stateless). They arrive with the first real data, with `hilt-navigation-compose`.
- `data/` and `domain/` packages are not created yet; create them with the first real class.
- `material-icons-extended` added for filled/outlined icon pairs required by the design system.
- Package/applicationId `com.mindvault` is an assumption (not yet confirmed).

### Not built (by design)
Room, repositories, Retrofit/OkHttp, authentication, AI, add/edit form, vault list, flashcard review, settings, tests.

---

## Entry 2 — Dark mode

**Status:** Written, not built.

**Goal:** Add a dark theme derived from the Sage & Stone palette, following the system light/dark setting. (The design system forbids a second palette "unless explicitly requested" — it was requested.)

### Files changed

| File | Change |
|---|---|
| `…/mindvault/core/theme/Color.kt` | **Rewritten.** Added `MindVaultPalette`, `LightPalette` (same values as Entry 1), `DarkPalette`, `LocalMindVaultPalette`. `MindVaultColors.X` is now a set of composable getters that read the active palette. |
| `…/mindvault/core/theme/Theme.kt` | **Rewritten.** `MindVaultTheme(darkTheme = isSystemInDarkTheme())` provides the palette and builds the Material color scheme from it. |
| `…/mindvault/app/MainActivity.kt` | `enableEdgeToEdge()` now uses defaults, so status/navigation bar icons follow light/dark (previously forced light style). |

### Files created

| File | Purpose |
|---|---|
| `app/src/main/res/values-night/themes.xml` | Dark launch window background `#141A16`, parent `android:Theme.Material.NoActionBar` (avoids a light flash at launch) |

### Unchanged on purpose
All screens and components. They already use `MindVaultColors.Forest` etc., which now resolve to light or dark values. **Rule going forward:** never hardcode a hex color in a screen; read `MindVaultColors.*` inside composable code only (they cannot be used in top-level `val`s or non-composable functions).

### Dark palette (hex)

| Token | Light | Dark |
|---|---|---|
| Background | `#F0F1E9` | `#141A16` |
| Surface | `#FAFAF6` | `#1D241F` |
| SurfaceVariant | `#E6E9DF` | `#2A332C` |
| SageLight | `#D5E0D2` | `#2F4237` |
| Sage | `#779782` | `#8FB09A` |
| SageDeep | `#4F765F` | `#6F9B80` |
| Forest | `#286348` | `#7DC4A0` |
| ForestPressed | `#1E5039` | `#62AD87` |
| TextPrimary | `#202820` | `#E8ECE4` |
| TextSecondary | `#5F685F` | `#A7B1A6` |
| TextOnPrimary | `#FFFFFF` | `#0E2418` |
| Border | `#D6DCCF` | `#3A443B` |
| Disabled | `#AEB5AA` | `#6B746A` |
| Scrim | `#202820` @ 32% | `#000000` @ 50% |

Accents (foreground / background), dark: Gold `#E6C27A`/`#3A2E16`, Terracotta `#E8A596`/`#3F2621`, Blue `#9DB7F0`/`#1E2A47`, Lilac `#BFAEF0`/`#2E2650`, Mint `#8FD1B0`/`#1D3A2C`.

### Key decisions and limits
- In dark mode **Forest becomes a lighter green** and **TextOnPrimary becomes dark**, so primary buttons and active tab labels stay readable. Forest is still the primary/active color.
- Dark values were chosen by judgment and **contrast has not been measured**. Check them before release.
- Theme follows the **system** setting only. A manual light/dark/system switch in Profile needs persisted settings (DataStore or Room), so it is deferred; `MindVaultTheme` already accepts `darkTheme` for that.

---

## Entry 3 — Phase 2: Data model and Room database

**Status:** Written, not built. Nothing here is Verified until a note says so.

**Goal:** Local data layer: entities, DAOs, database, domain models, repository interfaces and implementations, mappers, Hilt wiring. No UI, ViewModels, use cases, external APIs, auth or AI.

### Files modified
| File | Change |
|---|---|
| `gradle/libs.versions.toml` | Added Room 2.6.1, JUnit 4.13.2, androidx test ext-junit 1.2.1 and runner 1.6.2 |
| `app/build.gradle.kts` | Room runtime/ktx/compiler (KSP), `ksp { room.schemaDirectory }`, test dependencies, `testInstrumentationRunner` |
| `.gitignore` | Ignores Gradle/IDE/build output, `local.properties`, keystores and secrets. Deliberately does **not** ignore `app/schemas/`. |
| `README.md` | Setup, current status, structure, doc index |
### Files created (under `…/mindvault/`)
| Area | Files |
|---|---|
| `core/util/` | `IdAndTime.kt` (`newId()` UUID string, `nowMillis()`) |
| `domain/model/` | `KnowledgeType`, `KnowledgeItem`, `KnowledgeItemDraft`, `Category`, `Deck`, `Flashcard`, `ReviewState`, `FlashcardDetail`, `AddToDeckResult` |
| `domain/error/` | `DomainErrors.kt` (`DuplicateNameException`, `DefaultDeckProtectedException`) |
| `domain/repository/` | `KnowledgeRepository`, `CategoryRepository`, `DeckRepository`, `FlashcardRepository` |
| `data/local/entity/` | `KnowledgeItemEntity`, `CategoryEntity`, `DeckEntity`, `FlashcardEntity`, `ReviewStateEntity` |
| `data/local/relation/` | `FlashcardWithDetails` |
| `data/local/dao/` | `KnowledgeItemDao`, `CategoryDao`, `DeckDao`, `FlashcardDao`, `ReviewStateDao` |
| `data/local/database/` | `MindVaultDatabase` (v1, exportSchema), `DefaultDeckCallback`, `Migrations.kt` |
| `data/mapper/` | `KnowledgeMappers`, `DeckMappers`, `FlashcardMappers` |
| `data/repository/` | `KnowledgeRepositoryImpl`, `CategoryRepositoryImpl`, `DeckRepositoryImpl`, `FlashcardRepositoryImpl` |
| `core/di/` | `DatabaseModule`, `RepositoryModule` |
| `app/src/test/…/data/mapper/` | `MappersTest.kt` |
| `app/src/androidTest/…/data/local/` | `MindVaultDatabaseTest.kt` |

### Key decisions
- **IDs are UUID strings** (not Long) so records stay unique across devices when sync is added. Timestamps are UTC epoch millis.
- Entities store `type` as text; mappers convert to the enum and fall back to `OTHER` for unknown values.
- Unique, case-insensitive names for categories and decks. Unique `(knowledgeItemId, deckId)` on flashcards.
- FKs: item→category `SET_NULL`; flashcard→item and flashcard→deck `CASCADE`; review state→flashcard `CASCADE`.
- Default deck (`Deck.DEFAULT_DECK_ID`) is seeded in `DefaultDeckCallback.onCreate`; deleting it is blocked in both the DAO and the repository.
- `addToDeck` creates the flashcard and its initial review state in one transaction.
- Schema exported to `app/schemas/` (commit it). `MINDVAULT_MIGRATIONS` is empty; no destructive fallback.
- Vault filter/sort queries are deliberately not here; they belong with the Vault feature and need no schema change (indexes already exist).
- Validation (required fields) is deferred to the use cases in the next phase; repositories only trim text.

### Not built (by design)
Use cases, ViewModels, Add/Edit form, Vault queries, review scheduling logic, external Search, auth, AI, sync metadata.

---

## Entry 5 — Phase 3: Add form, saving, and basic Vault list

**Status:** Written, not built.

**Goal:** First real feature. Add a concept through a validated form, save it to Room, and see it in the Vault. No detail, edit, delete, filter, sort, Search or flashcards.

### Files modified
| File | Change                                                                                                        |
|---|---------------------------------------------------------------------------------------------------------------|
| `gradle/libs.versions.toml` | Added `lifecycle` 2.8.7 and `hiltNavigationCompose` 1.2.0 with three libraries                                |
| `app/build.gradle.kts` | Added hilt-navigation-compose, lifecycle-runtime-compose, lifecycle-viewmodel-compose, change ksp { room.schemaDirectory } to ksp { room.schemaLocation }.                       |
| `…/feature/knowledge/AddKnowledgeScreen.kt` | **Replaced.** Real form replaces the placeholder; same signature plus `viewModel = hiltViewModel()`           |
| `…/feature/vault/VaultScreen.kt` | **Replaced.** Loading/empty/error/list states and an Add icon in the top bar; same signature plus `viewModel` |

### Files created (under `…/mindvault/`)
| File | Purpose |
|---|---|
| `domain/model/KnowledgeValidation.kt` | `KnowledgeField`, `ValidationError`, `SaveKnowledgeResult`, `CreateCategoryResult` |
| `domain/model/VaultEntry.kt` | Item + category name for list rows |
| `domain/usecase/SaveKnowledgeItemUseCase.kt` | Validation (title/definition required, length limits) then save |
| `domain/usecase/CreateCategoryUseCase.kt` | Name validation, then get-or-create |
| `domain/usecase/ObserveCategoriesUseCase.kt` | Category Flow |
| `domain/usecase/ObserveVaultItemsUseCase.kt` | Items joined with category names |
| `core/ui/components/MindVaultTextField.kt` | Shared text input per design system |
| `feature/knowledge/` | `KnowledgeTypeLabel.kt`, `AddKnowledgeUiState.kt`, `AddKnowledgeViewModel.kt`, `CategoryPicker.kt` |
| `feature/vault/` | `VaultUiState.kt`, `VaultViewModel.kt` |
| `app/src/test/…/domain/usecase/` | `FakeRepositories.kt`, `UseCasesTest.kt` |

### Key decisions
- **No navigation changes.** Screens obtain ViewModels with `hiltViewModel()`, scoped to their back-stack entry.
- **Required fields:** title and definition. Limits: title 200, definition 5000, notes 5000, example 1000, category name 50. Defined in the use cases, not the UI.
- **Duplicate titles are allowed** on manual add (a word can have several senses). Duplicate warnings arrive with Search, using `KnowledgeRepository.findByTitle`.
- **Double-save blocked:** the form stays in the saving state until the screen closes.
- **Unsaved changes:** back asks to discard if anything was entered.
- **Categories:** picker with "New category…". A category created in the dialog persists even if the concept is discarded.
- **Strings are hardcoded**, as in Phase 1. Move them to `strings.xml` before release.
- Form state lives in the ViewModel: it survives rotation but not process death.
  Schema export was misconfigured (wrong KSP option name) until Entry 5 follow-up fix; corrected to room.schemaLocation.

### Not built (by design)
Item detail/edit/delete, Vault filter/sort/search, Search, flashcard UI, settings, ViewModel unit tests.

---

## Entry 6 — Item detail view and Phase 3 build fixes

**Status:** Written, not built (the fixes below were reported working on emulator).

### Fixes to Entries 3 and 5
- `feature/knowledge/CategoryPicker.kt`: removed the invalid import of `matchParentSize` (it is a `BoxScope` member and needs no import).
- `app/build.gradle.kts`: KSP option corrected from `room.schemaDirectory` to `room.schemaLocation`. Room's schema export had been silently disabled; `app/schemas/.../1.json` should now be generated and committed.

### Files modified
| File | Change |
|---|---|
| `core/navigation/MindVaultDestination.kt` | Added `KNOWLEDGE_ID_ARG` and `KnowledgeDetail` route (`knowledge/detail/{itemId}`) |
| `core/navigation/MindVaultNavHost.kt` | Detail `composable` with a String argument; Vault passes `onItemClick` |
| `feature/vault/VaultScreen.kt` | Added `onItemClick`; cards are tappable |

### Files created
`domain/usecase/ObserveKnowledgeDetailUseCase.kt`, `feature/knowledge/KnowledgeDetailUiState.kt`, `KnowledgeDetailViewModel.kt`, `KnowledgeDetailScreen.kt`.

### Key decisions
- Read-only for now; the item ID arrives through `SavedStateHandle`.
- The detail screen always shows a category tag ("Uncategorized" when none). Empty notes and example sections are hidden. Text is selectable.
- Detail uses the same `VaultEntry` model as the list, so there is one source of truth.

### Not built
Edit, delete, flashcard creation from the detail screen.

---

## Entry 7 — Phase 4a: Edit and delete

**Status:** Written, not built.

**Goal:** Edit a saved item (reusing the Add form) and delete it with confirmation.

### Files modified
| File | Change |
|---|---|
| `domain/model/KnowledgeValidation.kt` | Added `UpdateKnowledgeResult` |
| `domain/usecase/SaveKnowledgeItemUseCase.kt` | Replaced; validation moved to `KnowledgeValidator.kt` |
| `core/navigation/MindVaultDestination.kt` | Added `KnowledgeEdit` route (`knowledge/edit/{itemId}`) |
| `core/navigation/MindVaultNavHost.kt` | Detail gets `onEditClick`; new edit `composable` reusing `AddKnowledgeScreen` |
| `feature/knowledge/AddKnowledgeUiState.kt`, `AddKnowledgeViewModel.kt`, `AddKnowledgeScreen.kt` | Replaced. Edit mode: loads the item, prefills, calls the update use case. `isDirty` now compares against the starting values. |
| `feature/knowledge/KnowledgeDetailUiState.kt`, `KnowledgeDetailViewModel.kt`, `KnowledgeDetailScreen.kt` | Replaced. Edit and Delete icons; delete confirmation dialog with loading and error states. |
| `…/test/…/FakeRepositories.kt`, `UseCasesTest.kt` | Fake `update` now persists; 4 new tests |

### Files created
`domain/usecase/KnowledgeValidator.kt`, `UpdateKnowledgeItemUseCase.kt`, `DeleteKnowledgeItemUseCase.kt`.

### Key decisions
- One form for Add and Edit; the ViewModel picks the mode from the presence of `itemId`.
- Create and update share one validator, so the rules can't diverge.
- Edit keeps `createdAt`; `updatedAt` changes, so the item moves to the top of the Vault.
- Delete is permanent and cascades to flashcards and review state (database foreign keys, tested in Phase 2). The dialog says so.
- After delete the detail screen shows a loading state rather than "not found" while it closes.
- Unsaved edits prompt before discarding.

### Not built
Undo for delete, bulk delete, Vault filter/sort/search.

---

## Entry 8 — Phase 4b: Vault search, filter and sort

**Status:** Written, not built.

**Goal:** Search saved items, filter by type and category, sort by recent activity or alphabetically.

### Files modified
| File | Change |
|---|---|
| `core/ui/components/MindVaultTextField.kt` | Added optional `leadingIcon` |
| `feature/vault/VaultUiState.kt` | Replaced: `VaultStatus` + one state holding query, categories, total and results |
| `feature/vault/VaultViewModel.kt` | Replaced: combines items, categories, search text and filters |
| `feature/vault/VaultScreen.kt` | Replaced: search field, type chips, category and sort dropdowns, count, clear filters, no-match state |

### Files created
`domain/model/VaultQuery.kt`, `domain/usecase/ApplyVaultQueryUseCase.kt`, `core/ui/components/MindVaultFilterChip.kt`, `app/src/test/…/domain/usecase/ApplyVaultQueryUseCaseTest.kt`.

### Key decisions
- Filtering and sorting run in memory on the observed list through one pure use case; no schema or DAO change. Move to SQL/FTS only if vaults get very large.
- Search is case-insensitive; every word must match in title, definition, notes, example or category name.
- Sort options: recently updated (default), recently added, A–Z, Z–A. Alphabetical ignores case.
- Search text is Compose state in the ViewModel (not a StateFlow) so typing doesn't lag or jump the cursor.
- Filters live in the ViewModel, so they survive rotation and tab switches. They reset when the app process restarts. "Clear filters" keeps the sort.
- "Vault empty" (no saved items) and "no matches" (filters hide everything) are separate states.
- Built a search box from `MindVaultTextField` because `MindVaultSearchField.kt` had not been reviewed; swap if desired.

### Not built
Persisting sort/filter across restarts (needs settings storage), highlighting matched text, multi-select filters.

## Entry 9 — Phase 5: External Search and save

**Status:** Built and run on the emulator (developer-reported). **Interim version:** to be replaced by Search v2 (`docs/SEARCH_V2_REQUIREMENTS.md`).

**Goal:** Search words through the Free Dictionary API, show loading / results / no result / offline / error, save a result to the Vault directly or after editing, and flag results that are already saved. The provider sits behind one interface so it can be swapped (see `docs/SEARCH_PROVIDER.md`).

### Files modified
| File | Change |
|---|---|
| `AndroidManifest.xml` | Added `INTERNET` permission |
| `gradle/libs.versions.toml` | Added Retrofit 2.11.0, OkHttp 4.12.0, kotlinx-serialization-json 1.7.3, kotlinx-coroutines-test 1.9.0, serialization plugin |
| `build.gradle.kts` (root) | Registered the serialization plugin (`apply false`) |
| `app/build.gradle.kts` | Applied the plugin, added Retrofit/OkHttp/serialization dependencies, `testImplementation(libs.kotlinx.coroutines.test)` (needed to run ViewModel tests off the main dispatcher), `testOptions { unitTests.isReturnDefaultValues = true }` so unit tests can run code that calls `Log` |
| `core/di/RepositoryModule.kt` | Added `SearchRepository` binding |
| `core/navigation/MindVaultDestination.kt` | Added `AddKnowledgeFromSearch` route and `PREFILL_*_ARG` constants |
| `core/navigation/MindVaultNavHost.kt` | Search gets view/edit-first callbacks; new prefilled-form route |
| `feature/knowledge/AddKnowledgeViewModel.kt` | Replaced. Reads prefill arguments into the initial state; unchanged otherwise |
| `feature/search/SearchScreen.kt` | Replaced. Real states instead of the disabled shell |

### Files created (under `…/mindvault/`)
| Area | Files |
|---|---|
| `domain/model/` | `SearchResult.kt` (models, limits, `limited()`, `toSaveContent()`, `toTitleKey()`), `SearchOutcome.kt` (`SearchOutcome`, `SaveSearchResult`) |
| `domain/repository/` | `SearchRepository.kt` |
| `domain/usecase/` | `SearchDefinitionsUseCase.kt`, `SaveSearchResultUseCase.kt`, `ObserveSavedTitlesUseCase.kt` |
| `data/remote/` | `DefinitionDataSource.kt` (the swap point) |
| `data/remote/freedictionary/` | `FreeDictionaryApi.kt`, `FreeDictionaryDto.kt`, `FreeDictionaryMapper.kt`, `FreeDictionaryDataSource.kt` |
| `data/repository/` | `SearchRepositoryImpl.kt` |
| `core/di/` | `SearchProviderModule.kt` (the only file that names the active provider) |
| `feature/search/` | `SearchUiState.kt`, `SearchViewModel.kt`, `SearchResultCard.kt` |
| `app/src/test/…` | `domain/model/SearchResultTest.kt`, `data/remote/freedictionary/FreeDictionaryMapperTest.kt`, `data/repository/SearchRepositoryImplTest.kt`, `domain/usecase/SearchTestSupport.kt` (fakes and builders), `SearchDefinitionsUseCaseTest.kt`, `ObserveSavedTitlesUseCaseTest.kt`, `SaveSearchResultUseCaseTest.kt`, `feature/search/SearchViewModelTest.kt` |
| `docs/` | `SEARCH_PROVIDER.md` |

### Key decisions
- **Provider:** Free Dictionary API, no key. Chosen for the MVP; coverage of concepts is poor and it has no service guarantee. Revisit before release.
- **One result per word.** Entries for the same word are merged. Each card shows at most 4 meanings and 2 definitions per meaning, and the saved definition is exactly what the card shows.
- **Saved as:** type `WORD`, no category, definition lines like `(noun) text`, first example found.
- **Duplicates:** matched by title, case-insensitive. The card is derived live from the Vault, so it flips to "Already in your Vault" with a View action as soon as the save lands. `SaveSearchResultUseCase` rechecks with `findByTitle` before saving. Limitation: one saved item per title; add other senses by editing the item.
- **Save paths:** "Save" saves directly. "Edit first" opens the existing Add form prefilled through route query arguments, so it works after process death. The form counts as unsaved, so back asks to discard.
- **Search runs on the keyboard's Search action**, not on every keystroke. Clearing the field resets to the idle state.
- **Error mapping:** `IOException` → offline; empty list → no result; other exceptions → error. 404 from this provider is mapped to "no result" inside its data source.
- **JSON:** kotlinx.serialization (no reflection, safer under R8, which is enabled for release).
- `SearchResultCard` uses `Surface` with design tokens because `MindVaultCard.kt` was not reviewed.
- Strings are hardcoded, as elsewhere. Move them to `strings.xml` before release.

### Observations (docs vs. supplied files)
- `libs.versions.toml` has AGP 8.9.1 and `app/build.gradle.kts` has compile/target SDK 36, plus release signing and minification. The docs still say AGP 8.7.3 and SDK 35. The supplied files were treated as correct.

### Not built (by design)
"Search recommendations" setting, recent searches, Search scope filters, languages other than English, creating a flashcard from a result, UI (Compose) tests. The new use-case and ViewModel tests assume `SaveKnowledgeItemUseCase(knowledgeRepository)` as its constructor; if that is wrong, fix `newSaveSearchResultUseCase` in `SearchTestSupport.kt`.

### Follow-up
This interim Search will later be replaced by Search v2. See `docs/SEARCH_V2_REQUIREMENTS.md` for the requirements and the questions to settle first.

## Entry 10 — Phase 6: Flashcards & Decks

**Status:** Written, not built. Nothing here is verified until a note says so.

**Goal:** Create flashcards from Vault entries; create, view, rename and delete decks; add and remove cards; a study mode with flip, previous/next and shuffle; basic progress and deck completion. All persisted through the existing Phase 2 database and repositories.

### Database, dependencies, setup
- **No schema change and no migration.** Schema v1 already has decks, flashcards and review states; `MINDVAULT_MIGRATIONS` stays empty. No new DAO queries either: counts reuse `observeCountByDeck` and `observeUnreviewedCountByDeck`.
- **No new dependencies.**
- Setup: copy the files to the paths below, then run File > Sync Project with Gradle Files and `gradlew.bat assembleDebug`.

### Files created (under `app/src/main/java/com/mindvault/`)
| Area | Files |
|---|---|
| `domain/model/` | `DeckSummary.kt` (`DeckSummary`, `DeckDetail`, `DeckOption`), `DeckResults.kt` (`SaveDeckResult`, `DeleteDeckResult`, `AddItemsToDeckResult`), `StudySession.kt` (pure study logic) |
| `domain/usecase/` | `DeckNameValidator.kt`, `ObserveDeckSummariesUseCase.kt`, `ObserveDeckDetailUseCase.kt`, `ObserveDeckOptionsUseCase.kt`, `CreateDeckUseCase.kt`, `RenameDeckUseCase.kt`, `DeleteDeckUseCase.kt`, `AddItemsToDeckUseCase.kt`, `RemoveCardFromDeckUseCase.kt`, `RecordCardReviewUseCase.kt` |
| `core/ui/components/` | `SecondaryButton.kt` |
| `feature/flashcards/` | `DeckFormUiState.kt`, `DeckNameDialog.kt`, `DeckProgress.kt`, `FlashcardsUiState.kt`, `FlashcardsViewModel.kt`, `DeckDetailUiState.kt`, `DeckDetailViewModel.kt`, `DeckDetailScreen.kt`, `AddCardsUiState.kt`, `AddCardsViewModel.kt`, `AddCardsScreen.kt`, `StudyUiState.kt`, `StudyViewModel.kt`, `StudyScreen.kt` |
| `feature/knowledge/` | `AddToDeckUiState.kt`, `AddToDeckViewModel.kt`, `AddToDeckSheet.kt` |

### Files modified
| File | Change |
|---|---|
| `core/navigation/MindVaultDestination.kt` | `DECK_ID_ARG`, `STUDY_SHUFFLE_ARG`; routes `DeckDetail`, `DeckAddCards`, `Study` (with a boolean `shuffle` argument) |
| `core/navigation/MindVaultNavHost.kt` | Flashcards tab opens decks; three new routes. Bottom bar stays hidden on deck, picker and study screens (they are not in `bottomBarRoutes`) |
| `feature/flashcards/FlashcardsScreen.kt` | **Replaced.** Deck list with progress and a "New deck" action instead of the empty shell. New signature: `FlashcardsScreen(onDeckClick, modifier, viewModel)` |
| `feature/knowledge/KnowledgeDetailScreen.kt` | One new top-bar action ("Add to flashcards"), one state flag, and the sheet. The ViewModel and UI state are unchanged: the sheet's own ViewModel reads the item ID from the same back-stack entry |

### Key decisions
- **Reviewed means revealed.** The first reveal of a card in a pass saves `reviewCount + 1` and `lastReviewedAt`. Flipping back and forth in a pass doesn't count again. `nextReviewAt`, `easeFactor` and `intervalDays` are left untouched, so "due" counts are deliberately not shown.
- **Deck completion** = the deck has cards and every card has `reviewCount > 0`. The session summary computes this from the session snapshot, so it never races the database writes.
- **Study session** is pure logic (`StudySession`) on one snapshot taken when the screen opens, so cards never reshuffle as they get reviewed. Default order: unreviewed first, then oldest first.
- **Shuffle:** a toggle chip on the study screen, plus a "Shuffle" button on the deck screen that opens study with shuffle on. Toggling mid-session keeps the current card and the ones already passed; only the cards ahead are reordered, and turning it off restores the default order for them. The choice is not persisted (no settings storage yet).
- **Adding cards:** from the item detail screen via a sheet (default deck preselected; decks that already hold the item show "Already added"; "New deck" inline), or from a picker inside a deck that reuses `ObserveVaultItemsUseCase` and `ApplyVaultQueryUseCase`. The sheet closes only after the database write succeeds.
- **Deletes:** deleting a deck or removing a card never touches the Vault item; both ask for confirmation and say what is lost. The default deck can't be deleted (hidden in the UI, blocked by the DAO and repository).
- **Names:** deck names are required, up to 50 characters, unique ignoring case. Duplicates surface from the existing repository exception as "A deck with this name already exists".
- Strings are hardcoded, as elsewhere. `SecondaryButton` is new because no secondary button existed.
- `AddMenuSheet` is untouched, so "Create a flashcard" is still not in the Add menu.

### Assumptions to check at build time
- `AddCardsViewModel` builds `VaultQuery(searchText = ..., sort = VaultSort.A_TO_Z)` and assumes the other `VaultQuery` fields have defaults (the file was not supplied).
- `KnowledgeItem.id` is used for picker membership; `FlashcardDetail.knowledgeItem` is the Vault item.

### Not built (by design)
Tests (to follow once the test files are supplied), spaced repetition and self-rating, due counts, persisted shuffle, Home dashboard card stats, "Create a flashcard" in the Add menu, bulk Vault selection, reset deck progress, moving cards between decks.

### Manual checks to run on the emulator
1. Flashcards tab: the default deck is listed with "No cards yet". Create a deck, try a blank name and a duplicate name.
2. Open a saved item, tap the cards icon: the default deck is preselected. Add it. Reopen the sheet: the deck shows "Already added".
3. In a deck: "Add cards" lists the Vault with search; items already in the deck are disabled.
4. Study: flip by tapping and by the button, go Next and Previous, turn Shuffle on and off mid-session, finish, then "Study again".
5. Back on the deck screen: reviewed counts and the progress bar updated; reviewing every card shows "Deck complete".
6. Remove a card (confirm), rename a deck, delete a custom deck (confirm). The default deck has no delete action.
7. Delete a Vault item that has a card: the card disappears from its deck.

## Entry 11 — Phase 7a: Home dashboard

**Status:** Written, not built.

**Goal:** Replace Home's static content with real data from the existing saved items and decks. No schema change, no new dependency, no Profile/Settings work (deferred).

### Files created (under `app/src/main/java/com/mindvault/`)
| File | Purpose |
|---|---|
| `domain/model/HomeSummary.kt` | `HomeSummary` and `CategoryCount` |
| `domain/usecase/HomeSummaryBuilder.kt` | Pure logic: category counts, 5 most recent, in-progress decks, "completed today" check |
| `domain/usecase/ObserveHomeSummaryUseCase.kt` | Combines `ObserveVaultItemsUseCase`, `ObserveDeckSummariesUseCase` and `FlashcardRepository.observeByDeck` |
| `feature/home/HomeUiState.kt`, `HomeViewModel.kt` | Loading / Error / Content state |
| `app/src/test/.../domain/usecase/HomeSummaryBuilderTest.kt` | 5 unit tests (deck-based cases pending `Deck.kt`) |

### Files modified
| File | Change |
|---|---|
| `feature/home/HomeScreen.kt` | Replaced. Existing search shortcut, welcome card and quick actions unchanged. Adds "Continue studying", "Categories", real "Recently saved" with "See all". New params `onItemClick`, `onDeckClick`, `viewModel`. |
| `core/navigation/MindVaultNavHost.kt` | Home route passes `onItemClick` (detail) and `onDeckClick` (deck detail) |

### Key decisions
- **Flashcard status:** shows up to 2 decks that are started but not complete (progress + tap opens the deck). If cards exist but none are started, one "Start studying" row opens Flashcards. No "due" count is shown (scheduling is a later feature).
- **Decks completed today (no new storage):** nothing records when a deck was completed, so a deck counts as completed today when it has cards and **every card's `lastReviewedAt` is today**. Known limits: a finished deck fully re-studied on a later day counts again; the count refreshes on the next data change, not at midnight. An exact count would need a `completedAt` column (Room migration v1→v2, non-destructive) — deferred.
- **Category counts:** read-only pills, sorted by count; items without a category are grouped as "Uncategorized" (same label as the detail screen).
- **Recently saved:** 5 newest by `createdAt`; "See all" opens the Vault.
- Sections with no real data are hidden; a new user sees the existing "Nothing saved yet" state. No streaks.
- Home reuses the same records as Vault and Flashcards (no copies). `DeckProgress` reused from the flashcards feature.

### Not built
Profile & Settings (theme preference), due counts, tappable category filters, midnight refresh of "completed today".

## Entry 12 — Phase 7b: Profile & Settings (theme preference)

**Status:** Written, not built.

**Goal:** One working setting: theme (System / Light / Dark) that persists and drives `MindVaultTheme`. Nothing for unimplemented features (no sync, notifications or search-recommendation toggles).

### New dependency (only one)
`androidx.datastore:datastore-preferences:1.1.1` for a single small preference. Room stays the store for knowledge and review state. Added in `gradle/libs.versions.toml` and `app/build.gradle.kts`. No Room schema change, no migration.

### Files created (under `app/src/main/java/com/mindvault/`)
| File | Purpose |
|---|---|
| `domain/model/ThemePreference.kt` | Enum SYSTEM / LIGHT / DARK, plus `isDark(systemInDarkTheme)` |
| `domain/repository/SettingsRepository.kt` | Observe and set the theme preference |
| `domain/usecase/ObserveThemePreferenceUseCase.kt`, `SetThemePreferenceUseCase.kt` | Use cases |
| `data/repository/SettingsRepositoryImpl.kt` | DataStore-backed; unreadable file or unknown value falls back to SYSTEM |
| `core/di/SettingsModule.kt` | Hilt binding |
| `app/AppThemeViewModel.kt` | Exposes the saved preference to `MainActivity` |
| `feature/profile/ProfileUiState.kt`, `ProfileViewModel.kt` | Profile state and theme selection; shows an inline error if saving fails |
| `app/src/test/.../domain/usecase/FakeSettingsRepository.kt`, `ThemePreferenceTest.kt` | 4 unit tests |

### Files modified
| File | Change |
|---|---|
| `app/MainActivity.kt` | Replaced. Reads the preference, passes `darkTheme` to `MindVaultTheme`, and re-applies edge-to-edge bar styles so status/navigation icons match the chosen theme |
| `feature/profile/ProfileScreen.kt` | Replaced. "Appearance" card with three radio rows; same top bar and back behavior |
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | DataStore entries |

### Key decisions
- No navigation change: Profile is still opened from Home's top bar.
- The app can show the system theme for a moment at cold start before the saved choice loads (initial value is SYSTEM). Fix later with a splash hold if it is noticeable.
- The launch window background (`values-night/themes.xml`) still follows the system setting.
- Failed saves show an inline message; the selection is never shown as saved until DataStore confirms (the radio follows the stored value).
- ViewModel tests are not included: the version catalog has no `kotlinx-coroutines-test` entry, so tests use `runBlocking` on the use cases.
- Remembering the study-mode shuffle choice was skipped (optional in the brief); can be added as one more DataStore key.

### Not built
Shuffle persistence, in-app language or notification settings, search-recommendation toggle.

### Entry 13 — Phase A1a: Supabase Auth foundation (no UI yet)

Status: Written, not built.

Goal: Email sign-up / sign-in / sign-out, encrypted persistent session with automatic refresh, guest mode as "signed out". Backend, DI, config and tests only. UI and navigation wiring follow in A1b. No cloud sync, no per-user data, no Google sign-in (A2).

New dependencies (verified against the supplied catalog by reading supabase-kt's own version files, not by building)
Library	Version	Why
io.github.jan-tennert.supabase:bom + auth-kt	3.0.2	Supabase Auth. 3.0.2 is built with Kotlin 2.0.21 and Ktor 3.0.1, matching this project. 3.0.3+ is built with Kotlin 2.1+; 3.7.0 with Kotlin 2.4.
io.ktor:ktor-client-okhttp	3.0.1	HTTP engine; reuses OkHttp 4.12.0.
Gradle sync is the real proof of resolution. If a version fails to resolve, report the error.
Files modified
File	Change
gradle/libs.versions.toml	supabase, ktor versions and three libraries
app/build.gradle.kts	Reads supabase.url / supabase.anonKey from local.properties into BuildConfig; buildConfig = true; three dependencies
app/src/main/AndroidManifest.xml	launchMode="singleTop"; deep-link filter mindvault://auth-callback; backup rules
app/MainActivity.kt	Injects AuthDeepLinkHandler; handles links in onCreate and onNewIntent
app/proguard-rules.pro	-dontwarn for Ktor/slf4j/management
Files created (under …/mindvault/)

domain/model/AuthModels.kt, domain/repository/AuthRepository.kt, domain/usecase/{AuthValidator,ObserveSessionStateUseCase,SignInWithEmailUseCase,SignUpWithEmailUseCase,SignOutUseCase}.kt, data/auth/{AuthConfig,EncryptedSessionManager,AuthRepositoryImpl,AuthDeepLinkHandler}.kt, core/di/AuthModule.kt; res/xml/{backup_rules,data_extraction_rules}.xml; tests AuthValidatorTest.kt, AuthUseCasesTest.kt; docs/AUTH_SETUP.md.

Key decisions
Guest mode = SessionState.SignedOut. No gate; sign-in is optional and opens from Profile (A1b).
No keys → guest-only build. Blank URL/key: accounts hidden, nothing crashes.
Session stored encrypted (AES-256-GCM, Android Keystore) instead of the library's plain SharedPreferences; undecryptable data is discarded (user signs in again); file excluded from backup.
PKCE for email links, so a second app registering the same custom scheme cannot steal a session. Limit: a link opened on a different device than the one that signed up cannot complete; user then just signs in.
No account enumeration: sign-up for an existing address returns the same "check your email" outcome; sign-in failure is a generic "incorrect email or password".
Password rules: sign-up 8–72 bytes; sign-in only requires non-empty. Email trimmed and lowercased; password never altered.
Expired session (SessionState.Expired) is separate from signed out; offline refresh failure keeps the user signed in with needsRefresh = true.
Sign out always ends the local session, even when the server is unreachable. Phase A sign-out does not touch local data (Phase B adds that).
Auth types stay in data/auth and core/di; MainActivity only sees AuthDeepLinkHandler.
Known limitation until Phase B

A signed-in user still sees the same local (guest) vault. Do not release in this state.

Likely compile-fix spots (written against the library from memory of its API)

AuthRepositoryImpl.toAuthError() (error-code names), SessionStatus subclasses, clearSession(), AuthDeepLinkHandler import of handleDeeplinks.

Not built

Sign-in/up screens, Profile account section, navigation, Google sign-in, password reset UI, RLS/SQL (none needed yet), sync.

### Entry 14 — Phase A1b: Sign in / Create account screen

Status: Written, not built. Screen not yet reachable: navigation wiring waits for MindVaultDestination.kt, MindVaultNavHost.kt and the Profile files.

Files created (under …/mindvault/)

domain/usecase/IsAuthAvailableUseCase.kt, feature/auth/AuthUiState.kt, AuthViewModel.kt, AuthScreen.kt.

Key decisions
One screen, two modes (Sign in / Create account). Email + password, show/hide password, inline field errors, one form-level error, spinner on submit, double submit blocked, typed input kept on failure (password cleared only on mode switch and after sign-up).
After sign-up with confirmation on: "Check your email" panel (same message whether or not the address exists).
Closes itself when the session becomes signed-in (also when the email link is opened while the screen is showing).
"Continue without an account" keeps guest mode one tap away.
No Google button, no "Forgot password" yet (A2 / D2): nothing is shown that does not work.
Uses Material 3 theme values only (scheme colors, typography); corner radii are literals (12 / 16 dp) pending the token files. Strings hardcoded, as elsewhere.
No ViewModel test: the version catalog has no kotlinx-coroutines-test.

Entry 15 — Google-only sign-in (email hidden by a flag) and in-app account deletion

Status: Written, not built.

What changed
Google-only for now. Email/password code stays but is hidden by a build flag auth.emailEnabled (default false). With the flag off, the sign-in page shows only "Continue with Google" and "Continue without an account". The repository also refuses email calls when the flag is off, so nothing can bypass the UI. To bring email back later: set auth.emailEnabled=true in local.properties once custom SMTP works.
Delete account (Profile → Account → "Delete account"): confirmation dialog that explains the effect and requires typing DELETE. It calls the database function delete_my_account (SQL in docs/supabase_delete_account.sql) through supabase-kt Postgrest, then ends the local session. Failures show a message and keep the user signed in. Items saved on this device are not deleted.
New dependency

io.github.jan-tennert.supabase:postgrest-kt (version from the existing 3.0.2 BOM). Used only for the delete call now; sync in Phase C reuses it.

Files modified

gradle/libs.versions.toml, app/build.gradle.kts (AUTH_EMAIL_ENABLED, postgrest), core/di/AuthModule.kt (installs Postgrest, passes the flag), data/auth/AuthConfig.kt, data/auth/AuthRepositoryImpl.kt (deleteAccount, isEmailSignInEnabled, email guard), domain/repository/AuthRepository.kt, feature/auth/{AuthUiState,AuthViewModel,AuthScreen}.kt, feature/profile/{ProfileScreen,Profileuistate,Profileviewmodel}.kt, AuthUseCasesTest.kt.

Files created

domain/usecase/{DeleteAccountUseCase,IsEmailSignInEnabledUseCase}.kt, docs/supabase_delete_account.sql.

Decisions and limits
Deleting an account does not delete data stored on the device (the data is not tied to the account until Phase B). The dialog says so.
The SQL function is security definer, deletes only auth.uid(), and is executable only by signed-in users.
Still required for Play, not built: a public web page where people can request account deletion, and the privacy policy page (Phase D3).
Password reset is no longer needed while email is off (Phase D2 postponed).

## Entry 16 — Phase B: per-account local data and user switching

**Status:** Written, not built.

**Goal:** Each signed-in account sees only its own on-device vault; guests keep the existing vault; switching accounts never shows another account's items.

### Database and migration impact
- No schema change, no migration. `MindVaultDatabase` stays at version 1, `app/schemas/1.json` is unchanged, `MINDVAULT_MIGRATIONS` stays empty (Room's schema check would fail at launch if the schema had changed).
- Guest vault is still `mindvault.db` in the normal databases folder. Nothing is copied, moved or rewritten.
- Each account gets `no_backup/users/mindvault_<userId>.db`, opened by absolute path, with the same schema and its own seeded default deck (`default-deck`). Android never backs up `no_backup/`.

### How it works
- `ActiveDatabase` (new) owns the open database. Session SignedIn -> that account's vault; SignedOut -> guest vault; Loading and Expired keep the current vault.
- `ActiveOwnerStore` (new) remembers the last open vault (a plain user ID) so the right vault opens at app start.
- `AccountDataRepositoryImpl` (new) follows `AuthRepository.observeSessionState()` from `MindVaultApp.onCreate` and calls `ActiveDatabase.switchTo`.
- The four Room repositories use `ActiveDatabase`: one-off calls take `active.current` once (read-then-write and the flashcard transaction use a single database); observed flows use `active.observe { }` (`flatMapLatest` on the owner).
- `MindVaultNavHost` watches the owner through `AppSessionViewModel`; any change navigates to Home, pops the whole back stack and clears the saved tab states. `AuthScreen` close only pops when there is a previous entry.
- `DeleteAccountUseCase` deletes the account's local vault after the server deletion succeeds: switches to guest, closes and deletes the files. The guest vault is never touched.
- Theme stays global (DataStore).
- `backup_rules.xml` was still the Android Studio sample, so `mindvault_auth` was backed up on API 26-30 (Entry 13 said it was excluded). Both XML files now exclude `mindvault_auth.xml` and `mindvault_active_owner.xml`.

### Files modified
`data/repository/{Knowledge,Category,Deck,Flashcard}RepositoryImpl.kt` (replaced), `domain/usecase/DeleteAccountUseCase.kt` (replaced), `app/MindVaultApp.kt` (replaced), `core/navigation/MindVaultNavHost.kt`, `feature/profile/ProfileScreen.kt` (dialog wording only), `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`.

### Files created
`domain/model/DataOwner.kt`, `domain/repository/AccountDataRepository.kt`, `domain/usecase/ObserveDataOwnerUseCase.kt`, `data/local/database/{ActiveDatabase,ActiveOwnerStore}.kt`, `data/repository/AccountDataRepositoryImpl.kt`, `core/di/AccountDataModule.kt`, `app/AppSessionViewModel.kt`.

### File deleted
`core/di/DatabaseModule.kt`. The database and DAOs are intentionally no longer injectable. A "missing binding" build error for them means a file not yet reviewed still injects one.

### Decisions
1. Sign-out keeps the account's vault on the device; it reopens on the next sign-in. Phase C adds "clear after a confirmed sync".
2. Delete account also deletes that account's local vault, not the guest vault.
3. A new account starts empty; guest items stay in the guest vault (Phase D1 will offer to import).
4. Any account change returns to Home and clears history. Sign-out then sign-in only; no direct account switching.
5. Theme is global. Default deck ID stays `default-deck` in every vault.
6. Phase B changes no tables, so there is no Room migration. Isolation is covered by `ActiveDatabaseTest` (using its own test files) and by a manual check that existing guest data is intact after installing over the previous build.

### Known limits
- Until Phase C, an account's data exists only on this phone: uninstalling or clearing app data deletes it.
- At cold start the last account's vault opens while the session loads. If the session turns out to be dead, the app switches to guest a moment later.
- While the session is Expired in the same process, the previous account's vault stays open; after a restart the app starts in guest mode and that vault stays hidden until the user signs in again.
- A vault belongs to a Supabase user ID. Deleting an account and signing in with the same Google account creates a new ID and an empty vault.
- Opened vaults stay open until deleted (a handful at most). If deleting a vault file fails, a leftover file is never reused.
- Account vault file names replace any character outside A-Z, a-z, 0-9, `_` and `-` with `_`; Supabase IDs are UUIDs and unaffected.

### Built (developer-reported)
`ActiveDatabaseTest` and the test updates (waiting for the test files), clearing the local vault after sync, guest-to-account import.

## Entry 17 — Phase C1: sync foundation (cloud schema, Room v2, change recording)

**Status:** Written, not built. The SQL file was run against a local PostgreSQL 16 with a stubbed `auth` schema (not against Supabase). The migration SQL was run against a SQLite database built from `app/schemas/.../1.json`. The Kotlin has not been compiled and the instrumented tests have not been run.

**Goal:** Everything the sync engine (C2) needs, with no sync running yet: cloud tables with Row Level Security and push/pull functions, a non-destructive Room v1 -> v2 migration, and every local write recorded in a pending-changes table in the same transaction. No network calls, no WorkManager, no UI.

### Decisions (confirmed 2026-11-07)
1. Conflicts: the newer `updatedAt` wins for a whole row (client time, clamped to server time + 5 minutes). Local edits are always at least 1 ms newer than the version they replace, so a device clock moved backwards cannot lose edits.
2. A deletion wins over a concurrent edit. A card for a deleted item or deck is not created.
3. Same name on two devices: categories merge (older kept, items re-pointed); the later deck is renamed "Name (2)". Duplicate cards for the same item and deck: the earlier card is kept. The app resolves these after pulling (C2); the cloud has no unique indexes on names or (item, deck).
4. Sign-out: final sync, then delete the account's local vault only if everything is uploaded; otherwise keep it and warn (C3).
5. Sync runs on app open, a few seconds after a change, and about every 6 hours, on any network (C3). Status is shown only in the Profile account card (C3).
6. Sync metadata lives in two new tables, not extra columns on every entity.

### Room schema v1 -> v2 (migration impact)
- `MIGRATION_1_2` only adds. No row is moved, rewritten or deleted.
  - `categories.updatedAt` and `review_states.updatedAt` (`INTEGER NOT NULL DEFAULT 0`), filled from the best timestamp each row has: category `createdAt`; review state `lastReviewedAt`, or `nextReviewAt` for a card never reviewed.
  - New empty tables `sync_outbox` and `sync_state`.
- It runs on the guest vault and on every account vault the first time each is opened (`ActiveDatabase` already passes `MINDVAULT_MIGRATIONS`). Guest data gains two filled columns and two empty tables; the guest vault never syncs.
- The new columns carry `@ColumnInfo(defaultValue = "0")` so a migrated database and a freshly created one have the same schema.
- `MindVaultDatabase` version is now 2. Building the app exports `app/schemas/com.mindvault.data.local.database.MindVaultDatabase/2.json`: **commit it**; the migration test needs it.
- New account vaults seed the default deck with `updatedAt = 0` (was creation time) so an untouched default deck never overrides one renamed on another device. Renaming it sets a real `updatedAt`.

### What is recorded for sync
`sync_outbox` holds one row per entity (type, id, operation, changedAt); a newer change replaces an older one (a delete replaces a pending edit). Written inside the same `withTransaction` as the change, so a failed write leaves no entry.

| Action | Recorded |
|---|---|
| Item create / edit | KNOWLEDGE_ITEM upsert |
| Item delete | KNOWLEDGE_ITEM delete (cloud removes its cards and review states) |
| Category create / rename | CATEGORY upsert |
| Category delete | CATEGORY delete (cloud clears the category on its items, keeping their `updatedAt`) |
| Deck create / rename | DECK upsert |
| Deck delete | DECK delete (cloud removes its cards) |
| Add card | FLASHCARD upsert + REVIEW_STATE upsert |
| Remove card | FLASHCARD delete |
| Save review state | REVIEW_STATE upsert, `updatedAt` = save time |

Rows created before C1 have no entry. The first sync of an account vault will upload everything (C2).
Deleting an item, deck or category removes its dependants locally by database cascade; pending entries for those dependants may remain and the engine must drop an upsert whose row no longer exists (C2).

### Cloud (docs/supabase_sync_schema.sql — run in the dev project first, then prod)
- Tables: `categories`, `decks`, `knowledge_items`, `flashcards`, `review_states`. Primary key `(user_id, id)`, `user_id` references `auth.users` with ON DELETE CASCADE (so `delete_my_account()` also removes cloud data). Timestamps are `timestamptz`.
- RLS: signed-in users can only SELECT their own rows. There are no write policies and table privileges are removed, so clients cannot write directly.
- `sync_push(jsonb)`: the only way to write. Uses `auth.uid()`, applies the conflict rules, is safe to retry, tombstones deletions (`deleted_at`) and cascades them, repairs an item pointing at a deleted category, never deletes the default deck, and returns rows it rejected instead of failing the batch. At most 1000 changes per call.
- `sync_pull(table, since, after_id, limit)`: one page (default 500, max 1000) of rows changed after the cursor, oldest first, including tombstones; timestamps as epoch ms, `server_updated_at` as ISO text to pass back. The app must re-read from a few minutes before its saved cursor and apply rows idempotently.
- Only `authenticated` can execute the functions.

### Files
Created: `data/local/entity/{SyncOutboxEntity,SyncStateEntity}.kt`, `data/local/dao/SyncOutboxDao.kt`, `data/repository/SyncTimestamps.kt`, `docs/supabase_sync_schema.sql`, `app/src/androidTest/.../data/local/{MigrationTest,OutboxRecordingTest}.kt`.
Modified (replaced): `data/local/entity/{CategoryEntity,ReviewStateEntity}.kt`, `data/local/database/{MindVaultDatabase,Migrations,DefaultDeckCallback}.kt`, `data/mapper/{KnowledgeMappers,FlashcardMappers}.kt`, `data/repository/{Knowledge,Category,Deck,Flashcard}RepositoryImpl.kt`.
No new dependencies.

### Known limits
- Nothing is uploaded yet; the outbox only grows until C2.
- Server-side name and duplicate-card conflicts are resolved by the app in C2, so the cloud can briefly hold two same-named decks or categories.
- Tombstones are kept indefinitely (small).
- A review done on one device can be overwritten by a newer save of the same card on another.
- Supabase free projects pause after a week of inactivity (see AUTH_SETUP.md).

### Not built
Push/pull engine, conflict resolution on pull, first full upload, WorkManager, sync status UI, sign-out clearing (C2/C3); guest-to-account import (D1).

## Entry 18 — Phase C2 + C3: sync engine, triggers, status UI, sign-out with final sync

**Status:** Built and verified on device (developer-reported): the v1 -> v2 migration ran with existing data intact, no errors
**Goal:** Upload pending changes and download cloud changes for the open account vault, resolve conflicts by the Entry 17 rules, sync on app open / a few seconds after a change / about every 6 hours (any network), show honest status in the Profile account card, and make sign-out do a final sync before it removes the account's on-device vault.

### Database, dependencies, setup
- **No schema change and no migration.** Room stays at version 2; `app/schemas/.../2.json` is unchanged. Only DAO queries and one DAO accessor (`syncStateDao()`) were added.
- **New dependencies:** `androidx.work:work-runtime-ktx:2.9.1`, `androidx.hilt:hilt-work:1.2.0`, `androidx.hilt:hilt-compiler:1.2.0` (KSP).
- **Manifest:** WorkManager's default initializer is removed; `MindVaultApp` provides the configuration with Hilt's worker factory.
- **Delete `core/di/DatabaseModule.kt`.** Entry 16 said it was removed, but it was still in the project tree. It provides a second, unused database on the guest file.

### Files created (under `app/src/main/java/com/mindvault/`)
| File | Purpose |
|---|---|
| `data/sync/SyncModels.kt` | Wire types, run result, failure kinds, sync_state keys |
| `data/sync/SyncRemote.kt` | Interface to the cloud (push / pull) |
| `data/sync/SyncLocalStore.kt`, `RoomSyncLocalStore.kt` | Everything the engine needs from one vault; Room implementation |
| `data/sync/SyncWire.kt` | Builds the `data` object for an upsert from the vault |
| `data/sync/SyncEngine.kt` | One run: first full upload, push, pull, push again |
| `data/sync/SyncApplier.kt` | The conflict policy for pulled rows |
| `data/sync/SupabaseSyncRemote.kt` | `sync_push` / `sync_pull` through Postgrest; refuses another user's vault |
| `data/sync/SyncRepositoryImpl.kt` | Runs the engine one at a time, triggers, status, final sync |
| `data/sync/SyncScheduler.kt`, `SyncWorker.kt` | WorkManager: one-time and 6-hourly |
| `data/local/dao/SyncStateDao.kt` | Sync bookkeeping access |
| `core/di/SyncModule.kt` | Hilt bindings |
| `domain/model/SyncStatus.kt`, `domain/repository/SyncRepository.kt` | Status model and contract |
| `domain/usecase/SyncStatusBuilder.kt`, `ObserveSyncStatusUseCase.kt`, `SyncNowUseCase.kt`, `SignOutWithSyncUseCase.kt` | Use cases |
| `app/src/test/.../data/sync/FakeSyncLocalStore.kt`, `FakeSyncRemote.kt`, `SyncApplierTest.kt`, `SyncEngineTest.kt`; `.../domain/usecase/SyncStatusBuilderTest.kt` | 36 unit tests |

### Files modified
`data/local/dao/{SyncOutbox,Category,Deck,KnowledgeItem,Flashcard,ReviewState}Dao.kt` (new queries), `data/local/database/MindVaultDatabase.kt` (`syncStateDao()`), `app/MindVaultApp.kt`, `feature/profile/{ProfileScreen,Profileuistate,Profileviewmodel}.kt`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `AndroidManifest.xml`.

### Key decisions
- **Order of a run:** queue the first full upload (once), push, pull every table (parents first), push again if conflict handling queued changes.
- **Push order:** upserts category+deck, item, card, review state; then deletions card, item, deck, category. A row waits while something it depends on is still queued (the server skips a card whose item is missing without reporting it).
- **Outbox safety:** an entry is removed only if it is unchanged since it was read (a newer edit during the upload stays queued). An upsert whose row no longer exists is dropped.
- **Pull:** cursor per table in `sync_state`, re-read from 5 minutes before the cursor, pages of 500, cursor saved only after a table finished. Pulled rows are written without outbox entries.
- **Conflicts:** newer `updatedAt` wins (equal or older ignored); deletion wins; same-name categories merge (older kept, items re-pointed with `updatedAt` moved forward, loser deleted in the cloud, alias stored so late items map to the kept one); the later deck becomes "Name (2)" and is uploaded, the default deck is never renamed; of two cards for the same item and deck the earlier is kept; no card for a missing item or deck; a pulled card gets a starting review state that the cloud's state overrides.
- **Safety:** sync runs only while the signed-in user is the owner of the open vault (checked in the repository and again in the remote). The guest vault never syncs.
- **Triggers:** app open or account change (one-time work), 5 s after the last change (debounced), every 6 hours; network required. WorkManager retries with exponential backoff, up to 5 attempts. A missing sign-in is not retried.
- **Status:** Syncing, Not synced yet, Waiting (N changes), Synced (time), Needs attention (offline / sign in again / server problem / N changes refused). "Sync now" runs in the foreground.
- **Sign-out:** final sync (25 s limit) → sign out → remove the account's on-device vault only if everything was uploaded; otherwise the vault stays and Profile says so. Account deletion is unchanged.
- **Moved items:** items re-pointed by a category merge move to the top of "recently updated" once.
- **Old `SignOutUseCase`** is unchanged and now unused (its existing test still compiles); remove it when convenient.

### Known limits
- A review done on one device can be overwritten by a newer save of the same card on another.
- A category merge can briefly leave two same-named categories in the cloud until the next sync finishes.
- Whole vault tables are read into memory for the first upload (fine for personal vaults).
- Status text for "Synced" does not tick forward on its own; it refreshes on the next status change.
- Not covered by tests: Supabase calls, WorkManager, UI, process death mid-run.

### Likely compile-fix spots
`SupabaseSyncRemote` (`RestException.statusCode`, `HttpRequestException`, `PostgrestResult.data`, `rpc` parameter overload), `SyncWorker`/`MindVaultApp` (Hilt-work and `Configuration.Provider` property form), `SyncRepositoryImpl` (`SessionState.SignedIn.user.id`).

### Manual checks to run
1. Sign in on the emulator, add an item: Profile shows "1 change is waiting…" then "Synced just now". Check the row in the Supabase table editor.
2. Airplane mode, add and edit items: status shows waiting; go online: it syncs without opening the app screen.
3. Two devices (or emulator + phone), same account: edit the same item on both, the later edit wins on both.
4. Delete an item on one device: it disappears on the other after sync, with its cards.
5. Create a category named "Science" on both devices while offline, then sync both: one category remains, items keep a category.
6. Create a deck with the same name on both: the later one becomes "Name (2)" on both.
7. Sign out online: Home as guest, the account's items are gone from the device; sign in again: they come back.
8. Sign out in airplane mode: sign-out works, Profile says the account's items were kept on this device; sign in later and they sync.
9. Delete account: cloud rows are gone (unchanged behavior).
10. Force-stop the app with pending changes, reopen: they upload.

## Entry 19 — Flashcards V2, Phase 1 of 3: SM-2 scheduling logic, new-card limit setting, reset of existing cards

**Status:** Written, not built. Nothing here is Verified until a note says so.

**Goal:** Spaced repetition for flashcards (SM-2). Phase 1 adds the logic layer only (scheduler, review queues, session logic, settings storage, data reset). Phase 2 adds the rated Review screen and the deck screen's Review section. Phase 3 adds the Home "Due for review" rows and the Profile setting.

### Decisions (confirmed)
- SM-2 behind a `ReviewScheduler` interface; FSRS move planned in `docs/FSRS_MIGRATION_PLAN.md`.
- Ratings: Again / Hard / Good / Easy. **Again loops in the same session**; a card's schedule is set by its **first** rating in the session. Repeats only decide when the card leaves the session.
- Due = **by calendar day** (device time zone): a card is due if `nextReviewAt` is before the start of tomorrow.
- New cards: first rating Again 1d, Hard 1d, Good 2d, Easy 4d. Later: Again 1d; Hard max(prev+1, prev×1.2); Good prev×ease; Easy prev×ease×1.3. Ease: Again −0.20, Hard −0.15, Good 0, Easy +0.15, clamped 1.3–3.0. Interval cap 365 days.
- **Difficult Cards** = reviewed at least once and ease factor below 2.0 (no lapse count, no schema change).
- **Daily new-card limit: 20 per deck per day**, stored as a setting (DataStore, per device, default 20, range 1–100). Cards introduced today are counted from existing review state (`reviewCount == 1` and `lastReviewedAt` today), so nothing new is stored and the count follows the card across devices.
- **Practice (Review All) records nothing**: no ratings, no review count, no schedule change.
- Deck "complete" and Home "decks completed today" keep their current rules.
- Review history log: postponed to a later phase.

### Database and migration impact
- Room **v2 -> v3**, `MIGRATION_2_3`: **no table or column changes**. It resets every review state to a new card (`reviewCount 0`, `lastReviewedAt` NULL, `nextReviewAt` now, ease 2.5, interval 0) and sets `updatedAt = MAX(updatedAt + 1, now)` so the reset wins sync conflicts. It then writes a `REVIEW_STATE` UPSERT into `sync_outbox` for every row so the cloud receives the reset. Vault items, decks, cards and categories are untouched.
- Runs on the guest vault and on every account vault the first time each is opened.
- Building the app exports `app/schemas/com.mindvault.data.local.database.MindVaultDatabase/3.json`: **commit it** (the migration test needs it).
- No Supabase change, no new dependency.

### Files created (under `app/src/main/java/com/mindvault/`)
| File | Purpose |
|---|---|
| `domain/model/ReviewModels.kt` | `ReviewRating`, `ReviewMode`, `ReviewCounts` |
| `domain/model/ReviewSession.kt` | Pure rated-session logic (flip, answer, Again loop, tally) |
| `domain/scheduler/ReviewScheduler.kt` | `ReviewScheduler` interface and `Sm2Scheduler` |
| `domain/usecase/ReviewQueueBuilder.kt` | Pure due / new / difficult queues and counts |
| `domain/usecase/NewCardsPerDayUseCases.kt` | Observe and set the daily new-card limit |
| `core/di/SchedulerModule.kt` | Binds `ReviewScheduler` to `Sm2Scheduler` |
| `app/src/test/.../domain/scheduler/Sm2SchedulerTest.kt`, `.../domain/usecase/ReviewQueueBuilderTest.kt`, `.../domain/model/ReviewSessionTest.kt`, `.../domain/usecase/RecordCardReviewUseCaseTest.kt` | Unit tests |

### Files modified
`domain/usecase/RecordCardReviewUseCase.kt` (now takes a rating and uses the scheduler), `domain/repository/SettingsRepository.kt`, `data/repository/SettingsRepositoryImpl.kt`, `data/local/database/Migrations.kt`, `data/local/database/MindVaultDatabase.kt` (version 3), `feature/flashcards/StudyViewModel.kt` (Practice no longer saves reviews), `feature/flashcards/StudyScreen.kt` (Practice wording), tests `FakeSettingsRepository.kt`, `MigrationTest.kt`.

### Known effects
- After the update every existing card shows as "not reviewed" and "decks completed today" drops to 0 until cards are rated again. This is the intended reset.
- The new-card limit is per device (not synced); the count of cards introduced today is derived from synced review state.
- Until Phase 2 there is no rating UI: the Study screen is Practice only.

### Not built in Phase 1
Review screen and rating buttons, deck Review section, Home due rows, Profile setting, review history log, FSRS.


### Entry 19 — Phase 2 of 3 (append under Entry 19): rated Review screen and deck Review section

**Status:** Written, not built. Phase 1 is verified (developer-reported: builds and tests pass).

**Goal:** Let the user actually do spaced repetition: rate cards, loop Again cards, and start Daily / New / Difficult reviews from the deck screen. Practice stays as it was (flip only, records nothing).

### Database, dependencies, setup
No schema change, no migration, no new dependency, no Supabase change.

### Files created (under `app/src/main/java/com/mindvault/`)
| File | Purpose |
|---|---|
| `domain/usecase/ObserveDeckReviewCountsUseCase.kt` | Live due / new / difficult counts for one deck (uses the daily limit setting) |
| `domain/usecase/LoadReviewQueueUseCase.kt` | One-off snapshot of the cards for a review mode (`ReviewQueue`) |
| `feature/flashcards/ReviewUiState.kt` | Loading / Error / Empty / Active |
| `feature/flashcards/ReviewViewModel.kt` | Loads the queue, saves each card's first rating, drives `ReviewSession` |
| `feature/flashcards/ReviewScreen.kt` | Card, Show answer, rating buttons with next-interval previews, Again repeats, summary |
| `app/src/test/.../domain/usecase/ObserveDeckReviewCountsUseCaseTest.kt` | Counts follow the limit |

### Files modified
`core/navigation/MindVaultDestination.kt` (`REVIEW_MODE_ARG`, `Review` route), `core/navigation/MindVaultNavHost.kt` (Review route, deck screen gets `onReviewClick`), `feature/flashcards/DeckDetailScreen.kt` (Review section, Study renamed Practice), `feature/flashcards/DeckDetailViewModel.kt` (exposes `reviewCounts`), `feature/flashcards/StudyScreen.kt` (one word: `FlashcardFace` is now `internal` so Review reuses it).

### Key decisions
- **Separate Review route** (`flashcards/review/{deckId}/{mode}`) so Practice and rated review never share state. Bottom bar stays hidden, like Study.
- **Snapshot per session:** the queue is read once when the screen opens, so cards never reshuffle mid-session. Cards rated today leave the counts on the deck screen as soon as they are saved.
- **Save before advancing:** a card's first rating is saved, then the session moves on. If saving fails the card stays, a message is shown and the buttons work again. Repeat cards (after Again) save nothing.
- **Repeat cards** show only "Again" and "Got it"; they only decide whether the card leaves the session.
- **Rating buttons** show the next interval (for example "Good · 2d"); Good uses the primary button, the others the secondary button, so meaning never rests on color alone.
- **Deck screen:** a Review card (Daily review with its count, New cards, Difficult) above Practice and Shuffle. Buttons are disabled when their count is 0, and a caught-up message replaces the count.
- Practice, Shuffle, Add cards, remove card, rename and delete behave as before.

### Known limits
- The counts do not refresh at midnight; they update on the next change or when the screen is reopened.
- No ViewModel or Compose tests (no ViewModel test setup in the project yet).
- Home due rows and the Profile setting are Phase 3; until then the daily limit is the 20 default.

### Manual checks
1. Open a deck with cards: the Review card shows e.g. "0 due · 8 new" (new cards capped at 20), Practice and Shuffle still work and change nothing.
2. Daily review: Show answer, then rate. Buttons show intervals (New card: Again 1d, Hard 1d, Good 2d, Easy 4d). Rate Again: that card returns at the end with only "Again / Got it".
3. Finish: the summary lists the Again / Hard / Good / Easy counts. Back on the deck screen the card now counts as reviewed, due 0, and the daily count dropped.
4. Rate 20+ new cards in one day in one deck: "New cards" shows 0 afterwards.
5. Rate a card Again three times (over three days, or by editing test data): it appears under Difficult cards.
6. Airplane mode is not needed (local only); signed in, the ratings upload (Profile status).

Entry 19 — Phase 3 of 3 (append under Entry 19): Home due rows, Profile setting, repeat-card change

Status: Written, not built. Phases 1 and 2 are verified (developer-reported).

Change to Phase 2 (repeat cards): a card marked Again comes back once more at the end of the session and now asks Hard / Good / Easy (no Again this time), instead of "Again / Got it". The answer is saved and sets when the card returns: Hard 1 day, Good 2 days, Easy 3 days. A relearn answer changes only the interval and due date; ease and review count are untouched, so one lapse is never penalised twice and the "new cards introduced today" count stays correct. Because Again is not offered on the repeat, every card is seen at most twice per session.

Goal of Phase 3: show decks with cards to review on Home, and add the daily new-card limit as a Profile setting that can be removed by deleting one line.

Database, dependencies, setup

No schema change, no migration, no new dependency, no Supabase change.

Files created (under app/src/main/java/com/mindvault/)
File	Purpose
feature/home/DueDecksSection.kt	"Due for review" rows on Home (deck name and cards to review; tap opens the deck)
feature/profile/NewCardsPerDayViewModel.kt	Observes and saves the daily new-card limit
feature/profile/NewCardsPerDaySetting.kt	Self-contained "Flashcards" settings block (5, 10, 20, 30, 50)
app/src/test/.../domain/usecase/ReviewRelearnTest.kt	Relearn intervals and saving
Files modified

domain/scheduler/ReviewScheduler.kt (relearn methods), domain/usecase/RecordCardReviewUseCase.kt (relearn), domain/model/HomeSummary.kt (DeckDue, dueDecks), domain/usecase/HomeSummaryBuilder.kt, domain/usecase/ObserveHomeSummaryUseCase.kt, feature/flashcards/ReviewViewModel.kt, feature/flashcards/ReviewScreen.kt; one inserted block each in feature/home/HomeScreen.kt and feature/profile/ProfileScreen.kt.

Key decisions
Home shows a deck only when it has cards to review today (due reviews plus new cards still allowed). The existing Continue studying / Start studying / decks completed today rows are unchanged.
The Profile setting lives in its own composable and ViewModel. To remove it, delete the single NewCardsPerDaySetting() line in ProfileScreen.kt; the stored value, the 20 default and everything else keep working.
The limit is per device (DataStore), not synced.
Known limits
Counts on Home and the deck screen do not refresh at midnight; they update on the next change or when the screen is reopened.
No ViewModel or Compose tests.
Reviewing the same card more than once in one day through Difficult cards can compound its interval; a fix is pending a product decision (see conversation).

Entry 19 — Phase 4 (append under Entry 19): Difficult cards any time, extra practice for cards that are not due

Status: Written, not built. Phases 1-3 are verified (developer-reported) except Phase 3, which is awaiting the build result.

Goal: Let the user review difficult cards again even after a Daily review, without distorting the SM-2 schedule. Reviewing a card that is not due and rating it as if it were due makes its interval grow as if a full gap had passed (rating the same card Good three times in one day gives 2d, 4d, 8d).

Behaviour
Difficult cards (reviewed cards with ease factor below 2.0) can be opened at any time.
A difficult card that is due today or overdue is rated normally (Again / Hard / Good / Easy), exactly like Daily review, including the Again repeat.
A difficult card that is not due (for example one just reviewed today) is extra practice: it shows "Still hard" and "Got it", and nothing is saved or rescheduled. "Still hard" puts the card back at the end of the session; "Got it" removes it.
Due difficult cards are listed before extra ones, each group hardest (lowest ease) first.
The summary reports scheduled reviews and extra practice separately.
Daily review, New cards and Practice are unchanged.
Database, dependencies, setup

No schema change, no migration, no new dependency, no Supabase change.

Files modified (under app/src/main/java/com/mindvault/)

domain/usecase/ReviewQueueBuilder.kt (extraCardIds, due-first ordering for Difficult), domain/model/ReviewSession.kt (isExtra, answerExtra, extraCount, progress), domain/usecase/LoadReviewQueueUseCase.kt (ReviewQueue.extraCardIds), feature/flashcards/ReviewViewModel.kt (onExtraAnswer, guard), feature/flashcards/ReviewScreen.kt (extra note and buttons, summary).

Files created

app/src/test/.../domain/usecase/DifficultExtraPracticeTest.kt.

Known limits
Whether a card is extra is decided when the session starts; a card rated during the session is not re-evaluated.
"Still hard" can loop a card as often as the user wants; the user ends the session by choosing "Got it" or going back.
Extra practice leaves no record (the review history log is a later phase).
Manual checks
Review a due difficult card in Daily review (rate Again so its ease drops below 2.0), then open Difficult cards: it appears as extra practice with "Still hard / Got it" and a note that the schedule won't change.
After "Got it", leave and reopen the deck: the card's due date is unchanged.
With one due difficult card and one extra card, Difficult cards lists the due one first and rates it with the four buttons.
"Still hard" brings the card back at the end; "Got it" finishes it.
Documentation to adjust

In the Phase 3 entry's Known limits (and V2_DOC_UPDATES.md copy), remove the bullet about compounding intervals; it is resolved here. In IMPLEMENTATION_STATUS.md §7 Next Task, the difficult-cards decision is done: the next task is the review history log.

Entry 19 — follow-up (append under Entry 19): Difficult cards threshold

Status: Written, not built.

Problem: "Difficult cards" is disabled (shows 0) until a card's ease factor drops below 2.0. Ease only drops on Again (-0.20) and Hard (-0.15), starting from 2.5, so a card needs about three Again/Hard ratings on three different days to qualify. In practice the option looked broken and could not be used right after a Daily review.

Change: a reviewed card is difficult when its ease factor is below the starting ease (2.5), i.e. it has been rated Again or Hard more than Easy. One Again or Hard is enough. Good keeps the ease unchanged and Easy raises it, so those cards are not difficult. The threshold is now tied to ReviewState.DEFAULT_EASE_FACTOR instead of a separate number.

Files modified

domain/usecase/ReviewQueueBuilder.kt (DIFFICULT_EASE_BELOW), test ReviewQueueBuilderTest.kt (one card changed from ease 2.0 to 2.5).

No schema, dependency or sync change.
Documentation to adjust

In Entry 19 Phase 4 and in IMPLEMENTATION_STATUS.md §6, replace "ease factor below 2.0" with "ease factor below the starting ease (2.5)".

Manual checks
In a deck, rate one new card Again or Hard in Daily review. The deck's "Difficult (n)" button now shows 1 and opens.
A card rated Good or Easy is not counted.
Opening Difficult cards right after the Daily review lists that card as extra practice ("Still hard" / "Got it") with no schedule change.

## Entry 20 — Default deck holds every Vault item, category decks (Phase 1 of 3)

**Status:** Written, not built. Phases 1-4 of Flashcards V2 (Entry 19) are verified (developer-reported) apart from the latest Difficult-cards fix and Phase 3/4 items still awaiting your build result.

**Overall goal (3 phases):**
1. **Default deck** contains every Vault item automatically and is locked (this entry).
2. **Category decks:** every category gets a deck with its items; items join automatically; Room v4 -> v5 adds `decks.categoryId`; Supabase column and sync updates.
3. **Vault category menu:** Rename category, Delete category (asks keep or delete the deck), Create flashcard deck; "remove from the old category's deck?" dialog (with "remember my choice") and a Profile setting for it.

### Confirmed decisions (all phases)
| # | Decision |
|---|---|
| 1 | A deck is created automatically for every category (new and existing). The Vault option "Create flashcard deck" appears only for a category that has no deck, and recreates it with the category's items. |
| 2 | Items join their category's deck automatically (when saved with the category or moved into it). |
| 3 | Taking an item out of a category asks "Also remove from the <category> deck? Its review progress there will be lost." with Remove / Keep and a "Remember my choice" checkbox. A Profile setting chooses: Ask me each time (default), Always remove, Always keep. Moving between categories always adds the card to the new category's deck. |
| 4 | **Default deck:** holds every Vault item; cards are added automatically for new items and once for existing items; cannot be renamed or deleted; cards cannot be added or removed by hand (they leave only when the item is deleted). |
| 5 | Decks get a `categoryId` link (Room migration, Supabase column, sync updates). |
| 6 | Deleting a category asks whether to keep or delete its deck. |
| 7 | Renaming a category renames its deck, unless the deck was given a different name. |
| 8 | A name clash creates the category deck as "Name (2)". |
| 9 | The Vault gets a category menu: Rename, Delete, Create flashcard deck. |
| 10 | The same item can have a card in the default deck and in its category deck, each with its own schedule. |

This supersedes the earlier decision that the default deck can be renamed.

### Phase 1: what changes
- **New items** get a flashcard (and its starting review state) in the default deck in the same transaction as the item, and both are queued for sync.
- **Existing items** get a default-deck card once, through Room migration **v3 -> v4** (data only, no table or column changes). Cards are created with the item's creation date so new cards come oldest first; the daily new-card limit (default 20 per deck) keeps the default deck from flooding. Existing cards and their progress are untouched. The new cards are queued for sync.
- **Default deck locked:** rename is blocked in the repository and hidden in the UI; removing a card from it is blocked in the repository and hidden in the UI; the "Add cards" button is hidden. It still cannot be deleted. Deleting a Vault item still removes its cards from every deck.
- The "Add to flashcards" sheet shows the default deck as "Already added".

### Database, dependencies, setup
- Room version 4, `MIGRATION_3_4`. Building exports `app/schemas/.../4.json`: **commit it**.
- No new dependency, no Supabase change in this phase.

### Files modified (under `app/src/main/java/com/mindvault/`)
`data/repository/KnowledgeRepositoryImpl.kt`, `data/repository/DeckRepositoryImpl.kt`, `data/repository/FlashcardRepositoryImpl.kt`, `domain/repository/DeckRepository.kt` (doc only), `data/local/database/Migrations.kt`, `data/local/database/MindVaultDatabase.kt`, `feature/flashcards/DeckDetailScreen.kt`. Test `OutboxRecordingTest.kt` rewritten for the new rules.

### Files created
`app/src/androidTest/.../data/local/DefaultDeckBackfillMigrationTest.kt`.

### Known limits
- An item that arrives from a device still on an older app version has no default-deck card until it is created by that device's update; this phase does not repair it.
- Items from before this update get cards as new cards, so the default deck can show many new cards; the daily limit applies.
- A user who already renamed the default deck keeps that name; it cannot be changed again.

### Manual checks
1. Install over the previous build: Flashcards > Default deck now lists every Vault item, all "not reviewed".
2. Add a new item: it appears in the default deck immediately.
3. Default deck screen: no rename icon, no Add cards button, no remove icon on cards; Practice, Shuffle and Review still work.
4. Delete a Vault item: its card disappears from the default deck and any other deck.
5. Signed in: Profile shows changes waiting, then Synced.


### Entry 20 — Phase 2 of 3 (append under Entry 20): category decks

**Status:** Written, not built. Phase 1 is verified (developer-reported).

**Goal:** Every category has a flashcard deck with the category's name, created automatically; items join it automatically; the deck is a normal customisable deck. Phase 3 adds the Vault category menu and the "remove from the old deck?" dialog.

### Behaviour
- **Creating a category** creates its deck (name = category name; if a deck already has that name the deck is "Name (2)", "Name (3)"...) and links it with `decks.categoryId`. Existing categories get a deck, with cards for all their items, through the migration.
- **Items join the deck:** saving an item with a category, or moving an item into a category, adds its card to that category's deck (as a new card). The item's card in its previous category's deck is left alone (Phase 3 adds the question).
- **Renaming a category** renames its deck too, unless the user gave the deck a different name.
- **Deleting a category** keeps its deck as an ordinary deck (the link is cleared). Phase 3 asks whether to keep or delete the deck.
- **A deck the user deleted is not recreated** when more items are saved in that category. The repository method `DeckRepository.ensureCategoryDeck(categoryId)` recreates it with all the category's items (used by the Vault option in Phase 3).
- Category decks are normal decks: rename, delete, add and remove cards, review, practice. Changing a deck never changes the Vault.

### Database and migration impact
- Room **v4 -> v5**, `MIGRATION_4_5`: adds the nullable column `decks.categoryId` (foreign key to `categories.id`, ON DELETE SET NULL, indexed), then creates one deck per existing category with a card (new card, starting review state) for each of its items, and queues all of it for sync. Nothing existing is changed or deleted. Building exports `app/schemas/.../5.json`: **commit it**.
- **Supabase:** `docs/supabase_sync_schema.sql` is replaced by the updated file (adds `decks.category_id`, its foreign key, and updates `sync_push` and `sync_pull`). **Run it in the dev project first, then prod, before running the new app version signed in.** It is safe to run again.
- No new dependency.

### Sync rules added
- A deck row carries `category_id`. A category that no longer exists (or was merged away) becomes "no category"; a deck pointing at a category that lost a same-name merge is re-pointed to the kept category and uploaded again (same rule as items).
- Deleting a category clears the link on its decks in the cloud, keeping their `updated_at`.

### Files created (under `app/src/main/java/com/mindvault/`)
`data/repository/CategoryDecks.kt`; tests `app/src/androidTest/.../data/local/CategoryDecksTest.kt`, `.../CategoryDecksMigrationTest.kt`, `app/src/test/.../data/sync/SyncApplierDeckCategoryTest.kt`.

### Files modified
`domain/model/Deck.kt`, `data/local/entity/DeckEntity.kt`, `data/local/dao/DeckDao.kt`, `data/mapper/DeckMappers.kt`, `domain/repository/DeckRepository.kt`, `data/repository/{Knowledge,Category,Deck}RepositoryImpl.kt`, `data/local/database/Migrations.kt`, `data/local/database/MindVaultDatabase.kt` (version 5), `data/sync/SyncWire.kt`, `data/sync/SyncApplier.kt` (`deck`), tests `FakeSyncLocalStore.kt`, `FakeSyncRemote.kt`, `MigrationTest.kt`, `OutboxRecordingTest.kt`.

### Known limits
- Items re-pointed by a category merge do not get a card in the kept category's deck.
- An item moved out of a category keeps its card in that category's deck until Phase 3.
- The link lives on the deck, so if two devices create the same category offline, one category (and its link) is merged away and its deck stays as an ordinary deck.

### Manual checks
1. Install over the previous build: every existing category now has a deck with its name and cards for its items (Flashcards tab).
2. Create a new category in the Add form: a deck with that name appears.
3. Save an item in that category: its card is in the category deck and in the default deck.
4. Edit an item and move it into a category: the card appears in that deck.
5. Rename the category: the deck is renamed. Rename the deck yourself first, then rename the category: the deck keeps your name.
6. Delete the deck, save another item in the category: the deck is not recreated.
7. Signed in: Profile shows changes waiting, then Synced; the deck row in Supabase has `category_id`.

### Entry 20 — Phase 3 of 3 (append under Entry 20): Vault category menu and "remove from the old deck?"

**Status:** Written, not built. Phases 1 and 2 are verified (developer-reported).

**Goal:** Manage categories from the Vault (rename, delete with a keep-or-delete-deck choice, create the deck for a category that has none) and let the user decide what happens to an item's card in the old category's deck when the item leaves a category.

### Behaviour
- **Vault category menu:** when one category is selected in the Vault's Category filter, a "⋮" button appears next to the filters with **Rename category**, **Create flashcard deck** (only when the category has no deck) and **Delete category**.
  - Rename: the category's deck follows the new name unless the user gave the deck a different name (Phase 2 rule).
  - Delete: the category's items stay in the Vault as Uncategorized. If the category has a deck, the dialog asks: **Keep the deck** (default; it becomes an ordinary deck) or **Delete the deck too** (its cards and review progress are lost). The Vault filter returns to All.
  - Create flashcard deck: creates the deck with the category's name (a number is added if the name is taken) and a card for each of its items.
- **Taking an item out of a category** (changing its category or clearing it in the Edit form): if the item has a card in the old category's deck, the app asks "Remove from the “Science” deck too? Its review progress in that deck will be lost." with **Remove from deck**, **Keep in deck** and a **Remember my choice** checkbox. Tapping outside the dialog cancels the save and keeps the form open. The item's card in the default deck and in other decks is never touched. Moving the item into another category always adds its card to that category's deck.
- **Profile setting** "When I take an item out of a category": **Ask me each time** (default), **Always remove it from the deck**, **Always keep it in the deck**. The "Remember my choice" checkbox writes this setting. The setting is one self-contained block: delete or comment out the single `CategoryDeckSetting()` line in `ProfileScreen.kt` to hide it (the default "Ask me each time" keeps working).

### Database, dependencies, setup
No schema change, no migration, no Supabase change, no new dependency.

### Key decisions
- The item update and the card removal are one transaction, so a failed save never removes a card and a removal never happens without the save.
- A card is never removed without the user's choice (or a stored "always remove" setting).
- Deleting a category with its deck: the deck is looked up first, then the category is deleted, then the deck; if deleting the deck fails the deck simply stays as an ordinary deck.

### Files created (under `app/src/main/java/com/mindvault/`)
`domain/model/DeckRemovalPreference.kt`, `domain/model/CategoryResults.kt`, `domain/usecase/CategoryManagementUseCases.kt`, `domain/usecase/DeckRemovalUseCases.kt`, `feature/vault/CategoryManagement.kt`, `feature/knowledge/DeckRemovalDialog.kt`, `feature/profile/DeckRemovalViewModel.kt`, `feature/profile/DeckRemovalSetting.kt`; tests `app/src/test/.../domain/usecase/CategoryManagementUseCasesTest.kt`, `app/src/androidTest/.../data/local/CategoryDeckRemovalTest.kt`.

### Files modified
`domain/repository/{Knowledge,Deck,Settings}Repository.kt`, `data/repository/{Knowledge,Deck,Settings}RepositoryImpl.kt`, `data/repository/CategoryDecks.kt`, `domain/usecase/UpdateKnowledgeItemUseCase.kt`, `feature/vault/{VaultScreen,VaultUiState,VaultViewModel}.kt`, `feature/knowledge/{AddKnowledgeUiState,AddKnowledgeViewModel}.kt`, one inserted block each in `feature/knowledge/AddKnowledgeScreen.kt` and `feature/profile/ProfileScreen.kt`; tests `FakeRepositories.kt`, `FakeSettingsRepository.kt`.

### Known limits
- Cancelling the "remove from deck?" dialog keeps the form open with the edits; nothing is saved.
- Items re-pointed by a sync category merge do not get cards in the kept category's deck.
- The "Remember my choice" setting is per device (DataStore), not synced.
- No ViewModel or Compose tests.

### Manual checks
1. Vault > choose a category in the Category filter: a "⋮" button appears. Rename: the Vault filter and the deck both show the new name (unless you renamed the deck).
2. Delete a category with a deck: choose Keep, and the deck remains under Flashcards, no longer tied to a category; items show as Uncategorized. Repeat choosing Delete the deck too, and the deck is gone.
3. Delete a category deck from Flashcards, then Vault > that category > ⋮ > Create flashcard deck: the deck returns with all the category's items.
4. Edit an item, clear its category, Save: the dialog appears (deck holds its card). Choose Keep: the card stays. Repeat with Remove: the card leaves that deck but stays in the default deck.
5. Tick "Remember my choice": the dialog stops appearing; Profile shows the stored choice and can switch back to "Ask me each time".
6. Edit an item, move it from category A to B: B's deck gets its card; A's deck asks (or follows the setting).
7. Signed in: Profile shows changes waiting, then Synced.

### Documentation to adjust
- `IMPLEMENTATION_STATUS.md`: add rows "Default deck holds every Vault item", "Category decks" and "Vault category management" (Written, unverified until built); add the Room schema 5 and the `decks.category_id` Supabase column to §2/§6; record decisions 1-10 of Entry 20; §7 Next Task: review history log.
- `ARCHITECTURE.md` §6: `Deck` gains an optional `categoryId` (category deck); the default deck is locked (no rename, no manual add or remove of cards).
- `DESIGN_SYSTEM.md` §7.2 Vault: category filter with a "⋮" menu (Rename, Create flashcard deck, Delete); §7.8 Profile: "When I take an item out of a category" setting.
- `docs/supabase_sync_schema.sql`: already replaced in Phase 2.

## Entry 21 — Search V2, Phase 1 of 3: Wiktionary + Wikipedia search, ranking, concept cards, save as WORD/CONCEPT

**Status:** Built and working (developer-reported). Follow-up change after the first build: the source footnote is no longer added to the saved text (see "Change after build" below).

**Goal:** Replace the interim Free Dictionary Search with Wiktionary (words) and Wikipedia (concepts). Filter All / Words / Concepts; "All" queries both at the same time. Ranked results, compact cards that expand, partial failure with Retry per source, Save / Edit first for words and concepts. Phase 2 adds autocomplete, recent searches and the Profile setting. Phase 3 adds R8 rules, polish and the docs.

### Decisions (confirmed)
1. No server; integrations live in `data/remote`. English only for now (bilingual later). No AI. No keyed APIs.
2. In `All`, words and concepts appear in one list, each tagged Word or Concept; exact matches of both kinds first, then prefix, then contains, then the rest.
3. Word cards: definitions grouped by part of speech, plus examples. No pronunciation, synonyms or etymology (Wiktionary's definition endpoint does not return them).
4. Concept card: title, short description, summary, link to the article.
5. The source (name, licence, link) is shown as a footnote on the Search card only. It is **not** part of the saved text, because the definition also appears on flashcards. **No schema change.** (Original decision was a source line at the end of the saved definition; replaced, see below.)
6. Duplicate rule is title + type: a word and a concept with the same title can both be saved. `findByTitle` is unchanged; `findByTitleAndType` was added.
7. Wikimedia rules: descriptive User-Agent, in-memory cache + HTTP cache, back-off on 429/503, at most 2 requests in flight.
8. Free Dictionary code stays untouched as "V1"; switching back is one line in `RepositoryModule.kt`.

### Database, dependencies, setup
- **No Room schema change, no migration, no Supabase change, no new dependency.** The new DAO query uses the existing `type` column.
- **Before any public release, replace `CONTACT`** in `core/di/WikimediaModule.kt` with a project email address or website URL. It is sent in the User-Agent of every request.
- Delete nothing. `DatabaseModule.kt` and all `freedictionary/*` files are untouched.

### What the real API responses showed (and how the code handles it)
- Wiktionary `page/definition/{term}`: a map of language code to sections. The `"en"` list also holds other languages, so only `language == "English"` is kept. `definition` is HTML (tags, entities, empty label spans); a definition with sub-senses embeds them in an `<ol>` and the sub-senses also appear as separate entries, so text is cut at `<ol>` and the parent's example list (a union of its children's) is ignored. Empty definitions are dropped. Sections with the same part of speech are merged. A missing word is HTTP 404 → "no result". Terms are case-sensitive, so a miss on a capitalised query is retried in lower case.
- Wikipedia `search/page`: `pages[]` with `key`, `title`, `description`. Disambiguation hits (title ends "(disambiguation)", or description "Topics referred to by the same term") are dropped.
- Wikipedia `page/summary/{title}`: `type`, `title`, `description`, `extract`, `content_urls.desktop.page`. Only `type == "standard"` with text is kept (Mercury, a disambiguation page, is dropped).
- Request budget per search in `All`: 1 Wiktionary + 1 Wikipedia search + summaries for the top 2 hits = 4 requests. The other concept cards show title + description and load their summary when opened, saved or edited first.

### Files created (under `app/src/main/java/com/mindvault/`)
| File | Purpose |
|---|---|
| `domain/model/SearchScope.kt` | `SearchSource` (WORDS / CONCEPTS) and `SearchScope` (ALL / WORDS / CONCEPTS) |
| `domain/usecase/RankSearchResultsUseCase.kt` | Pure ranking and de-duplication |
| `domain/usecase/LoadConceptSummaryUseCase.kt` | Loads a concept's summary on demand |
| `data/remote/ConceptDataSource.kt` | Seam for the concept source (Wikipedia) |
| `data/remote/wikimedia/WikimediaDto.kt` | DTOs and the lenient `WikimediaJson` |
| `data/remote/wikimedia/WiktionaryApi.kt`, `WikipediaApi.kt` | Retrofit interfaces |
| `data/remote/wikimedia/HtmlText.kt` | HTML → plain text (no dependency) |
| `data/remote/wikimedia/WikimediaUrls.kt` | Source names, licence, hosts, article URLs |
| `data/remote/wikimedia/WiktionaryMapper.kt`, `WikipediaMapper.kt` | DTO → domain mapping |
| `data/remote/wikimedia/WiktionaryDataSource.kt`, `WikipediaDataSource.kt` | The two data sources |
| `data/remote/wikimedia/SearchCache.kt` | In-memory cache (5 min, 50 entries) |
| `data/remote/wikimedia/WikimediaInterceptors.kt` | User-Agent interceptor, `BackoffPolicy`, `BackoffInterceptor` |
| `data/repository/WikimediaSearchRepository.kt` | V2 repository: error mapping, caching |
| `core/di/WikimediaModule.kt` | Wikimedia OkHttp client (`@Named("wikimedia")`), APIs, bindings |
| Tests (under `app/src/test/java/com/mindvault/`) | `domain/usecase/{RankSearchResultsUseCaseTest,LoadConceptSummaryUseCaseTest}.kt`, `data/remote/wikimedia/{HtmlTextTest,WiktionaryMapperTest,WikipediaMapperTest,SearchCacheTest,BackoffTest}.kt`, `data/repository/WikimediaSearchRepositoryTest.kt` |

### Files modified / replaced
| File | Change |
|---|---|
| `domain/model/SearchResult.kt` | Replaced. Additive fields (`kind`, `description`, `summary`, `sourceUrl`, `license`, `needsSummary`), `savedItemKey`, type-aware `toSaveContent` with the source line |
| `domain/model/SearchOutcome.kt` | Replaced. Adds `Busy` (HTTP 429/503) |
| `domain/repository/SearchRepository.kt` | Replaced. `search(query, source)` and `loadSummary(result)` |
| `domain/usecase/SearchDefinitionsCase.kt` | Replaced. Runs the chosen sources in parallel; returns one outcome per source |
| `domain/usecase/SaveSearchResultUseCase.kt`, `ObserveSavedTitlesUseCase.kt` | Replaced. Title + type duplicate rule; keys are `TYPE:title` |
| `domain/repository/KnowledgeRepository.kt`, `data/repository/KnowledgeRepositoryImpl.kt`, `data/local/dao/KnowledgeItemDao.kt` | Added `findByTitleAndType` (existing methods untouched) |
| `data/repository/SearchRepositoryImpl.kt` | Replaced. V1 adapted to the new interface (words only) |
| `core/di/RepositoryModule.kt` | Binds `WikimediaSearchRepository`. For V1, bind `SearchRepositoryImpl` instead |
| `core/navigation/MindVaultDestination.kt`, `MindVaultNavHost.kt` | New `PREFILL_TYPE_ARG`; "Edit first" passes the type |
| `feature/knowledge/AddKnowledgeViewModel.kt` | Reads the prefill type (two small insertions) |
| `feature/search/Searchuistate.kt`, `Searchviewmodel.kt`, `SearchScreen.kt`, `Searchresultcard.kt` | Replaced. Filters, notices with Retry, expandable cards |
| Tests | `FakeRepositories.kt` (one new method), `Searchtestsupport.kt`, `Savesearchresultusecasetest.kt`, `Searchdefinitionsusecasetest.kt`, `Observesavedtitlesusecasetest.kt`, `Searchrepositoryimpltest.kt`, `Searchresulttest.kt` (was an empty class), `Searchviewmodeltest.kt` |

### Key behaviour
- **Partial failure:** each source has its own outcome. If one fails, the other's results show with a notice ("Couldn't reach Wikipedia, so concepts are missing") and a Retry for that source only. If every source fails, one full-screen message (offline, or busy/error) with Try again.
- **Save needs the real summary:** a concept card below the top 2 has no summary yet. Opening it, Save or Edit first loads the summary first. If that fails, nothing is saved and the card says so.
- **Filter change** after a search runs the search again for the new sources (cached answers return instantly).
- **Back-off:** on 429/503 wait 1 s then 2 s (or the server's Retry-After if ≤ 3 s), at most 2 retries. After a final 429, no requests for 30 s (or Retry-After, 5–120 s); the notice shows "busy".
- Saved items go through `KnowledgeRepository.create`, so they still get their default-deck card.
- The saved text contains no source information (see above).

### Change after build: source is a card footnote only
`SearchResult.toSaveContent()` no longer appends `Source: ...` to the definition. Save and "Edit first" now carry just the definition (words: `(noun) text` lines; concepts: the summary). The card still shows "Source: Wikipedia · CC BY-SA 4.0" and, when expanded, "Open on Wikipedia". Files: `domain/model/SearchResult.kt`; tests `Searchresulttest.kt`, `Savesearchresultusecasetest.kt`, `Searchviewmodeltest.kt` updated. Items saved before this change keep the source line in their definition; edit them to remove it. If the source should be kept with a saved item later, it needs real `sourceName`/`sourceUrl` fields (Room v6, Supabase columns, sync changes) so it can be shown on the item detail screen only. Not done.

### Known limits
- Wikimedia's global API limits are new and may change. It is not confirmed whether the 200 requests/minute for an identified client is counted per IP or per User-Agent. Contact Wikimedia before launch (see the note in the Phase 3 docs).
- Wiktionary lists words of many languages; only the English section is shown.
- A concept whose summary fails to load can be retried but not saved; one with no usable summary (404, disambiguation) is dropped.
- Searching "All" for a very common word returns only the top 5 Wikipedia hits.
- Strings are hardcoded, as elsewhere. No Compose/UI tests.

### Likely compile-fix spots
`SearchScreen.kt` / `Searchresultcard.kt` (token names `ShapeMedium`, `ShapeSmall`, `MindVaultColors.SurfaceVariant`), `WikimediaModule.kt` (Hilt qualifier on the binding, `BuildConfig` import), `BackoffTest.kt` (OkHttp `Response.Builder`), `WiktionaryMapperTest.kt` / `WikipediaMapperTest.kt` (`decodeFromString` import).

### Commands
```
gradlew.bat testDebugUnitTest
gradlew.bat assembleDebug
```
Record the real output. If a test fails, paste the failure and the file.

### Manual checks
1. Search "run" with All: a Word card (Wiktionary) and Concept cards (Wikipedia) appear, each tagged; "Show more" on the word shows all senses and examples.
2. Search "entropy": the word entropy and the concept Entropy rank first; "Entropy (disambiguation)" never appears; Mercury shows planet/element pages but no list page.
3. Filter Words: only Wiktionary results. Filter Concepts: only Wikipedia. Changing the filter re-runs the search.
4. Save the concept "Entropy" and the word "entropy": both save (type Concept / Word); each card then says "Already in your Vault" for its own type only. Open the saved item: the text has no source line.
5. "Edit first" on a concept: the form opens with Concept selected and the summary filled in.
6. Open a concept card that was not in the top 2: "Loading summary…" then the summary. Save it directly (without opening): it saves with the summary.
7. Airplane mode, then search: full-screen "Can't connect". Online again, Try again works.
8. Block only one source (for example turn off the network after Wiktionary answered, then retry a concept search) or simulate: the other source's results stay, a notice with Retry shows, and Retry fills it in.
9. Search "zzqxv": "Nothing found" with Add a concept.
10. Existing flows: manual Add, Vault, Flashcards, Profile behave as before; a saved search result has a card in the default deck.
11. Switch to V1 (bind `SearchRepositoryImpl` in `RepositoryModule`): word search works as before, Concepts shows "Nothing found".

### Next
Phase 2: autocomplete (2 characters, 300 ms), recent searches (last 4), Profile setting "Search suggestions". Phase 3: R8 keep rules, attribution polish, rewrite `docs/SEARCH_PROVIDER.md`, update `IMPLEMENTATION_STATUS.md`, `ARCHITECTURE.md`, `DESIGN_SYSTEM.md`.

## Entry 21 — Phase 2 of 3 (append under Entry 21): autocomplete, recent searches, Profile "Search suggestions" setting

**Status:** Written, not built in Android Studio. Phase 1 is built and working (developer-reported). Nothing here is Verified until you report a real build and test run.
What was checked outside your project (sandbox, Kotlin 2.0.21 compiler against small stand-ins for Android, Compose, Hilt, Retrofit, JUnit, kotlinx.serialization.json and the coroutines test library): the new domain, parser, repository and ViewModel code compiled and 126 tests (Phase 1 and 2 together) ran and passed there, with real-time waits instead of virtual time. That is **not** your Gradle build. Not checked at all: Compose UI (`SearchScreen`, `SearchSuggestionsSetting`), Hilt wiring, DataStore (`Settingsrepositoryimpl`), Retrofit decoding of the `JsonElement` response, `BackoffTest`, and the mapper tests that decode JSON.

**Goal:** Suggest complete words and concept titles while typing; remember the last 4 successful searches and show them when the field is empty; a Profile setting to switch suggestions off.

### Behaviour
- **Suggestions:** start after 2 characters and a 300 ms pause in typing, only from the sources the filter allows (All = both). Every keystroke cancels the pending request, so fast typing sends one request per source. Results of the two sources are interleaved, de-duplicated ignoring case, an exact match first, at most 6. Tapping a suggestion runs the search (with the current filter). No suggestions are shown for the text that was just searched; typing again brings them back. A source that fails simply gives no suggestions (no error message).
- **Where they show:** while the search field is focused, suggestions replace the results area; with an empty focused field, the recent searches show instead ("Recent searches" with Clear). Pressing Search or tapping a row removes focus and shows the results.
- **Recent searches:** the last 4, newest first, repeated searches move up (case ignored). Only a search that found something is remembered. Stored on this device (DataStore), not tied to an account, cleared with "Clear" on the list or "Clear recent searches" in Profile.
- **Profile > Search:** a switch "Search suggestions" (default on) with a note that typing is sent to Wiktionary and Wikipedia, plus "Clear recent searches" when there are any. One self-contained block: delete or comment out the single `SearchSuggestionsSetting()` line in `ProfileScreen.kt` to hide it (suggestions then stay on).
- Switching the filter while text is typed refreshes the suggestions for the new sources (or re-runs the search if one was already submitted).

### Database, dependencies, setup
No Room schema change, no migration, no Supabase change, no new dependency. Two new DataStore keys: `search_suggestions_enabled` (Boolean, default true) and `recent_searches` (one string, one search per line).

### Endpoints
Both suggestion sources use MediaWiki `action=opensearch` (`https://en.wiktionary.org/w/api.php?action=opensearch&format=json&search=...&limit=6` and the same on en.wikipedia.org). The Wiktionary response was verified from your sample (`[query, [titles], [descriptions], [urls]]`); the Wikipedia one is assumed to have the same shape (standard MediaWiki format). The parser returns no titles instead of failing on any other shape, and drops titles ending "(disambiguation)" for Wikipedia. Wiktionary's title search also suggests words of other languages ("entendre"); searching one may end in "Nothing found". Only the first part of a suggestion's life (typing) is sent to Wikimedia.

### Files created (under `app/src/main/java/com/mindvault/`)
| File | Purpose |
|---|---|
| `domain/model/RecentSearches.kt` | Pure add / de-duplicate / cap-at-4 logic |
| `domain/usecase/SuggestSearchTermsUseCase.kt` | Autocomplete: parallel sources, interleave, de-dupe, exact first, cap |
| `domain/usecase/SearchSuggestionUseCases.kt` | Observe/set the setting, observe/add/clear recent searches |
| `data/remote/SuggestionDataSource.kt` | Seam for autocomplete sources |
| `data/remote/wikimedia/OpenSearchParser.kt` | Reads the opensearch response |
| `feature/profile/SearchSuggestionsSetting.kt`, `SearchSuggestionsViewModel.kt` | The Profile block |
| Tests (under `app/src/test/java/com/mindvault/`) | `domain/model/RecentSearchesTest.kt`, `domain/usecase/SuggestSearchTermsUseCaseTest.kt`, `data/remote/wikimedia/OpenSearchParserTest.kt` |

### Files modified / replaced
| File | Change |
|---|---|
| `domain/model/SearchResult.kt` | `SearchLimits` gains `MIN_SUGGESTION_LENGTH`, `SUGGESTION_DEBOUNCE_MILLIS`, `MAX_SUGGESTIONS` (and the source-footnote change of Phase 1) |
| `domain/repository/SearchRepository.kt` | Adds `suggest(query, source)` (best effort, never throws except cancellation) |
| `domain/repository/SettingsRepository.kt`, `data/repository/Settingsrepositoryimpl.kt` | Suggestions on/off, recent searches |
| `data/repository/WikimediaSearchRepository.kt` | Constructor takes two suggestion sources; `suggest` with its own cache. **Four constructor parameters now** |
| `data/repository/SearchRepositoryImpl.kt` | V1: `suggest` returns an empty list |
| `data/remote/wikimedia/WiktionaryApi.kt`, `WikipediaApi.kt` | `openSearch` (returns `JsonElement`) |
| `data/remote/wikimedia/WiktionaryDataSource.kt`, `WikipediaDataSource.kt` | Also implement `SuggestionDataSource` |
| `core/di/WikimediaModule.kt` | Two `@Named` bindings for the suggestion sources |
| `feature/profile/ProfileScreen.kt` | One Spacer + the `SearchSuggestionsSetting()` line, after `CategoryDeckSetting()` |
| `feature/search/Searchuistate.kt`, `Searchviewmodel.kt`, `SearchScreen.kt` | `suggestions` and `recentSearches` in the state; debounce, suppression after a search, recent searches; suggestion and recent lists in the screen. `SearchViewModel` has five new constructor parameters |
| Tests | `Fakesettingsrepository.kt` (new members), `Searchtestsupport.kt` (`suggest`), `Searchrepositoryimpltest.kt`, `WikimediaSearchRepositoryTest.kt`, `Searchviewmodeltest.kt` (15 new tests) |

### Known limits
- Suggestions are an extra 1–2 requests per pause in typing. They are cached for 5 minutes and cancelled when typing continues; the 200 requests/minute Wikimedia limit is not a concern for one user, but see the Phase 1 note about launch-scale counting.
- Recent searches are per device, shared by guest and every account on it. Clear them with the Clear buttons.
- Suggestions come from the titles Wikimedia has (prefix match), so a misspelling gets none.
- The ViewModel's debounce is tested with the coroutines test library's virtual time (`advanceTimeBy`); the screen's focus handling has no automated test.

### Likely compile-fix spots
`SearchScreen.kt` (new imports, `SearchBody` extension), `SearchSuggestionsSetting.kt` (`toggleable`, `Switch`), `Settingsrepositoryimpl.kt` (`booleanPreferencesKey` import), `WikimediaModule.kt` (two `@Named` bindings of the same implementation), `WiktionaryApi.kt` / `WikipediaApi.kt` (`JsonElement` return type with the Retrofit converter), `OpenSearchParserTest.kt` (`parseToJsonElement`).

### Commands
```
gradlew.bat testDebugUnitTest
gradlew.bat assembleDebug
```
Record the real output.

### Manual checks
1. Search tab, All filter: type "en" and pause: a short list of suggestions appears (words and concepts mixed). Type one more letter quickly several times: the list updates once you pause, not per letter.
2. Tap a suggestion: the field fills, the keyboard closes, the search runs, the list disappears.
3. Filter Words: suggestions are Wiktionary words only. Filter Concepts: titles only (no "(disambiguation)").
4. Type one character: no suggestions. Clear the field: no suggestions.
5. After a search for "entropy", tap the field again without typing: the results stay, no list covers them. Add a letter: suggestions return.
6. Do 5 searches that find something: tap the empty field: the last 4 show, newest first, with a Clear button. Tap one: it runs. Search a nonsense word: it is not added.
7. Profile > Search: turn "Search suggestions" off: typing shows no suggestions (searching still works; recent searches still show). Turn it on again. "Clear recent searches" empties the list (the button disappears).
8. Airplane mode while typing: no list, no error message.
9. Close and reopen the app: the setting and the recent searches are still there.
10. Existing flows (Add, Vault, Flashcards, Profile settings) unchanged; saved search results still get their default-deck card.

### Next
Phase 3: R8 keep rules for the new DTOs, attribution polish, rewrite `docs/SEARCH_PROVIDER.md`, update `IMPLEMENTATION_STATUS.md`, `ARCHITECTURE.md`, `DESIGN_SYSTEM.md`.

## Entry 21 — Phase 3 of 3 (append under Entry 21): R8 rules, source not in the saved text, docs

**Status:** Written, not built. Phase 1 is built and working (developer-reported). Phase 2 is written and awaiting your build. Nothing here is Verified until you report a real build, test run and release (R8) check.

**Goal:** Release-build safety for the new Search classes, confirm the saved definition carries no source text, and bring the docs in line with Search V2.

### Source is not in the saved text
Done in the Phase 1 follow-up and re-checked here: `SearchResult.toSaveContent()` returns only the definition (words: `(noun) text` lines; concepts: the summary). No code path appends a source. "Edit first" opens the form with the same text. The card still shows "Source: Wikipedia · CC BY-SA 4.0" and, expanded, an "Open on Wikipedia/Wiktionary" link; that stays on the card only. Search for `Source: ` in `app/src/main` finds only that card label.
Existing items saved before the change keep a `Source: …` line at the end of their definition. Remove it by editing the item (Vault → item → Edit). There is no automatic cleanup (it would need a data migration and sync changes); say so if you have many items and want one.

### Database, dependencies, setup
No schema change, no migration, no Supabase change, no new dependency.

### Files modified / created
| File | Change |
|---|---|
| `app/proguard-rules.pro` | Appended keep rules for the Search DTOs: `*$$serializer`, `Companion` and `serializer(...)` of `com.mindvault.data.remote.**` (covers V1 and V2), and the `Signature`/annotation attributes Retrofit needs for generic return types. Your existing rules are unchanged |
| `docs/SEARCH_PROVIDER.md` | Rewritten for V2: endpoints and real response shapes, layering, Wikimedia rules, rate-limit research, licensing and privacy notes, switching back to V1, release checklist |
| `ARCHITECTURE.md` | Search feature description, the "External Search and saving" flow, navigation, testing and "not designed yet" lines. Based on the copy in the project files; diff it against your current copy |
| `DESIGN_SYSTEM.md` | §7.4 Search rewritten (filters, suggestions, recent searches, cards, notices); §7.8 gets the "Search" settings line. Based on the copy in the project files |
| `docs/IMPLEMENTATION_STATUS_SEARCH_V2_ADDITIONS.md` | Rows and sections to merge into your `IMPLEMENTATION_STATUS.md` (the project copy is older than the code, so it was not rewritten) |

### Key points
- The R8 rules are the standard kotlinx.serialization rules scoped to `com.mindvault.data.remote.**`. The libraries also ship their own rules; these are belt and braces. They are **unverified** until a release build is run.
- Attribution: the card shows source name, licence and link. For release also add an About / licences screen and update the privacy policy and Play data-safety form (search text is sent to Wikimedia). Both are listed in the checklist in `docs/SEARCH_PROVIDER.md`; neither is built.
- Reminder: replace `CONTACT` in `core/di/WikimediaModule.kt` before release, and contact Wikimedia about the rate limit for a mobile app.

### Known limits
- No R8/release verification yet.
- `ARCHITECTURE.md` and `DESIGN_SYSTEM.md` outputs are based on the project-file copies; if your local copies have newer edits, merge the changed sections instead of replacing the files.

### How to check the R8 rules
A signed release build needs your keystore (`keystore.properties`). Without one, temporarily set `isMinifyEnabled = true` (and `isShrinkResources = true`) in the `debug` build type of `app/build.gradle.kts`, run `gradlew.bat assembleDebug`, install, and then revert the change.
1. Word search ("run"), concept search ("entropy"), All filter: results appear (a crash or "Search isn't working" on every query means a DTO was stripped or renamed).
2. Save a word and a concept; "Edit first" on a concept.
3. Type "en" and pause: suggestions appear. Profile > Search switch works.
4. Search the V1 words path if you switch back (bind `SearchRepositoryImpl`).
   If something fails, paste `adb logcat` lines for `SearchVM`, `WikimediaSearch` and any `SerializationException`.

### Commands
```
gradlew.bat testDebugUnitTest
gradlew.bat assembleDebug
```
Record the real output.

### Next
Build and test Phase 2 and these rules, fix what they report; then the review history log, language editions for Search, and the launch items above (contact address, Wikimedia contact, privacy policy, About screen).

## Entry 22 — Quick search, Phase 1 of 3: receive selected / shared text

**Status:** Written, not built. Nothing here is Verified until you report a real build, test run and device check.

**Goal (whole feature):** Select a word in any app, tap **MindVault** in the selection menu (or Share → MindVault), and land on Search with the word filled in and searched, with **Save**, **Edit first** and **Add manually** available. Phase 1 only *receives* the text. Phase 2 makes Search use it. Phase 3 is docs.

### Decisions (my defaults from the plan; you sent the files without changing them)
1. Selection menu **and** Share sheet.
2. Auto-run the search (Phase 2), with "Add manually" always visible.
3. Text over 100 characters: use the first 100.
4. Filter resets to **All** for these lookups.
5. Same for guests and signed-in users; no gate.
6. Menu label "MindVault", from a string resource.
7. Android only; iOS later needs its own Share/Action extension.
8. Docs as in Entry 21 (Phase 3).

Added in Phase 1 (not in the plan): a bare link (`https://…`, `www.…`) is ignored, because a shared page address is not a word. A link inside longer text is kept.

### Design change from the plan: a trampoline activity now, not as a fallback
`MainActivity` is `singleTop` in MindVault's own task. If it received `PROCESS_TEXT` / `SEND` directly, Android would open it inside the *other app's* task. So a small no-screen activity, `QuickLookupActivity`, receives the intents, cleans the text, starts `MainActivity` in MindVault's own task (`FLAG_ACTIVITY_NEW_TASK`) with a private extra, and closes. `MainActivity` puts the text into an in-memory inbox, the same pattern as `AuthDeepLinkHandler`.

### Database, dependencies, setup
No Room schema change, no migration, no Supabase change, no new dependency. Nothing is stored: the lookup lives in memory only.

### Files created (under `app/src/main/java/com/mindvault/`)
| File | Save to | Purpose |
|---|---|---|
| `QuickLookup.kt` | `domain/model/` | `QuickLookup` (id + text) and `QuickLookupParser` (trim, collapse whitespace, ignore blank / bare link, cap at 100) |
| `QuickLookupInbox.kt` | `domain/usecase/` | `@Singleton` holder of one pending lookup: `submit`, `consume`, `pending` flow |
| `QuickLookupIntents.kt` | `app/` | Reads the text from a `PROCESS_TEXT` or text `SEND` intent; the private extra name |
| `QuickLookupActivity.kt` | `app/` | The no-screen trampoline |
| `QuickLookupParserTest.kt` | `app/src/test/java/com/mindvault/domain/model/` | 9 tests |
| `QuickLookupInboxTest.kt` | `app/src/test/java/com/mindvault/domain/usecase/` | 7 tests |

### Files modified (complete files supplied; replace yours)
| File | Change |
|---|---|
| `app/src/main/AndroidManifest.xml` | New `QuickLookupActivity` (exported, no display, excluded from recents, empty task affinity) with filters for `PROCESS_TEXT` and `SEND`, both `text/plain`. Everything else unchanged |
| `app/src/main/res/values/strings.xml` | Added `quick_lookup_label` ("MindVault") and `quick_lookup_nothing_selected` |
| `app/MainActivity.kt` | Injects `QuickLookupInbox`; `handleQuickLookup(intent)` runs on a fresh launch and in `onNewIntent`, then removes the extra. Auth handling and theme code unchanged |

### Key behaviour
- Blank selection or a bare link: a short toast ("Select a word or short phrase to look up in MindVault.") and MindVault does not open.
- A newer lookup replaces an unconsumed older one. The same text sent twice still counts as new.
- The extra is removed after reading, and `onCreate` only handles the intent on a fresh launch, so rotation or recreation does not replay it.
- `MainActivity` is exported for the launcher, so another app could send the private extra. The inbox cleans the text again, and the worst case is a prefilled search.

### Known limits (Phase 1)
- **No visible effect in the app yet.** The lookup sits in the inbox until Phase 2 wires Search to it. What you can check now is that MindVault appears in the menus and opens.
- Some apps (and some Android versions) hide `PROCESS_TEXT` items under the "⋮" overflow of the selection menu, or don't offer them in non-selectable text.
- Selecting text in a field MindVault can't see (password fields, some secure apps) isn't possible; nothing to fix.
- The privacy policy and Play data-safety text should mention that text the user sends to MindVault is searched on Wiktionary and Wikipedia (already on the launch list for Search).

### Commands
```
gradlew.bat testDebugUnitTest
gradlew.bat assembleDebug
```
Record the real output. If a test fails, paste the failure and the file.

### Likely compile-fix spots
`QuickLookupActivity.kt` (`com.mindvault.R` import: it assumes the namespace is `com.mindvault`), `QuickLookup.kt` (`Regex.replace` on a `CharSequence`).

### Manual checks
1. Install over the current build. Open a browser or notes app, long-press a word: **MindVault** is in the selection menu (maybe under "⋮").
2. Tap it: MindVault comes to the front in its own window. The recent-apps screen shows one MindVault entry, and the original app is unchanged.
3. Share text from another app: MindVault is in the Share sheet; tapping it behaves the same.
4. Select only spaces, or share a plain link: a toast appears and MindVault does not open.
5. With MindVault already open on any tab: send a word; the app comes to the front and nothing else changes yet (expected in Phase 1).
6. Rotate the phone after a lookup: nothing is replayed.
7. Existing flows (sign-in, Vault, Flashcards, Search, Profile) behave as before.

### Next
Phase 2: `NavHost` switches to Search when a lookup arrives (also after an account-change reset); `SearchScreen` runs it once it is on screen; "Add manually" opens the Add form with the title filled in. This needs one new constructor parameter on `SearchViewModel`, so `Searchviewmodeltest.kt` will need one extra argument; I'll ask for that file then. A lookup that arrives while an Add / Edit form is open is not to be allowed to discard the form's edits; I'll handle that in Phase 2.

### Entry 22 — Phase 2 of 3 (append under Entry 22): Search runs the lookup, "Add manually", navigation

**Status:** Written, not built. Phase 1 was written but you have not yet reported its build or device check; Phase 2 depends on it. Nothing here is Verified until you report a real build, test run and device check.

**Goal:** A word sent from another app opens Search with the text filled in and searched (filter reset to All), and offers **Add manually**, which opens the Add form with the title filled in.

### Database, dependencies, setup
No Room schema change, no migration, no Supabase change, no new dependency.

### Files created (under `app/src/main/java/com/mindvault/`)
| File | Save to | Purpose |
|---|---|---|
| `QuickLookupViewModel.kt` | `app/` | Exposes only the pending lookup's ID to the NavHost; never consumes it |

### Files modified (complete files; the edits were applied to your uploaded copies by exact-match replacement, so nothing else changed)
| File | Save to | Change |
|---|---|---|
| `Searchuistate.kt` | `feature/search/` | New field `quickLookupText: String? = null` (default, so existing code and tests still compile) |
| `Searchviewmodel.kt` | `feature/search/` | New last constructor parameter `quickLookupInbox`; `pendingQuickLookup` flow; `onQuickLookup(lookup)`; `quickLookupText` state added to the `combine` and `buildState`; cleared when the user types something else or searches something else |
| `SearchScreen.kt` | `feature/search/` | Collects the pending lookup and runs it once on screen; a text button "Add “word” manually" under the filters while the shown search came from another app; two imports |
| `MindVaultNavHost.kt` | `core/navigation/` | Gets `QuickLookupViewModel`; one `LaunchedEffect` that opens Search when a lookup is pending; a private list of "hold" routes |

Diff size against your copies: Searchviewmodel +28 / −1 (the `combine` transform line), Searchuistate +2, SearchScreen +28, NavHost +25. `MindVaultDestination.kt` is unchanged: "Add manually" reuses the existing `AddKnowledgeFromSearch` route with an empty definition.

### Required follow-up in tests
`SearchViewModel` has one new constructor parameter. In `Searchviewmodeltest.kt` (and anywhere else a `SearchViewModel(...)` is built), add `QuickLookupInbox()` as the **last argument**, with `import com.mindvault.domain.usecase.QuickLookupInbox`. I have not seen that file, so I have not changed it.

### Key behaviour
- **Who runs the lookup:** `SearchScreen` (a `LaunchedEffect` keyed on the lookup's ID), not the ViewModel, so only a Search screen that is actually on screen acts. The ViewModel's `onQuickLookup` first takes the lookup out of the inbox, so it runs once even if two Search screens exist.
- **What it does:** cancels any running search, resets to a clean state with filter **All**, puts the text in the field, closes the keyboard and runs the normal Search V2 search. Save, Edit first, Retry, partial-failure notices and recent searches all behave as usual (a lookup that finds something is added to recent searches).
- **Add manually:** shown while the field still holds the looked-up text, including during loading, offline, error and no-result states, so the user can always type their own definition. It opens the Add form with the title filled in, type Word (changeable in the form), empty definition. It disappears when the user edits the field or searches something else.
- **Navigation:** when a lookup is pending the NavHost goes to Search the same way the bottom bar does (other tabs keep their state; from a cold start Back returns to Home). If Search is already open nothing moves.
- **Account change:** the effect also depends on the active vault, so a lookup that arrives around a sign-in / sign-out reset still ends up on Search, after the reset to Home.
- **Unsaved work:** while the Add, Add-from-search or Edit form, or the sign-in screen, is open, the lookup waits and opens as soon as the user leaves that screen. Nothing is discarded.
- **No navigation before the graph exists:** the effect does nothing until the current route is known (navigating earlier would crash on a cold start).

### Known limits
- A lookup held behind a form gives no sign that it arrived; it appears when the user leaves the form.
- On a cold start Home may flash for a moment before Search opens.
- A lookup is in memory only; if the process dies before it runs, it is gone.
- Review and Practice sessions are left (not held) when a lookup arrives. Each rating is saved as it is made; Practice records nothing.
- "Add manually" always starts as type Word; a phrase or concept needs the type changed in the form.
- No ViewModel test for `onQuickLookup` yet (I need `Searchviewmodeltest.kt` and `Searchtestsupport.kt` to write it against your fakes). No Compose or NavHost tests.

### Likely compile-fix spots
`Searchviewmodel.kt` (the five-flow `combine` overload), `SearchScreen.kt` (`TextOverflow`, `KnowledgeType` imports), `MindVaultNavHost.kt` (the `by lazy` top-level property).

### Commands
```
gradlew.bat testDebugUnitTest
gradlew.bat assembleDebug
```
Record the real output.

### Manual checks
1. Select a word (e.g. "entropy") in another app, tap **MindVault**: Search opens with "entropy" in the field, filter All, results loading then shown, and "Add “entropy” manually" under the filters.
2. **Save** a result and **Edit first** on another: both work as before. A saved result shows "Already in your Vault".
3. Tap **Add “entropy” manually**: the Add form opens with the title filled in and an empty definition; type a definition and save; the item is in the Vault and in the default deck.
4. Edit the text in the field: the "Add manually" button disappears. Search something else: it stays gone.
5. Send the same word twice in a row: the search runs again each time.
6. With MindVault on the Vault tab (or Flashcards), send a word: Search opens; the other tabs keep their filters when you return.
7. Open the Add form, type something, then send a word from another app: MindVault shows the form with your text intact; after you save or go back, Search opens with the word.
8. Airplane mode, send a word: "Can't connect" shows, and "Add manually" is still there and works.
9. Send a 150-character sentence: the field holds the first 100 characters. Send a bare link: toast, nothing opens (Phase 1).
10. Sign out while a lookup is pending, or send a word right after signing in: you end on Search, not stuck on Home.
11. Existing flows (Search typing/suggestions/recent, Vault, Flashcards, Profile) behave as before.

### Next
Phase 3: docs only. `IMPLEMENTATION_STATUS`, `ARCHITECTURE` (Search feature gets a "quick lookup" paragraph) and `DESIGN_SYSTEM` §7.4 (the "Add manually" row) additions as separate files, plus the privacy-policy / Play data-safety note. I'll write them after you report the Phase 1 and 2 build and device results.
### Entry 22 — follow-up (append under Entry 22): only the selected word, test fixes, ViewModel tests

**Status:** Written, not built. Your `gradlew.bat testDebugUnitTest` run stopped at `compileDebugUnitTestKotlin` (3 errors), so nothing in Entry 22 has run yet. Nothing is Verified until you report a real build and test run.

### 1. Only the selected word is searched
**Problem:** Sharing selected text from a browser (and some other apps) sends the word *plus* the page link, for example `“entropy” https://…`. Phase 1 only ignored a text that was *only* a link, so the whole string went to Search.

**Change:** `QuickLookupParser.parse` now
- removes every link (`http://`, `https://`, `www.`) wherever it appears,
- removes quotes (straight, curly, low-9, guillemets) only when they wrap the whole text, so `don't` and `dogs'` are untouched,
- then trims, collapses whitespace and caps at 100 characters as before.
  A text that is only a link (or only quotes) still gives nothing: toast, MindVault does not open.

**Files:** `domain/model/QuickLookup.kt` (replace), `QuickLookupParserTest.kt` (replace; the old "link inside text is kept" test is replaced by 5 new tests, 13 tests in total).

**Limit:** apps that add other text around the word (a page title, "Shared from …") can't be cleaned reliably; only links and wrapping quotes are removed. Selecting text and using the selection menu's **MindVault** item sends exactly the selection with no link.

### 2. Compile error caused by Entry 22
`Searchviewmodeltest.kt:67 No value passed for parameter 'quickLookupInbox'` — expected, as announced in Phase 2. Fixed in the supplied `Searchviewmodeltest.kt`: new field `inbox = QuickLookupInbox()`, passed as `quickLookupInbox = inbox`, plus the import. Complete file, save to `app/src/test/java/com/mindvault/feature/search/`.

### 3. New ViewModel tests (7, in `Searchviewmodeltest.kt`)
Filter reset to All and the search run from the lookup; runs only once; a stale lookup is ignored; "Add manually" state kept while the field is unchanged and cleared on another edit; cleared by a different search; kept when offline; a lookup that finds something goes into recent searches. They use the existing fakes only (`FakeSearchRepository`, `FakeSettingsRepository`, `FakeKnowledgeRepository`) and have not been run.

### 4. Two compile errors NOT caused by Entry 22 (not fixed yet)
Neither Phase 1 nor Phase 2 touches these files, so they are older test drift that the first full test compile has revealed:
- `FakeRepositories.kt:48` — `FakeCategoryRepository` does not implement `delete`. The `CategoryRepository` interface gained `delete` with the category-management work (Entry 20 Phase 3); the fake was not updated. Needs `domain/repository/CategoryRepository.kt` to match the exact signature.
- `RankSearchResultsUseCaseTest.kt:12` — `Unresolved reference 'invoke'`. The test calls `RankSearchResultsUseCase().invoke(query, results)`; the class evidently has no `invoke`. Needs `domain/usecase/RankSearchResultsUseCase.kt` to see what the method is called.
  Send those two source files and I will fix the two test files (no production code needs to change unless the files show otherwise).

### Commands
```
gradlew.bat testDebugUnitTest
gradlew.bat assembleDebug
```
Record the real output.

### Manual checks
1. In Chrome, select a word and use **Share** → MindVault: the field holds only the word (no link, no quotes).
2. Select the word and use the selection menu's **MindVault** item: same result.
3. Share a page with no selection (just a link): toast, MindVault does not open.

### Entry 22 — Phase 3 of 3 (append under Entry 22): docs, test-fake fix, launch notes

**Status:** Documentation only, plus one test-file fix. No production code changed. Phases 1 and 2 work on a device (developer-reported). The unit tests for Entry 22 have **not** been run yet: `testDebugUnitTest` stopped at the test compile (see below). Nothing here counts as test-verified until you paste a passing run.

### Test-file fix
- `FakeRepositories.kt` (save to `app/src/test/java/com/mindvault/domain/usecase/`): `FakeCategoryRepository` now implements `delete(id)` (removes the category). `CategoryRepository.delete` was added with category management (Entry 20 Phase 3); the fake was never updated. This was the error `Class 'FakeCategoryRepository' is not abstract and does not implement abstract member 'delete'`.
- `RankSearchResultsUseCaseTest.kt` is **unchanged**. The supplied `RankSearchResultsUseCase` does have `operator fun invoke(rawQuery: String, results: List<SearchResult>)`, so `RankSearchResultsUseCase().invoke(query, results)` should resolve. I can't explain the reported `Unresolved reference 'invoke'` from the files I have; it may disappear once the other errors are fixed. If it remains after this fix, send the full compiler output and `Searchtestsupport.kt`.

### Documentation files supplied (merge into your copies)
| File | Merge into |
|---|---|
| `IMPLEMENTATION_STATUS_QUICK_SEARCH_ADDITIONS.md` | `IMPLEMENTATION_STATUS.md` (§2 rows, §3 rows, §5 limits, §6 decisions, §7 next task) |
| `ARCHITECTURE_QUICK_SEARCH_ADDITIONS.md` | `ARCHITECTURE.md` (§3 packages, §5 data flow, §9 navigation, §10 tests, §12) |
| `DESIGN_SYSTEM_QUICK_SEARCH_ADDITIONS.md` | `DESIGN_SYSTEM.md` (§7.4 Search) |

They are additions, not replacement files, because your copies change often.

### Before a public release (not built)
1. **Privacy policy and Play data-safety form:** text the user selects and sends to MindVault is searched on Wiktionary and Wikipedia (the same disclosure as typing in Search). MindVault does not store the text unless the user saves an item.
2. **Store listing / onboarding:** one line telling users that selecting a word and choosing "MindVault" (or Share → MindVault) looks it up.
3. **iOS:** needs its own Share extension (and an action extension for selected text); nothing from this entry carries over except the parser rules and the idea of an in-memory inbox.

### Commands
```
gradlew.bat testDebugUnitTest
gradlew.bat assembleDebug
```
Record the real output.

### Next
Paste the real `testDebugUnitTest` and `assembleDebug` output. After that the review history log (postponed from Flashcards V2) is still the next feature task, then Search language editions.

## Entry 23 — Profile stats: "Your progress" card, Stats screen, review history log (synced)

**Status:** Written, not built. Nothing here is Verified until you report a real build, test run and device check.

**Goal:** A compact "Your progress" card on Profile (between Account and Appearance) that opens a full Stats screen. Streak, reviews per day and the rating split need a review history, so a new append-only `review_log` is added locally, synced to Supabase, and written atomically with each rating.

### Decisions (confirmed)
1. Option A: summary card on Profile + "View stats" opening a Stats screen; all phases merged in one delivery.
2. A new method `FlashcardRepository.recordReview(...)` saves the review state and the log entry in one transaction. `saveReviewState` stays. The interface method has a default body (saves only) so fakes that were not supplied keep compiling; `FlashcardRepositoryImpl` overrides it.
3. History survives deleting a card, deck or item (no foreign key, locally and in the cloud).
4. **Again is not a category and is never logged. Only Hard, Good and Easy answers are logged.** A card rated Again is saved (its schedule still changes) but not logged; when it comes back in the same session, its answer (Hard, Good or Easy) is the entry that is logged. A card rated Hard, Good or Easy the first time is logged right away. Extra practice and Practice are never logged. If a session ends before an Again card is answered again, that card has no entry. So "Reviewed today", the streak and the rating split count answers, and the rating split shows three rows: Hard, Good, Easy.
5. Streak = consecutive days (device time zone) with at least one logged review; today with no review yet does not break it.
6. "Due today" = reviewed cards due by calendar day (same rule as the review queues); new cards are shown separately. The count is labelled "Flashcards" (one per deck membership).
7. Learning = interval under 21 days, Mature = 21 days or more. "Difficult" uses `ReviewQueueBuilder.DIFFICULT_EASE_BELOW` (so Stats always matches the deck screen).
8. Card placed between Account and Appearance, for guests and signed-in users. The guest vault records history but never syncs.
9. The rating split covers all time. The "N changes are waiting" sync text now also counts review-log rows.

### Database and migration impact
- Room **v5 -> v6**, `MIGRATION_5_6`: only creates the empty table `review_log` (`id`, `flashcardId`, `rating` = HARD/GOOD/EASY, `reviewedAt`, `intervalDaysBefore`, `intervalDaysAfter`) and an index on `reviewedAt`. No existing row is touched; no backfill (history starts when this version first runs). Building exports `app/schemas/.../6.json`: **commit it** (`ReviewLogMigrationTest` needs it).
- **Supabase:** replace `docs/supabase_sync_schema.sql` with the supplied file and **run it in the dev project first, then prod, before running the new app signed in.** It is safe to run again. It adds the table `review_logs` (RLS: read own rows only), its pull index, a "5b" insert-only step in `sync_push`, and a `review_logs` branch in `sync_pull`.
- No new dependency.

### Files created (under `app/src/main/java/com/mindvault/` unless noted)
| File | Purpose |
|---|---|
| `data/local/entity/ReviewLogEntity.kt` | The history row |
| `data/local/dao/ReviewLogDao.kt` | Insert-if-absent, get by ID |
| `data/local/dao/StatsDao.kt` | Read-only aggregate queries for stats |
| `data/local/relation/StatsRows.kt` | Row types for those queries |
| `data/repository/StatsRepositoryImpl.kt` | Combines the queries into `StatsRaw` |
| `domain/model/StatsModels.kt` | `StatsQuery`, `StatsRaw`, `ProgressStats`, `DayActivity`, ... |
| `domain/repository/StatsRepository.kt` | Contract |
| `domain/usecase/StatsBuilder.kt` | Pure logic: streak, 7-day activity, categories, query boundaries |
| `domain/usecase/ObserveProgressStatsUseCase.kt` | Use case |
| `feature/stats/StatsViewModel.kt`, `StatsScreen.kt` | Stats screen |
| `feature/profile/ProfileProgressCard.kt` | The Profile card (self-contained; delete its one call in `ProfileScreen` to hide it) |
| Tests (`app/src/test/java/com/mindvault/`) | `domain/usecase/StatsBuilderTest.kt`, `domain/usecase/RecordCardReviewLogTest.kt`, `data/sync/SyncReviewLogTest.kt` |
| Tests (`app/src/androidTest/java/com/mindvault/data/local/`) | `StatsDaoTest.kt`, `ReviewLogMigrationTest.kt` |

### Files modified (complete files supplied; only the lines below changed)
| File | Change |
|---|---|
| `data/local/database/MindVaultDatabase.kt` | version 6, `ReviewLogEntity`, `reviewLogDao()`, `statsDao()` |
| `data/local/database/Migrations.kt` | `MIGRATION_5_6`, added to `MINDVAULT_MIGRATIONS` |
| `data/local/entity/Syncoutboxentity.kt` | `SyncEntityType.REVIEW_LOG` |
| `data/sync/Syncmodels.kt` | `SyncTable.REVIEW_LOGS` |
| `data/sync/Syncwire.kt` | `REVIEW_LOG` upload data |
| `data/sync/Synclocalstore.kt`, `Roomsynclocalstore.kt` | `reviewLog(id)`, `insertReviewLog(entry)` |
| `data/sync/Syncengine.kt` | one more push phase (log, after review states) |
| `data/sync/Syncapplier.kt` | pulled log: insert if the ID is new, ignore otherwise |
| `domain/repository/FlashcardRepository.kt` | `recordReview` (with default body) |
| `data/repository/FlashcardRepositoryImpl.kt` | `recordReview`: state + log + both outbox entries in one transaction |
| `domain/usecase/Recordcardreviewusecase.kt` | Hard/Good/Easy (first rating or the answer after Again) call `recordReview`; Again only saves |
| `core/di/RepositoryModule.kt` | binds `StatsRepository` |
| `core/navigation/MindVaultDestination.kt`, `MindVaultNavHost.kt` | `Stats` route (no bottom bar); Profile gets `onViewStatsClick` |
| `feature/profile/ProfileScreen.kt` | new parameter `onViewStatsClick`; one inserted block between Account and Appearance |
| `docs/supabase_sync_schema.sql` | see above |
| Test `data/sync/Fakesynclocalstore.kt` | `logRows`, the two new methods |

### Known limits
- The streak is counted from the last 400 days of history, and starts the day this ships (no backfill).
- Numbers refresh on any data change or when the screen is reopened, not at midnight.
- Review history in the cloud is new user data: add it to the privacy policy and the Play data-safety form.
- If another file in your project has an exhaustive `when` over `SyncTable` (or implements `SyncLocalStore`), the compiler will point at it; the fix is one more branch / two methods.
- `recordReview` against a real database (atomicity, outbox entries) is not covered by an automated test yet; extend `OutboxRecordingTest.kt` (send it and I will).
- No Compose or ViewModel tests for the new screens.

### Likely compile-fix spots
`StatsScreen.kt` (icon imports `Icons.Filled.BarChart`, `Icons.AutoMirrored.Filled.KeyboardArrowRight`), `StatsDao.kt` (Room must accept the single-row multi-subselect query into `StatsCountsRow`), `ReviewLogMigrationTest.kt` (the `MigrationTestHelper` constructor your other migration tests use).

### Commands
```
gradlew.bat testDebugUnitTest
gradlew.bat assembleDebug
gradlew.bat connectedDebugAndroidTest
```
Record the real output.

### Manual checks
1. Install over the current build: Profile shows "Your progress" between Account and Appearance with Saved, Due today, Day streak (– for a moment, then numbers). Existing data is intact.
2. Tap the card: Stats opens, no bottom bar, Back returns to Profile. A brand-new vault shows "No stats yet".
3. Do a Daily review of 3 cards (rate one Again, then answer it Easy when it returns, and rate the other two Good and Hard): Stats shows "Reviewed today: 3", the Today bar is 3, the streak is 1, and "How reviews went" shows Hard 1, Good 1, Easy 1 (no Again row). Rate a card Again and leave the session before it returns: it adds nothing.
4. Delete a deck that has reviewed cards: the reviews, streak and rating split do not change.
5. Check Flashcards numbers: "Due today" equals the sum of due reviews on the deck screens (without new cards); Difficult matches the deck screen's Difficult count.
6. Signed in: Profile shows changes waiting, then Synced; in Supabase the `review_logs` rows exist. Sign in on a second device or reinstall: the streak and history come back.
7. Guest vault: reviews are logged and stats work; nothing syncs.
8. Dark mode: card, bars and legend are readable.

### Documentation to adjust
- `IMPLEMENTATION_STATUS.md`: add rows "Review history log" (Room v6, Supabase `review_logs`) and "Profile stats" (Written, unverified); §6 decisions above; §7 Next Task: UI overhaul / welcome / demo (your plan), Search language editions.
- `DESIGN_SYSTEM.md` §7.8: Profile gets a "Your progress" card (3 numbers + "View stats") between Account and Appearance; new §7.9 Stats screen: Overview tiles, Activity (7-day bars with counts), Flashcards (New / Learning / Mature stacked bar with legend), "How reviews went", Library. Meaning is always carried by text and numbers, not colour alone.
- `ARCHITECTURE.md` §6: `ReviewLog` (append-only, no foreign key); §5: stats flow `StatsDao -> StatsRepository -> StatsBuilder -> StatsViewModel`.
- Privacy policy and Play data-safety: review history is stored locally and, for signed-in users, in the cloud.

## Entry 23 — Fix failing unit tests

`testDebugUnitTest` reported 5 failures out of 346 tests; these fixes target all 5.

### Fixes
| Test(s) | Cause | File changed |
|---|---|---|
| `SaveSearchResultUseCaseTest` (3 tests), `SearchViewModelTest` line 302 | `SaveSearchResultUseCase` was still the V1 version: duplicate check ignored type and the saved type was hardcoded to WORD | `domain/usecase/SaveSearchResultUseCase.kt` (duplicate check is now title + type via `findByTitleAndType`; type comes from `SearchSaveContent.type`) |
| `QuickLookupParserTest.aQuotedLinkAloneIsIgnored` | Link regex swallowed the closing quote, leaving a stray opening quote that was returned instead of null | `domain/model/QuickLookup.kt` (link replacement keeps trailing quote characters so the wrapping-quotes rule still applies) |

No test files, schema, dependency or sync changes.

