# Isaac Ong — Project Portfolio Page

## Overview

TutorLedger is a Java command-line application for independent tutors teaching primary-school,
secondary-school and junior-college students. It keeps student profiles, lessons, homework, scores and
payment records together in a local data file.

My contributions focus on helping a tutor keep track of each student's work and progress between lessons:
homework, test scores, end-of-term progress summaries and follow-up reminders.

## Summary of contributions

**Code contributed:** [My RepoSense code dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=ong04&breakdown=true)
· [Authored pull requests](https://github.com/AY2627S1-CS2113-W12-2/tp/pulls?q=is%3Apr+author%3Aong04).

### Enhancements implemented

* **Homework** ([commit](https://github.com/AY2627S1-CS2113-W12-2/tp/commit/fddd348e219997b8b2a57d0fc0efd6249724db03),
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
* **Follow-up tasks** ([issue #26](https://github.com/AY2627S1-CS2113-W12-2/tp/issues/26)):
  added `task`, `tasks` and `done` for reminders such as "Bring Worksheet 4B". A task may be linked to a
  student and may have a due date, and both are optional. A student's pending tasks also appear in `next`, so
  they are seen when the tutor prepares for the lesson. <!-- TODO: add the PR link once it is opened -->

### Testing and documentation

* Wrote `HomeworkCommandsTest`, `ScoreCommandsTest` and `TaskCommandsTest`, and extended `StorageTest` to
  check that scores and tasks are saved and loaded, including data files from older versions.
* Wrote the [User Guide](../UserGuide.md) sections for homework, scores and progress, and follow-up tasks,
  with example output taken from the running app, and kept `help`, the command summary and the data file
  format description in sync.
* Wrote the [Developer Guide](../DeveloperGuide.md) sections *Scores and progress* and *Follow-up tasks*,
  including a [class diagram](../diagrams/followup-class.svg) and a
  [sequence diagram](../diagrams/followup-add-sequence.svg) with PlantUML sources, design alternatives and
  manual testing steps.

### Team contributions

* Reviewed teammates' pull requests, including
  [PR #79](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/79) (lesson cancellation), where I reproduced
  and reported a bug where `fee_statement` still listed a cancelled lesson as unpaid, and
  [PR #80](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/80) (lesson filter and aliases), where I flagged
  how it would interact with cancelled lessons. <!-- TODO: replace with links to your actual review comments -->

<!-- Draft notes: add the follow-up tasks PR link after it is opened, and any issues you created.
Before final submission, export this page to PDF and check the two-page limit for overview and summary. -->
