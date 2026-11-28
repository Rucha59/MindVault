# MindVault — Architecture

## 1. Purpose

This document defines the intended technical structure for MindVault's Android MVP. It is a design contract, not evidence that the architecture has already been implemented. Update it when an intentional architectural decision changes.

Read this alongside `PROJECT_BRIEF.md`. The project brief defines product scope; this document defines how the code should be organized.

## 2. Technology Stack

Preferred stack:
- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- MVVM
- Kotlin Coroutines, Flow, and StateFlow
- Room / SQLite
- Hilt
- Retrofit and OkHttp for external Search
- Gradle Kotlin DSL
- JUnit and appropriate Android/Compose testing libraries

Rules:
- Inspect the repository's actual Gradle and version-catalog files before changing dependencies.
- Reuse existing versions and conventions where possible.
- Do not upgrade unrelated dependencies without a clear reason.
- Add a dependency only when a specific implementation requires it.
- Keep secrets out of the Android app. A provider requiring a private API key must be accessed through a backend.

## 3. Package Structure

Use a single Android application module initially unless the project has a demonstrated need for multiple Gradle modules. Organize code by feature and architectural responsibility.

Suggested package layout (adapt the root package to the actual application namespace):

```text
Refer to the given Tree structure of the project
```

The exact directory layout may differ if the existing project uses another consistent convention. Do not move files merely to match this example if doing so creates unnecessary churn.

### Feature package responsibilities

- `home`: dashboard UI and state.
- `search`: query input, external results, result details and save flow.
- `vault`: saved-item list, filters, sorting and category browsing.
- `knowledge`: add, detail, edit and delete flows.
- `flashcards`: deck selection, card display, review actions and review progress.
- `profile`: profile summary and settings.
- `core`: app-wide infrastructure and reusable UI.
- `data`: persistence, remote API access, mapping and repository implementations.
- `domain`: app concepts, repository contracts and business use cases.scheduler/ (spaced-repetition algorithm behind ReviewScheduler; Sm2Scheduler is the only implementation).
- Repositories reach Room only through ActiveDatabase; DAOs and the database are not injectable
Keep feature-specific UI and state inside the relevant feature package. Do not create a generic framework for hypothetical future features.

## 4. Architecture Boundaries

Preferred dependency direction:

```text
Feature UI (Compose)
        ↓
    ViewModel
        ↓
     Use Case
        ↓
Repository interface (domain)
        ↑
Repository implementation (data)
        ↓
Local / Remote data source
```

Responsibilities:

### Presentation
- Composables render state and emit user actions.
- ViewModels coordinate screen behavior and expose `StateFlow<UiState>` or another consistent observable state pattern.
- UI state should represent loading, content, empty, and error conditions where relevant.
- Composables must not access Room DAOs, Retrofit services, or repositories directly.
- Keep business rules out of Composables.

### Domain
- Contains domain models, repository interfaces, and use cases for meaningful business operations.
- Owns business rules such as validation rules that are not purely visual, deck membership rules, and flashcard review scheduling.
- Must not depend on Android UI classes, Compose, Room entities, or Retrofit DTOs.
- Avoid one-line use cases created solely to add layers without meaningful value; remain consistent with the established project pattern.

### Data
- Implements repository interfaces.
- Owns Room entities, DAOs, database configuration, API DTOs, remote services, and mappers.
- Converts persistence and network models to domain models.
- Isolates external API provider details from feature code.
- Exposes useful observable data through repositories.

### Core
- Holds shared theme, navigation infrastructure, reusable UI components, dependency injection setup, and small general utilities.
- Avoid placing feature-specific business logic in `core`.

## 5. Data Flow

### Reading saved knowledge

```text
Screen → ViewModel → Use Case → Repository → Room DAO → Room
Screen ← UiState ← ViewModel ← Flow of domain data
```

### Adding or editing knowledge

1. The user edits a form.
2. The screen sends an action to its ViewModel.
3. The ViewModel validates form state and invokes the relevant use case.
4. The use case calls the repository.
5. The repository persists through Room.
6. Observable queries emit updated data.
7. Home, Vault, and other screens reflect the change without maintaining separate copies.

### External Search and saving

1. Search ViewModel receives a query.
2. Search use case invokes the Search repository.
3. The remote data source requests the selected provider.
4. API DTOs are mapped to a stable domain search-result model.
5. The user reviews or edits the result.
6. Saving creates a normal local `KnowledgeItem` through the existing knowledge repository.
7. The Vault and Home observe the saved record through Room.

