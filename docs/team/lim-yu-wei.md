# Lim Yu Wei — Project Portfolio Page

## Overview

TutorLedger is a Java command-line application for independent tutors teaching primary-school,
secondary-school and junior-college students. It keeps students, lessons, homework and payments in one
place, and every task is a single typed command.

My contributions focus on keeping a tutor's records safe between sessions, and on making the app faster
to learn and use: saving and loading data, the `help` command, command shortcuts, lesson filtering and
one-line student entry.

## Summary of contributions

**Code contributed:** [My RepoSense code dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=lim-yu-wei&breakdown=true)
· [Authored pull requests](https://github.com/AY2627S1-CS2113-W12-2/tp/pulls?q=is%3Apr+author%3ALim-Yu-Wei).

### Enhancements implemented

* **Storage: auto-save, `save` and `reload`** ([PR #69](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/69)):
  before this, all records were lost when the app closed. Records are now kept in a plain-text file,
  `data/tutorledger.txt`, which is loaded on startup and saved automatically after every command that
  changes data. `save [FILE_PATH]` confirms a save or writes a backup copy, and `reload` reads the file
  again after a tutor edits it by hand.
  * Changes are detected by comparing the saved form of the data before and after each command. Commands
    added later by teammates are therefore saved without extra code, and display-only commands never
    overwrite a tutor's hand edits.
  * A corrupted line is skipped and reported with its line number instead of crashing the app. Records are
    rebuilt through the same validation as typed commands, and ID counters are saved so that deleted IDs
    are never reused.
* **`help` command** ([PR #62](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/62)):
  lists the format and one example of every command, grouped by feature as in the User Guide, so new users
  can discover commands without leaving the app. The text lives in its own `HelpCommand` class, which kept
  merge conflicts with teammates' command handlers small.
* **Lesson filter** ([PR #80](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/80)):
  `filter [STUDENT_ID] [from/DATE] [to/DATE] [pay/STATUS]` lists the lessons matching every filter given,
  earliest first, with the total of their fees. With no filters it lists every lesson, which was not
  possible before. It is a separate read-only `LessonFilterCommand` class.
* **Command shortcuts** ([PR #80](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/80)):
  short forms such as `ls`, `sch`, `v` and `fl` are expanded to the full command word once, in the main
  loop, so no command handler needs to know about them. The shortcuts are kept in one list
  (`CommandAliases`), which `help` also uses to print its SHORTCUTS section, so the two cannot drift apart.
* **One-line student entry** ([PR #80](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/80)):
  `add` now accepts optional `s/SUBJECT` values, so a whole student record can be entered in one command.
  Blank subjects are rejected before the student is added, so no half-entered record is left behind.

### Enhancement awaiting merge

* **Cancelled lessons in `filter`**: `filter` was written before lesson cancellation existed, so it listed
  cancelled lessons like normal ones and added their fees to the total. The fix shows a
  `(cancelled: REASON)` marker, as `lessons` does, and leaves cancelled fees out of the total, matching how
  the rest of the app treats cancelled lessons. Implemented in the `filter-cancelled-lessons` branch; not
  yet merged upstream.

### Testing and documentation

* Wrote the automated tests for my features: `StorageTest`, `StorageCommandsTest`, `HelpCommandTest`,
  `LessonFilterCommandTest` and `CommandAliasesTest`, and extended `StudentCommandsTest` for one-line
  entry. `CommandAliasesTest` checks that no shortcut clashes with a real command word in any handler,
  including handlers teammates added later.
* Added logging (at `FINE` level) and assertions to `Storage` and `StorageCommands`, following the
  approach the team used for the other features.
* Updated the [User Guide](../UserGuide.md) with the storage commands and data file, `filter`, `add`
  with subjects, and the command shortcuts, checking each example against the real output.
* Wrote the original Storage section of the [Developer Guide](../DeveloperGuide.md#storage), covering
  the file format, the auto-save design and its alternatives. A teammate later restructured the DG and
  redrew its diagrams.
* Prepared Developer Guide sections for `help`, command shortcuts and `filter` (not yet merged), with
  five PlantUML diagrams of three types: class diagrams for the input classes and the filter, a sequence
  diagram of shortcut expansion, a sequence diagram of filtering, and an object diagram of one filter's
  criteria. These sections also add manual-testing steps.

### Team contributions

* Renamed the project template to TutorLedger in the README, and added my About Us entry
  ([PR #5](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/5)).
* Designed storage so that teammates' later features (scores, follow-up tasks, hourly rates, archiving)
  could add their own record types to the same file and be saved automatically.

<!-- Before final submission: once the DG and filter-cancelled-lessons PRs are merged, add their links and
move them out of "awaiting merge". Add any PR reviews you give. Export to PDF and check the page limit. -->
