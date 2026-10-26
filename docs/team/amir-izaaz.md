# Amir Izaaz — Project Portfolio Page

## Overview

TutorLedger is a Java command-line application that helps independent tutors manage student profiles,
lessons, homework and payments in a local data file.

My contributions focus on payment tracking and reporting: recording payments, seeing what students owe,
reviewing payment histories and summarising income. I also contributed to the application's core data
model, diagnostics and project documentation.

## Summary of contributions

**Code contributed:** [My RepoSense code dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=Izaaz&breakdown=true)
· [Authored pull requests](https://github.com/AY2627S1-CS2113-W12-2/tp/pulls?q=is%3Apr+author%3AChomus-ai).

### Enhancements implemented

* **Foundational data model** ([PR #59](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/59)):
  established the `Student`, `Lesson` and `Homework` classes and their initial tests, giving the application
  separate models for its main records.
* **Payment status and outstanding balances**
  ([PR #63](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/63),
  [PR #68](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/68)):
  added commands to mark lessons paid or unpaid, list past and due unpaid lessons, and total outstanding
  balances by student. The unpaid list can be filtered by student and is ordered from oldest to newest.
* **Fee statements** ([PR #77](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/77)):
  added a per-student payment history showing lesson dates, fees and payment status, so tutors can check
  what has been paid.
* **Income summaries** ([PR #86](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/86)):
  added monthly and yearly income reports with lesson-level payment details. Reports account for amounts
  paid and exclude cancelled lessons.
* **Debug logging** ([PR #72](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/72)):
  added diagnostic messages across argument parsing, student, lesson and homework operations, and shared
  data handling to make application behaviour easier to trace during development.

### Enhancement awaiting merge

* **Multi-session payments** ([PR #90](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/90), open):
  implemented a `pay` command that allocates one payment across selected lessons, including partial
  balances. The change also updates lesson data, storage and filtering to support those balances. This
  enhancement is still awaiting merge.

### Testing and documentation

* Added and extended payment tests for status changes, unpaid-list filtering and ordering, owed totals,
  fee statements, monthly and yearly income, and invalid command arguments. The multi-session payment
  tests cover partial allocations, overpayments and lessons that cannot be paid; rejected payments leave
  lesson records unchanged.
* Updated the [User Guide](../UserGuide.md) and command help with payment command formats, examples and
  behaviour.
* Expanded the [Developer Guide](../DeveloperGuide.md) with the fee-statement design and manual test
  instructions for income summaries.

### Team contributions

* Integrated partial-payment support across lesson, storage, filtering, student-command and help surfaces
  so it works with the team's existing lesson and student features.
* Followed up on review feedback for the open multi-session payment PR with a corrective commit; the PR
  remains open while the enhancement is reviewed.