External search results are not automatically saved. Saving must be an explicit user action.

### Flashcard review
1. The user opens a deck and chooses Daily review, New cards or Difficult cards.
2. LoadReviewQueueUseCase reads the deck once and ReviewQueueBuilder selects the cards (due by calendar day, new cards within the daily limit, difficult by ease).
3. The UI shows the term; the user reveals the answer and rates it.
4. RecordCardReviewUseCase asks ReviewScheduler for the next state and the repository saves it (and queues it for sync) before the session moves on.
5. A card rated Again returns once at the end of the session and asks Hard, Good or Easy; that answer goes through relearn.
6. Practice uses StudySession and saves nothing.

## 6. Core Data Model Guidance

Use stable IDs and explicit relationships. The initial conceptual model is:

- `KnowledgeItem`: title, type, definition, personal notes, optional example, optional category ID, timestamps.
- `Category`: ID, name, creation timestamp.
- `Deck`: ID, name, creation and update timestamps.
- `Flashcard`: ID, knowledge item ID, deck ID, creation timestamp.
- `ReviewState`: flashcard ID, review status, last-reviewed timestamp, next-due timestamp and scheduling data.

Important decisions:
- One knowledge item can be reused across screens and referenced by flashcards.
- Avoid copying the definition and notes into each flashcard.
- Prevent duplicate membership for the same knowledge item in the same deck.
- Define foreign keys and indexes where appropriate.
- Use an explicit default deck strategy.
- Use a consistent timestamp representation and document it.
- Use migrations that preserve user data; never use destructive migration as a shortcut.
- Do not add tags, sync metadata, or other entities until the approved scope requires them.

If the existing implementation has a different approved schema, inspect it and document the difference before changing it.

## 7. Naming Conventions

Follow Kotlin conventions:
- Classes, interfaces, objects, and composables: `PascalCase`.
- Functions, properties, and local variables: `camelCase`.
- Constants: `UPPER_SNAKE_CASE` where appropriate.
- ViewModels: `<Feature>ViewModel`, e.g. `VaultViewModel`.
- Screens: `<Feature>Screen`, e.g. `VaultScreen`.
- UI state: `<Feature>UiState`, e.g. `VaultUiState`.
- Use cases: descriptive verb phrases ending in `UseCase`, e.g. `SaveKnowledgeItemUseCase`.
- Repository interfaces: `<Concept>Repository`.
- Repository implementations: `<Concept>RepositoryImpl`.
- Room entities: descriptive singular nouns, e.g. `KnowledgeItemEntity`.
- DAO types: `<Concept>Dao`.
- API DTOs: descriptive names ending in `Dto` when that distinction improves clarity.
- Prefer domain names that express the product concept rather than technical implementation details.

Use one naming pattern consistently; do not rename existing symbols without a clear reason.

## 8. State, Errors, and Coroutines

- Expose UI state through immutable state objects.
- Keep mutable state private to the ViewModel.
- Use `viewModelScope` for ViewModel-owned work.
- Collect flows with lifecycle-aware Compose APIs where available.
- Avoid blocking the main thread.
- Represent network, validation, and persistence errors in a user-actionable way.
- Do not swallow exceptions silently.
- Avoid duplicate requests caused by recomposition.
- Respect lifecycle cancellation and avoid retaining Activity or View references in ViewModels.
- Use a consistent approach to one-off UI events, navigation and snackbar messages.

## 9. Navigation and UI Rules

- Main destinations: Home, Search, Vault, Flashcards, Profile.
- Add Knowledge should be reachable from Home and Vault.
- Search results can open a detail/edit-before-save flow.
- Vault items open a detail view that supports editing and deletion.
- Settings should be accessible from Profile.
- Keep navigation route definitions centralized and typed or otherwise consistently defined.
- Avoid scattered hardcoded route strings.
- Use the shared theme and reusable components.
- Provide empty, loading, error, validation, and confirmation states where relevant.
- Do not introduce decorative controls that have no behavior.
- Preserve accessibility, readable contrast, keyboard handling, and back navigation.

## 10. Testing and Verification

