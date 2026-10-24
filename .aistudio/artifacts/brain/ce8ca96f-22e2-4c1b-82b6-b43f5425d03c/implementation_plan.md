# Edense — Teaching State Tracker for Ghanaian SHS Teachers

Edense is an offline-first Android application designed specifically for Senior High School (SHS) teachers in Ghana. It functions as an intelligent **Teaching State Tracker**, answering at a glance what class is active, what comes next, where teaching stopped last time, and what assignments or deadlines need immediate attention.

***

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and UX decisions reflect your exact specifications for teacher subject limits and timetable building:

- **Confirmed Decision 1 (Ghana SHS Subject Catalog & 2-Subject Teaching Limit)**:
  - Initial setup preloads the official Ghanaian SHS subject catalog (Core Mathematics, English Language, Integrated Science, Social Studies, Elective Physics, Chemistry, Biology, Elective Mathematics, Economics, Geography, Government, Financial Accounting, etc.).
  - A teacher can select **at most 2 subjects** they are assigned to teach for the academic year.
- **Confirmed Decision 2 (Weekly Period Target Quota)**:
  - For each selected subject, the teacher inputs their assigned classes (e.g. `SHS 2B`, `SHS 3A`), default classroom (e.g. `Room 14`), and the **target number of periods per week** (e.g. 6 periods/week).
- **Confirmed Decision 3 (No Predefined Timetable & Strict Subject Gating)**:
  - Edense starts with **no predefined timetable entries**.
  - When setting up the timetable, the period creation builder **exclusively permits the teacher's selected subjects (max 2)**.
  - The timetable builder includes a live quota tracker showing scheduled periods against the weekly target (e.g., *"Core Math: 4 of 6 periods scheduled"*).
- **Confirmed Decision 4 (Academic Year, Semesters & Vacation Notification Pausing)**:
  - School setup configures Academic Year (e.g., `2026/2027`) and Semesters/Terms with start and end dates.
  - During school vacations or outside term dates, **all class reminders and alarms are automatically paused**, and the Home screen reflects a calm vacation status with return date.
- **Confirmed Decision 5 (Double Period Continuous Block Engine & 10-Min Reminder Lead Time)**:
  - Consecutive periods sharing the same subject and block ID merge into a single uninterrupted class block on the Home screen and countdown ticker.
  - Class alarms are scheduled **10 minutes before class starts**.

***

## 1. Overview & Core Concept

### What It Does
Edense eliminates the cognitive burden Ghanaian SHS teachers face when juggling multiple classes, subjects, continuous double periods, term schedules, and paper lesson notebooks:
1. **Guided Onboarding & Subject Allocation**: Preloads Ghanaian curriculum subjects, enforces the realistic policy constraint of maximum 2 subjects per teacher, and records weekly period targets per subject.
2. **Personalized Timetable Builder**: Allows teachers to place their 1 or 2 selected subjects into weekly single and double periods with conflict detection and quota progress.
3. **Academic Calendar & Vacation Awareness**: Recognizes academic years, semesters/terms, and vacation breaks. Automatically pauses class alarms during school vacations so teachers are not disturbed during recess.
4. **Live "Now & Next" Command Center**: Instantly shows the ongoing class or the upcoming class with an accurate countdown (e.g., *"Starts in 42 mins"*). During vacations, displays a vacation recap mode with resumption countdown.
5. **Double Period Engine**: Properly groups consecutive periods sharing the same subject and block ID into a single continuous class block (e.g., `07:00 – 09:00`), preventing false alarms or "next class" prompts in the middle of a double session.
6. **Teaching State Tracker & Carry-Forward**: Quickly captures what was covered and what is planned next right after class ends. When the teacher next meets that class, Edense presents the previous topic and unresolved notes.
7. **Academic Deadline & Task Manager**: Tracks homework collections, mock exams, class tests, and projects with priority status and scheduled reminders.

### Target Audience & Persona
Senior High School teachers across Ghana (e.g., teaching Core Math & Elective Math, or English & Literature) moving between classrooms, managing 5–8 periods a day across semesters, and needing an organized, distraction-free workflow.

***

## 2. User Experience & Visual Design

### Key User Flows

