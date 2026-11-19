# Gapwise

**Remember the last time. Know the next.**

Gapwise tracks how long it's been since you last did the things that don't run on a fixed schedule: a haircut, an oil change, calling home, washing the bedsheets. It learns your usual rhythm and gives you a gentle nudge around when something is usually due. There are no streaks and no guilt.

## Features

- **One-tap logging.** Tap a tile to log it, with undo. Long-press or tap ⓘ to open the details.
- **Learned rhythm.** After 3 logs it estimates your usual gap (for example, "Usually every 36 days") and when the next one is expected. One-off long gaps, like a trip, are ignored.
- **Gentle nudges.** A notification arrives around when something is usually due. You can log straight from it, and ignoring it is fine.
- **Multi-day events.** Periods, trips and sick days have a start tap and an end tap. Gapwise shows how long they usually last.
- **Insights.** Each event has a gap chart, a calendar and an editable history with notes. You can log a past day.
- **Organize.** Categories, pin, hide, pause, search, sorting (due soon, recent, name), and grid or list view.
- **Log from anywhere.** There's a home-screen widget (tap a row to log it) and launcher shortcuts for your top events.
- **Welcome tour.** Over 40 starter events, each seeded with an approximate last date.
- **Private.** No account, ads or tracking. JSON export and import for backups.

## Design

The app uses Material 3 Expressive. Each event's icon sits in a shape that shows where it is in its cycle: a circle when it's fresh, a scallop as it builds, a sun when it's around now, and a soft burst when it's past your usual. Logging morphs the shape back. Numbers are set in Bitter and the interface text in DM Sans. Both fonts are under the SIL Open Font License; see `licenses/`.

## Build

```
./gradlew :app:assembleDebug
```

Debug and CI builds are signed with the committed `app/debug.keystore` (standard debug passwords), so a new APK installs over the previous one.

Requires JDK 17 and the Android SDK (compileSdk 36, minSdk 26). The stack is Kotlin, Jetpack Compose with Material 3, Room, WorkManager and Glance.

## Latest build

Every push to `master` builds an APK and publishes it to the [`latest-master`](../../releases/tag/latest-master) release.