Use the testing approach already configured in the repository. At minimum, test:
- Knowledge create, update, delete and retrieval.
- Category filtering and sorting.
- Mapping external Search results into domain models.
- Saving Search results and duplicate handling.
- Deck membership and flashcard retrieval.
- Review scheduling and persistence.
- ViewModel state for loading, success, empty and error cases where practical.

After a change:
1. Review the changed files.
2. Run relevant unit tests.
3. Run the project's build task, typically `gradlew.bat assembleDebug` on Windows.
4. Report exact commands and actual results.
5. Manually verify the affected app flow when UI behavior changes.

Do not report a test or build as successful unless the actual output is available.

## 11. Implementation Rules for AI Tools

AI tools may not have direct repository access. Therefore:
- Use only source files, package trees, Gradle files, logs, and screenshots supplied in the current task.
- Do not pretend to inspect files that were not provided.
- Ask for specific missing files rather than guessing.
- Before multi-file work, list the proposed file changes and why they are needed.
- Provide complete contents for new or modified files, or precise patches.
- Do not duplicate existing classes or invent dependencies without checking the supplied configuration.
- Do not rewrite unrelated working code.
- Keep each task scoped to one feature or architectural change.
- Include acceptance criteria and verification commands.
- Update `IMPLEMENTATION_STATUS.md` only using verified information.
- If a proposed change conflicts with this document or `PROJECT_BRIEF.md`, flag the conflict before making a broad change.

## 12. Deliberately Not Designed Yet

Do not build these unless explicitly requested:
- AI services or AI-generated content
- Cloud sync or multi-device conflict resolution
- Social features or collaboration
- Large bundled knowledge databases
- Accessibility-service-based text capture
- Multi-module Gradle restructuring without a demonstrated need

External Search is part of the MVP, but the selected provider must be documented when decided.



# MindVault — Architecture

## 1. Purpose

This document defines the intended technical structure for MindVault's Android MVP. It is a design contract, not evidence that the architecture has already been implemented. Update it when an intentional architectural decision changes.

Read this alongside `PROJECT_BRIEF.md`. The project brief defines product scope; this document defines how the code should be organized.

## 2. Technology Stack

Preferred stack:
- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- MVVM
- Kotlin Coroutines, Flow, and StateFlow
- Room / SQLite
- Hilt
- Retrofit and OkHttp for external Search
- Gradle Kotlin DSL
- JUnit and appropriate Android/Compose testing libraries

Rules:
- Inspect the repository's actual Gradle and version-catalog files before changing dependencies.
- Reuse existing versions and conventions where possible.
- Do not upgrade unrelated dependencies without a clear reason.
- Add a dependency only when a specific implementation requires it.
- Keep secrets out of the Android app. A provider requiring a private API key must be accessed through a backend.

## 3. Package Structure

Use a single Android application module initially unless the project has a demonstrated need for multiple Gradle modules. Organize code by feature and architectural responsibility.

Suggested package layout (adapt the root package to the actual application namespace):

```text
Refer to the given Tree structure of the project
```

The exact directory layout may differ if the existing project uses another consistent convention. Do not move files merely to match this example if doing so creates unnecessary churn.

### Feature package responsibilities

- `home`: dashboard UI and state.
- `search`: query, All / Words / Concepts filter, autocomplete and recent searches, ranked results from two sources, expandable result cards and the save flow.
- `vault`: saved-item list, filters, sorting and category browsing.
- `knowledge`: add, detail, edit and delete flows.
- `flashcards`: deck selection, card display, review actions and review progress.
- `profile`: profile summary and settings.
- `core`: app-wide infrastructure and reusable UI.
- `data`: persistence, remote API access, mapping and repository implementations.
- `domain`: app concepts, repository contracts and business use cases.scheduler/ (spaced-repetition algorithm behind ReviewScheduler; Sm2Scheduler is the only implementation).
- Repositories reach Room only through ActiveDatabase; DAOs and the database are not injectable
  Keep feature-specific UI and state inside the relevant feature package. Do not create a generic framework for hypothetical future features.
  app: also holds the quick-search entry points: QuickLookupActivity (no screen; receives selected / shared text and forwards it to MainActivity), QuickLookupIntents (reads the text from an intent) and QuickLookupViewModel (lets the navigation host see that a lookup is waiting).
  domain/model/QuickLookup.kt: QuickLookup and QuickLookupParser (pure cleaning rules).
  domain/usecase/QuickLookupInbox.kt: in-memory holder of the one pending lookup. It is a state holder rather than a use case; it sits with the use cases so Hilt can inject it without a repository, since nothing is persisted.

