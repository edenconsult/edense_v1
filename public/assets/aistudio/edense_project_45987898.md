# Edense --- Project Master Plan and Implementation-Ready Technical Blueprint

**Company:** Eden Creative Systems (formerly Eden Creative Consult)\
**Product:** Edense\
**Document version:** 1.0\
**Date:** 24 October 2026\
**Platform:** Android first (Kotlin, Jetpack Compose); iOS may be
considered later\
**Tagline:** *Make sense of your teaching day.*

------------------------------------------------------------------------

## 1. Executive summary and vision

Edense is a mobile application designed for Senior High School (SHS)
teachers in Ghana. It addresses the cognitive load of remembering
timetables, tracking what was taught, planning the next topic, and
managing assignments and deadlines.

The central value proposition is that Edense is not just a digital
timetable; it is a **Teaching State Tracker**. It helps a teacher know
what class is happening, what comes next, where teaching stopped, what
remains unfinished, and which tasks need attention.

The product should prioritise speed, reliability, simple English, and
practical use in staff rooms and classrooms where internet access may be
unreliable.

### Core user promise

When a teacher opens Edense, the app should answer:

1.  What class am I teaching now?
2.  What class comes next, and when does it start?
3.  What did I cover the last time I taught this subject/class?
4.  What should I teach next?
5.  Which assignments, tests, projects, or deadlines need attention?

## 2. Brand identity

-   **Name:** Edense
-   **Company:** Eden Creative Systems (formerly Eden Creative Consult)
-   **Tagline:** "Make sense of your teaching day."
-   **Primary colours:** Deep navy blue, green, and a restrained yellow
    accent.
-   **Brand meanings:** Navy for trust and structure; green for growth
    and education; yellow for highlights and alerts.
-   **Platform:** Android first, with a possible future iOS version.
-   **Visual style:** Professional, calm, accessible, modern, and not
    overly flashy.

Preserve the approved Eden Creative Systems logo exactly if a final
asset exists. Do not distort, recolour, crop, or redraw it without
approval.

## 3. Ghanaian SHS context and design constraints

### Offline-first operation

Internet access may be unreliable in staff rooms and classrooms. Core
functions must work without internet, including viewing the timetable,
checking the current and next class, recording a session, and managing
tasks.

Room is the authoritative local database. Firestore is the cloud
synchronisation and backup layer. The UI should not read from Room for
some screens and directly from Firestore for others, because that can
produce inconsistent results.

### Double periods

Core subjects may occupy continuous blocks, for example 7:00 AM--9:00
AM. A double period must be treated as one continuous class block for
the home screen and reminders. It must not create a false "next class"
at the middle of the block.

### School operating hours

The teacher can configure school start and end times. These settings
should guide contextual greetings and default reminder behaviour, but
must not prevent the teacher from viewing or editing information outside
school hours.

### Curriculum alignment

Future AI features must be aligned with the relevant GES/NaCCA
curriculum and should not invent curriculum requirements. AI-generated
suggestions must remain suggestions and must not silently overwrite a
teacher's own plan.

### Practical user experience

-   Minimise the number of taps needed to log a lesson.
-   Use readable text, accessible touch targets, and simple language.
-   Avoid unnecessary animations and data-heavy screens.
-   Make offline and sync status understandable without obstructing the
    workflow.
-   Support smaller Android phones.

## 4. MVP scope

### Include in the initial MVP

-   Teacher profile and school hours
-   Subject and class management
-   Weekly timetable
-   Single and double periods
-   Current class and next class
-   Countdown to the next class
-   Session logging and unfinished-topic tracking
-   Assignment, test, examination, and project tracking
-   Local notifications
-   Room local database and offline operation
-   Firebase Authentication and cloud synchronisation after core local
    workflows are stable

### Defer to later phases

-   AI lesson planning and AI carry-forward suggestions
-   Advanced voice-to-text
-   Advanced reporting
-   School-wide collaboration and shared staff data
-   Student attendance, grading, and full school administration
-   iOS application

**Scope decision:** Version 1 should serve an individual teacher well.
Do not turn the first release into a full school management system.

