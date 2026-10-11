# Developer Guide

## Acknowledgements

{list here sources of all reused/adapted ideas, code, documentation, and third-party libraries -- include links to the original source as well}

## Design & implementation

{Describe the design and implementation of the product. Use UML diagrams and short code snippets where applicable.}

### Storage

The storage feature (F5.1 automatic save, F5.2 human-editable data file) keeps every record in one
plain-text file, `data/tutorledger.txt`, so that data survives restarts and a tutor can fix or bulk-edit
records in a text editor.

```mermaid
classDiagram
    class TutorLedger {
        +main(String[])
    }
    class StorageCommands {
        +handles(String)$ boolean
        +execute(String) String
        +loadOnStartup() String
        +snapshot() String
        +saveIfChanged(String) String
    }
    class Storage {
        +DEFAULT_FILE_PATH$ String
        +load() LoadResult
        +save(TutorLedgerData)
        +saveTo(Path, TutorLedgerData)$
        +encode(TutorLedgerData)$ String
        +decode(List~String~)$ LoadResult
    }
    class LoadResult {
        <<record>>
        data : TutorLedgerData
        problems : List~String~
    }
    TutorLedger --> StorageCommands
    StorageCommands --> Storage
    StorageCommands --> TutorLedgerData
    Storage ..> LoadResult
    LoadResult --> TutorLedgerData
```

* **`Storage`** knows the file format and does the file input/output. It never prints anything, so it can
  be tested on its own.
* **`StorageCommands`** runs `save` and `reload`, loads the file at startup and saves automatically. It turns
  file errors into messages for the user, following the same `handles`/`execute` pattern as the other
  command classes.
* **`TutorLedgerData`** gained `importLesson`, `importHomework`, `setNextLessonNumber`,
  `setNextHomeworkNumber` and `replaceAllWith` so that records can be restored with their saved IDs.

#### File format

Each record is one line of fields separated by ` | `:

```
NEXT | S3 | L2 | H2
S | S1 | Amirah Binte Rahman | Sec 3 | 91234567 | E Math | A Math
L | L1 | S1 | 15-09-2026 | 1600 | A Math | 60 | present | paid | Quadratics \| discriminant
H | H1 | S1 | 29-09-2026 | outstanding | Worksheet 4A
```

* The `NEXT` line saves the ID counters. Without it, deleting the newest student and restarting would
  hand the deleted student's ID to the next new student, breaking the rule that IDs are never reused.
* A `|` or `\` inside a value is escaped as `\|` or `\\`, so notes and subjects can contain any text.
* Values are written in the same format the user types them (e.g. `DD-MM-YYYY`), so a hand-edited file
  uses the rules users already know.

*Alternatives considered:* JSON (via a library such as Jackson) handles nesting and escaping for us, but it
is harder for a tutor to edit by hand, and it needs a third-party library. CSV is similar to the chosen
format, but its quoting rules are less obvious to people editing it by hand.

#### Loading and validation

`Storage.decode` reads the file in two passes: students (and the `NEXT` line) first, then lessons and
homework. Lessons and homework must belong to an existing student, so this order lets lines appear in any
order in the file. Every record is rebuilt through the same constructors and `TutorLedgerData` methods that
commands use, so a hand-edited file is checked by exactly the same rules as typed input. For example, the
rules reject an invalid date, a duplicate ID, two lessons in one slot, or a lesson for a missing student.

A line that fails a check is skipped and reported as `Line N: reason`. Loading carries on with the
remaining lines, so one typo does not lose the whole file.

#### Automatic saving

Before each command, the main loop takes a `snapshot()` (the text the file would contain). After the
command, `saveIfChanged` compares a new snapshot with it and writes the file only if they differ.

```mermaid
sequenceDiagram
    participant Main as TutorLedger
    participant SC as StorageCommands
    participant FC as Feature command class
    participant S as Storage
    Main->>SC: snapshot()
    SC-->>Main: before
    Main->>FC: execute(line)
    FC-->>Main: output
    Main->>SC: saveIfChanged(before)
    alt data changed
        SC->>S: save(data)
    end
```

* Comparing snapshots means a command that teammates add later is saved automatically. Nobody has to
  remember to update a list of "commands that change data".
* Commands that only display data never write the file, so they cannot overwrite a tutor's hand edits
  before `reload`.
* `save` and `reload` skip the automatic save. In particular, `reload` must not save, or the lines it just
  skipped would be erased before the tutor could fix them.
* The cost is encoding the data twice per command. For the few hundred records a tutor has, this takes
  well under a millisecond.

*Alternative considered:* saving after every command, whether or not it changed anything, is simpler. But a
read-only command such as `list` would then overwrite hand edits made while the app is open.

*Possible improvement:* write to a temporary file and then rename it over the data file. This means a crash
part-way through writing can never leave a half-written data file.


## Product scope
### Target user profile

{Describe the target user profile}

### Value proposition

{Describe the value proposition: what problem does it solve?}

## User Stories

|Version| As a ... | I want to ... | So that I can ...|
|--------|----------|---------------|------------------|
|v1.0|new user|see usage instructions|refer to them when I forget how to use the application|
|v2.0|user|find a to-do item by name|locate a to-do without having to go through the entire list|

## Non-Functional Requirements

{Give non-functional requirements}

## Glossary

* *glossary item* - Definition

## Instructions for manual testing

{Give instructions on how to do a manual product testing e.g., how to load sample data to be used for testing}

### Saving and loading data

1. Automatic save and restart
   1. Start TutorLedger in an empty folder. Expected: `No saved data yet...`.
   1. Run `add n/Amirah Binte Rahman l/Sec 3 p/91234567`, then `exit`.
   1. Start TutorLedger again. Expected: `Loaded 1 student, 0 lessons and 0 homework items...`.
1. Deleted IDs are not reused
   1. Add two students (S1, S2), `delete S2`, then restart TutorLedger.
   1. Add another student. Expected: it is given the ID S3, not S2.
1. Hand-editing and `reload`
   1. Open `data/tutorledger.txt` and change a student's phone number, then run `reload`.
      Expected: `view` shows the new number.
   1. Change a lesson's date to `31-02-2026`, then run `reload`.
      Expected: that line is reported as skipped with its line number; all other records load.
   1. Delete `data/tutorledger.txt`, then run `reload`.
      Expected: `No data file found...` and the current data is unchanged.
1. Backups
   1. Run `save backups/test.txt`. Expected: the file and the `backups` folder are created.