## 4. Architecture Boundaries

Preferred dependency direction:

```text
Feature UI (Compose)
        ↓
    ViewModel
        ↓
     Use Case
        ↓
Repository interface (domain)
        ↑
Repository implementation (data)
        ↓
Local / Remote data source
```

Responsibilities:

### Presentation
- Composables render state and emit user actions.
- ViewModels coordinate screen behavior and expose `StateFlow<UiState>` or another consistent observable state pattern.
- UI state should represent loading, content, empty, and error conditions where relevant.
- Composables must not access Room DAOs, Retrofit services, or repositories directly.
- Keep business rules out of Composables.

### Domain
- Contains domain models, repository interfaces, and use cases for meaningful business operations.
- Owns business rules such as validation rules that are not purely visual, deck membership rules, and flashcard review scheduling.
- Must not depend on Android UI classes, Compose, Room entities, or Retrofit DTOs.
- Avoid one-line use cases created solely to add layers without meaningful value; remain consistent with the established project pattern.

### Data
- Implements repository interfaces.
- Owns Room entities, DAOs, database configuration, API DTOs, remote services, and mappers.
- Converts persistence and network models to domain models.
- Isolates external API provider details from feature code.
- Exposes useful observable data through repositories.

### Core
- Holds shared theme, navigation infrastructure, reusable UI components, dependency injection setup, and small general utilities.
- Avoid placing feature-specific business logic in `core`.

## 5. Data Flow

### Reading saved knowledge

```text
Screen → ViewModel → Use Case → Repository → Room DAO → Room
Screen ← UiState ← ViewModel ← Flow of domain data
```

### Adding or editing knowledge

1. The user edits a form.
2. The screen sends an action to its ViewModel.
3. The ViewModel validates form state and invokes the relevant use case.
4. The use case calls the repository.
5. The repository persists through Room.
6. Observable queries emit updated data.
7. Home, Vault, and other screens reflect the change without maintaining separate copies.

### External Search and saving (Search V2)

Two sources: **words** from English Wiktionary and **concepts** from English Wikipedia. See `docs/SEARCH_PROVIDER.md`.

1. The Search ViewModel receives the query and the filter (All / Words / Concepts) and runs `SearchDefinitionsUseCase`, which asks every source the filter allows **at the same time** and returns one outcome per source.
2. `SearchRepository` (`WikimediaSearchRepository`) calls the source's data source. Failures become outcomes (`Offline`, `Busy` for HTTP 429/503, `Failure`), never exceptions, so one failing source does not hide the other's results. Successful answers are cached in memory for a few minutes.
3. Data sources (`WiktionaryDataSource`, `WikipediaDataSource`) call Retrofit APIs. API DTOs are mapped by `WiktionaryMapper` / `WikipediaMapper` to the domain `SearchResult`; DTOs never leave `data/remote/wikimedia`.
4. `RankSearchResultsUseCase` orders the combined results (exact, prefix, contains, rest; words before concepts inside a tier; duplicates removed). The ViewModel shows them with per-source notices and Retry.
5. A concept below the first two hits has no summary yet. `LoadConceptSummaryUseCase` loads it when the card is opened, saved or edited first; a concept without a summary is never saved.
6. The user reviews or edits the result ("Edit first" opens the Add form with the title, definition and type prefilled).
7. Saving creates a normal local `KnowledgeItem` (type WORD or CONCEPT) through `SaveKnowledgeItemUseCase` and the existing knowledge repository. The duplicate check is title plus type. The source name, licence and link are shown on the card only; they are not stored in the item.
8. The Vault, Home and Flashcards observe the saved record through Room.

Autocomplete: `SuggestSearchTermsUseCase` (after 2 characters and a 300 ms pause, cancelled by further typing) asks `SearchRepository.suggest`, which is best effort (a failure means no suggestions). Recent searches and the "Search suggestions" setting live in DataStore through `SettingsRepository`.

Wikimedia rules: every request carries a descriptive User-Agent (`UserAgentInterceptor`), an HTTP cache, back-off on 429/503 (`BackoffInterceptor`) and at most two requests in flight, all on the `@Named("wikimedia")` OkHttp client in `WikimediaModule`. The previous provider (Free Dictionary API, words only) remains as V1: bind `SearchRepositoryImpl` instead of `WikimediaSearchRepository` in `RepositoryModule` to switch back.