#### Flow 1: Setup & Subject Selection (Max 2 Subjects)
- **School & Term Setup**: Teacher enters name, school name (e.g., *"Achimota SHS"*), school hours (`07:00 AM – 03:00 PM`), Academic Year (`2026/2027`), and Semester/Term start & end dates.
- **Subject Catalog Selection**:
  - Displays Ghanaian SHS subject catalog grouped by Core (Core Mathematics, English Language, Integrated Science, Social Studies) and Electives (Physics, Chemistry, Biology, Elective Math, Economics, etc.).
  - Teacher taps to select their subjects. **Capped at a strict maximum of 2 subjects** (the UI clearly states *"Max 2 subjects per teacher"* and disables other options once 2 are selected).
- **Subject Configuration Modal**:
  - For each chosen subject, the teacher specifies:
    - Assigned Class(es) (e.g., `SHS 2B` and `SHS 3A`)
    - Default Classroom (e.g., `Room 14`, `Science Lab 1`)
    - **Periods per Week Target** (e.g., 6 periods/week)
    - Color tag for timetable visual identification.

#### Flow 2: Building the Timetable (Strict Subject Gating)
- The timetable starts empty with an encouraging setup prompt: *"Schedule your periods for [Subject 1] and [Subject 2]"*.
- **Weekly Quota Tracker Card** at the top of the timetable:
  - Progress bar for Subject 1 (e.g., `Core Math: 0 / 6 periods placed`)
  - Progress bar for Subject 2 (e.g., `Elective Math: 0 / 4 periods placed`)
- **Add Period Dialog**:
  - Subject dropdown **only presents the teacher's selected subjects** (no clutter from unselected catalog subjects).
  - Select Day (Monday to Friday), Start Time, End Time, Classroom, and option to link as a **Double Period** (assigning the same `blockId`).
  - Real-time overlap warning if the new period collides with an existing class.

#### Flow 3: Daily "Now & Next" Dashboard
- The teacher opens the app and sees a contextual greeting (*"Good morning, Mr. Mensah"*), the current date, and an active banner:
  - **If on Vacation / Holiday**: A calm banner: *"School Vacation — Semester resumes on [Date] (Notifications Paused)"*.
  - **If during class**: A prominent Navy & Green **NOW** card showing Subject, Class, period time range, and progress. A quick button says **"Log Lesson Now"**.
  - **If between classes**: A clean **NEXT** card highlighting the upcoming block, classroom location, and a live countdown ticker (*"Starts in 10 mins"*).
  - **If timetable is empty**: A prominent button directing the teacher to schedule their first period.

#### Flow 4: Rapid Session Logging (Post-Class)
- Tapping **"Log Session"** opens an intuitive bottom sheet:
  - Pre-fills the subject, class, and date.
  - **"What did you cover today?"**: Text area (e.g., *"Factorisation of quadratic expressions"*).
  - **"Status"**: Simple toggle between **Completed**, **Partial**, or **Skipped**.
  - **"What is planned for next time?"**: Text area (e.g., *"Completing the square method"*).
  - **"Add Follow-up Task"** toggle: Quickly creates an assignment (e.g., *"Collect Exercise 4B Homework"*) due on the next class date.
  - Saves instantly to Room with tactile feedback.

#### Flow 5: Task & Deadline Hub
- Categorized list: **Assignments**, **Tests**, **Exams**, and **Projects**.
- Filter by pending vs. completed. One-tap completion checkbox.
- Due date indicators with urgency tags (Today, Tomorrow, Upcoming).

### Visual Identity & Theme

- **Aesthetic Direction**: Professional, calm, academically structured, and modern. High legibility in Ghanaian staff rooms and classrooms.
- **Color Palette**:
  - **Primary (Deep Navy Blue)**: `#0F1E36` — conveys structure, authority, and calm focus.
  - **Secondary / Accent (Academic Forest Green)**: `#1E6F5C` and `#289672` — represents growth, clarity, and successful lesson completion.
  - **Highlight / Alert (Warm Amber Yellow)**: `#D97706` / `#F59E0B` — used for countdown timers, urgent deadlines, and double-period tags.
  - **Vacation / Recess Accent (Soft Sky Blue)**: `#0284C7` — indicates restful vacation status.
  - **Background & Surfaces**: `#F8FAFC` (light canvas) and `#FFFFFF` (card surfaces) with subtle borders (`#E2E8F0`).