## 5. Technical architecture

### Recommended stack

  -----------------------------------------------------------------------
  Component               Technology              Responsibility
  ----------------------- ----------------------- -----------------------
  Language                Kotlin                  Android application
                                                  logic

  UI                      Jetpack Compose +       Modern Android
                          Material 3              interface

  Navigation              Navigation Compose      Screen navigation

  Local database          Room                    Reliable offline
                                                  persistence

  Preferences             DataStore               Simple settings and
                                                  onboarding state

  Dependency injection    Hilt                    Wiring databases,
                                                  repositories, and
                                                  services

  Background work         WorkManager             Deferred work and sync
                                                  retries

  Time-sensitive          AlarmManager, where     Class and task alarms
  reminders               permitted               

  Authentication          Firebase Authentication User identity

  Cloud database          Cloud Firestore         Remote persistence and
                                                  multi-device sync

  Testing                 JUnit, coroutine        Unit, integration, and
                          testing, Compose UI     UI verification
                          tests                   

  Cloud AI (future)       Firebase Cloud          Securely mediated AI
                          Functions or another    API requests
                          trusted server          
  -----------------------------------------------------------------------

Start with one Android application module. Organise code by
responsibility without introducing unnecessary multi-module complexity
at the beginning.

### Architecture overview

``` text
Jetpack Compose UI
    Home / Timetable / Sessions / Tasks / Settings
                    |
                    v
ViewModels + Domain Logic
    Current class / Next class / Session state / Task rules
                    |
                    v
Repositories + Room
    Local source of truth / Immediate writes / Pending sync queue
             |                         |
             v                         v
     Firebase Auth + Firestore   Android system services
     Cloud synchronisation       AlarmManager / WorkManager
```

### Offline-first rules

1.  Every screen observes Room.
2.  Every teacher action saves to Room first.
3.  A pending-operation queue records changes that require cloud
    synchronisation.
4.  WorkManager uploads pending changes when connectivity returns.
5.  Remote changes are validated and merged into Room.
6.  The UI indicates whether data is pending, synced, or needs
    attention.
7.  Synchronisation must be idempotent: retrying an operation must not
    create duplicate records.

Firestore has its own Android offline cache and write queue. However,
Edense deliberately uses Room as the authoritative local store. Avoid
mixing direct Firestore reads with Room-backed screens. Explicit
synchronisation requires more implementation but gives the app more
control over offline behaviour and conflict handling.

## 6. Recommended project structure

Package names are illustrative; adjust them to the final application ID.

