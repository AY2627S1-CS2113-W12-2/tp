# User Guide

## Introduction

{Give a product intro}

## Quick Start

{Give steps to get started quickly}

1. Ensure that you have Java 25 or above installed.
1. Download the latest version of `TutorLedger` from the project's Releases page.

## Features 

{Give detailed description of each feature}

### Adding a todo: `todo`
Adds a new item to the list of todo items.

Format: `todo n/TODO_NAME d/DEADLINE`

* The `DEADLINE` can be in a natural language format.
* The `TODO_NAME` cannot contain punctuation.  

Example of usage: 

`todo n/Write the rest of the User Guide d/next week`

`todo n/Refactor the User Guide to remove passive voice d/13/04/2020`

### Storage

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
Saved 10 students, 42 lessons and 7 homework items
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
* If `data/tutorledger.txt` cannot be found, TutorLedger keeps the data it already has and tells you the
  file is missing.
* Words typed after `reload` are ignored.

Example output:

```
> reload
Reloaded data/tutorledger.txt
Loaded 10 students, 43 lessons and 7 homework items.
Skipped 1 line that could not be read:
  Line 18: Date must be a real date in DD-MM-YYYY format.
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

## FAQ

**Q**: How do I transfer my data to another computer? 

**A**: On the old computer, run `save FILE_PATH` to write a copy somewhere you can carry, such as a USB
drive. On the new computer, put that file at `data/tutorledger.txt` inside the folder you run TutorLedger
from, then start TutorLedger.

**Q**: I deleted something by mistake. Can I get it back?

**A**: Only if you have a backup from `save FILE_PATH`. Copy the backup over `data/tutorledger.txt` and run
`reload`. Everything goes back to how it was when the backup was made, so changes made since then are lost.

**Q**: Does TutorLedger need an internet connection?

**A**: No. Everything is stored on your own computer.

## Command Summary

{Give a 'cheat sheet' of commands here}

* Add todo `todo n/TODO_NAME d/DEADLINE`
* Save `save [FILE_PATH]`, e.g. `save backups/term1.txt`
* Reload data file `reload`
