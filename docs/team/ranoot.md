# Reynard Seow — Project Portfolio Page

## Overview

TutorLedger is a Java command-line application for independent tutors teaching primary-school,
secondary-school and junior-college students. It keeps student profiles, lessons, homework, scores and
payment records together in a local data file.

My contributions focus on lessons: planning a tutor's week, recording what happened in each lesson and
handling changes of plan. I also wrote the first complete User Guide and set up the team's issue tracker.

## Summary of contributions

**Code contributed:** [My RepoSense code dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=ranoot&breakdown=true)
· [Authored pull requests](https://github.com/AY2627S1-CS2113-W12-2/tp/pulls?q=is%3Apr+author%3Aranoot).

### Enhancements implemented

* **Core lesson commands** ([PR #67](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/67),
  [epic #54](https://github.com/AY2627S1-CS2113-W12-2/tp/issues/54)):
  added `schedule`, `lessons [week]`, `record`, `edit` and `delete` for lessons. Lesson IDs are never
  reused, two lessons cannot share a slot, and only past lessons can be recorded. `edit` and `delete` are
  shared with student commands, so they are routed by the ID's `L` prefix. I moved the prefix parser out
  of `StudentCommands` into a shared `ArgumentParser`, which five command classes now use.
* **Lesson changes and weekly slots** ([PR #79](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/79),
  issues [#12](https://github.com/AY2627S1-CS2113-W12-2/tp/issues/12),
  [#19](https://github.com/AY2627S1-CS2113-W12-2/tp/issues/19),
  [#27](https://github.com/AY2627S1-CS2113-W12-2/tp/issues/27),
  [#38](https://github.com/AY2627S1-CS2113-W12-2/tp/issues/38),
  [#39](https://github.com/AY2627S1-CS2113-W12-2/tp/issues/39)):
  added `log`, `reschedule`, `cancel`, `next` and `schedule ... w/WEEKS`. Cancellation is a separate field
  rather than an attendance value. A cancelled lesson frees its slot, is not charged and keeps its reason.
  Weekly scheduling checks every slot before storing any, so a clash never leaves a half-scheduled series.
  The hardest part was that cancellation affects features owned by four teammates. I updated
  `unpaid`/`owed`, `progress`, `view` and storage to handle it. The storage change is backward-compatible,
  so existing data files load and save unchanged.
* **Topic search** ([PR #117](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/117),
  issue [#28](https://github.com/AY2627S1-CS2113-W12-2/tp/issues/28)):
  added `k/KEYWORD` to `topics`, so a tutor can find every lesson that covered a topic, with any student.
  Lesson notes act as the topic tag. A separate tag field would have needed a new data-file column and
  changes to three commands, and the tutor would have typed each topic twice.
* **Bug fixes** ([PR #107](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/107)):
  resolved all four items in the User Guide's *Known issues* and four more bugs found by testing the
  release JAR. Notes can now contain words such as `and/or`, `edit L3 a/none` clears attendance, `uncancel`
  undoes a cancellation, and homework can be edited and deleted. Overlapping lessons are rejected, not just
  lessons that start at the same time, and routine log messages no longer appear in the tutor's terminal.
* **Bug fixes in the v2.1 features** ([PR #120](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/120)):
  names containing `s/o` or `d/o`, which are common in Singapore, were split into a name and a subject or
  rejected. They are now kept whole in `add`, `edit` and `list`. I also moved the first-launch sample lessons
  so that the Quick start's `lessons week` shows one on any day, not only on weekdays.
* **Defensive programming and code quality** ([PR #75](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/75),
  [PR #94](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/94)):
  added assertions and debug logging to the lesson features, enabled assertions for `gradlew run`, and
  split long methods in `LessonCommands`.

### Testing

* Wrote about 80 JUnit tests, mainly in `LessonCommandsTest` and `TutorLedgerDataTest`. They cover each
  command's rejections, slot clashes, all-or-nothing weekly scheduling and cancelled lessons in storage.
  The tests use a fixed `Clock`, so their results do not depend on the real date.
* For the bug fixes and topic search, added tests for each fix and each search rule, plus end-to-end
  checks that run the real command loop across restarts. To confirm that the topic tests can fail, I
  broke the case-insensitive match on purpose and checked that they caught it.
* For v2.1, tested the new features written by the whole team. I ran 16,000 random commands against the
  release JAR and checked that every saved file reloads cleanly; no crashes were found. I then wrote tests
  for the two bugs found, each failing before its fix, and an end-to-end test that runs flags, both list
  sorts and command history together across a restart.

### Documentation

* **User Guide** ([PR #74](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/74),
  [PR #79](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/79)):
  wrote the first complete [User Guide](../UserGuide.md); before that it covered only `save` and `reload`.
  I added a section for every v1.0 command, the format notes, the FAQ and the command summary. All
  example outputs came from one scripted run of the release JAR. Where the draft specification disagreed
  with the code, I documented the code and listed the differences for the team. I later added the v2.0
  lesson commands, and later the topic search, with an FAQ entry on finding lessons by topic.
* **Developer Guide** ([PR #94](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/94)):
  wrote the [Developer Guide](../DeveloperGuide.md) section *Lesson scheduling and records*, including
  design alternatives, lesson user stories and manual tests. It has six PlantUML diagrams:
  [class](../diagrams/lesson-class.svg), [object](../diagrams/lesson-weekly-object.svg), two sequence
  diagrams ([weekly](../diagrams/lesson-weekly-sequence.svg),
  [reschedule](../diagrams/lesson-reschedule-sequence.svg)) that share a
  [`ref` fragment](../diagrams/lesson-parse-sd.svg), and an [activity](../diagrams/lesson-next-activity.svg)
  diagram. I later added the section *Covered topics and topic search*, with a
  [sequence diagram](../diagrams/topic-search-sequence.svg), and the matching manual tests.

### Team-based tasks

* Set up the issue tracker with 44 user stories
  ([#9](https://github.com/AY2627S1-CS2113-W12-2/tp/issues/9)–[#52](https://github.com/AY2627S1-CS2113-W12-2/tp/issues/52))
  and 6 feature epics
  ([#53](https://github.com/AY2627S1-CS2113-W12-2/tp/issues/53)–[#58](https://github.com/AY2627S1-CS2113-W12-2/tp/issues/58)),
  each with type and priority labels. I also created the `v2.0b` milestone.
* Helped fix the failing master build after PR #108 by pushing a commit to a teammate's fix,
  [PR #109](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/109). A routing test read the real data file,
  which is not in the repository, so it failed on every fresh clone. I made the test use a temporary file.
* Fixed the documentation site, which was returning 404, and made the Developer Guide's Mermaid diagrams
  display on GitHub Pages ([PR #100](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/100)).

### Review contributions

* Within the team: reviewed and approved PRs
  [#77](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/77),
  [#93](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/93),
  [#95](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/95),
  [#103](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/103),
  [#109](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/109),
  [#113](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/113) and
  [#115](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/115). For #113, I checked every PR link and
  counted the diagram changes its page claimed. For #115, I found and wrote up the fix for its merge
  conflict with the student-list sorting from #116.
* Beyond the team: in the week 11 peer review, left 14 comments on each of two other teams' Developer
  Guides:
  [CS2113-W10-4 Jaweeper](https://github.com/NUS-CS2113-AY2627-S1/tp/pull/4#pullrequestreview-5677888593)
  and [CS2113-W10-3 NUS BudgetBites](https://github.com/NUS-CS2113-AY2627-S1/tp/pull/10#pullrequestreview-5677866134).

<!-- Draft notes: add PE-D bug reports to "Review contributions" after week 12.
Before final submission, export this page to PDF and check the two-page limit for overview and summary. -->
