# MindVault — Project Brief

## 1. Product Overview

**MindVault** is an Android app for capturing, organizing, and remembering words and concepts a person learns.

The central idea is a personal knowledge vault: users can look up a word or concept, save it, add their own notes, organize it, and revisit it through flashcards.

**Product principle:** Save knowledge once, reuse it throughout the app, and make it easy to find and remember.

## 2. Core Features

The MVP is centered on four core features: **Search, Add, Vault, and Flashcards**.

### 2.1 Search

- Let users search for words and concepts and view available definitions and relevant details.
- Use an external definition API initially, behind an isolated integration so the provider can be changed later.
- Show loading, empty-result, and error states.
- Let users review a result and save it directly to the Vault.
- Allow users to edit the result before saving it.
- Indicate when a result is already saved and avoid accidental duplicate records.
- Do not invent definitions when the source has no result.
- External sources may have uneven coverage: dictionary APIs often cover ordinary words better than specialist or academic concepts.

### 2.2 Add

Users can add knowledge manually or from Search.

A knowledge item should support:
- Title
- Type: `WORD`, `CONCEPT`, or `OTHER`
- Definition
- Personal notes
- Example (optional)
- Category (optional)
- Created and updated timestamps

Requirements:
- Validate required fields.
- Support creating, viewing, editing, and deleting items.
- Manual entry must continue to work when Search recommendations are disabled in Settings.
- When a Search result is saved, reuse the same knowledge-item model as manual entry.
- Saving and editing must persist across app restarts.

### 2.3 Vault

The Vault is the user's library of saved knowledge.

- Display saved items in a list.
- Open an item to view its definition and personal notes.
- Allow users to edit an item and delete it with confirmation.
- Filter by category and item type.
- Sort by recently added, recently updated, A–Z, and Z–A.
- Provide search within saved knowledge.
- Provide a clear empty state and an Add action.
- Home, Search, Vault, and Flashcards must reuse the same saved knowledge records rather than maintain separate copies.

### 2.4 Flashcards

- Let users add a saved knowledge item to a flashcard deck.
- Provide a default deck and support custom decks.
- When adding an item to flashcards, let the user choose a deck; select the default deck initially.
- Prevent the same knowledge item from being added more than once to the same deck.
- Show the concept title or keyword on the front of the card.
- Reveal the definition and personal notes on the back.
- Support a review flow and track whether cards have been reviewed and when they are next due.
- If spaced repetition is implemented, keep scheduling logic outside the UI layer.
- Show deck counts, unreviewed cards, and cards due for review when this data is available.
- Flashcards should reference saved knowledge instead of duplicating its definition and notes.

## 3. Main App Areas

1. **Home:** Category counts, recently added knowledge, and flashcard review status, with shortcuts to Search and Add.
2. **Search:** Look up a word or concept and save the result.
3. **Vault:** Browse, filter, sort, view, edit, and delete saved knowledge.
4. **Add / Edit Knowledge:** Create or update an item manually, or review a Search result before saving.
5. **Flashcards:** Choose a deck, study cards, and track review state.
6. **Profile and Settings:** Basic preferences, including theme and whether Search recommendations appear during manual entry.

Landing and authentication screens may be added separately. They are not prerequisites for the local-first core MVP.

## 4. Technology Stack

Use the following stack unless an explicit project decision changes it:

- **Platform:** Android
- **Language:** Kotlin
- **UI:** Jetpack Compose and Material 3
- **Navigation:** Navigation Compose
- **Architecture:** MVVM with clear presentation, domain, and data boundaries
- **Local persistence:** Room / SQLite
- **Asynchronous work and observable state:** Kotlin Coroutines, Flow, and StateFlow
- **Dependency injection:** Hilt
- **Networking:** Retrofit and OkHttp for external Search when needed
- **Build:** Gradle Kotlin DSL
- **Tests:** Unit tests for domain logic, repositories, mappings, and review scheduling; add UI/database tests where appropriate
- **Version control:** Git

Do not add dependencies without a clear need. Follow the versions and conventions already present in the project rather than arbitrarily upgrading them.

## 5. Architecture and Data Rules

Preferred flow:

`Compose UI → ViewModel → Use Case → Repository → Data Source`