``` text
EdenseProject/
├── app/
│   ├── src/main/
│   │   ├── java/com/edensys/edense/
│   │   │   ├── EdenseApp.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── core/
│   │   │   │   ├── common/
│   │   │   │   ├── time/
│   │   │   │   ├── notifications/
│   │   │   │   └── sync/
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── dao/
│   │   │   │   │   ├── entity/
│   │   │   │   │   └── EdenseDatabase.kt
│   │   │   │   ├── remote/
│   │   │   │   ├── repository/
│   │   │   │   └── sync/
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   └── usecase/
│   │   │   ├── feature/
│   │   │   │   ├── onboarding/
│   │   │   │   ├── home/
│   │   │   │   ├── timetable/
│   │   │   │   ├── sessions/
│   │   │   │   ├── tasks/
│   │   │   │   └── settings/
│   │   │   └── ui/theme/
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   ├── src/test/
│   └── src/androidTest/
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

Keep generated configuration files and dependency versions under version
control. Never commit API secrets or service-account private keys.

## 7. Database and data model

The original conceptual schema includes users, subjects, periods,
sessions, and tasks. For a production-minded implementation, add stable
IDs, timestamps, deletion markers, and sync metadata.

### TeacherProfile

Suggested fields:

-   `userId` --- primary key and Firebase UID
-   `name`
-   `email`
-   `schoolName`
-   `schoolStartTime`
-   `schoolEndTime`
-   `timeZoneId`
-   `createdAt`
-   `updatedAt`

### Subject

Suggested fields:

-   `subjectId` --- stable UUID
-   `userId`
-   `name`
-   `className` (e.g. SHS 2B)
-   `colorCode`
-   `isArchived`
-   `createdAt`
-   `updatedAt`
-   `deletedAt` (nullable deletion marker, if using tombstones)

### Period / timetable block

Suggested fields:

-   `periodId` --- stable UUID
-   `userId`
-   `subjectId`
-   `dayOfWeek`
-   `startTime`
-   `endTime`
-   `blockId` --- groups periods that form one continuous block
-   `updatedAt`
-   `deletedAt` (nullable)

Use a stable `blockId` rather than relying only on `isDouble`. Linked
periods with the same block ID form one continuous block. Validate that
the start time is before the end time and that conflicting blocks are
flagged.

### TeachingSession

Suggested fields:

-   `sessionId` --- stable UUID
-   `userId`
-   `subjectId`
-   `periodBlockId` (optional link to the scheduled block)
-   `sessionDate`
-   `lastTopicCovered`
-   `nextTopicPlanned`
-   `notes`
-   `status`: `COMPLETED`, `PARTIAL`, or `SKIPPED`
-   `createdAt`
-   `updatedAt`
-   `deletedAt` (nullable)

A timetable period describes a planned recurring class; a teaching
session records what happened on a particular date. Do not combine these
concepts into one record.

### Task

Suggested fields:

-   `taskId` --- stable UUID
-   `userId`
-   `subjectId`
-   `title`
-   `type`: `ASSIGNMENT`, `TEST`, `EXAM`, or `PROJECT`
-   `dueAt` --- timestamp/date-time
-   `reminderAt` --- optional timestamp/date-time
-   `status`: `PENDING`, `COMPLETED`, or `CANCELLED`
-   `createdAt`
-   `updatedAt`
-   `deletedAt` (nullable)

Use explicit date-time fields when a reminder must fire at a specific
time. Store and interpret time zones deliberately.

### SyncOperation

Suggested fields:

-   `operationId`
-   `userId`
-   `entityType`
-   `entityId`
-   `operationType` (create/update/delete)
-   `createdAt`
-   `attemptCount`
-   `lastError`
-   optional sync status

Generate stable UUIDs when records are created so the same record
retains the same identity across devices. Do not create a new cloud
document ID on every retry.

The Room and Firestore schemas do not need to be identical. Use explicit
conversion functions and validate records at the repository boundary.

## 8. Home screen and screen behaviour

### Home / "Now" screen

Display:

-   Contextual greeting based on school hours
-   Current class, if one is active
-   Next class and start time
-   Countdown, such as "Starts in 45 mins"
-   Indication of a continuous/double block
-   Upcoming assignment or task alerts
-   Quick action to log the current or most recent session
-   Clear local/offline/sync status

### Timetable

Include:

-   Weekly view
-   Subject and class labels
-   Consistent subject colours
-   Single and double blocks
-   Easy period creation and editing
-   Overlap/conflict warnings
-   Validation against school hours where appropriate

### Session logging

The logging form should be quick enough to use immediately after class.

-   Title: "Log Session: \[Subject Name\]"
-   Field: "What did you cover today?"
-   Optional microphone/voice-to-text action
-   Field: "What is planned for next time?"
-   Status: Completed / Partial / Skipped
-   Optional notes
-   Optional checkbox to add a task
-   If a task is added, show task title, type, due date, and reminder
    time
-   "Save Session" button

Save to Room immediately. Cloud sync can happen later.

### Task manager

Track homework collection, assignments, tests, examinations, projects,
due dates, reminder times, and completion status. Tasks can be created
from a session or directly in the task screen.

### Settings

Include profile, school hours, notification preferences, account/sync
status, privacy information, data export where implemented, and
sign-out.

## 9. Double periods and countdown logic

This is a high-priority correctness requirement.

### Rules

1.  A timetable entry belongs to a weekday and a recurring time slot.
2.  Linked periods with the same `blockId` form one continuous class
    block.
3.  A class is active when the current time is within its start/end
    interval.
4.  If a class is active, show it as current and choose the earliest
    later block as next.
5.  If no class is active, choose the earliest block whose start time is
    in the future.
6.  Never treat the middle of a double block as a separate upcoming
    class.
7.  If two different blocks overlap, display a conflict rather than
    arbitrarily choosing one.
8.  A day with no remaining classes should show a sensible empty state.
9.  When a block ends, offer to log the session; do not automatically
    mark it completed.
10. The screen refreshes on resume and around minute boundaries, not by
    waking every second.

### Example

  Class                         Time           Interpretation
  ----------------------------- -------------- -----------------------------
  Mathematics --- SHS 2B        07:00--09:00   One continuous double block
  English Language --- SHS 2B   09:00--10:00   Separate next block

At 07:30, Mathematics is current and English is next. At 08:15,
Mathematics remains current; the app must not show a new Mathematics
class starting at 08:00.

### Scheduling engine requirements

Create a pure Kotlin function that accepts the current local date/time
and the periods for that weekday. It should group blocks, sort by start
time, return the active block or next future block, handle empty days,
and format the countdown.

Test midnight/date boundaries, timetable edits, device time-zone
changes, and overlapping blocks. If school holidays are later supported,
exclude affected dates from the schedule.

## 10. Teaching State Tracker and carry-forward

The data flow should distinguish what was scheduled from what happened.

``` text
Scheduled class
  Subject + class + start/end time
          |
          v