- **Typography & Hierarchy**:
  - Strong, structured display titles for classes and subjects.
  - Clean, legible numeric fonts for time badges and countdown counters.
  - High-contrast body copy meeting accessibility standards (minimum touch target 48dp).

***

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Ghana SHS Preloaded Catalog with 2-Subject Limit**
  - *Chosen Approach*: Onboarding offers a rich catalog of Ghanaian SHS subjects, but strictly caps teacher selection at 2 subjects with target weekly period quotas.
  - *Why*: In the Ghanaian educational system, SHS teachers specialize in 1 or 2 subjects across specific forms/classes. Gating the timetable to these 2 subjects eliminates clutter and ensures the timetable only reflects the teacher's actual workload.

- **Decision 2: Empty Initial Timetable with Quota Tracking**
  - *Chosen Approach*: Rather than assuming a hardcoded timetable, the timetable is initialized empty, allowing the teacher to easily place periods while monitoring their target quota (e.g. 6 periods/week).
  - *Why*: Timetables vary completely from school to school in Ghana. An empty starting grid with intuitive slotting is far easier to customize than having to delete someone else's sample timetable.

- **Decision 3: Academic Year & Vacation Schedule Awareness**
  - *Chosen Approach*: `AcademicCalendarEntity` holds the active academic year, term type (Semester vs. Term), active term start/end date, and vacation pause flag. The alarm engine verifies `isVacationActive()` before scheduling or firing any class alarm.
  - *Why*: Teachers strongly dislike having phone alarms ring during school holidays, vacations, or exam breaks.

- **Decision 4: Double Period Scheduling Engine as Continuous Block**
  - *Chosen Approach*: Linked periods sharing a `blockId` render and trigger as a unified block (e.g. `07:00 – 09:00`), preventing false intermediate alarms.

