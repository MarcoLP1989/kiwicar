# KiwiCar

KiwiCar is a small Android app for keeping track of a car's **fuel fill-ups** and **oil top-ups**, and for seeing at a glance how much the car consumes and how much it costs to run.

It works fully **offline**: there is no account, no cloud and no network access. All data stays on the phone. The user interface is in Italian.

## Features

- **Log entries** — record a fuel fill-up or an oil top-up with date, odometer reading (km) and liters. Oil has quick choices (0.5 L, 1 L, other).
- **Fuel consumption** in L/100 km, calculated full tank to full tank: liters of a fill-up ÷ km driven since the previous fill-up × 100.
- **Oil consumption** in L/1,000 km: liters added ÷ km driven since the previous top-up × 1,000.
- **Average fuel cost** per month and per year, based on the whole history and a configurable price per liter.
- **Charts** for oil top-ups, fuel consumption, oil consumption and monthly fuel cost, with a last-month / last-year selector. Tapping a point opens the entry details, where it can be edited or deleted.
- **History** of all entries, with edit and delete.
- **Settings** for car name, starting odometer, fuel and oil prices, plus **CSV export** as a backup.

### How the numbers are calculated

- The "previous" entry used for consumption is the one of the same type with the **closest lower odometer reading**, not the last one entered. Entries added out of order therefore stay consistent.
- The first entry of each type has no consumption (shown as `—`).
- Costs are never stored. They are always liters × the price currently set in Settings, so changing a price updates every cost shown.
- Editing or deleting an entry recalculates the next entry of the same type.

## Tech stack

- Kotlin, Jetpack Compose (Material 3), dark theme
- Room (SQLite) for entries, DataStore Preferences for settings
- MVVM with Kotlin Coroutines / Flow
- JUnit 4 and Robolectric for unit tests
- Gradle (Kotlin DSL) with Android Gradle Plugin 9; `minSdk` 26, `targetSdk` 35

## Building

Requirements: the Android SDK (with platform 36.1) and a JDK. The Gradle wrapper selects the JDK for the build daemon (see `gradle/gradle-daemon-jvm.properties`).

Point Gradle to the SDK with a `local.properties` file in the project root (not committed):

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

Run the unit tests:

```bash
./gradlew testDebugUnitTest
```

Build the installable APK:

```bash
./gradlew assembleRelease
```

The APK is copied to `app/build/outputs/kiwicar/kiwicar-<versionName>.apk` (debug builds produce `kiwicar-<versionName>-debug.apk`). The version comes from `versionName` in `app/build.gradle.kts`.

## Installing

KiwiCar is distributed as an APK for sideloading, not through the Play Store. Copy the APK to the phone and open it, allowing installation from unknown sources if asked.

The release build is signed with the local debug keystore. Updates can only be installed over an existing installation if they are signed with the **same key**, so keep a backup of that keystore. Otherwise the app has to be uninstalled first, which deletes its data.

## Data and backup

- Entries are stored in a local Room database; settings in DataStore.
- **Settings → Esporta CSV** shares a CSV file (`tipo;data;km;litri`, Italian decimal commas) that can be saved to Files, Drive or email. There is no CSV import yet.
- Android Auto Backup is enabled, so app data can be restored with the phone's own backup.

## Project structure

```
app/src/main/java/com/kiwicar/
├── data/        Room entities, DAO, database, settings store, repository
├── domain/      Pure consumption and cost calculations
├── export/      CSV exporter
└── ui/          Compose screens (home, entry, charts, history, settings),
                 shared components, formatting helpers and theme
```
