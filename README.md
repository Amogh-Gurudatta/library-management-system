# Library Management System

[![CI](https://github.com/Amogh-Gurudatta/library-management-system/actions/workflows/ci.yml/badge.svg)](https://github.com/Amogh-Gurudatta/library-management-system/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Dependencies: none](https://img.shields.io/badge/dependencies-none-brightgreen)
![Interface: CLI](https://img.shields.io/badge/interface-command--line-4c1)
[![Docs](https://img.shields.io/badge/docs-design%20%7C%20developer-blue)](docs/DESIGN.md)

A small command-line program that runs a college library: it keeps track of books, magazines and reference books, the people who borrow them, who has what, when it is due back, and how much they owe if it comes back late.

You type commands (or feed it a file of them), and it answers each one on its own line.

---

## Quick start

You need **Java 21 or newer**. Check with `java -version`. Maven is not needed; the project includes its own wrapper (`mvnw`).

```bash
make                          # compile
make run                      # run it, typing commands yourself (Ctrl+D to finish)
make run INPUT=src/test/resources/golden/sample.in   # or run a whole file of commands
make test                     # run the full test suite
make help                     # list every target
```

No `make`? Use Maven directly. The included wrapper downloads the right version for you:

```bash
./mvnw verify                                      # compile and test
java -jar target/library-management-system.jar     # run (after verify or package)
```

---

## Interactive mode

Typing commands by hand? Start the friendly mode:

```bash
make run          # or: java -jar target/library-management-system.jar -i
```

```text
Library Management System
Type HELP for the command list, EXIT to quit.
library (day 0)> ADD_BOOK|B1|Intro to Algorithms|Cormen
OK
library (day 0)> ADVANCE|20
OK today is day 20
library (day 20)> INVENTORY
ITEM  TYPE  STATUS
----  ----  ---------
B1    BOOK  AVAILABLE
```

- A prompt that shows the current day.
- Green `OK` and red `ERROR` lines (set `NO_COLOR=1` to turn colours off).
- Reports as aligned tables with column headings, and `(none)` when empty.
- `HELP` lists every command. `EXIT`, `QUIT` or Ctrl+D leave.

Interactive mode only changes how things look. Every command behaves exactly the same. When the program is run without `-i`, for example `java -jar ... < commands.txt`, it prints the plain, exact output described below, which is what scripts and tests rely on.

---

## A short example

```text
ADD_BOOK|B1|Introduction to Algorithms|Cormen      →  OK
ADD_MEMBER|S1|Ananya Rao|STUDENT                   →  OK
BORROW|S1|B1                                       →  OK due on day 14
ADVANCE|20                                         →  OK today is day 20
RETURN|B1                                          →  OK fine 12
```

Ananya, a student, borrowed the book on day 0. Students keep books for 14 days, so it was due on day 14. She brought it back on day 20, which is 6 days late. Books cost ₹2 per late day, so she owes ₹12.

> **Time is pretend.** The library starts on day 0, and only `ADVANCE` moves the clock. The program never looks at the real date.

---

## How the library works

| Item | Can it be borrowed? | Late fine |
| --- | --- | --- |
| Book | Yes | ₹2 per day |
| Magazine | Yes | ₹1 per day |
| Reference book | No, library use only | none |

| Member | Items at once | Loan length |
| --- | --- | --- |
| Student | 3 | 14 days |
| Staff | 4 | 21 days |
| Faculty | 5 | 30 days |

- An item can only be with one person at a time.
- There is no fine if an item comes back on or before its due day.

---

## Commands

Fields are separated by `|`, so titles and names can contain spaces.

| Command | What it does | Example reply |
| --- | --- | --- |
| `ADD_BOOK\|id\|title\|author` | Adds a book | `OK` |
| `ADD_MAGAZINE\|id\|title\|issue` | Adds a magazine | `OK` |
| `ADD_REFERENCE\|id\|title\|author` | Adds a reference book | `OK` |
| `ADD_MEMBER\|id\|name\|type` | Adds a member (`STUDENT`, `STAFF` or `FACULTY`) | `OK` |
| `BORROW\|memberId\|itemId` | Lends an item | `OK due on day 14` |
| `RETURN\|itemId` | Takes an item back and works out the fine | `OK fine 12` |
| `ADVANCE\|days` | Moves the clock forward | `OK today is day 20` |
| `RESERVE\|memberId\|itemId` | Joins the waiting list for an item that's out | `OK reserved` |
| `SEARCH\|text` | Finds items whose title contains the text | `B1\|BOOK\|Introduction to Algorithms` |
| `LOANS\|memberId` | Lists what a member currently has | `B1\|Introduction to Algorithms\|14` |
| `OVERDUE` | Lists everything past its due day | `B1\|S1\|6\|12` (item, member, days late, fine so far) |
| `INVENTORY` | Lists every item and where it is | `B1\|BOOK\|ON_LOAN\|S1` |

- Reports print one line per result. If there's nothing to show, they print `NONE`.
- When a reserved item is returned, it goes straight to the first person waiting who still has room, and the reply says so: `OK fine 0, issued to T1 due on day 41`.

---

## When something goes wrong

The program prints `ERROR` followed by a reason, then carries on with the next command. Nothing is changed by a command that fails.

| Situation | Message |
| --- | --- |
| Unknown item or member | `ERROR item B9 not found` / `ERROR member X1 not found` |
| Item already out, or not lendable | `ERROR B1 is on loan` / `ERROR R1 cannot be borrowed` |
| Member already at their limit | `ERROR S1 has reached the limit of 3` |
| Returning something that isn't out | `ERROR B4 is not on loan` |
| Reservation not allowed | `ERROR B1 is available` / `ERROR S1 already has B1` / `ERROR S2 already reserved B1` |
| Typo in a command | `ERROR unknown command BORROWW` |
| Wrong number of fields | `ERROR BORROW expects 2 fields: memberId\|itemId` |
| An empty field | `ERROR title cannot be empty` |
| A bad number of days | `ERROR days must be a non-negative number` |
| An unknown member type | `ERROR unknown member type ADMIN` |
| Reusing an ID | `ERROR duplicate id B1` |

---

## What's in the project

### Where things are

```text
library-management-system/
├── src/
│   ├── main/java/                  The program
│   └── test/
│       ├── java/                   JUnit tests
│       └── resources/golden/       Whole-program tests: .in = commands, .expected = exact output
├── docs/
│   ├── DESIGN.md                   Why the code is built the way it is
│   ├── DEVELOPER.md                Technical guide for anyone changing the code
│   ├── EXTENSIONS.md               Extra work beyond the course requirements
│   ├── capstone_problem_statement.pdf   The project brief
│   ├── diagram/                    Class diagram (PDF + Mermaid source)
│   └── milestones/                 Submitted milestone files
├── .github/workflows/ci.yml        Builds and tests automatically on GitHub
├── pom.xml, mvnw, .mvn/            Maven build and its wrapper (no Maven install needed)
├── Makefile                        Shortcuts: make, make run, make test
├── AGENTS.md                       Short brief for AI coding assistants
└── README.md                       You are here
```

### How the code fits together

A line you type travels like this:

```text
Main  ──►  CommandProcessor  ──►  Library  ──►  items, members and loans
(starts)   (checks the line,      (applies every rule
           prints the result)      and throws errors)
```

| Part | Classes | What they do |
|---|---|---|
| **Items** | `LibraryItem`, `Book`, `Magazine`, `ReferenceBook`, `Borrowable` | What the library holds. Books and magazines can be lent and have a fine rate. Reference books cannot. |
| **Members** | `Member`, `Student`, `Staff`, `Faculty` | Who borrows. Each type knows its own item limit and loan length. |
| **Loans** | `Loan`, `ReturnResult` | One lent item with its due day and fine, and the outcome of a return. |
| **The library** | `Library` | Holds everything and enforces every rule: who can borrow what, fines, reservations. |
| **Commands** | `Main`, `CommandProcessor` | Read each line, check it, run it and print the result. Errors are caught in one place. |
| **Interactive mode** | `InteractiveShell` | The optional friendly mode: prompt, colours, tables and help. |
| **Errors** | `LibraryException` and 8 kinds | One exception for each way a command can fail. |

### How it is tested

| Tests | What they check |
|---|---|
| `ItemTest`, `MemberTest`, `LoanTest` | The rules built into each type: fine rates, limits, due days, fines. |
| `LibraryTest` | Borrow, return and reserve rules, error messages, sort orders. |
| `CommandProcessorTest` | Every kind of bad input gives the right error. |
| `GoldenTest` | Complete sessions, including the brief's sample, match the expected output exactly. |
| `InteractiveShellTest` | The prompt, tables, help and colours of the interactive mode. |
| `InputRuleTest` | All input is read with `Scanner` only. |

## Learn more

- **[Project brief](docs/capstone_problem_statement.pdf):** the original problem statement.
- **[Class diagram](docs/diagram/class_diagram.pdf):** how the classes fit together (source in `docs/diagram/class_diagram.mmd`).
- **[DESIGN.md](docs/DESIGN.md):** every design decision explained, step by step.
- **[DEVELOPER.md](docs/DEVELOPER.md):** architecture, invariants, and how to add a new command or type.
- **[EXTENSIONS.md](docs/EXTENSIONS.md):** the extra work added on top of the course requirements.
