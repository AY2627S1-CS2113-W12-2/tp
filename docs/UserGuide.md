# User Guide

## Introduction

TutorLedger is a desktop app for independent tutors who teach primary-school, secondary-school and
junior-college students. It keeps your students, lessons, homework and payments in one place, and it is built
to be used by typing: every task is a single command, so a tutor who types quickly can log a lesson faster
than they could fill in a form.

TutorLedger does not teach online, message students or parents, or process payments. It is a personal record of
your own tutoring work.

## Contents

* [Quick start](#quick-start)
* [Notes about the command format](#notes-about-the-command-format)
* [Features](#features)
  * [Student book](#student-book): `add`, `subject`, `view`, `list`, `edit`, `archive`, `restore`, `delete`
  * [Lessons](#lessons): `schedule`, `log`, `lessons`, `filter`, `next`, `topics`, `record`, `reschedule`,
    `cancel`, `edit`, `delete`
  * [Homework](#homework): `assign`, `submit`, `homework`
  * [Payments](#payments): `mark`, `unpaid`, `owed`
  * [Scores and progress](#scores-and-progress): `score`, `scores`, `progress`
  * [Follow-up tasks](#follow-up-tasks): `task`, `tasks`, `done`
  * [Storage](#storage): `save`, `reload`
  * [Help and exiting](#help-and-exiting): `help`, `exit`, [shortcuts](#shortcuts-for-common-commands)
* [FAQ](#faq)
* [Known issues](#known-issues)
* [Command summary](#command-summary)

## Quick start

1. Ensure that you have Java 25 or above installed. To check, run `java -version` in a terminal.
1. Download the latest `TutorLedger.jar` from the project's
   [Releases page](https://github.com/AY2627S1-CS2113-W12-2/tp/releases).
1. Put the file in the folder you want TutorLedger to keep its data in.
1. Open a terminal in that folder and run `java -jar TutorLedger.jar`.
1. Type a command and press Enter. Some commands to try:
   * `add n/Amirah Binte Rahman l/Sec 3 p/91234567` adds a student, who is given the ID `S1`.
   * `subject S1 s/E Math s/A Math` gives that student two subjects.
   * `schedule S1 d/20-10-2026 t/1600 s/A Math f/60` schedules a lesson.
   * `view S1` shows everything about the student.
   * `help` lists every command.
1. When you are finished, type `exit`. Your data is saved after every change, so nothing is lost.

## Notes about the command format

* Words in `UPPER_CASE` are values you supply. In `add n/NAME`, `NAME` is replaced with the student's name, as
  in `add n/Amirah Binte Rahman`.
* Items in square brackets are optional.
* Items followed by `…` can be given more than once.
* Values with a prefix (such as `n/` or `d/`) can be given in any order.
* Unless a command says otherwise, each prefix can be given only once. For example,
  `schedule S1 d/20-10-2026 d/21-10-2026 ...` is rejected.
* Command words, prefixes and IDs are not case-sensitive, so `VIEW s1` works the same as `view S1`.
* Extra words after `help` and `reload` are ignored. For example, `help 123` is read as `help`.
* If a command cannot be carried out, TutorLedger shows a message starting with `Error:` that says what to fix,
  and nothing is changed.

**IDs.** TutorLedger gives every student, lesson and homework item an ID when it is created, and shows that ID
wherever the item is listed. Commands use the ID to say which item you mean.

| ID            | Identifies                          | Example |
|---------------|-------------------------------------|---------|
| `STUDENT_ID`  | A student. Starts with `S`.         | `S1`    |
| `LESSON_ID`   | A lesson. Starts with `L`.          | `L3`    |
| `HOMEWORK_ID` | A homework item. Starts with `H`.   | `H4`    |
| `SCORE_ID`    | A test score. Starts with `A`.      | `A2`    |
| `TASK_ID`     | A follow-up task. Starts with `T`.  | `T3`    |

An ID is never reused, even after its item is deleted.

**Values.**

| Value            | Prefix  | Accepted input                                                                  | Example              |
|------------------|---------|---------------------------------------------------------------------------------|----------------------|
| `NAME`           | `n/`    | Letters, with single spaces, hyphens or apostrophes between them.               | `n/Tan Wei Ming`     |
| `LEVEL`          | `l/`    | `Pri 1`–`Pri 6`, `Sec 1`–`Sec 5`, or `JC 1`–`JC 2`. Case is ignored; the space is optional. | `l/Pri5`, `l/JC 2` |
| `PHONE_NUMBER`   | `p/`    | Exactly 8 digits. Siblings may share a number.                                  | `p/91234567`         |
| `SUBJECT`        | `s/`    | The subject as you normally write it. Not case-sensitive.                       | `s/Pure Chemistry`   |
| `DATE`           | `d/`    | A real calendar date as `DD-MM-YYYY`.                                           | `d/20-10-2026`       |
| `DUE_DATE`       | `d/`    | Same as `DATE`.                                                                 | `d/20-10-2026`       |
| `TIME`           | `t/`    | 24-hour time as `HHMM`, from `0000` to `2359`.                                  | `t/1600`             |
| `FEE`            | `f/`    | Dollars without the `$`, up to 2 decimal places. Zero or more.                  | `f/62.50`            |
| `RATE`           | `r/`    | Default dollars per hour, up to 2 decimal places. Zero or more.                | `r/60`               |
| `MINUTES`        | `dur/`  | Whole minutes from 1 to 1440; defaults to 60 when scheduling.                 | `dur/90`             |
| `ATTENDANCE`     | `a/`    | `present`, `absent` or `late`.                                                  | `a/late`             |
| `NOTES`          | `note/` | Any text: what was covered and anything to remember. See [Known issues](#known-issues). | `note/Started vectors` |
| `WEEKS`          | `w/`    | A whole number of weeks from 2 to 52.                                           | `w/10`               |
| `REASON`         | `r/`    | Any text saying why a lesson was called off. Same limits as `NOTES`.            | `r/Student sick`     |
| `STATUS`         | (none)  | `paid` or `unpaid`.                                                             | `paid`               |
| `STATUS` (student list) | `status/` | `active`, `archived` or `all`, ignoring case. | `status/archived` |
| `DESCRIPTION`    | (none)  | Any text, written straight after the student ID.                                | `Worksheet 3B`       |
| `TEST_NAME`      | (none)  | Any text, written straight after the student ID.                                | `Mid-year exam`      |
| `MARKS/TOTAL`    | `m/`    | Marks scored, `/`, then the maximum marks. Up to 2 decimal places each.         | `m/28/40`            |
| `FILE_PATH`      | (none)  | A file path relative to the folder you run TutorLedger from.                    | `backups/term1.txt`  |

Example outputs use the students and lesson dates shown in their sections. Some examples assume a
particular current date so that past and upcoming lessons are easy to distinguish.

## Features

### Student book

#### Adding a student: `add`

Adds a student to TutorLedger and gives them a student ID.

Format: `add n/NAME l/LEVEL p/PHONE_NUMBER [s/SUBJECT]… [r/RATE]`

* `n/`, `l/` and `p/` are required.
* Levels use `Pri`, `Sec` or `JC` followed by a valid year. For example, `l/pRi5` is displayed and saved
  as `Pri 5`, and `l/jc1` becomes `JC 1`. Use these short labels rather than `Primary` or `Junior College`.
* `r/` optionally sets a default hourly rate (see [default rates](#setting-a-default-hourly-rate-r)).
* `s/` is optional and can be given more than once, so you can enter the whole record, subjects included, in
  one line. A subject repeated in a different case (such as `s/E Math s/e math`) is only added once.
* A student with the same name (ignoring case) and phone number as an existing student is not added, since they
  are almost certainly the same person.

Examples:

* `add n/Amirah Binte Rahman l/Sec 3 p/91234567`
* `add l/Sec 4 p/98765432 n/Tan Wei Ming`
* `add n/Amirah Binte Rahman l/Sec 3 p/91234567 s/E Math s/A Math`
* `add n/Alice Tan l/Pri5 p/82345678 s/Math` adds a primary-school student.
* `add n/Bob Tan l/JC 2 p/92345678 s/H2 Math` adds a junior-college student.

Example output:

```
> add n/Amirah Binte Rahman l/Sec 3 p/91234567
Added S1: Amirah Binte Rahman (Sec 3), 91234567
You now have 1 student.

> add n/Daniel Lim l/Sec 4 p/87654321 s/E Math s/A Math
Added S5: Daniel Lim (Sec 4), 87654321
Subjects: E Math, A Math
You now have 5 students.
```

> **Tip:** Give the student their subjects straight away, with `s/` in `add` or later with `subject`. A lesson
> can only be scheduled in a subject the student takes.

#### Adding subjects to a student: `subject`

Adds one or more subjects to a student's record.

Format: `subject STUDENT_ID s/SUBJECT [s/SUBJECT]…`

* The subjects are added to any the student already has.
* A subject the student already takes (ignoring case) is not added a second time.
* To remove or replace subjects, use [`edit`](#editing-a-student-edit).

Examples:

* `subject S1 s/E Math s/A Math`
* `subject S4 s/E Math`

Example output:

```
> subject S1 s/E Math s/A Math
Added 2 subjects to S1 Amirah Binte Rahman. Subjects: E Math, A Math
```

#### Viewing a student: `view`

Shows everything about one student on one screen: their details and subjects, every lesson with its attendance,
payment status and notes, their outstanding homework, and how much they owe. Use it in the minutes before a
lesson.

Format: `view STUDENT_ID`

* Lessons are shown newest first, so the last lesson's notes are near the top.
* Archived profiles remain accessible by ID and show `Status: archived`.
* The amount owed counts unpaid lessons dated today or earlier. Lessons still to come are not counted.

Example: `view S1`

Example output (today is Wed 14-10-2026):

```
> view S1
S1  Amirah Binte Rahman  (Sec 3)  91234567
Subjects: E Math, A Math
Default hourly rate: not set
Lessons
  L4  Tue 20-10-2026 1600  A Math  $60.00  not recorded  unpaid
  L3  Tue 13-10-2026 1600  A Math  $60.00  present  unpaid
       Discriminant recap. Started simultaneous equations.
  L2  Tue 06-10-2026 1600  A Math  $60.00  present  unpaid
       Quadratic equations. Struggled with the discriminant.
  L1  Tue 29-09-2026 1600  E Math  $60.00  late  paid
       Indices and surds. Finish surds next time.
Outstanding homework
  H4  Worksheet 4A  due Tue 20-10-2026
Amount owed: $120.00 (2 lessons)
```

#### Listing and finding students: `list`

Lists active students, or only the active students whose name contains what you type.

Format: `list [NAME] [status/STATUS]`

* With no `NAME`, students in the selected status are listed in ID order.
* `STATUS` is `active` (the default), `archived` or `all`, ignoring case. Place the optional status
  after the name query: `list tan status/archived`. Archived rows show `[archived]`.
* With a `NAME`, a student is listed if that text appears anywhere in their name. Case is ignored, so `list wei`
  finds both Tan Wei Ming and Tan Wei Ling.
* The text may contain spaces. `list tan wei` matches names that contain "tan wei".

Examples:

* `list`
* `list wei`

Example output:

```
> list wei
2 students match "wei":
  S2  Tan Wei Ming (Sec 4)  A Math, Pure Chemistry
  S3  Tan Wei Ling (Sec 2)  Math, Science
```

#### Editing a student: `edit`

Changes the details of an existing student.

Format: `edit STUDENT_ID [n/NAME] [l/LEVEL] [p/PHONE_NUMBER] [s/SUBJECT]… [r/RATE]`

* Give at least one value to change. Values you leave out stay the same.
* Use `l/` to change school stage as well as year, such as `edit S1 l/JC1`. The student's other details,
  default rate, subjects and lesson history stay the same. Update subjects separately if needed.
* Giving any `s/` replaces *all* of the student's subjects with the ones listed, so list every subject you want
  them to keep. Type `s/` with nothing after it to remove every subject.
* Lessons already scheduled keep the subject and total fee they were scheduled with.
* `r/RATE` sets a default hourly rate; a bare `r/` clears it. See the rate section below.

Examples:

* `edit S1 l/Sec 4` moves Amirah up a level at the start of a new year.
* `edit S2 p/98761234 s/A Math s/Pure Chemistry s/Pure Physics` changes the phone number and adds Pure Physics
  to the two subjects Wei Ming already takes.

Example output:

```
> edit S1 l/Sec 4
Edited S1: Amirah Binte Rahman (Sec 4), 91234567
Subjects: E Math, A Math
```

#### Setting a default hourly rate: `r/`

Set a rate once so scheduling does not require typing the fee for every lesson. Rates apply per student,
not per subject. They accept non-negative dollar amounts with at most two decimal places; `r/0` means
free lessons. New students have no rate unless one is supplied.

* Create a complete record: `add n/Amirah l/Sec 3 p/91234567 s/E Math r/60`.
* Set or replace a rate: `edit S1 r/65.50`.
* Check the rate: `view S1` displays `Default hourly rate: $65.50/hour`.
* Clear it: `edit S1 r/`. An omitted `r/` during an edit preserves the rate.
* `add ... r/` is rejected; omit `r/` when adding a student without a default rate.
* Repeated `r/`, negative values, scientific notation and values such as `60.001` are rejected.

For a student charging $60/hour:

```
> schedule S1 d/20-10-2026 t/1600 s/E Math dur/90
Scheduled L1 for S1 Amirah
  Tue 20-10-2026 1600 (90 min), E Math, $90.00
```

Omitting `dur/` means a one-hour lesson. An explicit `f/25` charges $25 for the entire lesson,
even with `dur/90`. A supplied blank or invalid `f/` is an error, not a request to use the rate.
If no rate is set and `f/` is omitted, scheduling asks you to set a rate or give an explicit fee.

**Existing charges stay fixed.** Changing a student's rate affects newly scheduled lessons only,
including when older lessons are still in the future. Editing a lesson's duration also keeps its fee.
Use `edit L1 f/AMOUNT` to deliberately change an existing charge.

#### Archiving and restoring a student: `archive`, `restore`

Archive students who have stopped lessons so the active list stays uncluttered while their history is kept.

Formats: `archive STUDENT_ID` and `restore STUDENT_ID`

* `archive S1` hides S1 from `list` and name searches. Use `list status/archived` or `list status/all`
  to find the profile again; `view S1` still shows its details and history.
* Lessons, homework, scores, subjects, default rates, balances and permanent IDs are preserved.
  Archiving does not cancel future lessons or hide them from lesson lists or `next`.
* New single or recurring scheduling requires restoration first. Existing lessons can still be corrected,
  rescheduled or cancelled. You can log past lessons, record attendance, edit the profile and settle payments.
* `restore S1` makes the same profile active again. Repeating either action is harmless.
* Add/save/load totals count all stored students, including archived profiles; the default list counts active ones.
  When archived profiles exist, `add` explains the breakdown, for example:
  `You now have 2 students in total (1 active, 1 archived).`

```
> archive S1
Archived S1: Amirah Binte Rahman (Sec 3).
History is kept. Existing lessons are unchanged; restore before scheduling new ones.
> list status/archived
1 student (archived):
  S1  Amirah Binte Rahman (Sec 3)  E Math, A Math  [archived]
> restore S1
Restored S1: Amirah Binte Rahman (Sec 3).
The student is active again. History and existing lessons are unchanged.
```

Archiving and restoration are saved automatically. A completed deletion still removes the profile and
its linked records permanently; archive when you want to keep them.

#### Deleting a student: `delete`

Deletes a student, together with all of their lessons, homework, scores and follow-up tasks. Scores and
follow-up tasks are only mentioned in the output if the student had any. Follow-up tasks that are not linked
to a student are kept.

Format: `delete STUDENT_ID [confirm]`

* If the student has any linked records (lessons, homework, scores or follow-up tasks), `delete STUDENT_ID`
  shows a warning without changing your records. The warning identifies the student and counts all of their
  lessons, homework and scores, including future, paid and cancelled lessons and submitted homework, plus
  their follow-up tasks if they have any.
* To delete the student after reviewing the warning, enter `delete STUDENT_ID confirm`. To keep the
  records, simply enter another command or exit. Repeating the command without `confirm` keeps warning.
* `confirm` is case-insensitive and can also be supplied directly, without first requesting the warning.
* A student with no linked records at all is deleted immediately.

Example: deleting S5, who has one lesson and no homework or scores:

```
> delete S5
Warning: deleting S5: Ryan Koh (Sec 1) will permanently remove:
  1 lesson, 0 homework items and 0 scores.
This cannot be undone. No records have been deleted.
To confirm, type: delete S5 confirm
Otherwise, continue with another command.
> delete S5 confirm
Deleted S5: Ryan Koh (Sec 1)
Also deleted 1 lesson and 0 homework items.
```

> **Caution:** A completed deletion cannot be undone. Run `save FILE_PATH` before confirming if you want
> a backup. The warning applies to student deletion; `delete LESSON_ID` still deletes a single lesson
> immediately.

### Lessons

#### Scheduling a lesson: `schedule`

Schedules a lesson for a student and gives it a lesson ID. With `w/`, schedules the same weekly slot for
several weeks at once.

Format: `schedule STUDENT_ID d/DATE t/TIME s/SUBJECT [dur/MINUTES] [f/FEE] [w/WEEKS]`

Archived students must be restored with `restore STUDENT_ID` before new lessons can be scheduled.
A rejected schedule creates no lessons and consumes no IDs.

* `SUBJECT` must be one of the student's subjects. Case is ignored, and the lesson uses the spelling on the
  student's record.
* `f/FEE` is an optional total charge for this lesson. It overrides the student's default hourly rate.
* Without `f/`, a default rate must be set. The fee is `rate × duration ÷ 60`, rounded to cents
  using half-up rounding. At $60/hour, `dur/90` costs $90.00.
* `dur/MINUTES` is optional, defaults to 60, and accepts whole minutes from 1 to 1440.
  Durations other than 60 minutes are shown in scheduling confirmations, lesson lists and student views.
* Duration is used for billing; TutorLedger still checks conflicts by identical start time, not overlap.
* Two lessons cannot start at the same date and time, even for different students.
* A new lesson starts as not recorded. Lessons with a positive fee start unpaid; $0 lessons are already paid.
* `w/WEEKS` adds one lesson a week, on the same day and at the same time, starting on `DATE`. Each lesson gets
  its own ID, so you can record, reschedule or cancel one week without affecting the others.
* If any week's slot is already taken, no lessons are added, and the error names the lesson in the way.

Examples:

* `schedule S1 d/20-10-2026 t/1600 s/A Math f/60`
* `schedule S2 s/Pure Chemistry f/62.50 d/10-10-2026 t/0900`
* `schedule S1 d/20-10-2026 t/1600 s/A Math f/60 w/10`

Example output:

```
> schedule S1 d/20-10-2026 t/1600 s/A Math f/60
Scheduled L4 for S1 Amirah Binte Rahman
  Tue 20-10-2026 1600, A Math, $60.00

> schedule S1 d/20-10-2026 t/1600 s/A Math f/60 w/10
Scheduled 10 weekly lessons for S1 Amirah Binte Rahman: L11 to L20
  Every Tue 1600, A Math, $60.00 each
  From Tue 20-10-2026 to Tue 22-12-2026
```

#### Logging a past lesson: `log`

Adds a lesson that has already happened, with its attendance and notes, in one step. Use it when you forgot to
schedule a lesson beforehand.

Format: `log STUDENT_ID d/DATE t/TIME s/SUBJECT f/FEE a/ATTENDANCE [note/NOTES]`

* Only a lesson dated today or earlier can be logged. Use `schedule` for a lesson that has not happened yet.
* The same rules as `schedule` and `record` apply. For example, the slot must be free.
* A logged lesson starts unpaid unless its fee is $0, in which case it is already paid.

Examples:

* `log S1 d/06-10-2026 t/1600 s/A Math f/60 a/present note/Completing the square`
* `log S2 d/03-10-2026 t/0900 s/Pure Chemistry f/62.50 a/absent`

Example output:

```
> log S1 d/06-10-2026 t/1600 s/A Math f/60 a/present note/Completing the square
Logged L21 for S1 Amirah Binte Rahman
  Tue 06-10-2026 1600, A Math, $60.00, present
  Notes: Completing the square
```

#### Listing lessons: `lessons`

Shows today's lessons, or this week's, in the order they happen.

Format: `lessons [week]`

* `lessons` shows today's lessons.
* `lessons week` shows lessons from Monday to Sunday of the current week, including any earlier this week.
* A cancelled lesson is still listed, with `(cancelled: REASON)` after it.

Examples:

* `lessons`
* `lessons week`

Example output (today is Wed 14-10-2026):

```
> lessons
Today, Wed 14-10-2026: 1 lesson
  L9  Wed 14-10-2026 1700  S3 Tan Wei Ling  Math

> lessons week
This week, Mon 12-10-2026 to Sun 18-10-2026: 5 lessons
  L3   Tue 13-10-2026 1600  S1 Amirah Binte Rahman  A Math
  L9   Wed 14-10-2026 1700  S3 Tan Wei Ling         Math
  L6   Thu 15-10-2026 1900  S4 Daniel Lim           E Math
  L8   Sat 17-10-2026 0900  S2 Tan Wei Ming         Pure Chemistry
  L10  Sat 17-10-2026 1030  S3 Tan Wei Ling         Science
```

#### Filtering lessons: `filter`

Lists every lesson that matches all the filters you give, earliest first, with the total of their fees. Use it
to look at one slice of your records, such as one student's unpaid lessons this month.

Format: `filter [STUDENT_ID] [from/DATE] [to/DATE] [pay/STATUS]`

* Every filter is optional, and they can be combined. A lesson is listed only if it matches all of them.
* `STUDENT_ID` lists only that student's lessons. It must come straight after `filter`.
* `from/DATE` and `to/DATE` list only lessons on or after, and on or before, those dates. Both dates are
  included. `from/` cannot be later than `to/`.
* `pay/STATUS` lists only lessons that are `paid` or `unpaid`. Unlike `unpaid`, this includes lessons that
  have not happened yet.
* `filter` on its own lists every lesson, past and future.

Examples:

* `filter S1` lists all of Amirah's lessons.
* `filter from/01-10-2026 to/31-10-2026` lists every lesson in October.
* `filter S1 from/01-10-2026 pay/unpaid` lists Amirah's unpaid lessons since 1 October.

Example output:

```
> filter S1 from/01-10-2026 pay/unpaid
2 lessons (S1 Amirah Binte Rahman, from Thu 01-10-2026, unpaid)
  L3  Tue 13-10-2026 1600  S1 Amirah Binte Rahman  A Math  $60.00  unpaid
  L7  Tue 20-10-2026 1600  S1 Amirah Binte Rahman  A Math  $60.00  unpaid
Total fees: $120.00
```

#### Listing topics already covered: `topics`

Review lesson notes to see what you have already covered with a student.

Format: `topics STUDENT_ID [s/SUBJECT]`

* Results show the complete notes, lesson ID, date, time and subject, newest first.
* Only lessons dated today or earlier with attendance `present` or `late` and non-blank notes appear.
  Cancelled, absent, unrecorded and future lessons are excluded. Paid and unpaid lessons both appear.
* Optional `s/SUBJECT` matches the whole historical subject name, ignoring case. It can find a subject
  even after you remove that subject from the student's profile.
* Notes are the topic description for each lesson. They are not split into individual topic names or
  deduplicated; repeated notes remain separate dated entries.
* Enter notes with `log` or `record`, and correct or clear them with `edit LESSON_ID note/NOTES` or
  `edit LESSON_ID note/`. The topics list updates immediately and never changes your data.

Example, after recording two attended A Math lessons with notes:

```
> topics S1 s/A Math
Topics covered with S1 Amirah Binte Rahman (subject: A Math): 2 lessons
  L2  Thu 22-10-2026 1700  A Math
    Completing the square
  L1  Wed 21-10-2026 1600  A Math
    Quadratic equations
```

If there are no matching lessons, the command reports `No topics recorded for ...`.
Missing or unknown student IDs, blank subjects, repeated `s/` prefixes and extra unprefixed arguments
produce errors.

#### Preparing for the next lesson: `next`

Shows everything you need for your next lesson in one place: who it is with, when, where you left off last
time, and what the student still owes you.

Format: `next [STUDENT_ID]`

* `next` shows the next lesson with anyone. `next STUDENT_ID` shows the next lesson with that student.
* The next lesson is the earliest one that has not started yet. Cancelled lessons and lessons that already
  have attendance are skipped.
* **Last topic** is the notes from the student's most recent earlier lesson that has notes.
* **Outstanding homework** lists homework that has not been submitted, soonest due first. Homework past its due
  date is marked `(overdue)`.
* **Unpaid** counts the student's unpaid lessons up to today, the same way as [`owed`](#showing-what-each-student-owes-owed).
* **Follow-ups** lists your pending [follow-up tasks](#follow-up-tasks) for the student, soonest due first.
  This line only appears when there are some.

Examples:

* `next`
* `next S2`

Example output (it is 1000 on Wed 14-10-2026):

```
> next
Next lesson: L9, Wed 14-10-2026 1700
  Student: S3 Tan Wei Ling (Sec 2)
  Subject: Math
  Last topic: Simultaneous equations (L5, Wed 07-10-2026)
  Outstanding homework: 2
    H3  Textbook p.42 Q1-10, due Mon 12-10-2026 (overdue)
    H6  Worksheet 5B, due Fri 16-10-2026
  Unpaid: 1 lesson, $45.00
```

#### Recording a lesson: `record`

Records whether the student came to a lesson, and what you covered.

Format: `record LESSON_ID a/ATTENDANCE [note/NOTES]`

* Only a lesson dated today or earlier can be recorded.
* A cancelled lesson cannot be recorded, because it did not take place.
* Recording a lesson again replaces its attendance, and its notes if `note/` is given. Leave out `note/` to keep
  the notes the lesson already has.
* The notes are shown under the lesson in `view`, ready for the next lesson.

Examples:

* `record L3 a/present note/Discriminant recap. Started simultaneous equations.`
* `record L7 a/absent`

Example output:

```
> record L3 a/present note/Discriminant recap. Started simultaneous equations.
Recorded L3 for S1 Amirah Binte Rahman (Tue 13-10-2026 1600)
  Attendance: present
  Notes: Discriminant recap. Started simultaneous equations.
```

#### Rescheduling a lesson: `reschedule`

Moves a lesson that has not happened yet to a new date, time, or both.

Format: `reschedule LESSON_ID [d/DATE] [t/TIME]`

* Give a new date, a new time, or both. The part you leave out stays the same.
* The new slot must not have passed yet, must be different from the current one, and must not be taken by
  another lesson.
* A lesson that already has attendance or notes has taken place, so it cannot be rescheduled. To correct its
  date or time, use [`edit`](#editing-a-lesson-edit).
* A cancelled lesson cannot be rescheduled. Schedule a make-up lesson instead, so the cancellation stays on
  record.

Examples:

* `reschedule L6 d/16-10-2026 t/1800`
* `reschedule L8 t/1030`

Example output:

```
> reschedule L6 d/16-10-2026 t/1800
Rescheduled L6 for S4 Daniel Lim
  From: Thu 15-10-2026 1900
  To:   Fri 16-10-2026 1800
```

#### Cancelling a lesson: `cancel`

Marks a lesson as called off and records why. A cancelled lesson is kept, so you can tell it apart from a
lesson the student missed.

Format: `cancel LESSON_ID r/REASON`

* A cancelled lesson frees its slot, so another lesson can be scheduled at that time.
* A cancelled lesson is not charged: it is left out of `unpaid`, `owed` and the amount owed in `view`. It is
  also left out of the attendance figures in `progress`.
* A lesson that already has attendance took place, so it cannot be cancelled.
* If any payment was received, including a partial payment, TutorLedger reminds you to refund or carry over
  the amount received. The saved payment is retained; cancellation does not record a refund automatically.
  Unpaid and $0 lessons do not need a refund reminder.
* A cancellation cannot be undone from inside TutorLedger. If you cancelled the wrong lesson, schedule it
  again, or see [Known issues](#known-issues).

Examples:

* `cancel L8 r/Student sick`
* `cancel L10 r/Public holiday`

Example output:

```
> cancel L8 r/Student sick
Cancelled L8 for S2 Tan Wei Ming (Sat 17-10-2026 0900)
  Reason: Student sick
```

#### Editing a lesson: `edit`

Changes the details of an existing lesson, such as moving it to another day.

Format: `edit LESSON_ID [d/DATE] [t/TIME] [s/SUBJECT] [f/FEE] [dur/MINUTES] [a/ATTENDANCE] [note/NOTES]`

* Give at least one value to change. Values you leave out stay the same.
* The same rules as `schedule` and `record` apply to the new values. For example, a lesson cannot be moved to a
  slot another lesson already has, and a lesson dated after today cannot have attendance or notes.
* Type `note/` with nothing after it to clear the lesson's notes.
* `dur/MINUTES` changes duration but keeps the saved fee. Give `f/FEE` explicitly to change the charge.
* Changing the fee keeps the amount already paid and does not change income. Raising a paid lesson's fee
  creates an outstanding balance; reducing it to the amount already paid settles it. A fee below the amount
  already paid is rejected, and none of the other edits in that command are applied.
* A cancelled lesson stays cancelled when edited, and cannot be given attendance.
* This is the same `edit` command used for students. The ID tells TutorLedger which kind of item to change.
* To change whether a lesson is paid, use [`mark`](#marking-a-lesson-paid-or-unpaid-mark).

Examples:

* `edit L6 d/16-10-2026 t/1800`
* `edit L8 f/55`

Example output:

```
> edit L6 d/16-10-2026 t/1800
Edited L6 for S4 Daniel Lim
  Fri 16-10-2026 1800, E Math, $55.00, not recorded, unpaid
```

#### Deleting a lesson: `delete`

Deletes a lesson.

Format: `delete LESSON_ID`

* This is the same `delete` command used for students. The ID tells TutorLedger which kind of item to delete.
* The student and their homework are not affected.

Example: `delete L10`

Example output:

```
> delete L10
Deleted L10 for S3 Tan Wei Ling: Sat 17-10-2026 1030, Science
```

> **Tip:** If a lesson was called off, use [`cancel`](#cancelling-a-lesson-cancel) instead, so you keep a record
> of it. If you do not charge for a lesson the student missed, delete it so it is not counted in what they owe.

### Homework

#### Assigning homework: `assign`

Gives a student a piece of homework with a due date, and gives the homework an ID.

Format: `assign STUDENT_ID DESCRIPTION d/DUE_DATE`

* `DESCRIPTION` is everything between the student ID and the last `d/`, so it may contain spaces, commas and even
  `d/` itself.
* `d/DUE_DATE` must come last.
* The due date may be in the past, for example when you log homework a few days late.

Examples:

* `assign S1 Worksheet 4A d/20-10-2026`
* `assign S2 TYS 2023 Paper 1, Q1-10 d/17-10-2026`

Example output:

```
> assign S1 Worksheet 4A d/20-10-2026
Assigned H4 to S1 Amirah Binte Rahman
  Worksheet 4A, due Tue 20-10-2026
```

#### Marking homework as submitted: `submit`

Marks a piece of homework as handed in, so it no longer shows as outstanding.

Format: `submit HOMEWORK_ID`

* Homework that is already submitted cannot be submitted again.

Example: `submit H1`

Example output:

```
> submit H1
Marked H1 as submitted: Worksheet 3B (S1 Amirah Binte Rahman)
```

#### Listing outstanding homework: `homework`

Lists homework that has not been submitted yet, for all students or for one.

Format: `homework [STUDENT_ID]`

* Homework due soonest is listed first.
* Homework past its due date is marked `OVERDUE`.

Examples:

* `homework`
* `homework S1`

Example output (today is Wed 14-10-2026):

```
> homework
3 homework items outstanding
  H2  S4 Daniel Lim           Algebra drill 2          due Mon 12-10-2026  OVERDUE
  H3  S2 Tan Wei Ming         TYS 2023 Paper 1, Q1-10  due Sat 17-10-2026
  H4  S1 Amirah Binte Rahman  Worksheet 4A             due Tue 20-10-2026
```

### Payments

#### Marking a lesson paid or unpaid: `mark`

Records that a lesson has been paid for. Marking it unpaid undoes a mistake.

Format: `mark LESSON_ID STATUS`

* `STATUS` is `paid` or `unpaid`.
* New lessons with a positive fee start unpaid. A $0 lesson is always paid, contributes no income and
  stays paid even if you use `mark LESSON_ID unpaid`.
* A lesson can be marked paid before it takes place, for example when a family pays in advance.
* `mark` sets the whole lesson fee to paid or resets it to unpaid. Use `pay` to record an amount that may be partial.

Examples:

* `mark L1 paid`
* `mark L2 unpaid`

Example output:

```
> mark L1 paid
Marked L1 as paid.
```

#### Recording a payment across lessons: `pay`

Records one payment for a student and allocates it to the listed lessons in the order given. Each lesson is
covered up to its remaining balance; any leftover amount continues to the next lesson. A final lesson may be
partially paid. The amount cannot exceed the combined outstanding balances of the listed lessons.

Format: `pay STUDENT_ID AMOUNT LESSON_ID [LESSON_ID...]`

Example: if L37 costs $62.50 and L38 costs $55.00, this records $62.50 against L37 and the remaining $17.50
against L38:

```
> pay S1 80 L37 L38
Recorded payment of $80.00 for 2 lessons: L37, L38.
```

The remaining $37.50 on L38 stays outstanding and is included in `unpaid` and `owed`. Partial allocations
are saved and included in income totals.

#### Listing unpaid lessons: `unpaid`

Lists lessons that have taken place but have not been paid for, for all students or for one.

Format: `unpaid [STUDENT_ID]`

* Only lessons dated today or earlier are listed. A partially paid lesson remains on this list with its remaining balance.
* Lessons are listed oldest first, so the longest-outstanding are at the top.

Examples:

* `unpaid`
* `unpaid S1`

Example output (today is Wed 14-10-2026):

```
> unpaid S1
        ____________________________________________________________
2 unpaid lessons
L2  Tue 06-10-2026  S1 Amirah Binte Rahman  A Math  $60.00
L3  Tue 13-10-2026  S1 Amirah Binte Rahman  A Math  $60.00
       ____________________________________________________________
```

#### Showing what each student owes: `owed`

Totals the unpaid lessons for each student, so you know how much to ask each family for.

Format: `owed`

* Counts unpaid lessons dated today or earlier, the same lessons `unpaid` lists.
* Students who owe nothing are left out. If nobody owes anything, TutorLedger says so.
* `owed` takes no values. Typing anything after it gives an error.

Example: `owed`

Example output (today is Wed 14-10-2026):

```
> owed
        ____________________________________________________________
Outstanding balances (5 students)
S1 Amirah Binte Rahman  $120.00  (2 lessons)
S2 Tan Wei Ming  $62.50  (1 lesson)
S3 Tan Wei Ling  $50.00  (1 lesson)
S4 Daniel Lim  $55.00  (1 lesson)
S5 Ryan Koh  $45.00  (1 lesson)
       ____________________________________________________________
```

#### Viewing a student's fee statement: `fee_statement`

Lists every lesson fee for one student, including lessons paid in advance, in date and time order. Each entry
shows the lesson ID, date, subject, fee, and current payment status. A partially paid lesson also shows its
amount paid so far.

Format: `fee_statement STUDENT_ID`

Example: `fee_statement S1`

Example output:

```
> fee_statement S1
        ____________________________________________________________
Payment history for S1 Amirah Binte Rahman (2 lessons)
L2  Tue 06-10-2026  A Math                $60.00  paid
L3  Tue 13-10-2026  A Math                $60.00  unpaid
       ____________________________________________________________
```

#### Viewing income by month or year: `income`

Shows amounts received for non-cancelled lessons, grouped by lesson date. Each listed payment includes its
lesson ID, date, student, subject and amount paid; partial payments are included. Lessons with no payment
are not included.

Format: `income MM-YYYY` or `income YYYY`

* Use a month and year, such as `income 09-2026`, to see the total and payments for that month.
* Use a year, such as `income 2026`, to see the total and payments for each of its 12 months.
* Months with no paid lessons are shown with a `$0.00` total.

Example: `income 09-2026`

```
> income 09-2026
        ____________________________________________________________
Income for September 2026: $62.50
  L37  Tue 22-09-2026  S1 Tan Wei Ming  Pure Chemistry  $62.50
       ____________________________________________________________
```

### Scores and progress

#### Recording a test score: `score`

Records a student's result for a test or assessment, and shows how it compares with their previous test.

Format: `score STUDENT_ID TEST_NAME m/MARKS/TOTAL [d/DATE]`

* `TEST_NAME` is everything between the student ID and the first `m/` or `d/`, so it may contain spaces.
* `MARKS/TOTAL` is the marks scored and the maximum marks, such as `m/28/40`. Half marks such as `m/17.5/20`
  are accepted. The marks cannot be more than the total.
* `DATE` is when the test was taken. If it is left out, today's date is used.
* Scores are compared as percentages, so tests with different totals can be compared fairly. The comparison
  is with the student's test dated just before this one.

Examples:

* `score S1 WA1 m/28/40 d/07-09-2026`
* `score S1 Mid-year exam m/55/100 d/21-09-2026`

Example output:

```
> score S1 Mid-year exam m/55/100 d/21-09-2026
Recorded A2 for S1 Amirah Binte Rahman
  Mid-year exam: 55/100 (55.0%) on Mon 21-09-2026
  Down 15.0 points from WA1 (70.0%)
```

#### Listing a student's scores: `scores`

Lists every score for one student, oldest first, so you can see whether they are improving.

Format: `scores STUDENT_ID`

* The last column shows the change in percentage points from the test before.
* The trend compares the first test with the latest one. At least 2 scores are needed for a trend.

Example: `scores S1`

Example output:

```
> scores S1
3 scores for S1 Amirah Binte Rahman
  A1  Mon 07-09-2026  WA1            28/40    70.0%
  A2  Mon 21-09-2026  Mid-year exam  55/100   55.0%  -15.0
  A3  Mon 12-10-2026  WA2            33/40    82.5%  +27.5
Trend: improving, up 12.5 points from the first test to the latest
```

#### Summarising a student's progress: `progress`

Summarises a student's attendance and scores over a period, such as a term, for an end-of-term review.

Format: `progress STUDENT_ID [from/DATE] [to/DATE]`

* Without `from/`, the summary starts from the student's earliest record. Without `to/`, it ends today.
* Attendance counts lessons in the period that have already taken place. Arriving late counts as attending.
  Lessons whose attendance has not been recorded are listed separately and do not affect the rate.
* Scores are those dated within the period, with their average and trend.

Examples:

* `progress S1`
* `progress S1 from/15-09-2026 to/14-10-2026`

Example output (today is Wed 14-10-2026):

```
> progress S1
Progress for S1 Amirah Binte Rahman, up to Wed 14-10-2026
Attendance: attended 2 of 3 recorded lessons (66.7%)
  1 present, 1 late, 1 absent
Scores: 3 tests, average 69.2%
  A1  Mon 07-09-2026  WA1            28/40    70.0%
  A2  Mon 21-09-2026  Mid-year exam  55/100   55.0%  -15.0
  A3  Mon 12-10-2026  WA2            33/40    82.5%  +27.5
Trend: improving, up 12.5 points from the first test to the latest

> progress S1 from/15-09-2026
Progress for S1 Amirah Binte Rahman, Tue 15-09-2026 to Wed 14-10-2026
Attendance: attended 0 of 1 recorded lesson (0.0%)
  0 present, 0 late, 1 absent
Scores: 2 tests, average 68.8%
  A2  Mon 21-09-2026  Mid-year exam  55/100   55.0%
  A3  Mon 12-10-2026  WA2            33/40    82.5%  +27.5
Trend: improving, up 27.5 points from the first test to the latest
```

### Follow-up tasks

Follow-up tasks are reminders for yourself, such as bringing a worksheet or returning a marked script, so you
do not forget them between lessons. A task can be linked to a student and can have a due date, but neither is
required.

#### Logging a follow-up task: `task`

Adds a reminder and gives it an ID.

Format: `task [STUDENT_ID] DESCRIPTION [d/DUE_DATE]`

* If the first word is a student ID, the task is linked to that student. Their pending tasks are then shown by
  [`next`](#preparing-for-the-next-lesson-next) before their lesson.
* `DESCRIPTION` is everything after the student ID (if any) and before the last `d/`, so it may contain spaces
  and commas.
* `d/DUE_DATE` is optional and must come last. It may be in the past.

Examples:

* `task S1 Bring Worksheet 4B d/16-10-2026`
* `task Print term calendar`

Example output:

```
> task S1 Bring Worksheet 4B d/16-10-2026
Added T1: Bring Worksheet 4B
  for S1 Amirah Binte Rahman, by Fri 16-10-2026

> task Print term calendar
Added T2: Print term calendar
```

#### Listing follow-up tasks: `tasks`

Lists the follow-up tasks you have not done yet, for everyone or for one student.

Format: `tasks [STUDENT_ID]`

* Tasks with a due date are listed first, soonest due first. Tasks without a due date come last.
* A task past its due date is marked `OVERDUE`.
* `tasks STUDENT_ID` lists only the tasks linked to that student.

Examples:

* `tasks`
* `tasks S1`

Example output (today is Wed 14-10-2026):

```
> tasks
3 follow-up tasks pending
  T3  Return graded test   S2 Tan Wei Ming         by Mon 12-10-2026  OVERDUE
  T1  Bring Worksheet 4B   S1 Amirah Binte Rahman  by Fri 16-10-2026
  T2  Print term calendar                          no date
```

#### Marking a follow-up task as done: `done`

Ticks off a follow-up task, so it no longer appears in `tasks` or `next`.

Format: `done TASK_ID`

* A task that is already done cannot be marked done again.

Example: `done T3`

Example output:

```
> done T3
Marked T3 as done: Return graded test
```

### Storage

TutorLedger loads `data/tutorledger.txt` when it starts and tells you what it found:

```
No saved data yet. TutorLedger will create data/tutorledger.txt when you add something.
```

```
Loaded 4 students, 9 lessons and 4 homework items from data/tutorledger.txt.
```

If a change cannot be written to the file (for example, because the folder is read-only), TutorLedger shows a
`Warning:` after the command. The change is kept while TutorLedger is open, but is lost when you exit unless a
later save succeeds.

#### Saving the data: `save`

TutorLedger saves your data to `data/tutorledger.txt` automatically after every command that changes it,
so you never need to save manually. Use `save` when you want to confirm that your data is saved and see
what was written, or to write a backup copy somewhere else.

Format: `save [FILE_PATH]`

* With no `FILE_PATH`, saves to `data/tutorledger.txt` and reports how many records were written.
* With a `FILE_PATH`, writes a copy of all your data to that file. TutorLedger carries on saving to
  `data/tutorledger.txt` as normal.
* `FILE_PATH` is relative to the folder you run TutorLedger from. Any folders in it that do not exist yet
  are created.
* If the data file or its `data` folder does not exist, TutorLedger creates it the first time it saves.

Examples:

* `save`
* `save backups/before-edit.txt`

Example output:

```
> save
Saved 4 students, 9 lessons and 4 homework items
to data/tutorledger.txt

> save backups/before-edit.txt
Saved a copy to backups/before-edit.txt
TutorLedger will keep saving to data/tutorledger.txt.
```

> **Caution:** If a file already exists at `FILE_PATH`, it is replaced. Use a new file name for each backup
> you want to keep.

#### Reloading the data file: `reload`

All your data is kept in a plain-text file at `data/tutorledger.txt`, inside the folder you run TutorLedger
from. Advanced users can open this file in any text editor to fix or bulk-edit records. Use `reload` to read
the file into TutorLedger again after editing it, without restarting.

Format: `reload`

* Replaces the data TutorLedger is currently showing with the contents of `data/tutorledger.txt`. Nothing is
  lost by doing this, because every change you made in TutorLedger was already saved to that file.
* A line that is not in the format TutorLedger expects is skipped, and its line number and the reason are
  shown so you can fix it.
* If a student's line is skipped, the lines for that student's lessons and homework are skipped too, because
  they belong to a student TutorLedger could not load. Fix the student's line and they all come back.
* If `data/tutorledger.txt` cannot be found, TutorLedger keeps the data it already has and tells you the
  file is missing.
* Words typed after `reload` are ignored.

Example output:

```
> reload
Reloaded data/tutorledger.txt
Loaded 2 students, 2 lessons and 3 homework items.
Skipped 1 line that could not be read:
  Line 11: Date must be a real date in DD-MM-YYYY format.
Fix the line and run reload, or it is removed the next time TutorLedger saves.

> reload
No data file found at data/tutorledger.txt.
Your current data has not been changed.
```

**Editing the file safely:**

1. Run `save backups/before-edit.txt` to keep a copy.
2. Edit `data/tutorledger.txt` in a text editor and save it.
3. Run `reload` in TutorLedger before running any other command.

**The file format.** The top of the file explains it. Each line is one record, with its fields separated by
`|`. Lines starting with `#` are ignored.

```
NEXT | S3 | L2 | H2 | A2 | T3
S | S1 | Amirah Binte Rahman | Sec 3 | 91234567 | E Math | A Math
L | L1 | S1 | 15-09-2026 | 1600 | A Math | 60 | present | paid | Quadratics \| discriminant
H | H1 | S1 | 29-09-2026 | outstanding | Worksheet 4A
A | A1 | S1 | 07-09-2026 | 28 | 40 | WA1
T | T1 | pending | 16-10-2026 | S1 | Bring Worksheet 4B
T | T2 | done |  |  | Print term calendar
```

| Line   | Fields, in order                                                                                    |
|--------|-----------------------------------------------------------------------------------------------------|
| `NEXT` | the IDs the next new student, lesson, homework item, score and follow-up task will get              |
| `R`    | student ID, default hourly rate; omit the record when no rate is set |
| `ARCHIVED` | student ID; omit the record for an active student |
| `S`    | student ID, name, level, phone number, then any number of subjects                                  |
| `L`    | lesson ID, student ID, date, time, subject, fee, attendance (`not recorded`, `present`, `absent` or `late`), `paid`, `partial` or `unpaid`, notes, cancellation reason (empty if active), duration in minutes, amount paid |
| `H`    | homework ID, student ID, due date, `outstanding` or `submitted`, description                        |
| `A`    | score ID, student ID, test date, marks, total marks, test name                                      |
| `T`    | task ID, `pending` or `done`, due date (empty if none), student ID (empty if none), description     |

* Older lesson lines without duration load as 60 minutes. Existing student lines without `R` records
  load with no default rate. New saves include duration and amount paid; keep a backup if you may return
  to an older version.
* New lesson lines have thirteen fields: the cancellation reason comes before duration and amount paid.
  For an active 90-minute lesson with $20 received, end the line with `NOTES |  | 90 | 20`.
  Old eleven-field cancellation and twelve-field duration lines remain accepted. Free lessons from older
  files are loaded as paid without adding income.
* If you saved data with the earlier hourly-rate branch, keep its generated `NOTES | MINUTES` header
  when loading it. That identifies its old duration field; without the header, an eleven-field row is
  interpreted as an old cancellation record. A new save uses the thirteen-field layout above.
* Example rate record: `R | S1 | 60`. Clearing a rate removes its record. Loading never recalculates saved fees.
  If a rate record is invalid, the student and their lessons still load; the invalid rate is reported by line number.
* Example archive record: `ARCHIVED | S1`. Without this line the student is active. Restoring removes it.
  Archive markers and rates may appear before or after the student line. Invalid or duplicate markers
  are reported by line number while other records still load.
* Keep a backup before returning to an older build: builds without archiving report archive markers as
  unknown records and load those students as active. Saving with an older build loses archive status.
* Values follow the same rules as when you type them in a command, for example dates as `DD-MM-YYYY`.
* To put a `|` inside a value, write `\|`. To put a `\` there, write `\\`.
* Lines may be in any order, and spaces around `|` do not matter.
* The `NEXT` line stops the IDs of deleted items from being given out again. If it is missing or too low,
  TutorLedger works out safe values from the IDs in the file.

> **Caution:** If you edit the data file while TutorLedger is open and then run a command that changes data
> without running `reload` first, TutorLedger saves over your edits. A skipped line is also removed from the
> file the next time TutorLedger saves, so fix it and reload again before carrying on. The same checks run
> when TutorLedger starts.

### Help and exiting

#### Viewing help: `help`

Shows every command TutorLedger accepts, with its format and one example, plus the supported school-level
ranges and input examples.

Format: `help`

Example output:

```
> help
Commands (UPPER_CASE = value you supply, [ ] = optional)

STUDENTS
  add n/NAME l/LEVEL p/PHONE_NUMBER [s/SUBJECT]... [r/RATE]
      add n/Amirah Binte Rahman l/Sec 3 p/91234567 s/E Math s/A Math
  subject STUDENT_ID s/SUBJECT [s/SUBJECT]...
      subject S1 s/E Math s/A Math
  view STUDENT_ID
      view S1
  list [NAME] [status/STATUS]
      list wei
  archive STUDENT_ID
      archive S5
  restore STUDENT_ID
      restore S5
  edit STUDENT_ID [n/NAME] [l/LEVEL] [p/PHONE_NUMBER] [s/SUBJECT]... [r/RATE]
      edit S1 l/Sec 4
  delete STUDENT_ID [confirm]
      delete S5 confirm

LESSONS
  schedule STUDENT_ID d/DATE t/TIME s/SUBJECT [dur/MINUTES] [f/FEE] [w/WEEKS]
      schedule S1 d/22-09-2026 t/1600 s/A Math f/60 w/10
  log STUDENT_ID d/DATE t/TIME s/SUBJECT f/FEE a/ATTENDANCE [note/NOTES]
      log S1 d/15-09-2026 t/1600 s/A Math f/60 a/present note/Discriminant
  lessons [week]
      lessons week
  filter [STUDENT_ID] [from/DATE] [to/DATE] [pay/STATUS]
      filter S1 from/01-09-2026 pay/unpaid
  next [STUDENT_ID]
      next
  topics STUDENT_ID [s/SUBJECT]
      topics S1 s/A Math
  record LESSON_ID a/ATTENDANCE [note/NOTES]
      record L41 a/present note/Started simultaneous equations
  reschedule LESSON_ID [d/DATE] [t/TIME]
      reschedule L42 d/25-09-2026 t/1800
  cancel LESSON_ID r/REASON
      cancel L43 r/Student sick
  edit LESSON_ID [d/DATE] [t/TIME] [s/SUBJECT] [f/FEE] [dur/MINUTES] [a/ATTENDANCE] [note/NOTES]
      edit L42 d/25-09-2026 t/1800
  delete LESSON_ID
      delete L44

HOMEWORK
  assign STUDENT_ID DESCRIPTION d/DUE_DATE
      assign S1 Worksheet 4A d/29-09-2026
  submit HOMEWORK_ID
      submit H7
  homework [STUDENT_ID]
      homework S1

PAYMENTS
  mark LESSON_ID STATUS
      mark L37 paid
  unpaid [STUDENT_ID]
      unpaid S1
  owed
      owed
  fee_statement STUDENT_ID
      fee_statement S1
  income MM-YYYY or income YYYY
      income 09-2026

SCORES AND PROGRESS
  score STUDENT_ID TEST_NAME m/MARKS/TOTAL [d/DATE]
      score S1 WA1 m/28/40 d/07-09-2026
  scores STUDENT_ID
      scores S1
  progress STUDENT_ID [from/DATE] [to/DATE]
      progress S1 from/01-07-2026 to/14-10-2026

FOLLOW-UP TASKS
  task [STUDENT_ID] DESCRIPTION [d/DUE_DATE]
      task S1 Bring Worksheet 4B d/20-10-2026
  tasks [STUDENT_ID]
      tasks
  done TASK_ID
      done T1

STORAGE
  save [FILE_PATH]
      save backups/term1.txt
  reload
      reload

OTHER
  help
      help
  exit
      exit

LEVELS (l/LEVEL; case-insensitive, optional space)
  Pri 1 to Pri 6, Sec 1 to Sec 5, or JC 1 to JC 2
      Examples: l/Pri5, l/Sec 3, l/JC1

STUDENT DELETION
  With lessons, homework, scores or tasks: confirm is required. With none: immediate.
  Run delete S5 to review the warning, then delete S5 confirm to delete.
  To keep the records, continue with another command.

STUDENT STATUS
  list shows active students. Use status/active, status/archived or status/all.
  Archiving keeps history and existing lessons. Restore before scheduling new lessons.

SHORTCUTS (type the short form instead of the command word)
  v    view
  sch  schedule
  ls   lessons
  rec  record
  fl   filter
  as   assign
  sub  submit
  hw   homework
  m    mark
  up   unpaid
  h    help
```

#### Shortcuts for common commands

The commands you use most have a short form. Type the short form in place of the command word; everything
after it stays the same. For example, `ls week` is the same as `lessons week`, and
`sch S1 d/20-10-2026 t/1600 s/A Math f/60` is the same as `schedule S1 d/20-10-2026 t/1600 s/A Math f/60`.

| Short form | Command    | Short form | Command    |
|------------|------------|------------|------------|
| `v`        | `view`     | `as`       | `assign`   |
| `sch`      | `schedule` | `sub`      | `submit`   |
| `ls`       | `lessons`  | `hw`       | `homework` |
| `rec`      | `record`   | `m`        | `mark`     |
| `fl`       | `filter`   | `up`       | `unpaid`   |
| `h`        | `help`     |            |            |

* Short forms are not case-sensitive, like command words.
* Only the first word is replaced, so a short form inside a value (such as `assign S1 hw ls d/20-10-2026`) is
  left as it is.
* `help` (or `h`) lists all the short forms under `SHORTCUTS`.

#### Exiting TutorLedger: `exit`

Closes TutorLedger. Your data has already been saved after every change, so there is nothing to save first.

Format: `exit`

* `exit` must be typed on its own. `exit now` is not recognised as a command.

## FAQ

**Q**: How do I transfer my data to another computer?

**A**: On the old computer, run `save FILE_PATH` to write a copy somewhere you can carry, such as a USB
drive. On the new computer, put that file at `data/tutorledger.txt` inside the folder you run TutorLedger
from, then start TutorLedger.

**Q**: I deleted something by mistake. Can I get it back?

**A**: Only if you have a backup from `save FILE_PATH`. Copy the backup over `data/tutorledger.txt` and run
`reload`. Everything goes back to how it was when the backup was made, so changes made since then are lost.
An unconfirmed student-deletion warning has not removed anything; just continue with another command.

**Q**: A student missed a lesson and I am not charging for it. What should I do?

**A**: If the lesson was called off beforehand, cancel it with `cancel LESSON_ID r/REASON`. It is then not
charged, and you keep a record of why it did not happen. If the student simply did not turn up, record them
as absent; if you also do not want to charge for it, delete the lesson with `delete LESSON_ID`.

**Q**: Does TutorLedger need an internet connection?

**A**: No. Everything is stored on your own computer.

## Known issues

1. **Homework cannot be edited or deleted from inside TutorLedger.** To fix a mistake, edit its line in
   `data/tutorledger.txt` and run `reload`. Deleting a student also deletes their homework.
1. **Notes cannot contain a word followed directly by `/`.** In `NOTES`, text such as ` and/or` or ` km/h` is
   read as an unknown prefix, and the command is rejected with `Error: Unexpected prefix and/.` (or similar).
   Write "and or" or "km per h" instead. Homework descriptions are not affected.
1. **A lesson's attendance cannot be set back to "not recorded"** once it has been recorded. Edit its line in
   `data/tutorledger.txt` and run `reload` if you need to.
1. **A cancellation cannot be undone from inside TutorLedger.** Remove the reason at the end of the lesson's
   line in `data/tutorledger.txt` and run `reload`.

## Command summary

| Action                  | Format, Examples                                                                                                        |
|-------------------------|-------------------------------------------------------------------------------------------------------------------------|
| **Add student**         | `add n/NAME l/LEVEL p/PHONE_NUMBER [s/SUBJECT]… [r/RATE]`<br>e.g. `add n/Amirah Binte Rahman l/Sec 3 p/91234567 s/E Math`       |
| **Add subjects**        | `subject STUDENT_ID s/SUBJECT [s/SUBJECT]…`<br>e.g. `subject S1 s/E Math s/A Math`                                      |
| **View student**        | `view STUDENT_ID`<br>e.g. `view S1`                                                                                      |
| **List / find students**| `list [NAME]`<br>e.g. `list wei`                                                                                         |
| **List archived / all** | `list [NAME] status/archived` or `list [NAME] status/all` |
| **Archive / restore** | `archive STUDENT_ID` / `restore STUDENT_ID`<br>e.g. `archive S1`, then `restore S1` |
| **Edit student**        | `edit STUDENT_ID [n/NAME] [l/LEVEL] [p/PHONE_NUMBER] [s/SUBJECT]… [r/RATE]`<br>e.g. `edit S1 l/Sec 4`                            |
| **Set / clear hourly rate** | `edit STUDENT_ID r/RATE` or `edit STUDENT_ID r/`<br>e.g. `edit S1 r/60` |
| **Delete student**      | `delete STUDENT_ID [confirm]`<br>e.g. `delete S5`, then `delete S5 confirm` for a student with lessons |
| **Schedule lesson**     | `schedule STUDENT_ID d/DATE t/TIME s/SUBJECT [dur/MINUTES] [f/FEE] [w/WEEKS]`<br>e.g. `schedule S1 d/20-10-2026 t/1600 s/A Math f/60 w/10` |
| **Log past lesson**     | `log STUDENT_ID d/DATE t/TIME s/SUBJECT f/FEE a/ATTENDANCE [note/NOTES]`<br>e.g. `log S1 d/06-10-2026 t/1600 s/A Math f/60 a/present` |
| **List lessons**        | `lessons [week]`<br>e.g. `lessons week`                                                                                  |
| **Filter lessons**      | `filter [STUDENT_ID] [from/DATE] [to/DATE] [pay/STATUS]`<br>e.g. `filter S1 from/01-10-2026 pay/unpaid`                 |
| **Next lesson**         | `next [STUDENT_ID]`<br>e.g. `next`                                                                                       |
| **Covered topics** | `topics STUDENT_ID [s/SUBJECT]`<br>e.g. `topics S1 s/A Math` |
| **Record lesson**       | `record LESSON_ID a/ATTENDANCE [note/NOTES]`<br>e.g. `record L3 a/present note/Started simultaneous equations`          |
| **Reschedule lesson**   | `reschedule LESSON_ID [d/DATE] [t/TIME]`<br>e.g. `reschedule L6 d/16-10-2026 t/1800`                                     |
| **Cancel lesson**       | `cancel LESSON_ID r/REASON`<br>e.g. `cancel L8 r/Student sick`                                                           |
| **Edit lesson**         | `edit LESSON_ID [d/DATE] [t/TIME] [s/SUBJECT] [f/FEE] [dur/MINUTES] [a/ATTENDANCE] [note/NOTES]`<br>e.g. `edit L6 d/16-10-2026 t/1800` |
| **Delete lesson**       | `delete LESSON_ID`<br>e.g. `delete L10`                                                                                  |
| **Assign homework**     | `assign STUDENT_ID DESCRIPTION d/DUE_DATE`<br>e.g. `assign S1 Worksheet 4A d/20-10-2026`                                |
| **Submit homework**     | `submit HOMEWORK_ID`<br>e.g. `submit H1`                                                                                 |
| **List homework**       | `homework [STUDENT_ID]`<br>e.g. `homework S1`                                                                            |
| **Mark paid / unpaid**  | `mark LESSON_ID STATUS`<br>e.g. `mark L1 paid`                                                                           |
| **Record a payment**    | `pay STUDENT_ID AMOUNT LESSON_ID [LESSON_ID...]`<br>e.g. `pay S1 80 L37 L38`                                             |
| **List unpaid lessons** | `unpaid [STUDENT_ID]`<br>e.g. `unpaid S1`                                                                                |
| **Show amount owed**    | `owed`                                                                                                                   |
| **Fee statement**       | `fee_statement STUDENT_ID`<br>e.g. `fee_statement S1`                                                                    |
| **Show income by month or year** | `income MM-YYYY` or `income YYYY`<br>e.g. `income 09-2026`                                                     |
| **Record score**        | `score STUDENT_ID TEST_NAME m/MARKS/TOTAL [d/DATE]`<br>e.g. `score S1 WA1 m/28/40 d/07-09-2026`                         |
| **List scores**         | `scores STUDENT_ID`<br>e.g. `scores S1`                                                                                  |
| **Progress summary**    | `progress STUDENT_ID [from/DATE] [to/DATE]`<br>e.g. `progress S1 from/01-07-2026`                                       |
| **Log follow-up task**  | `task [STUDENT_ID] DESCRIPTION [d/DUE_DATE]`<br>e.g. `task S1 Bring Worksheet 4B d/16-10-2026`                    |
| **List follow-up tasks**| `tasks [STUDENT_ID]`<br>e.g. `tasks S1`                                                                                  |
| **Finish follow-up task**| `done TASK_ID`<br>e.g. `done T1`                                                                                        |
| **Save**                | `save [FILE_PATH]`<br>e.g. `save backups/term1.txt`                                                                      |
| **Reload data file**    | `reload`                                                                                                                 |
| **Help**                | `help`                                                                                                                   |
| **Exit**                | `exit`                                                                                                                   |
