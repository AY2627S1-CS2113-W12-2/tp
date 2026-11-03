# Lim Yu Wei — Project Portfolio Page

## Overview

TutorLedger is a Java command-line application for independent tutors teaching primary-school,
secondary-school and junior-college students. It keeps students, lessons, homework and payments in one
place, and every task is a single typed command.

My contributions focus on keeping a tutor's records safe between sessions, and on making the app faster
to learn and use: saving and loading data, the `help` command, command shortcuts, lesson filtering,
one-line student entry and sorting the student list.

## Summary of contributions

**Code contributed:** [My RepoSense code dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=lim-yu-wei&breakdown=true)
· [Authored pull requests](https://github.com/AY2627S1-CS2113-W12-2/tp/pulls?q=is%3Apr+author%3ALim-Yu-Wei).

### Enhancements implemented

* **Storage: auto-save, `save` and `reload`** ([PR #69](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/69)):
  before this, all records were lost when the app closed. Records are now kept in `data/tutorledger.txt`,
  loaded on startup and saved automatically after every command that changes data. `save [FILE_PATH]`
  writes a backup copy and `reload` reads the file again after a hand edit.
  * Changes are detected by comparing the saved form of the data before and after each command, so
    teammates' later commands are saved without extra code and display-only commands never overwrite hand
    edits. A corrupted line is skipped and reported by line number instead of crashing the app, and saved ID
    counters stop deleted IDs from being reused.
* **`help` command** ([PR #62](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/62)):
  lists the format and an example of every command, grouped as in the User Guide, so new users can discover
  commands without leaving the app. It has its own `HelpCommand` class to keep merge conflicts small.
* **Lesson filter** ([PR #80](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/80)):
  `filter [STUDENT_ID] [from/DATE] [to/DATE] [pay/STATUS]` lists the lessons matching every filter given,
  earliest first, with their total fees. With no filters it lists every lesson, which was not possible before.
* **Command shortcuts** ([PR #80](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/80)):
  short forms such as `ls`, `sch` and `v` are expanded once in the main loop, so no command handler needs to
  know about them. `help` prints its shortcut list from the same `CommandAliases` list, so they cannot drift apart.
* **One-line student entry** ([PR #80](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/80)):
  `add` accepts optional `s/SUBJECT` values, so a whole student can be entered in one command. Blank subjects
  are rejected before anything is added.
* **Sorting the student list** ([PR #116](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/116)):
  `list sort/owed` puts the students who owe the most first, with each balance, and `list sort/last` puts
  those whose last lesson was longest ago first, with that date, so a tutor can see who to chase for payment
  or check in with. Balances match `owed`, cancelled and future lessons are ignored, ties stay in ID order,
  and both sorts combine with the name search and `status/`.

### Testing and documentation

* Wrote the tests for my features (`StorageTest`, `StorageCommandsTest`, `HelpCommandTest`,
  `LessonFilterCommandTest`, `CommandAliasesTest`, `StudentSortTest`) and extended `StudentCommandsTest`.
  `CommandAliasesTest` checks that no shortcut clashes with a command word in any handler, including ones
  added later. Added `FINE`-level logging and assertions to `Storage` and `StorageCommands`.
* [User Guide](../UserGuide.md): the storage commands and data file, `filter`, `add` with subjects,
  `list sort/` and the command shortcuts, with each example checked against the real output.
* [Developer Guide](../DeveloperGuide.md#storage): wrote the original Storage section, covering the file
  format, the auto-save design and its alternatives (later restructured by a teammate).

### Team contributions

* Renamed the project template to TutorLedger in the README, and added my About Us entry
  ([PR #5](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/5)).
* Designed storage so teammates' later records (scores, tasks, hourly rates, archiving) save to the same file.
* Added dashed return arrows to the 25 self-calls missing one across 11 teammates' DG sequence diagrams,
  so all of them follow UML notation ([PR #112](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/112)).

### Review and mentoring

* Reviewed [PR #79](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/79) (lesson logging, rescheduling,
  cancellation and weekly slots): built it merged with my own PR #80, confirmed that tests and checkstyle
  passed, and found that `fee_statement` listed cancelled lessons as unpaid, with a suggested fix.

### Contributions beyond the project team

* Reviewed the Developer Guide of team CS2113-W10-2 (ClubFlow)
  ([comments](https://github.com/NUS-CS2113-AY2627-S1/tp/pull/3)): pointed out empty sections, source-code
  links that return 404 on the published site, and a sequence diagram that disagreed with the text.
* Gave code-quality reviews on other students' iP pull requests
  ([#31](https://github.com/NUS-CS2113-AY2627-S1/ip/pull/31), [#1](https://github.com/NUS-CS2113-AY2627-S1/ip/pull/1)),
  covering missing Javadoc, magic strings, vague names and an overlong method.
