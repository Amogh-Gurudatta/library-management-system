# Extensions Beyond the Course

The capstone brief and the course slides define the required project. This file lists what the project adds on top, so the extra work stays easy to find and explain. Nothing here changes what the graded program prints: in its default mode, `java Main < file` still gives exactly the output the brief specifies.

How this list was made: it was compared against the syllabus and the lecture slides for Blocks 1 to 8 (object model through JVM and garbage collection). A topic may still have been mentioned in class without appearing on a slide.

## 1. Build and tooling

| Extension | Where | Notes |
|---|---|---|
| Maven build with a committed wrapper | `pom.xml`, `mvnw`, `.mvn/` | Pins Maven 3.9.9, so no install is needed. Builds a runnable jar with `Main` as its entry point. |
| Strict compiler settings | `pom.xml` | `-Xlint:all`, warnings as errors, `--release 21`. |
| Makefile shortcuts | `Makefile` | `make`, `make run`, `make test`, `make clean`, `make diagram`, `make help`. |
| GitHub Actions CI | `.github/workflows/ci.yml` | Builds and tests every push and pull request, with Maven dependency caching. |
| README status badges | `README.md` | CI status, Java version, dependencies, docs. |
| Diagram export pipeline | `docs/diagram/` | The class diagram is written in Mermaid and exported to PDF with `make diagram`, with colour-coded class groups. UML itself is in the syllabus; this tooling is not. |
| Documentation set | `README.md`, `docs/DESIGN.md`, `docs/DEVELOPER.md`, `AGENTS.md` | A plain-English README, a design-decision log, a developer reference (invariants, check orders, how-to recipes), and a short brief for AI coding tools. |

## 2. Testing beyond basic JUnit

JUnit itself is in the syllabus (from Block 9). These parts go further:

| Extension | Where |
|---|---|
| Parameterized tests (`@ParameterizedTest`, `@CsvSource`) | `CommandProcessorTest`, `MemberTest` |
| Dynamic tests (`@TestFactory`) that discover test files by themselves | `GoldenTest` |
| Golden-file end-to-end tests: each `.in` and `.expected` pair under `src/test/resources/golden/` is run through the whole program | `GoldenTest` |
| An architecture-rule test that scans the source and fails if a banned input class appears (the Scanner-only rule) | `InputRuleTest` |

## 3. Program features

| Extension | Where | Notes |
|---|---|---|
| Interactive mode (`-i` / `--interactive`) | `InteractiveShell`, `Main` | A prompt showing the current day, green `OK` and red `ERROR` lines, aligned report tables, `(none)` for empty reports, and `HELP`, `EXIT` and `QUIT`. |
| Colour control | `Main` | ANSI colours, switched off by the `NO_COLOR` environment variable (`System.getenv`). |
| Command-line options | `Main` | `-h` / `--help` usage text, and an unknown option exits with status 2 (`System.exit`). |
| `make run` starts the interactive shell | `Makefile` | `make run INPUT=file` still runs a file in plain mode. |

## 4. Java details the slides do not show

| Feature | Where |
|---|---|
| Varargs (`String... names`) | `CommandProcessor.fields` |
| `Map.computeIfAbsent` | `Library.reserve` |
| `Comparator.comparingInt` | `Library.loansOf`, `Library.overdueLoans` |
| `String.split` with a regex and a limit, `isBlank`, `stripTrailing`, `String.join`, `repeat`, `String.format` width padding | `CommandProcessor`, `InteractiveShell`, `Main` |
| `toLowerCase(Locale.ROOT)` for locale-independent matching | `LibraryItem.matchesTitle` |
| Static factory methods with private constructors | `ItemNotAvailableException`, `ReservationNotAllowedException` |
| Arrow-style `switch` (tests only) | `MemberTest` |
| `Files.walk` / `Files.list` and UTF-8 `PrintStream` (tests only) | `InputRuleTest`, `GoldenTest` |

## In the syllabus, but not an extension

These are required, so they are not listed above. The slides for them were not yet available when this list was written:

- JUnit tests (Block 9).
- UML class diagrams and SOLID (Block 10).
- At least one design pattern. `CommandProcessor.createMember` is a simple factory, and the polymorphic member rules are strategy-like. `docs/DESIGN.md` does not yet name a pattern explicitly.

Also not extensions: the RESERVE feature (optional in the brief) and input validation (added by the TAs).
