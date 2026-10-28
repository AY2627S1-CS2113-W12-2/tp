# Developer Guide UML diagrams

The [Developer Guide](../DeveloperGuide.md) embeds generated SVGs, with editable PlantUML sources
alongside each image. Open the [standalone HTML guide](../preview/DeveloperGuide.html) locally to inspect
the full document and enlarge any diagram.

| Area | PlantUML sources |
| --- | --- |
| Overall architecture | [Architecture](architecture.puml) |
| Storage | [Class diagram](storage-class.puml), [automatic-save sequence](storage-autosave-sequence.puml) |
| Scores and progress | [Class diagram](score-class.puml), [recording sequence](score-record-sequence.puml), [attendance activity](score-progress-activity.puml) |
| Follow-up tasks | [Class diagram](followup-class.puml), [task creation sequence](followup-add-sequence.puml) |
| Lesson scheduling and records | [Class diagram](lesson-class.puml), [weekly scheduling](lesson-weekly-sequence.puml), [parsing fragment](lesson-parse-sd.puml), [weekly lesson objects](lesson-weekly-object.puml), [rescheduling](lesson-reschedule-sequence.puml), [next-lesson activity](lesson-next-activity.puml) |
| Default hourly rates | [Class diagram](hourly-rate-class.puml), [rate edit](hourly-rate-edit-sequence.puml), [scheduling](hourly-rate-schedule-sequence.puml), [fee selection](hourly-rate-fee-activity.puml) |
| Fee statements | [Class diagram](fee-statement-class.puml), [routing and persistence](fee-statement-overview-sequence.puml), [request validation](fee-statement-request-sequence.puml), [row-building fragment](fee-statement-rows-sequence.puml) |

Edit the `.puml` source before regenerating an image. Use Java 25 and
[PlantUML 1.2026.9](https://github.com/plantuml/plantuml/releases/tag/v1.2026.9).
From the repository root, with the downloaded JAR in `/tmp`:

```sh
java -Djava.awt.headless=true -jar /tmp/plantuml-1.2026.9.jar -tsvg -nometadata docs/diagrams/*.puml
```

Class, object and component diagrams use the bundled [Smetana layout](https://plantuml.com/smetana02);
sequence and activity diagrams use built-in renderers. No separate Graphviz installation or client-side
diagram service is needed. Keep generated SVGs with their sources and check readability at normal
document width. The Pages include adds keyboard-accessible diagram enlargement without changing the
SVGs themselves.
