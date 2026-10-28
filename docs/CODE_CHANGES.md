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