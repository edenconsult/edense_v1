# Edense

Edense is an Android app for teachers to manage a weekly timetable, log teaching
sessions, track academic tasks, and receive class and task reminders. It is built
with Kotlin, Jetpack Compose, Room, and Firebase Authentication/Cloud Firestore.

## Requirements

- Android Studio with Android SDK 36 installed
- JDK 11 or newer
- An Android device or emulator running Android 7.0 (API 24) or newer

The Android application ID is `com.ecreativesys.edense`.

## Build

On Windows, from the repository root:

```powershell
.\gradlew.bat :app:assembleDebug
```

The debug APK is written to:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Run the unit tests and Android lint checks with:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
```

To install the debug build on a connected device:

```powershell
.\gradlew.bat :app:installDebug
```

## Firebase setup

Firebase configuration is project-specific. Register an Android app in the
Firebase project using the application ID above, enable Google sign-in, and add
the required Android signing certificate fingerprints in Firebase. Provide the
matching `google-services.json` in the app module before building with Firebase
features enabled.

Firestore uses the `(default)` database. The repository's owner-scoped rules are
in [`firestore.rules`](./firestore.rules), with deployment configuration in
[`firebase.json`](./firebase.json). Review and deploy the rules to the intended
Firebase project before using cloud sync. Do not put private keys, service
account credentials, or signing passwords in source control.

## Reminders

Class and task reminders are scheduled by Android alarms, so Edense does not
need to keep a continuous background process running. Allow notifications, and
grant exact-alarm access where the device requires it for on-time reminders.
Android force-stop can prevent alarms until the app is opened again.

Lesson logs are linked to a scheduled period on the selected date and can be
saved after the period has ended. Tasks may optionally link their reminder to
the next occurrence of a selected class period.

## Project layout

- `app/src/main/java`: Android application, data layer, scheduling, and UI
- `app/src/main/res`: Android resources, including launcher icons
- `app/src/test/java`: JVM unit tests
- `firestore.rules`: owner-scoped Firestore security rules
