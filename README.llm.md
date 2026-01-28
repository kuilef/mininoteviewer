# LLM Notes

## Project snapshot
- Android app module: `app`
- Language: Kotlin 2.0
- UI: Jetpack Compose
- Build: Gradle Kotlin DSL

## Architecture
- MVVM: Compose UI in `ui/*Screen.kt`, state/logic in `*ViewModel`.
- Navigation: Compose Navigation in `AppNav.kt` (browser, editor, search, templates, settings).
- Data layer: DataStore repositories (`PreferencesRepository`, `TemplateRepository`), file access via SAF in `FileRepository`.
- Dependency wiring: `MainActivity` → `AppDependencies` → `AppViewModelFactory`.

## Project structure
- `app/src/main/java/com/anotepad/`
  - `ui/` — Compose screens + ViewModels
  - `data/` — DataStore models/repos
  - `file/` — SAF file operations
- `app/src/main/res/` — strings/themes
- `legacy/` — archived legacy project sources

## Common commands
```bash
./gradlew assembleDebug
./gradlew installDebug
```