Teaching session
  What happened on this date?
          |
          +--> COMPLETED
          +--> PARTIAL
          +--> SKIPPED
          |
          v
Next teaching opportunity
  Review prior topic, identify unfinished work,
  and continue with the teacher's saved plan.
```

When the teacher opens the next scheduled session for the same subject
and class, Edense can display the last covered topic and next planned
topic.

Example:

> Last lesson: Introduction to quadratic equations.\
> Planned next: Solving quadratic equations by factorisation.\
> Suggested start: Briefly revisit the previous example before
> introducing the next method.

The first two lines must come from saved teacher records. In the MVP,
the suggested start can be a simple rule-based message. AI can be added
later.

If a lesson was marked `SKIPPED`, Edense must show that it was skipped
rather than implying that teaching took place. A `PARTIAL` session
should preserve the teacher's own plan. An AI suggestion must never
silently overwrite it.

## 11. Firebase Authentication and Firestore

### Authentication

Use Firebase Authentication with Google sign-in through Android
Credential Manager. After the Google credential is obtained,
authenticate with Firebase using the corresponding credential.

Expected flow:

1.  Teacher taps Google sign-in.
2.  Credential Manager provides a Google credential.
3.  Firebase Authentication validates the credential.
4.  Edense checks whether the teacher profile exists.
5.  New users complete school setup.
6.  Existing users proceed to Home.
7.  The app loads local data and starts the sync process.

Provide loading, cancellation, error, and retry states. Do not assume
sign-in always succeeds.

### Firestore structure

Recommended structure:

``` text
users/{uid}
    profile fields

users/{uid}/subjects/{subjectId}
    subject fields

users/{uid}/periods/{periodId}
    recurring timetable block

users/{uid}/sessions/{sessionId}
    actual teaching session

users/{uid}/tasks/{taskId}
    assignment or deadline

users/{uid}/syncMetadata/{metadataId}
    optional sync metadata
