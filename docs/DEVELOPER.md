# Developer Guide

Technical reference for developers and AI agents working on this codebase. For the reasoning behind decisions, see [DESIGN.md](DESIGN.md). For the authoritative behaviour spec, see [capstone_problem_statement.pdf](capstone_problem_statement.pdf). Input validation (section "Output contract") is an addition required by the TAs.

## Build and test

| Task | Command |
|---|---|
| Compile and package | `make` (or `./mvnw -DskipTests package`) |
| Run | `make run INPUT=input.txt` (or `java -jar target/library-management-system.jar < input.txt`) |
| Full test suite | `make test` (or `./mvnw verify`): compiles with warnings as errors, then runs every JUnit test, including the golden tests |
| CI | `.github/workflows/ci.yml` runs `./mvnw -B verify` on Temurin 21 for every push and PR |

- Target **Java 21** (the version used in the course), set in `pom.xml` (`maven.compiler.release`).
- Maven build via the committed wrapper (`mvnw`), so no Maven install is needed. The only dependency is JUnit 5, in test scope. The program itself has none.
- Layout: `src/main/java` (default package), `src/test/java` (JUnit), `src/test/resources/golden` (whole-program `.in`/`.expected` pairs).

## Architecture

```text
stdin ──► CommandProcessor.run()  ──► execute(line) ──► Library (rules + state) ──► domain objects
                 │   catch LibraryException ▲                                          │
                 ▼                           └────────── throws ◄───────────────────────┘
              stdout  ◄── List<String> lines formatted by CommandProcessor
```

| Layer | Classes | Responsibility |
|---|---|---|
| Entry | `Main` | Wires `new CommandProcessor(new Library())` to `System.in` / `System.out`. No logic. |
| Boundary | `CommandProcessor` | Splits lines on `\|`, trims, validates (`fields`, `parseDays`, `createMember`), calls `Library`, formats results. **The only `catch`.** |
| Domain service | `Library` | Owns all state (`items`, `members`, `loans`, `reservations`, `today`) and checks every business rule. No string formatting. |
| Domain model | `LibraryItem` ← `Book`, `Magazine`, `ReferenceBook`; `Borrowable` (interface); `Member` ← `Student`, `Staff`, `Faculty`; `Loan`; `ReturnResult` | Types and per-type rules, expressed as overridden methods. |
| Errors | `LibraryException` (abstract, checked) ← 8 subclasses | One class per failure kind, with messages built in constructors or static factories. |

### Per-type rules (single source of truth)

| Rule | Location |
|---|---|
| Fine per day | `Book.getFinePerDay()` = 2, `Magazine.getFinePerDay()` = 1 |
| Borrowable or not | `implements Borrowable`, or not (`ReferenceBook`) |
| Report type name | `getTypeName()` on each item class |
| Item limit and loan period | `getMaxItems()` / `getLoanPeriod()` on `Student`, `Staff`, `Faculty` |
| Due day | `Loan` constructor: `borrowDay + member.getLoanPeriod()` |
| Days late and fine | `Loan.daysLate(today)`, `Loan.fine(today)` |

## Invariants

1. An item is a key in `Library.loans` **if and only if** it is on loan. The map key makes double-lending impossible.
2. A member's held count is **derived**: `countLoans(member)` scans `loans`. There is no stored counter.
3. Only `Borrowable` items ever appear in `loans` or `reservations`.
4. `reservations` never holds an empty list. Empty queues are removed by `cancelReservation`.
5. A member is never in the queue of an item they currently hold: `reserve` rejects it, and `borrow` cancels their reservation.
6. `today` only increases, and only through `Library.advance`. There is no system clock anywhere.
7. Every `Loan` is created through `Library.lend`.
8. A command that throws changes no state. All checks run before any mutation.

## Output contract

- Each command prints exactly the lines listed in the spec, in order.
- Multi-line reports print `NONE` when empty.
- Errors print `ERROR <message>`.
- Blank or whitespace-only lines are skipped with no output.

| Command | Check order (first failure wins) |
|---|---|
| `BORROW` | fields → member exists → item exists → borrowable → not on loan → below limit |
| `RETURN` | fields → item exists → on loan |
| `RESERVE` | fields → member exists → item exists → borrowable → on loan → not holder → not already queued |
| `LOANS` | fields → member exists |
| `ADD_*` | fields → (member type valid) → ID not duplicate |

**Validation messages** (`InvalidInputException`, thrown in `CommandProcessor`):
- `unknown command X` (case-sensitive)
- `missing command`
- `<CMD> expects N field(s): a|b`
- `<field> cannot be empty`
- `days must be a non-negative number`
- `unknown member type X` (exact uppercase)

`DuplicateIdException` (`duplicate id X`) is thrown by `Library.addItem` / `addMember`. Item IDs and member IDs are separate namespaces.

**Sort orders** (String IDs compare lexicographically):

| Report | Sort |
|---|---|
| SEARCH | title, then ID |
| LOANS | due day, then item ID |
| OVERDUE | days late descending, then item ID |
| INVENTORY | ID |

## Recipes

### Add a command
1. Add a `case "NAME":` in `CommandProcessor.execute`. Start with `a = fields(parts, "field1", ...)`, which gives you count and empty-field validation for free.
2. Put any rule in a new `Library` method that throws a `LibraryException` subclass. Keep formatting in `CommandProcessor`.
3. If it's a report, build a `List<String>` and return it through `orNone(lines)`.
4. Add a golden pair `src/test/resources/golden/<name>.in` and `<name>.expected`, with the expected output derived by hand (`GoldenTest` finds it automatically), plus JUnit tests for any new `Library` rule. Then run `make test`.
5. Update `README.md`, this file, `docs/DESIGN.md`, and `docs/diagram/class_diagram.mmd` (then re-export the PDF; see DESIGN.md step 5).

### Add an item type
1. Create `class X extends LibraryItem`. Add `implements Borrowable` with `getFinePerDay()` if it can be lent. Implement `getTypeName()`.
2. Add an `ADD_X` case in `CommandProcessor.execute`.
3. Nothing in `Library` changes. It only ever sees `LibraryItem` and `Borrowable`.

### Add a member type
1. Create `class X extends Member` and implement `getMaxItems()` and `getLoanPeriod()`.
2. Add one `case "X":` to `CommandProcessor.createMember`. This is the only place member type names appear.

### Change a rule value
Edit the single method listed in the per-type rules table above. Then update any affected `.expected` files.

## Conventions

- One public class per file. Fields are `private final` unless they really change.
- Use checked exceptions (`extends LibraryException`) for every user-visible failure. Never print errors outside `run()`.
- No `if (type.equals("STUDENT"))`-style branching on types. Use polymorphism, or `instanceof Borrowable` for capabilities.
- `Library` returns domain objects. `CommandProcessor` turns them into strings.
- Keep a Javadoc purpose line on every class and on non-obvious methods.
- Tests: `.expected` files are written from the spec, never captured from program output.