- Composables must not call DAOs or networking clients directly.
- Keep business logic out of Composables.
- Room is the local source of truth for saved knowledge and review state.
- Use repositories to isolate local persistence from remote Search providers.
- Keep external API models separate from domain and database models.
- Use stable IDs, timestamps, appropriate foreign keys, and indexes.
- Model relationships explicitly; do not store relational data as an unstructured JSON blob.
- A saved knowledge item must be represented once and reused across the app.
- Use observable data flows so the UI updates after saves, edits, and deletes.
- Handle loading, empty, validation, and error states intentionally.
- Avoid destructive database migrations. Preserve user data when the schema changes.
- Keep external API credentials out of Android source code. If a provider requires a secret key, use a backend rather than embedding the secret in the app.

## 6. Design Direction

MindVault should feel like a **personal library and knowledge vault**: modern, calm, intelligent, and slightly playful.

- Use a restrained green palette with complementary green shades and warm neutral surfaces.
- Maintain readable contrast; do not make the interface excessively pale or overwhelmingly green.
- Use clear typography, consistent spacing, reusable components, and purposeful rounded surfaces.
- Avoid wrapping every section in a card; vary the visual hierarchy.
- Keep the core navigation simple: Home, Search, Vault, Flashcards, and Profile.
- Make Add easy to access from Home and Vault.
- Support light, dark, and system theme preferences if implemented.
- Design for Android phone layouts, keyboard use, accessibility, and sensible empty/loading/error states.
- Prefer consistent, functional UI over decorative effects.

Follow any more detailed, approved design-system document or provided screenshots when available.

## 7. Constraints for AI Coding Tools

AI tools may not have direct access to the repository. Therefore:

- Use only source files, project trees, build files, logs, and screenshots supplied in the current task.
- Do not claim to have inspected files that were not provided.
- Do not assume a class, dependency, route, or file exists unless it is shown or explicitly confirmed.
- Before a multi-file change, identify the relevant files and propose the smallest necessary change set.
- Provide complete contents for new or modified files, or clearly marked patches that can be applied unambiguously.
- Preserve existing naming, architecture, design, and working behavior.
- Do not rewrite unrelated code or duplicate existing implementations.
- If required context is missing, ask for the specific missing file or information instead of guessing.
- Include build/test commands and concrete acceptance checks.
- Never claim a build or test passed unless its actual output confirms success.
- After each phase, record changed files, completed work, known issues, and the next step in the project status notes.
- Prefer small, independently verifiable changes over large all-at-once implementations.

## 8. MVP Scope

### In scope

- Home dashboard
- Search and saving results
- Manual add, view, edit, and delete
- Room-backed local persistence
- Vault list, categories, filtering, and sorting
- Flashcards, decks, and basic review-state tracking
- Basic Profile and Settings
- Theme and Search-recommendation preferences

### Deliberately out of scope for the initial MVP

Unless explicitly requested in a later task, do not implement:
- AI-generated definitions, explanations, summaries, or flashcards
- A custom AI backend
- Authentication as a prerequisite for using the local app
- Cloud synchronization or multi-device conflict resolution
- Social features, sharing between users, or collaboration
- Payments, subscriptions, or advertisements
- Accessibility-service-based text capture from other apps
- Complex gamification, leaderboards, or achievements
- Large bundled encyclopedic datasets
- Unrequested features or architectural rewrites

External Search is part of the MVP; AI-powered Search is not.

## 9. MVP Acceptance Criteria

The MVP is ready for an initial release candidate when:

- The app builds and launches.
- Users can manually create, view, edit, and delete knowledge items.
- Knowledge and categories persist after restarting the app.
- Users can browse, filter, and sort the Vault.
- Users can search an external source, inspect a result, and save it to the Vault.
- Saved items appear consistently in Home, Vault, and Flashcards.
- Users can create/select a deck, add saved items, reveal card answers, and retain review state.
- Basic settings work and persist.
- Empty states and common errors are handled.
- Core behavior is covered by relevant tests.
- No success is reported for a feature that has not been verified.

## 10. Instruction to AI Assistants

Treat this document as the baseline product brief. Follow the existing repository and any more specific approved architecture or design documents. If instructions conflict, do not silently make a broad change: identify the conflict and ask for clarification. Implement only the requested phase, keep changes small, and verify the result using the evidence available.
