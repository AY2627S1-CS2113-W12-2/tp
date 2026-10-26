# Default hourly rate UML diagrams

These diagrams document Yan Hern's v2.0 implementation of US5 / issue #11. The Developer Guide embeds
the SVGs so GitHub Pages can display them without JavaScript or a PlantUML plugin.

Open [the combined preview](index.html) in a browser to review all four diagrams before merging.

| Diagram | Purpose |
| --- | --- |
| [Class diagram](hourly-rate-class.svg) | Responsibilities and relationships |
| [Rate edit sequence](hourly-rate-edit-sequence.svg) | Validated replacement followed by automatic saving |
| [Scheduling sequence](hourly-rate-schedule-sequence.svg) | Duration-based fee calculation and insertion |
| [Fee selection activity](hourly-rate-fee-activity.svg) | Overrides, defaults and rejection paths |

Edit the corresponding `.puml` source before regenerating its image. Use Java 25 and
[PlantUML 1.2026.9](https://github.com/plantuml/plantuml/releases/tag/v1.2026.9).
From the repository root, with the downloaded JAR in `/tmp`:

```sh
java -Djava.awt.headless=true -jar /tmp/plantuml-1.2026.9.jar -tsvg docs/diagrams/hourly-rate-*.puml
```

The lesson diagrams (`lesson-*.puml`, Reynard's section of the Developer Guide) are rendered the same way:

```sh
java -Djava.awt.headless=true -jar /tmp/plantuml-1.2026.9.jar -tsvg docs/diagrams/lesson-*.puml
```

`lesson-parse-sd.puml` is the `sd` frame that the two lesson sequence diagrams refer to with `ref`.

The class diagram uses PlantUML's [Smetana layout](https://plantuml.com/smetana02), which is included
in the JAR. The other diagrams use built-in renderers. No Graphviz installation or UML MCP is needed.
Keep the SVG and source changes in the same commit and check the diagrams at normal document width.