External search results are not automatically saved. Saving must be an explicit user action.

### Flashcard review
1. The user opens a deck and chooses Daily review, New cards or Difficult cards.
2. LoadReviewQueueUseCase reads the deck once and ReviewQueueBuilder selects the cards (due by calendar day, new cards within the daily limit, difficult by ease).
3. The UI shows the term; the user reveals the answer and rates it.
4. RecordCardReviewUseCase asks ReviewScheduler for the next state and the repository saves it (and queues it for sync) before the session moves on.
5. A card rated Again returns once at the end of the session and asks Hard, Good or Easy; that answer goes through relearn.
6. Practice uses StudySession and saves nothing.

## 6. Core Data Model Guidance

Use stable IDs and explicit relationships. The initial conceptual model is:

- `KnowledgeItem`: title, type, definition, personal notes, optional example, optional category ID, timestamps.
- `Category`: ID, name, creation timestamp.
- `Deck`: ID, name, creation and update timestamps.
- `Flashcard`: ID, knowledge item ID, deck ID, creation timestamp.
- `ReviewState`: flashcard ID, review status, last-reviewed timestamp, next-due timestamp and scheduling data.

  **Quick lookup (text sent from another app)**
 - The user selects a word and chooses MindVault in the selection menu, or shares text to MindVault.
 - QuickLookupActivity (exported, no UI, own empty task affinity, excluded from recents) reads the text with QuickLookupIntents, cleans it with QuickLookupParser and starts MainActivity with FLAG_ACTIVITY_NEW_TASK and a private extra, then closes. This is why MainActivity does not receive the selection intent directly: it would otherwise open inside the other app's task. If nothing is left after cleaning, a toast is shown and MindVault does not open.
 - MainActivity (onCreate on a fresh launch, onNewIntent otherwise) removes the extra and calls QuickLookupInbox.submit, which cleans the text again and stores one pending QuickLookup (a newer one replaces an unconsumed older one).
 - MindVaultNavHost observes the pending lookup's ID through QuickLookupViewModel and navigates to the Search tab like a bottom-bar tap. It waits while the Add, Add-from-search, Edit or sign-in screen is open (QuickLookupHoldRoutes) and acts once the user leaves it. It also depends on the active vault, so it still runs after an account-change reset to Home, and it does nothing until the navigation graph exists.
 - SearchScreen reacts to the pending lookup once it is on screen and calls SearchViewModel.onQuickLookup. That takes the lookup out of the inbox (consume, which succeeds for one caller only), resets the filter to All, fills the field and runs the normal Search V2 search.
 - While the shown search still belongs to the lookup, the screen offers Add “word” manually, which opens the existing Add-from-search route with the title filled in and an empty definition.
 -Rules: the lookup is never stored; the cleaned text is the only thing that leaves the device, and only through the normal Search request. SearchViewModel reads QuickLookupInbox directly (an exception to "ViewModel → use case", because the inbox is a stateless in-memory holder with no repository behind it).

Important decisions:
- One knowledge item can be reused across screens and referenced by flashcards.
- Avoid copying the definition and notes into each flashcard.
- Prevent duplicate membership for the same knowledge item in the same deck.
- Define foreign keys and indexes where appropriate.
- Use an explicit default deck strategy.
- Use a consistent timestamp representation and document it.
- Use migrations that preserve user data; never use destructive migration as a shortcut.
- Do not add tags, sync metadata, or other entities until the approved scope requires them.

If the existing implementation has a different approved schema, inspect it and document the difference before changing it.

## 7. Naming Conventions

Follow Kotlin conventions:
- Classes, interfaces, objects, and composables: `PascalCase`.
- Functions, properties, and local variables: `camelCase`.
- Constants: `UPPER_SNAKE_CASE` where appropriate.
- ViewModels: `<Feature>ViewModel`, e.g. `VaultViewModel`.
- Screens: `<Feature>Screen`, e.g. `VaultScreen`.
- UI state: `<Feature>UiState`, e.g. `VaultUiState`.
- Use cases: descriptive verb phrases ending in `UseCase`, e.g. `SaveKnowledgeItemUseCase`.
- Repository interfaces: `<Concept>Repository`.
- Repository implementations: `<Concept>RepositoryImpl`.
- Room entities: descriptive singular nouns, e.g. `KnowledgeItemEntity`.
- DAO types: `<Concept>Dao`.
- API DTOs: descriptive names ending in `Dto` when that distinction improves clarity.
- Prefer domain names that express the product concept rather than technical implementation details.

