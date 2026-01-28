# anotepad

Android-приложение на Kotlin 2.0 с Jetpack Compose.

## Стек
- Kotlin 2.0
- Android / Jetpack Compose
- Gradle (Kotlin DSL)

## Требования
- JDK 17
- Android Studio (рекомендуется)

## Архитектура
- MVVM на Jetpack Compose: экраны в `ui/*Screen.kt`, состояние и бизнес-логика во `ViewModel`.
- Навигация — Compose Navigation (`AppNav.kt`) с экранами: браузер файлов, редактор, поиск, шаблоны, настройки.
- Репозитории инкапсулируют доступ к данным: настройки и шаблоны через DataStore, файлы через SAF/DocumentFile.
- Зависимости собираются в `MainActivity` → `AppDependencies` → `AppViewModelFactory`.

## Структура проекта
- `app/` — основное Android‑приложение
  - `src/main/java/com/anotepad/`
    - `MainActivity.kt` — точка входа, установка темы/навигации
    - `AppNav.kt` — граф навигации Compose
    - `ui/` — Compose‑экраны и `ViewModel`
    - `data/` — модели и репозитории DataStore (настройки/шаблоны)
    - `file/` — работа с деревьями/файлами через SAF
  - `src/main/res/` — ресурсы (строки, темы)
- `legacy/` — архив старой версии проекта (исторический код)
- `build.gradle.kts`, `settings.gradle.kts` — конфигурация сборки

## Сборка
```bash
./gradlew assembleDebug
```

## Запуск на устройстве/эмуляторе
```bash
./gradlew installDebug
```