***

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             Jetpack Compose UI                              │
│                                                                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐  ┌───────────┐  │
│  │   HomeScreen    │  │ TimetableScreen │  │SessionsScreen│  │TasksScreen│  │
│  │ (Now/Next/Vaca) │  │ (Gated to 2 Subs│  │(History/Log) │  │(Deadlines)│  │
│  └────────┬────────┘  └────────┬────────┘  └──────┬───────┘  └─────┬─────┘  │
│           │                    │                  │                │        │
│           │            ┌───────┴──────────────────┴───────┐        │        │
│           │            │ Setup & Subject Allocation Modal │        │        │
│           │            │ (SHS Catalog, Max 2, Periods/Wk) │        │        │
│           │            └──────────────────┬───────────────┘        │        │
└───────────┼───────────────────────────────┼────────────────────────┼────────┘
            ▼                               ▼                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            ViewModels & Domain Logic                        │
│                                                                             │
│  • SetupViewModel (SHS catalog selection, 2-subject ceiling, period quotas) │
│  • HomeViewModel (Tick timer, active/next block, vacation banner state)    │
│  • TimetableViewModel (Add/edit period, filtered to selected subjects)      │
│  • SessionsViewModel (Log session, carry-forward lookup, status tags)       │
│  • TasksViewModel (Create task, toggle status, filter by urgency)           │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ Scheduling Engine: resolveClassSchedule(time, periods, isVacation)    │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                             Repository Layer                                │
│       (EdenseRepository: Single API for UI, coordinates Room & Alarms)       │
└──────────────────┬───────────────────────────────────────┬──────────────────┘
                   ▼                                       ▼
┌─────────────────────────────────────┐ ┌─────────────────────────────────────┐
│        Room Local Database          │ │      Alarm & Notification Engine    │
│  • TeacherProfileDao                │ │  • EdenseAlarmReceiver              │
│  • AcademicCalendarDao (Terms/Vaca) │ │  • 10-minute pre-class reminders   │
│  • SubjectDao (Catalog + Selected)  │ │  • Vacation guard (checks active)   │
│  • PeriodDao (Timetable blocks)     │ │  • Task morning reminders           │
│  • TeachingSessionDao               │ │  • Boot completed reschedule        │
│  • TaskDao                          │ └─────────────────────────────────────┘
└─────────────────────────────────────┘
```

### Data Model & Entities

1. **`TeacherProfileEntity`**:
   - `id`: `String` (UUID / "local_teacher")
   - `name`: `String` (e.g. "Mr. K. Mensah")
   - `schoolName`: `String` (e.g. "Achimota Senior High School")
   - `schoolStartTime`: `String` ("07:00")
   - `schoolEndTime`: `String` ("15:00")
   - `isOnboardingCompleted`: `Boolean`

2. **`AcademicCalendarEntity`**:
   - `id`: `String` (default "active_calendar")
   - `academicYear`: `String` ("2026/2027")
   - `termSystem`: `String` ("SEMESTER" or "TERM")
   - `currentTermName`: `String` ("Semester 1", "Semester 2", "Term 1", etc.)
   - `termStartDate`: `String` ("2026-09-01")
   - `termEndDate`: `String` ("2026-12-18")
   - `pauseAlarmsDuringVacation`: `Boolean` (default true)
   - `isManualVacationMode`: `Boolean` (default false)

3. **`SubjectEntity`**:
   - `id`: `String` (UUID)
   - `name`: `String` ("Core Mathematics", "English Language", etc.)
   - `category`: `String` ("CORE" or "ELECTIVE")
   - `isSelected`: `Boolean` (True only for the up to 2 subjects chosen by the teacher)
   - `assignedClasses`: `String` ("SHS 2B, SHS 3A")
   - `defaultClassroom`: `String` ("Room 14", "Science Lab")
   - `targetPeriodsPerWeek`: `Int` (e.g. 6)
   - `colorHex`: `String` ("#1E6F5C", "#0F1E36", etc.)

4. **`PeriodEntity`**:
   - `id`: `String` (UUID)
   - `subjectId`: `String` (Foreign key to Subject; must be an `isSelected == true` subject)
   - `dayOfWeek`: `Int` (1 = Monday ... 5 = Friday)
   - `startTime`: `String` ("07:00")
   - `endTime`: `String` ("08:00")
   - `blockId`: `String` (Shared across periods that form a single continuous double block)
   - `classroom`: `String` ("Room 14", "Science Lab 2")

5. **`TeachingSessionEntity`**:
   - `id`: `String` (UUID)
   - `subjectId`: `String`
   - `sessionDate`: `String` (YYYY-MM-DD)
   - `lastTopicCovered`: `String`
   - `nextTopicPlanned`: `String`
   - `status`: `String` ("COMPLETED", "PARTIAL", "SKIPPED")
   - `notes`: `String`
   - `createdAt`: `Long`

6. **`TaskEntity`**:
   - `id`: `String` (UUID)
   - `subjectId`: `String`
   - `title`: `String` ("Collect homework on Quadratic Equations")
   - `type`: `String` ("ASSIGNMENT", "TEST", "EXAM", "PROJECT")
   - `dueDate`: `String` (YYYY-MM-DD)
   - `reminderTime`: `String` ("06:00")
   - `isCompleted`: `Boolean`

### Interactive Component & State Mapping

- **Initial Setup Screen / Subject Selection**:
  - Displays list of Ghanaian SHS subjects with filter chips for Core and Electives.
  - Selection counter badge: *"Selected: X / 2 subjects"*.
  - When a subject is selected, a sheet prompts for target periods per week (e.g. 6), assigned class (e.g. `SHS 2B`), and default classroom.
  - Once 2 subjects are selected, other subjects are disabled with clear feedback.
- **Timetable Screen**:
  - Shows empty state with *"Add your first period"* when 0 periods are scheduled.
  - Header shows period quota progress: `[Subject 1]: X/Y periods` and `[Subject 2]: A/B periods`.
  - "Add Period" sheet dropdown **only lists the teacher's 1 or 2 selected subjects**.
  - Consecutive periods with the same subject and block ID automatically group into a continuous double period card.
- **Home Screen**:
  - Real-time resolution of current class and next upcoming class with 10-minute warning.
  - If vacation is active, displays the vacation recess banner with alarm pause indicator.
  - One-tap "Log Session" immediately saves what was covered and next planned topic to Room.
