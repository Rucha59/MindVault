# MindVault

An Android app for capturing, organizing and remembering words and concepts you learn. Look things up, save them with your own notes, organize them in a personal Vault, and revise with flashcards.

> **Status: early development.** The UI shell runs. The local database layer is written but not yet verified or connected to any screen. See [Current status](#current-status).

## Tech stack

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · MVVM · Hilt · Room (SQLite) · Coroutines/Flow · Gradle Kotlin DSL

## Requirements

- Android Studio Rabbit 1 (2026.2.1) or newer
- JDK 17 (bundled with Android Studio)
- Android emulator or device, API 26+

## Setup

1. Clone the repo and open the project folder in Android Studio.
2. Let Gradle sync finish (**File → Sync Project with Gradle Files** if it doesn't start).
3. Run the app on an emulator, or from a terminal:
```
   gradlew.bat assembleDebug
   gradlew.bat installDebug
```
4. Tests:
```
   gradlew.bat testDebugUnitTest          # JVM unit tests
   gradlew.bat connectedDebugAndroidTest  # database tests, needs a running emulator
```

No API keys are needed yet.

## Current status

| Area | State |
|---|---|
| App shell: theme (light/dark), navigation, shared components | Working, verified on emulator |
| Screens (Home, Search, Vault, Flashcards, Profile, Add) | Placeholders showing empty states |
| Room database, domain models, repositories, Hilt wiring | Written, **not yet built or tested** |
| Add/Edit form, Vault list, Search, flashcard review | Not started |

The app does not save or show any data yet. Detailed status: `docs/IMPLEMENTATION_STATUS.md`.

## What the data layer covers (Phase 2)

- **KnowledgeItem**: title, type (word / concept / other), definition, personal notes, example, category.
- **Category**, **Deck** (with a protected default deck), **Flashcard** (references a knowledge item, never copies it; one card per item per deck), **ReviewState** (review history and next due time).
- UUID string IDs, UTC epoch-millisecond timestamps, foreign keys with cascade rules, exported schema, and no destructive migrations.

## Project structure

```
app/src/main/java/com/mindvault/
├── app/        Application and MainActivity
├── core/       theme, navigation, shared UI, DI modules, utilities
├── domain/     models, repository interfaces, errors (plain Kotlin)
├── data/       Room entities, DAOs, database, mappers, repository implementations
└── feature/    home, search, vault, knowledge, flashcards, profile screens
```

Flow: `Compose UI → ViewModel → Use Case → Repository → Data source`.

## Docs

| File | Purpose |
|---|---|
| `docs/PROJECT_BRIEF.md` | Product scope and MVP |
| `docs/ARCHITECTURE.md` | Code structure and rules |
| `docs/DESIGN_SYSTEM.md` | Colors, type, components |
| `docs/IMPLEMENTATION_STATUS.md` | What is verified and what is not |
| `docs/CODE_CHANGES.md` | Log of every code change |

## Notes

- `app/schemas/` holds Room's exported schema history. Commit it; it is needed for migrations.
- Never commit keystores, `local.properties` or API secrets.
