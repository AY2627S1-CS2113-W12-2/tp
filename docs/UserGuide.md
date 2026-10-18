# User Guide

## Introduction

TutorLedger is a desktop app for independent tutors who teach a small group of secondary-school students. It
keeps your students, lessons, homework and payments in one place, and it is built to be used by typing: every
task is a single command, so a tutor who types quickly can log a lesson faster than they could fill in a form.

TutorLedger does not teach online, message students or parents, or process payments. It is a personal record of
your own tutoring work.

## Contents

* [Quick start](#quick-start)
* [Notes about the command format](#notes-about-the-command-format)
* [Features](#features)
  * [Student book](#student-book): `add`, `subject`, `view`, `list`, `edit`, `delete`
  * [Lessons](#lessons): `schedule`, `lessons`, `record`, `edit`, `delete`
  * [Homework](#homework): `assign`, `submit`, `homework`
  * [Payments](#payments): `mark`, `unpaid`, `owed`
  * [Storage](#storage): `save`, `reload`
  * [Help and exiting](#help-and-exiting): `help`, `exit`
* [FAQ](#faq)
* [Known issues](#known-issues)
* [Command summary](#command-summary)

## Quick start

1. Ensure that you have Java 25 or above installed. To check, run `java -version` in a terminal.
1. Download the latest `tutorledger.jar` from the project's
   [Releases page](https://github.com/AY2627S1-CS2113-W12-2/tp/releases).
1. Put the file in the folder you want TutorLedger to keep its data in.
1. Open a terminal in that folder and run `java -jar tutorledger.jar`.
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

An ID is never reused, even after its item is deleted.

**Values.**

| Value            | Prefix  | Accepted input                                                                  | Example              |
|------------------|---------|---------------------------------------------------------------------------------|----------------------|
| `NAME`           | `n/`    | Letters, with single spaces, hyphens or apostrophes between them.               | `n/Tan Wei Ming`     |
| `LEVEL`          | `l/`    | `Sec 1` to `Sec 5`. The space is optional.                                      | `l/Sec 3`            |
| `PHONE_NUMBER`   | `p/`    | Exactly 8 digits. Siblings may share a number.                                  | `p/91234567`         |
| `SUBJECT`        | `s/`    | The subject as you normally write it. Not case-sensitive.                       | `s/Pure Chemistry`   |
| `DATE`           | `d/`    | A real calendar date as `DD-MM-YYYY`.                                           | `d/20-10-2026`       |
| `DUE_DATE`       | `d/`    | Same as `DATE`.                                                                 | `d/20-10-2026`       |
| `TIME`           | `t/`    | 24-hour time as `HHMM`, from `0000` to `2359`.                                  | `t/1600`             |
| `FEE`            | `f/`    | Dollars without the `$`, up to 2 decimal places. Zero or more.                  | `f/62.50`            |
| `ATTENDANCE`     | `a/`    | `present`, `absent` or `late`.                                                  | `a/late`             |
| `NOTES`          | `note/` | Any text: what was covered and anything to remember. See [Known issues](#known-issues). | `note/Started vectors` |
| `STATUS`         | (none)  | `paid` or `unpaid`.                                                             | `paid`               |
| `DESCRIPTION`    | (none)  | Any text, written straight after the student ID.                                | `Worksheet 3B`       |
| `FILE_PATH`      | (none)  | A file path relative to the folder you run TutorLedger from.                    | `backups/term1.txt`  |

The example outputs in this guide were all taken from one session on Wed 14-10-2026, so they share the same
students, lessons and homework.

## Features

### Student book

#### Adding a student: `add`

Feature F1.1 · Story US2

Adds a student to TutorLedger and gives them a student ID.

Format: `add n/NAME l/LEVEL p/PHONE_NUMBER`

* All three values are required.
* A student with the same name (ignoring case) and phone number as an existing student is not added, since they
  are almost certainly the same person.

Examples:

* `add n/Amirah Binte Rahman l/Sec 3 p/91234567`
* `add l/Sec 4 p/98765432 n/Tan Wei Ming`

Example output:

```
> add n/Amirah Binte Rahman l/Sec 3 p/91234567
Added S1: Amirah Binte Rahman (Sec 3), 91234567
You now have 1 student.
```

> **Tip:** Give the student their subjects straight away with `subject`. A lesson can only be scheduled in a
> subject the student takes.

#### Adding subjects to a student: `subject`

Feature F1.2 · Story US3

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

Feature F1.3 · Stories US9, US10, US11, US25

Shows everything about one student on one screen: their details and subjects, every lesson with its attendance,
payment status and notes, their outstanding homework, and how much they owe. Use it in the minutes before a
lesson.

Format: `view STUDENT_ID`

* Lessons are shown newest first, so the last lesson's notes are near the top.
* The amount owed counts unpaid lessons dated today or earlier. Lessons still to come are not counted.

Example: `view S1`

Example output (today is Wed 14-10-2026):

```
> view S1
S1  Amirah Binte Rahman  (Sec 3)  91234567
Subjects: E Math, A Math
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

Feature F1.4 · Story US36

Lists all your students, or only the students whose name contains what you type.

Format: `list [NAME]`

* With no `NAME`, every student is listed in ID order.
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

Feature F1.5 · Story US30

Changes the details of an existing student.

Format: `edit STUDENT_ID [n/NAME] [l/LEVEL] [p/PHONE_NUMBER] [s/SUBJECT]…`

* Give at least one value to change. Values you leave out stay the same.
* Giving any `s/` replaces *all* of the student's subjects with the ones listed, so list every subject you want
  them to keep. Type `s/` with nothing after it to remove every subject.
* Lessons already scheduled keep the subject they were scheduled with.

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

#### Deleting a student: `delete`

Feature F1.6 · Story US31

Deletes a student, together with all of their lessons and homework.

Format: `delete STUDENT_ID`

Example: `delete S5`

Example output:

```
> delete S5
Deleted S5: Ryan Koh (Sec 1)
Also deleted 1 lesson and 0 homework items.
```

> **Caution:** TutorLedger does not ask for confirmation, and a deletion cannot be undone. If you are not sure,
> run `save FILE_PATH` first so you have a backup to go back to.

### Lessons

#### Scheduling a lesson: `schedule`

Feature F2.1 · Story US4

Schedules a lesson for a student and gives it a lesson ID.

Format: `schedule STUDENT_ID d/DATE t/TIME s/SUBJECT f/FEE`

* `SUBJECT` must be one of the student's subjects. Case is ignored, and the lesson uses the spelling on the
  student's record.
* `FEE` is what you charge for this lesson. It is used to work out how much a student owes.
* Two lessons cannot start at the same date and time, even for different students.
* A new lesson starts as not recorded and unpaid.

Examples:

* `schedule S1 d/20-10-2026 t/1600 s/A Math f/60`
* `schedule S2 s/Pure Chemistry f/62.50 d/10-10-2026 t/0900`

Example output:

```
> schedule S1 d/20-10-2026 t/1600 s/A Math f/60
Scheduled L4 for S1 Amirah Binte Rahman
  Tue 20-10-2026 1600, A Math, $60.00
```

#### Listing lessons: `lessons`

Feature F2.2 · Story US8

Shows today's lessons, or this week's, in the order they happen.

Format: `lessons [week]`

* `lessons` shows today's lessons.
* `lessons week` shows lessons from Monday to Sunday of the current week, including any earlier this week.

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

#### Recording a lesson: `record`

Feature F2.3 · Stories US15, US16, US10

Records whether the student came to a lesson, and what you covered.

Format: `record LESSON_ID a/ATTENDANCE [note/NOTES]`

* Only a lesson dated today or earlier can be recorded.
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

#### Editing a lesson: `edit`

Feature F2.4 · Story US30

Changes the details of an existing lesson, such as moving it to another day.

Format: `edit LESSON_ID [d/DATE] [t/TIME] [s/SUBJECT] [f/FEE] [a/ATTENDANCE] [note/NOTES]`

* Give at least one value to change. Values you leave out stay the same.
* The same rules as `schedule` and `record` apply to the new values. For example, a lesson cannot be moved to a
  slot another lesson already has, and a lesson dated after today cannot have attendance or notes.
* Type `note/` with nothing after it to clear the lesson's notes.
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

Feature F2.5 · Story US31

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

> **Tip:** If you do not charge for a lesson the student missed, delete it so it is not counted in what they owe.

### Homework

#### Assigning homework: `assign`

Feature F3.1 · Story US17

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

Feature F3.2 · Story US18

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

Feature F3.3 · Story US11

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

Feature F4.1 · Story US23

Records that a lesson has been paid for. Marking it unpaid undoes a mistake.

Format: `mark LESSON_ID STATUS`

* `STATUS` is `paid` or `unpaid`.
* Every new lesson starts unpaid.
* A lesson can be marked paid before it takes place, for example when a family pays in advance.

Examples:

* `mark L1 paid`
* `mark L2 unpaid`

Example output:

```
> mark L1 paid
Marked L1 as paid.
```

#### Listing unpaid lessons: `unpaid`

Feature F4.2 · Story US24

Lists lessons that have taken place but have not been paid for, for all students or for one.

Format: `unpaid [STUDENT_ID]`

* Only lessons dated today or earlier are listed.
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

Feature F4.3 · Story US25

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
shows the lesson ID, date, subject, fee, and current payment status.

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

Feature F5.1 · Story US42

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

Feature F5.2 · Story US43

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
NEXT | S3 | L2 | H2
S | S1 | Amirah Binte Rahman | Sec 3 | 91234567 | E Math | A Math
L | L1 | S1 | 15-09-2026 | 1600 | A Math | 60 | present | paid | Quadratics \| discriminant
H | H1 | S1 | 29-09-2026 | outstanding | Worksheet 4A
```

| Line   | Fields, in order                                                                                    |
|--------|-----------------------------------------------------------------------------------------------------|
| `NEXT` | the IDs the next new student, lesson and homework item will get                                     |
| `S`    | student ID, name, level, phone number, then any number of subjects                                  |
| `L`    | lesson ID, student ID, date, time, subject, fee, attendance (`not recorded`, `present`, `absent` or `late`), `paid` or `unpaid`, notes |
| `H`    | homework ID, student ID, due date, `outstanding` or `submitted`, description                        |

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

Feature F6.1 · Story US1

Shows every command TutorLedger accepts, with its format and one example.

Format: `help`

Example output:

```
> help
Commands (UPPER_CASE = value you supply, [ ] = optional)

STUDENTS
  add n/NAME l/LEVEL p/PHONE_NUMBER
      add n/Amirah Binte Rahman l/Sec 3 p/91234567
  subject STUDENT_ID s/SUBJECT [s/SUBJECT]...
      subject S1 s/E Math s/A Math
  view STUDENT_ID
      view S1
  list [NAME]
      list wei
  edit STUDENT_ID [n/NAME] [l/LEVEL] [p/PHONE_NUMBER] [s/SUBJECT]...
      edit S1 l/Sec 4
  delete STUDENT_ID
      delete S5

LESSONS
  schedule STUDENT_ID d/DATE t/TIME s/SUBJECT f/FEE
      schedule S1 d/22-09-2026 t/1600 s/A Math f/60
  lessons [week]
      lessons week
  record LESSON_ID a/ATTENDANCE [note/NOTES]
      record L41 a/present note/Started simultaneous equations
  edit LESSON_ID [d/DATE] [t/TIME] [s/SUBJECT] [f/FEE] [a/ATTENDANCE] [note/NOTES]
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
```

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

**Q**: A student missed a lesson and I am not charging for it. What should I do?

**A**: Delete the lesson with `delete LESSON_ID`, so it is not counted in what they owe.

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

## Command summary

| Action                  | Format, Examples                                                                                                        |
|-------------------------|-------------------------------------------------------------------------------------------------------------------------|
| **Add student**         | `add n/NAME l/LEVEL p/PHONE_NUMBER`<br>e.g. `add n/Amirah Binte Rahman l/Sec 3 p/91234567`                              |
| **Add subjects**        | `subject STUDENT_ID s/SUBJECT [s/SUBJECT]…`<br>e.g. `subject S1 s/E Math s/A Math`                                      |
| **View student**        | `view STUDENT_ID`<br>e.g. `view S1`                                                                                      |
| **List / find students**| `list [NAME]`<br>e.g. `list wei`                                                                                         |
| **Edit student**        | `edit STUDENT_ID [n/NAME] [l/LEVEL] [p/PHONE_NUMBER] [s/SUBJECT]…`<br>e.g. `edit S1 l/Sec 4`                            |
| **Delete student**      | `delete STUDENT_ID`<br>e.g. `delete S5`                                                                                  |
| **Schedule lesson**     | `schedule STUDENT_ID d/DATE t/TIME s/SUBJECT f/FEE`<br>e.g. `schedule S1 d/20-10-2026 t/1600 s/A Math f/60`             |
| **List lessons**        | `lessons [week]`<br>e.g. `lessons week`                                                                                  |
| **Record lesson**       | `record LESSON_ID a/ATTENDANCE [note/NOTES]`<br>e.g. `record L3 a/present note/Started simultaneous equations`          |
| **Edit lesson**         | `edit LESSON_ID [d/DATE] [t/TIME] [s/SUBJECT] [f/FEE] [a/ATTENDANCE] [note/NOTES]`<br>e.g. `edit L6 d/16-10-2026 t/1800` |
| **Delete lesson**       | `delete LESSON_ID`<br>e.g. `delete L10`                                                                                  |
| **Assign homework**     | `assign STUDENT_ID DESCRIPTION d/DUE_DATE`<br>e.g. `assign S1 Worksheet 4A d/20-10-2026`                                |
| **Submit homework**     | `submit HOMEWORK_ID`<br>e.g. `submit H1`                                                                                 |
| **List homework**       | `homework [STUDENT_ID]`<br>e.g. `homework S1`                                                                            |
| **Mark paid / unpaid**  | `mark LESSON_ID STATUS`<br>e.g. `mark L1 paid`                                                                           |
| **List unpaid lessons** | `unpaid [STUDENT_ID]`<br>e.g. `unpaid S1`                                                                                |
| **Show amount owed**    | `owed`                                                                                                                   |
| **Fee statement**       | `fee_statement STUDENT_ID`<br>e.g. `fee_statement S1`                                                                    |
| **Save**                | `save [FILE_PATH]`<br>e.g. `save backups/term1.txt`                                                                      |
| **Reload data file**    | `reload`                                                                                                                 |
| **Help**                | `help`                                                                                                                   |
| **Exit**                | `exit`                                                                                                                   |