```

Nesting records under the authenticated user simplifies individual
ownership enforcement. If a future version supports school-wide sharing,
introduce explicit school membership and role-based permissions rather
than weakening user-isolation rules.

### Illustrative Firestore rules

This is a starting example, not a complete production ruleset. The
example assumes each profile and child document has a `userId` field
matching the UID.

``` javascript
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    function ownsUser(uid) {
      return request.auth != null
          && request.auth.uid == uid;
    }

    match /users/{uid} {
      allow read: if ownsUser(uid);

      allow create: if ownsUser(uid)
          && request.resource.data.userId == uid;

      allow update: if ownsUser(uid)
          && request.resource.data.userId == uid;

      allow delete: if ownsUser(uid);

      match /{collection}/{documentId} {
        allow read, delete: if ownsUser(uid);

        allow create, update: if ownsUser(uid)
            && request.resource.data.userId == uid;
      }
    }
  }
}
```

Before production, validate allowed fields, field types, allowed status
values, immutable ownership, document sizes, and any collection-specific
constraints. If the profile document does not store `userId`, adjust its
rules accordingly.

Rules are enforced by the server and must be tested separately from the
app. Queries must also meet the ownership constraints; Firestore
security rules are not filters.

### Security testing

Use Firebase Emulator Suite tests to verify:

-   A signed-in user can access their own records.
-   A user cannot read or alter another user's records.
-   Unauthenticated requests are denied.
-   A user cannot create a record with another user's `userId`.
-   Invalid field types and invalid status values are rejected.

Enable Firebase App Check as an additional layer of abuse protection.
Never put private AI API keys or service-account credentials in the
Android application.

## 12. Notifications and background work

Use different mechanisms for different jobs.

  -----------------------------------------------------------------------
  Requirement                         Recommended mechanism
  ----------------------------------- -----------------------------------
  Class reminder 5 minutes before     AlarmManager where precision is
  start                               needed and permission is available

  Task reminder at 6:00 AM on due     Scheduled local alarm, with
  date                                appropriate fallback

  Sync pending records when           WorkManager
  connectivity returns                

  Rebuild alarms after device restart Boot receiver and rescheduling
                                      logic

  Update alarms after timetable/task  Cancel and replace affected alarms
  changes                             
  -----------------------------------------------------------------------

WorkManager is intended for deferrable work and is not a guarantee of
exact-time delivery. Android restricts exact alarms on newer versions.
Explain why precise reminders are useful, check permission availability,
and handle denial gracefully.

### Notification lifecycle

1.  Teacher creates or edits a class/task.
2.  Save the record in Room.
3.  Calculate the reminder time.
4.  Schedule or replace the alarm using a stable identifier.
5.  When the alarm fires, check that the record still exists and remains
    active.
6.  Deliver the notification if notification permission and system
    conditions permit.

### Reliability requirements

-   A double period creates only one start reminder.
-   Editing/deleting a task or period cancels and replaces its old
    alarm.
-   Completing or cancelling a task cancels its reminder.
-   Rebuild future alarms after device restart and relevant
    time/time-zone changes.
-   Handle notification and exact-alarm permission denial without
    crashing.
-   Test after the app is swiped away, the device restarts, and the
    phone is offline.
-   Do not promise exact-second delivery on every Android device; system
    restrictions and manufacturer behaviour can affect delivery.

For the MVP, schedule only the next few days of relevant class alarms
and reschedule as needed rather than maintaining an unlimited set of
recurring alarms.

## 13. User interface and brand system

The app should feel calm, professional, fast, and readable while a
teacher is moving between classrooms.

### Main navigation

Use a bottom navigation bar for:

-   Home
-   Timetable
-   Sessions
-   Tasks

Settings can be accessible from the profile or top app bar.

### Home screen composition

-   Greeting and date
-   "NOW" card for the active class
-   "NEXT" card with start time and countdown
-   Double-block indicator
-   Task alert
-   Quick session logging action
-   Small but clear sync/offline status

### Design principles

-   Deep navy for brand and navigation
-   Green for positive actions, saved states, and progress
-   Yellow for selected highlights and important reminders
-   White or soft neutral backgrounds
-   Material 3 components
-   Accessible font sizes and touch targets
-   No unnecessary animation or data-heavy views
-   Consistent subject colours
-   Preserve the approved Eden Creative Systems logo without
    modification

## 14. Privacy, security, and data ownership

Edense may store professional teaching notes and school information.
Privacy must be designed in from the beginning.

Required safeguards:

-   Firebase Authentication and restrictive Firestore rules
-   No publicly readable teacher collections
-   No private API secrets embedded in the app
-   Secure handling of authentication and local records
-   Explicit logout and account deletion workflow
-   Documented data export and recovery strategy
-   Clear consent and privacy disclosures for future AI processing
-   Minimal collection of personal information
-   Avoid student names and student-level records unless a future
    feature genuinely needs them
-   Clearly show when local changes have not yet reached the cloud

Account deletion must not leave cloud data indefinitely without a
documented policy. Implement remote data cleanup deliberately, including
any necessary trusted server-side operations.

## 15. Testing and acceptance criteria

The MVP should not be judged only by whether screens look correct. It
must work under realistic conditions.

### Release checklist

-   [ ] App opens and displays saved data without internet.
-   [ ] A session can be saved offline and survives app restart.
-   [ ] Home identifies the active class correctly.
-   [ ] A double period is represented as one continuous block.
-   [ ] Next-class calculation never selects the middle of a double
    block.
-   [ ] Overlapping blocks produce a visible conflict warning.
-   [ ] Editing a class updates its reminders.
-   [ ] Completing/deleting a task cancels its reminder.
-   [ ] A partial lesson retains the planned next topic.
-   [ ] Pending cloud changes sync after connectivity returns.
-   [ ] Failed sync retries without duplicating records.
-   [ ] One user's cloud data is inaccessible to another.
-   [ ] Permission denial and device restart do not crash the app.
-   [ ] The interface remains usable on smaller Android phones.

These are proposed criteria, not claims that the app has passed the
tests.

### Testing strategy

-   Unit tests for timetable grouping, countdown calculation, recurring
    periods, and session carry-forward.
-   Integration tests for Room, repository sync, and notification
    scheduling.
-   Firebase Emulator tests for access control.
-   Compose UI tests for critical teacher workflows.
-   Manual physical-device tests with internet disabled and
    battery-saving features enabled.

## 16. Development roadmap

### Phase 1 --- Foundation and local MVP

Set up Android Studio, Kotlin, Compose, navigation, Room, and the design
system. Implement teacher setup, subject management, timetable, Home,
session logging, and tasks.

**Exit condition:** The app is useful without signing in or connecting
to the internet.

### Phase 2 --- Scheduling and reminders

Implement the double-period engine, countdown, conflict detection, local
notifications, and rescheduling after timetable changes.

**Exit condition:** Automated tests confirm there are no duplicate class
reminders or incorrect next-class selections in tested scenarios.

### Phase 3 --- Authentication and cloud sync

Add Credential Manager, Firebase Authentication, Firestore,
user-specific rules, queued writes, and conflict resolution.

**Exit condition:** Local changes survive offline periods and reconcile
safely across devices.

### Phase 4 --- Pilot and refinement

Test with a small group of SHS teachers. Improve onboarding and
accessibility, measure battery usage, and address real-world scheduling
issues.

**Exit condition:** Pilot feedback and release criteria are satisfied.

### Phase 5 --- AI and advanced features

Add curriculum-aware carry-forward suggestions, optional voice input,
lesson-planning assistance, and further reporting.

**Exit condition:** AI features are useful, curriculum-grounded,
privacy-conscious, and independently testable.

## 17. Budget and implementation trade-offs

Avoid introducing a custom backend server unless a real requirement
emerges.

-   Android development can begin with local Room storage and no
    recurring cloud-server cost.
-   Firebase costs depend on usage and selected services. Set billing
    alerts and monitor usage before inviting a large group of teachers.
-   AI calls should be optional and server-mediated, with spending
    limits and abuse protection.
-   Prioritise data integrity and offline usability over advanced AI
    features.
-   Do not assume a free cloud allowance guarantees zero cost or
    production reliability.

The principal early investment is likely to be development and testing
time rather than local database infrastructure.

## 18. Definition of done for the MVP

The first release is ready for a pilot when a teacher can:

1.  Set up their profile and school hours.
2.  Add subjects and create a weekly timetable.
3.  View the current and next class accurately.
4.  See a countdown without duplicate double-period entries.
5.  Record what was taught and what is planned next.
6.  Track assignments, tests, and deadlines.
7.  Receive appropriately scheduled local reminders.
8.  Continue using core features without internet.
9.  Sign in and synchronise records securely when online.
10. Recover from interrupted synchronisation without losing or
    duplicating records.

## 19. Original AI prompt library

The prompts below preserve the original development workflow while
incorporating the architecture and security decisions in this blueprint.
Use them one at a time, and ask the AI assistant to inspect the existing
project before changing files.

### Prompt 1 --- Firebase and Firestore setup

**Role:** You are an expert Firebase and Firestore architect.

**Task:** Help me implement Firebase Authentication and Firestore for
Edense, an Android application for SHS teachers in Ghana.

**Requirements:**

1.  Use Firebase Authentication with Google sign-in through Android
    Credential Manager.
2.  Use a user-owned Firestore structure:
    -   `users/{uid}`
    -   `users/{uid}/subjects/{subjectId}`
    -   `users/{uid}/periods/{periodId}`
    -   `users/{uid}/sessions/{sessionId}`
    -   `users/{uid}/tasks/{taskId}`
3.  Provide example documents for each collection.
4.  Write restrictive Firestore security rules that validate ownership,
    field types, and permitted values.
5.  Ensure a user cannot access another user's data.
6.  Explain how to test rules with the Firebase Emulator Suite.
7.  Do not place private keys or AI API secrets in the Android app.
8.  Assume Room is the authoritative local source of truth and Firestore
    is the remote sync layer.

Inspect the project's existing files and dependency versions before
proposing changes. Explain each file to add or edit and provide
verification steps.

### Prompt 2 --- Google sign-in and onboarding

**Role:** You are a senior Android developer specialising in Kotlin,
Jetpack Compose, Firebase, and Android Credential Manager.

**Task:** Implement Google sign-in for Edense.

**Requirements:**

1.  Use current supported Credential Manager APIs, not deprecated
    `GoogleSignInClient` APIs.
2.  Integrate Firebase Authentication.
3.  Provide a reusable Compose sign-in component and proper
    loading/error/cancellation states.
4.  After authentication, check whether the teacher profile exists.
5.  New teachers complete a School Setup screen with school name, start
    time, and end time.
6.  Existing teachers proceed to Home.
7.  Store profile data locally in Room and enqueue cloud
    synchronisation.
8.  Use Material 3.
9.  Never store passwords or private credentials in plaintext.
10. Do not assume a sign-in request always succeeds.

Inspect the current project before changing files. Provide compatible
dependencies, configuration instructions, Kotlin code, and tests.

### Prompt 3 --- Double periods and countdown

**Role:** You are an expert Kotlin scheduling and time-logic engineer.

**Task:** Implement Edense's current-class and next-class calculation.

**Requirements:**

1.  Accept the current local date/time and the timetable blocks for the
    relevant weekday.
2.  Group linked periods using a stable `blockId`.
3.  Return the active block or the next future block, including
    subject/class name, start/end time, and a formatted countdown.
4.  Never show the middle of a double block as a new class.
5.  Detect overlapping blocks and report conflicts.
6.  Handle empty days, timetable edits, time-zone changes, and date
    boundaries.
7.  Implement a Compose countdown that updates around minute boundaries
    and on screen resume.
8.  Use a testable pure Kotlin scheduling function.
9.  Supply unit tests for boundary conditions and double periods.

Do not create a continuously running second-by-second timer. Inspect
existing code before implementation and explain how to integrate the
function into the app.

### Prompt 4 --- Session logging bottom sheet

**Role:** You are an expert Android UI/UX developer using Jetpack
Compose and Material 3.

**Task:** Implement Edense's quick teaching-session logging workflow.

**Requirements:**

1.  Display "Log Session: \[Subject Name\]".
2.  Pre-fill the subject/class from the selected timetable block.
3.  Include "What did you cover today?" and "What is planned for next
    time?" fields.
4.  Include an optional microphone/voice-to-text action if supported.
5.  Provide Completed, Partial, and Skipped statuses.
6.  Allow optional notes.
7.  Include an option to create a future task.
8.  If selected, display task title, type, due date, and reminder time.
9.  Save immediately to Room.
10. Queue cloud sync without blocking the UI.
11. Preserve a teacher's next-topic plan; never overwrite it silently
    with an AI suggestion.
12. Provide validation, accessibility labels, loading states, and tests.

Inspect the existing entities, DAOs, repositories, and theme before
implementing the UI.

### Prompt 5 --- Reminders and WorkManager

**Role:** You are an Android background-processing and notification
specialist.

**Task:** Implement reliable local reminders for Edense.

**Requirements:**

1.  Schedule a class reminder five minutes before the start of a class
    block.
2.  Schedule a task reminder at the teacher's chosen time, with a
    default of 6:00 AM on the due date if the teacher selects that
    default.
3.  Use AlarmManager for time-sensitive reminders where permission and
    platform rules permit.
4.  Use WorkManager for deferrable work such as sync retries.
5.  Handle notification permission and exact-alarm permission denial.
6.  Use stable identifiers so edits replace old alarms instead of
    duplicating them.
7.  Cancel reminders for completed or deleted tasks.
8.  Reschedule relevant alarms after device restart, timetable edits,
    and time-zone/system-time changes.
9.  Check that a task or period remains active before showing its
    notification.
10. Explain manifest declarations, Android version differences, and
    physical-device testing.

Do not claim exact-second delivery is guaranteed on every Android
device. Inspect existing project configuration before changes.

### Prompt 6 --- AI carry-forward (future phase)

**Role:** You are an AI integration engineer familiar with secure
server-side API design and education workflows.

**Task:** Design an optional AI carry-forward feature for Edense.

**Requirements:**

1.  Trigger or request a suggestion when a teaching session is marked
    Partial.
2.  Use the saved `lastTopicCovered`, `nextTopicPlanned`, subject, class
    level, and teacher-provided context.
3.  Produce a concise three-sentence suggestion for opening the next
    lesson, such as a short recap activity.
4.  Align suggestions with verified GES/NaCCA curriculum materials where
    available.
5.  Treat the teacher's plan as authoritative; never silently overwrite
    it.
6.  Call the AI provider from a trusted backend such as Firebase Cloud
    Functions, not directly from the Android client with a secret API
    key.
7.  Validate inputs, limit data sent to the provider, handle failures,
    and control costs.
8.  Return structured output and allow the teacher to accept, edit, or
    dismiss the suggestion.
9.  Explain secure configuration, error handling, tests, and deployment.
10. Do not claim curriculum alignment unless the relevant source
    material has been supplied or verified.

Inspect the project before implementation. Keep this feature out of the
initial MVP until the local teaching workflow is reliable.

## 20. Immediate next steps

1.  Create an `EdenseProject` folder.
2.  Open Android Studio and create a Kotlin/Jetpack Compose project.
3.  Select a stable, compatible dependency set and record the versions.
4.  Build the design system and navigation.
5.  Implement Room entities, DAOs, and repositories.
6.  Build the timetable and double-period scheduling engine with unit
    tests.
7.  Build the Home screen and session logging.
8.  Add task management and local reminders.
9.  Add Firebase authentication and explicit synchronisation.
10. Run the release checklist before a teacher pilot.

**Recommended starting point:** Phase 1, before Google sign-in. First
establish a reliable local application, timetable model, and Teaching
State Tracker. This provides a working product before authentication and
cloud synchronisation add complexity.

## 21. Official implementation references

Use current official documentation when implementing the project, and
verify dependency versions at implementation time.

-   Android offline-first architecture:
    https://developer.android.com/topic/architecture/data-layer/offline-first
-   Android alarms and reminders:
    https://developer.android.com/develop/background-work/services/alarms
-   Firebase Google authentication on Android:
    https://firebase.google.com/docs/auth/android/google-signin
-   Firestore security rules:
    https://firebase.google.com/docs/firestore/security/get-started
-   Firestore offline persistence:
    https://firebase.google.com/docs/firestore/manage-data/enable-offline
-   Android WorkManager:
    https://developer.android.com/develop/background-work/background-tasks/persistent
-   Android Credential Manager:
    https://developer.android.com/identity/sign-in/credential-manager

------------------------------------------------------------------------

**Document status:** This is a project blueprint and implementation
specification. It does not claim that the app has been built, that the
security rules are production-complete, or that the release tests have
passed.
