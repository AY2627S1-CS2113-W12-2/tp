# Amir Izaaz — Project Portfolio Page

## Overview

TutorLedger is a Java command-line application that helps independent tutors manage student profiles,
lessons, homework and payments in a local data file.

My contributions span the payment workflow, its supporting data model and tests, and project documentation.
I also worked on command routing, diagnostics, build reliability and first-launch onboarding.

## Summary of contributions

**Code contributed:** [My RepoSense code dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=Chomus-ai&breakdown=true)
· [Authored pull requests](https://github.com/AY2627S1-CS2113-W12-2/tp/pulls?q=is%3Apr+author%3AChomus-ai).

### Enhancements implemented

* **Foundational data model** ([PR #59](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/59)):
  established the initial `Student`, `Lesson` and `Homework` classes and their tests.
* **Payment tracking and balances**
  ([PR #63](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/63),
  [PR #68](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/68)):
  added commands to mark lessons paid or unpaid, list outstanding lessons, and summarise amounts owed
  by student. The unpaid list supports student filtering and chronological ordering.
* **Fee statements** ([PR #77](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/77)):
  added a per-student history of lesson dates, fees and payment status.
* **Income summaries** ([PR #86](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/86)):
  added monthly and yearly income reports with lesson-level payment details, accounting for amounts
  received and excluding cancelled lessons.
* **Multi-session payments** ([PR #90](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/90)):
  implemented `pay` to allocate one payment across selected lessons, supporting partial payment and
  updating lesson, storage, filtering and command-help behavior.
* **Diagnostics** ([PR #72](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/72)):
  added debug messages across argument parsing, student, lesson and homework operations, and shared
  data handling.
* **Command routing and payment error handling**
  ([PR #108](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/108)):
  added CLI integration tests that exercise representative commands through `TutorLedger.main`, including
  aliases and storage behavior, and centralized payment argument-error handling.
* **Build reliability** ([PR #109](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/109)):
  fixed a Checkstyle issue in payment-command handling that caused the Gradle build to fail, with related
  command-routing updates and tests.
* **Payment validation and refactoring**
  ([PR #110](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/110)):
  separated payment parsing, validation, allocation and confirmation formatting, ensuring invalid requests
  are rejected before lesson records are changed. Documented the validation and allocation behavior in the
  Developer Guide.
* **First-launch sample data** ([issue #13](https://github.com/AY2627S1-CS2113-W12-2/tp/issues/13),
  [PR #119](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/119)):
  added a small synthetic dataset for new users, including students, lessons, a homework item, a score and
  a follow-up task. The dataset is loaded and persisted only when no data file exists; existing data files
  remain authoritative.

### Testing and documentation

* Added and extended tests for payment status changes, unpaid-list filtering and ordering, owed totals,
  fee statements, income reports, payment allocation and invalid payment requests.
* Added tests for sample-data contents, independent dataset creation, first-launch loading and persistence.
* Updated the [User Guide](../UserGuide.md) and command help with payment command formats, examples and
  behavior.
* Updated the project README, [User Guide](../UserGuide.md) and [Developer Guide](../DeveloperGuide.md) to
  explain the first-launch sample records and how to try commands before entering real data.
* Expanded the [Developer Guide](../DeveloperGuide.md) with the fee-statement design
  ([PR #83](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/83)), income-summary test instructions
  ([PR #86](https://github.com/AY2627S1-CS2113-W12-2/tp/pull/86)), and project-specific appendices.
