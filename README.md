# nifi2md

Java CLI that parses an **Apache NiFi flow definition JSON file** and generates
**Markdown documentation**, one file per process group, each with a **Mermaid diagram**,
all cross-linked from a main `index.md`.

Documentation is annotated with the
[Enterprise Integration Patterns](https://www.enterpriseintegrationpatterns.com/)
vocabulary (Content-Based Router, Splitter, Aggregator, Messaging Gateway, ...) so the
generated pages describe *what role each processor plays*, not only which class it is.

## Build

Requires JDK 17+ and Gradle (no wrapper checked in; use a local Gradle 8.x).

```bash
gradle build          # compiles, runs tests, builds build/libs/nifi2md-<version>-all.jar
gradle fatJar         # just the executable uber-jar
gradle run --args="path/to/ACME.json"
```

## Usage

```bash
java -jar build/libs/nifi2md-0.1.0-all.jar test/ACME.json
```

Default output: a `<input name>-docs/` directory next to the input file.

```
test/ACME-docs/
├── index.md                      # summary, parameter contexts, services, group index
└── process-groups/
    ├── acme.md                   # root group: diagram + processors + connections
    ├── acme-service-1.md
    └── ...
```

### Options

| Option | Default | Description |
| --- | --- | --- |
| `-o, --output <dir>` | `<input name>-docs` | Output directory |
| `-t, --title <title>` | root process group name | Documentation title |
| `-d, --direction <dir>` | `LR` | Mermaid layout: `LR`, `RL`, `TB`, `TD`, `BT` |
| `--no-diagrams` | off | Skip Mermaid diagrams |
| `--no-properties` | off | Skip component property tables |
| `--max-property-length <n>` | `200` | Truncate property values (`0` = no truncation) |
| `-q, --quiet` | off | Do not print written files |
| `-h, --help` | | Help |
| `-V, --version` | | Version |

## Generated documentation

**`index.md`**

- Flow summary (counts of groups, processors, connections, ports, services)
- Process group index: tree of links to every group file
- Parameter contexts (sensitive values masked as `********`)
- Controller services with configuration

**`process-groups/<group>.md`**

- Breadcrumb links: back to main, up to parent, down to children
- `mermaid` flowchart of the group
- Processor table with its mapped integration pattern, then full property tables
- Connections table (source, selected relationship, destination)
- Input / output ports, child group links, controller services,
  remote process groups, funnels and canvas labels

### Diagram conventions

| Node | Meaning |
| --- | --- |
| `([ name ])` stadium | Endpoint processor, or process group port |
| `{ name }` rhombus | Message router |
| `{{ name }}` hexagon | Splitter / aggregator / recipient list |
| `[/ name /]` trapezoid | Message translator |
| `( name )` round | Processor without a mapped pattern |
| `[[ name ]]` subroutine | Nested process group (opens its own page) |

Connections pointing at a port of a child group are drawn against that child group node, so each level stays readable.

## License

GNU General Public License v3.0 or later, see [LICENSE](LICENSE).
Every source file carries `SPDX-License-Identifier: GPL-3.0-or-later`.
