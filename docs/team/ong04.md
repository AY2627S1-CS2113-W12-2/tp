# Isaac Ong — Project Portfolio Page

## Overview

TutorLedger is a Java command-line application for independent tutors teaching primary-school,
secondary-school and junior-college students. It keeps student profiles, lessons, homework, scores and
payment records together in a local data file.

My contributions focus on helping a tutor keep track of each student's work and progress between lessons:
homework, test scores, end-of-term progress summaries, follow-up reminders and flags for students who need
extra attention.

## Summary of contributions

**Code contributed:** [My RepoSense code dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=ong04&breakdown=true)
· [Authored pull requests](https://github.com/AY2627S1-CS2113-W12-2/tp/pulls?q=is%3Apr+author%3Aong04).

### Enhancements implemented

* **Homework** ([PR #61](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/61),
  [PR #76](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/76)):
  added `assign`, `submit` and `homework`. Homework is given a permanent ID, and the outstanding list is
  sorted by due date with overdue items flagged. The description may contain spaces, commas and `d/`, because
  only the last `d/` is read as the due date. PR #76 added logging and assertions to these commands.
* **Test scores and progress summaries** ([PR #78](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/78),
  issues [#25](https://github.com/AY2627S1-CS2113-W12-2/tp/issues/25) and
  [#50](https://github.com/AY2627S1-CS2113-W12-2/tp/issues/50)):
  added `score`, `scores` and `progress`. Scores are compared as percentages, so tests with different totals
  can be compared, and each new score is compared with the previous test straight away. `progress` summarises
  attendance and scores over any period for an end-of-term review. Marks use `BigDecimal`, so half marks are
  stored exactly. Scores are saved with a backward-compatible change to the data file format, and are removed
  when their student is deleted.
* **Follow-up tasks** ([PR #96](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/96),
  [issue #26](https://github.com/AY2627S1-CS2113-W12-2/tp/issues/26)):
  added `task`, `tasks` and `done` for reminders such as "Bring Worksheet 4B". A task may be linked to a
  student and may have a due date, and both are optional. A student's pending tasks also appear in `next`, so
  they are seen when the tutor prepares for the lesson.
* **Attention flags** ([issue #20](https://github.com/AY2627S1-CS2113-W12-2/tp/issues/20)):
  added `flag`, `unflag` and `flagged`, so a tutor can mark students who need more preparation time, with a
  reason such as an upcoming exam. The flag is shown where the tutor already looks: `list`, `view` and `next`.
  It is stored on the student like the archive state, so edit, archive and restore keep it and deletion
  removes it without extra code. Older data files still load.
* **Deletion warning for all linked records** ([PR #99](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/99),
  [issue #98](https://github.com/AY2627S1-CS2113-W12-2/tp/issues/98)):
  a teammate's warning before deleting a student only checked for lessons, so a student with only homework,
  scores or tasks was deleted at once, along with those records. I reported the bug and extended the
  warning to count every kind of linked record.

### Testing

* **Unit and command tests for my features:** wrote `HomeworkCommandsTest`, `ScoreCommandsTest`,
  `TaskCommandsTest` and `StudentFlagTest`, added flag tests to `StudentTest`, and extended `StorageTest`
  and `StudentDeletionWarningTest`. The tests use equivalence partitions and boundary values (for example
  marks above the total, a total of 0, and a task due today versus yesterday), and a fixed `Clock`, so results do not
  depend on the date. They also check that data files from older versions still load.
* **End-to-end tests for the whole app:** wrote `EndToEndTest`, which feeds whole sessions to the real command
  loop and checks both the printed output and the saved file. It covers every command, including
  teammates' features:
  * a full workflow across three sessions, checking that data survives restarts, that display-only
    commands never rewrite the file, and that IDs are never reused;
  * 73 invalid inputs, at least one for each command, each of which must show an error and leave the data
    file byte-for-byte unchanged;
  * every shortcut giving the same output and saved data as its full command;
  * hand-edited and unreadable data files.
* **Storage error paths:** added `StorageCommandsTest` cases for an unreadable data file on start-up and
  reload, and an invalid save path. These cases avoid OS-specific messages, so they pass on all three CI
  platforms.
* **Exploratory testing:** ran every command against the real app with valid, boundary and invalid input.
  This found a bug where `tasks` printed `Error: %-0s` when no pending task had a student. I fixed it and
  added a regression test, which I confirmed fails without the fix.

### Documentation

* Wrote the [User Guide](../UserGuide.md) sections *Scores and progress*, *Follow-up tasks* and *Flagging a
  student*, with example output taken from the running app, and kept `help`, the command summary and the data
  file format description in sync.
* Wrote the [Developer Guide](../DeveloperGuide.md) sections *Scores and progress*, *Follow-up tasks* and
  *Attention flags*, with design alternatives and manual testing steps.
* Addressed the tutor's DG review ([issue #101](https://github.com/AY2627S1-CS2113-W12-2/tp/issues/101),
  [PR #111](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/111)): replaced the remaining Mermaid diagrams
  with PlantUML, added sequence diagrams for the command flows that had none (`income`, `pay`, `progress`,
  `next` and `assign`), fixed UML notation across the class and sequence diagrams, and added the missing user
  stories.

### Team contributions

* Reviewed teammates' pull requests after building and testing them locally:
  * [PR #79](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/79) (lesson cancellation): reproduced and
    reported a bug where `fee_statement` still listed a cancelled lesson as unpaid.
  * [PR #80](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/80) (lesson filter and aliases): pointed out how
    it would interact with cancelled lessons and how to resolve the conflicts with PR #79. I then merged it.
  * [PR #110](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/110) (payment refactor): showed that its failing
    test came from an outdated base branch and passed once merged with master.
  * [PR #112](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/112) (DG return arrows): checked every diagram
    against its source, then added the two constructor returns it had missed.
  * Also reviewed [PR #75](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/75) and
    [PR #113](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/113).
* Fixed the About Us portfolio links, which returned 404 on the website
  ([PR #97](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/97)).

### Contributions beyond the project team

* Reviewed the Developer Guide of team CS2113-T10-1 (ScheduleFlow)
  ([comments](https://github.com/NUS-CS2113-AY2627-S1/tp/pull/29)), with five questions on its diagrams and
  design descriptions.

<!-- Before final submission: add the PR link for attention flags once it is opened, and add the CakeQueue
(CS2113-W12-1) DG review if it has a link. Export this page to PDF and check the page limit. -->
