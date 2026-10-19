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