Use one naming pattern consistently; do not rename existing symbols without a clear reason.

## 8. State, Errors, and Coroutines

- Expose UI state through immutable state objects.
- Keep mutable state private to the ViewModel.
- Use `viewModelScope` for ViewModel-owned work.
- Collect flows with lifecycle-aware Compose APIs where available.
- Avoid blocking the main thread.
- Represent network, validation, and persistence errors in a user-actionable way.
- Do not swallow exceptions silently.
- Avoid duplicate requests caused by recomposition.
- Respect lifecycle cancellation and avoid retaining Activity or View references in ViewModels.
- Use a consistent approach to one-off UI events, navigation and snackbar messages.

## 9. Navigation and UI Rules

- Main destinations: Home, Search, Vault, Flashcards, Profile.
- Add Knowledge should be reachable from Home and Vault.
- Search results can open the Add form prefilled with the result (title, definition, type) to edit before saving.
- Vault items open a detail view that supports editing and deletion.
- Settings should be accessible from Profile.
- Keep navigation route definitions centralized and typed or otherwise consistently defined.
- Avoid scattered hardcoded route strings.
- Use the shared theme and reusable components.
- Provide empty, loading, error, validation, and confirmation states where relevant.
- Do not introduce decorative controls that have no behavior.
- Preserve accessibility, readable contrast, keyboard handling, and back navigation.
-  A quick lookup opens the Search tab through the same navigateToMain as the bottom bar, so other tabs keep their saved state.
-  Work in progress is never discarded by a lookup: forms and the sign-in screen hold it back.
## 10. Testing and Verification

Use the testing approach already configured in the repository. At minimum, test:
- Knowledge create, update, delete and retrieval.
- Category filtering and sorting.
- Mapping external Search results (Wiktionary, Wikipedia) into domain models, using real sample responses.
- Ranking, merging and de-duplicating results; partial failure of one source.
- Saving Search results and the title-plus-type duplicate rule.
- Autocomplete debounce and recent searches.
- Deck membership and flashcard retrieval.
- Review scheduling and persistence.
- ViewModel state for loading, success, empty and error cases where practical.
- Lookup cleaning (links, quotes, whitespace, length, surrogate pairs) and the inbox (replace, consume once, stale ID).
- SearchViewModel.onQuickLookup: filter reset, runs once, stale lookup ignored, "Add manually" state, offline behaviour, recent searches.
- Not covered by automated tests: QuickLookupActivity, the manifest filters, MainActivity intent handling and the NavHost effect (device checks listed in Entry 22).

After a change:
1. Review the changed files.
2. Run relevant unit tests.
3. Run the project's build task, typically `gradlew.bat assembleDebug` on Windows.
4. Report exact commands and actual results.
5. Manually verify the affected app flow when UI behavior changes.

Do not report a test or build as successful unless the actual output is available.

## 11. Implementation Rules for AI Tools

AI tools may not have direct repository access. Therefore:
- Use only source files, package trees, Gradle files, logs, and screenshots supplied in the current task.
- Do not pretend to inspect files that were not provided.
- Ask for specific missing files rather than guessing.
- Before multi-file work, list the proposed file changes and why they are needed.
- Provide complete contents for new or modified files, or precise patches.
- Do not duplicate existing classes or invent dependencies without checking the supplied configuration.
- Do not rewrite unrelated working code.
- Keep each task scoped to one feature or architectural change.
- Include acceptance criteria and verification commands.
- Update `IMPLEMENTATION_STATUS.md` only using verified information.
- If a proposed change conflicts with this document or `PROJECT_BRIEF.md`, flag the conflict before making a broad change.

## 12. Deliberately Not Designed Yet

Do not build these unless explicitly requested:
- AI services or AI-generated content
- Cloud sync or multi-device conflict resolution
- Social features or collaboration
- Large bundled knowledge databases
- Accessibility-service-based text capture
- Multi-module Gradle restructuring without a demonstrated need
  iOS Share / action extension for quick search.
  Quick search from other places than text selection and Share (overlay, floating button, accessibility-service capture stay out of scope).
External Search is part of the MVP. The selected providers (Wiktionary and Wikipedia) are documented in `docs/SEARCH_PROVIDER.md`.