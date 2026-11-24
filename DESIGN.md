# Design and Implementation Decisions

A guide to *why* the code is shaped the way it is.
Each section names the class that owns a rule, so you can find it quickly.

## At a glance

```text
Main ──► CommandProcessor ──► Library ──► LibraryItem / Member / Loan
        (text in/out,         (all rules,
         validation,           all state,
         the one catch)        throws exceptions)
```

### Where each rule lives

| Rule (from the brief) | Class.method |
|---|---|
| Book fine ₹2/day, magazine ₹1/day | `Book.getFinePerDay()`, `Magazine.getFinePerDay()` |
| Reference books can't be borrowed | `ReferenceBook` does **not** implement `Borrowable`; checked in `Library.borrow` |
| Student 3/14, Staff 4/21, Faculty 5/30 | `Student` / `Staff` / `Faculty`: `getMaxItems()`, `getLoanPeriod()` |
| Due day = borrow day + loan period | `Loan` constructor |
| Fine = days late × rate, none if on time | `Loan.daysLate()`, `Loan.fine()` |
| One member per item | `Library.loans` map keyed by item ID; checked in `Library.borrow` |
| Member limit | `Library.borrow` (`countLoans` vs `getMaxItems()`) |
| Reservation rules and auto-issue | `Library.reserve`, `Library.issueToNextInQueue` |
| Unique IDs | `Library.addItem`, `Library.addMember` |
| Well-formed input | `CommandProcessor.fields`, `parseDays`, `createMember` |
| Errors caught in one place | `CommandProcessor.run` |

### Making common changes

| If you want to… | Change |
|---|---|
| Change a borrowing limit or loan period | That member class's `getMaxItems()` / `getLoanPeriod()` (one line) |
| Change a fine rate | That item class's `getFinePerDay()` (one line) |
| Make reference books borrowable | `ReferenceBook implements Borrowable` + add `getFinePerDay()` |
| Add a member type (e.g. `GUEST`) | New `class Guest extends Member` + one `case "GUEST":` in `CommandProcessor.createMember` |
| Add an item type (e.g. `DVD`) | New `class Dvd extends LibraryItem implements Borrowable` + an `ADD_DVD` case in `CommandProcessor.execute` |
| Add a command | New `case` in `CommandProcessor.execute` using `fields(...)`; rule logic in a new `Library` method; report lines via `orNone(...)` |
| Change a sort order | The `Comparator` in the matching `Library` method (`search`, `loansOf`, `overdueLoans`, `inventory`) |
| Cap fines (e.g. max ₹50) | `Loan.fine()`: `Math.min(50, ...)` |
| Change an error message | That exception's constructor or static factory |
| Change the check order | Reorder the `if` blocks in `Library.borrow` / `Library.reserve` |

After any change: `./run_tests.sh`, then update the affected `.expected` files.

The rest of this file is a step-by-step log of decisions, in build order.

## Step 5: Design

- The class diagram is in `class_diagram.mmd` (source) and `class_diagram.pdf` (export).
- **Two hierarchies.** Items (`LibraryItem` → `Book`, `Magazine`, `ReferenceBook`) and members (`Member` → `Student`, `Staff`, `Faculty`).
- **One interface.** Only `Book` and `Magazine` can be borrowed, so only they implement `Borrowable`. "Can this be borrowed?" becomes a type question (`item instanceof Borrowable`), not a flag.
- **One coordinator.** `Library` holds the catalogue, the members, the active loans and today's day. Every rule is checked there.
- **`Loan` is its own class**, not just a link between a member and an item. A loan has data of its own (borrow day, due day, fine rate) and behaviour of its own (days late, fine). Putting that on `Member` or `LibraryItem` would mix responsibilities.
- **Exceptions.** Every failure is a subclass of `LibraryException`. They are thrown where the rule is checked and caught once, in the command loop.

## Step 6: Classes, fields, constructors, getters

- **Fields are `private final`** wherever the value never changes after construction (IDs, titles, authors). Only `Library.today` is mutable.
- **The rules live in overridden methods, not in fields or `if` chains.**
  - `Book.getFinePerDay()` returns 2 and `Magazine.getFinePerDay()` returns 1.
  - `Student`, `Staff` and `Faculty` each override `getMaxItems()` and `getLoanPeriod()`.
  - Changing a rule is a one-line edit in one class.
- **`getTypeName()` is abstract** in `LibraryItem`, so every new item type is forced to say what it is called in reports.
- **Loan's constructor takes the `Borrowable` as a parameter** as well as the `LibraryItem`. Callers pass the same object twice (`new Loan(item, (Borrowable) item, ...)` after an `instanceof` check). The fine rate is copied into the loan, so `Loan` never has to cast anything.
- **The due day is computed once, in the `Loan` constructor**, as `borrowDay + member.getLoanPeriod()`.
- **`Library.loans` is keyed by item ID.** A map key is unique, so "an item can be on loan to only one member" holds by construction.
- **Exceptions with two or three messages use static factory methods**, so the call site reads like the rule:
  - `ItemNotAvailableException.onLoan(id)` and `.notBorrowable(id)`
  - `ReservationNotAllowedException.available(id)`, `.alreadyHolds(...)` and `.alreadyReserved(...)`
- **`LibraryException` extends `Exception`, not `RuntimeException`.** That makes every exception *checked*: the compiler forces each method that can fail to declare it with `throws`. That makes it easy to see which methods can fail.

## Step 7: Hand test from main

- `Main` built one item and one member of each type and printed their rules. Output:
  ```
  B1 BOOK fine 2/day
  M1 MAGAZINE fine 1/day
  R1 REFERENCE not borrowable
  S1 max 3 period 14
  T1 max 4 period 21
  F1 max 5 period 30
  ```
  These match the tables in sections 2 and 3 of the brief.
- The test uses **`instanceof` pattern matching** (`item instanceof Borrowable b`). If the check passes, `b` is already the item viewed as a `Borrowable`, with no separate cast. The same idiom is used in `Library.borrow`.
- The test loops over `LibraryItem[]` and `Member[]` and calls the overridden methods. This is **polymorphism**: the loop doesn't know or care which subclass it has.

## Step 8: Command loop, ADD_ commands, INVENTORY

- **Who does what.**
  - `Main` only wires things together: `new CommandProcessor(new Library()).run(...)`.
  - `CommandProcessor` turns text into method calls and results back into text.
  - `Library` knows nothing about text formats or `|` separators.
- **`execute(line)` returns a `List<String>`** of lines to print instead of printing directly. Single-line results and multi-line reports are handled the same way, and only `run()` touches the output stream.
- **Splitting.** `line.split("\\|", -1)`:
  - The `\\|` escapes the pipe, because `split` takes a regex and a bare `|` means "or".
  - The `-1` keeps trailing empty fields, so `ADD_BOOK|B1|Title|` gives 4 fields, not 3. Step 11 validation relies on this.
- **Every field is trimmed**, so stray spaces or a Windows `\r` at the end of a line don't break IDs.
- **Blank lines are skipped silently**, so an empty line at the end of a test file doesn't print an extra `ERROR`. To report them instead, change the `continue` in `run()` to throw an `InvalidInputException`.
- **`createMember` is the only place the member type text appears.** It is a *factory*: it turns `"STUDENT"` into a `Student`. After that, no code ever checks the type again, because the object answers for itself through `getMaxItems()` and `getLoanPeriod()`. The brief's warning about `if (type.equals("STUDENT"))` means exactly this: the text is translated once, at the boundary.
- **INVENTORY status** comes from two facts:
  - not a `Borrowable` → `NOT_BORROWABLE`;
  - otherwise it depends on whether `library.findLoan(id)` returns a loan.

  There is no stored status field that could drift out of sync with the loans map.
- **Sorting.** INVENTORY sorts with `Comparator.comparing(LibraryItem::getId)`. IDs are compared as strings, so `B10` comes before `B2`.
- **`orNone(lines)`** turns an empty report into the single line `NONE`, as the brief requires for every report.

## Step 9: BORROW and RETURN

- **Exceptions from the start.** The brief's step 11 says to print errors first and replace them with exceptions later. Here they were thrown straight away, since the exception classes already existed from step 6. Step 11 instead adds input validation.
- **Where each BORROW rule lives.**

  | Rule | Checked in | Throws |
  |---|---|---|
  | Member must exist | `Library.findMember` | `MemberNotFoundException` |
  | Item must exist | `Library.findItem` | `ItemNotFoundException` |
  | Item must be borrowable | `Library.borrow` (`instanceof Borrowable`) | `ItemNotAvailableException.notBorrowable` |
  | Item must not be on loan | `Library.borrow` (`loans.containsKey`) | `ItemNotAvailableException.onLoan` |
  | Member below their limit | `Library.borrow` (`countLoans` vs `member.getMaxItems()`) | `BorrowLimitExceededException` |

- **Check order: member, item, borrowable, on loan, limit.** The order matters when more than one rule fails at once.
  - The brief's sample agrees: `BORROW|T1|B9` reports the item as not found.
  - A member at their limit asking for an item that is on loan gets "is on loan". Item checks come before the limit check.
  - To change the order, reorder the `if` blocks in `Library.borrow`.
- **`if (!(item instanceof Borrowable borrowable)) throw ...;`** After this line, `borrowable` is in scope and known to be a `Borrowable`. The compiler allows it because the only way past the `if` is when the check succeeded (*flow scoping*).
- **The held count is computed, not stored.** `countLoans(member)` counts the member's active loans. A separate counter on `Member` would have to be kept in step with `loans` on every borrow and return, and one missed update would cause a bug.
- **Members are compared with `==` in `countLoans`.** Each member exists exactly once in the library, so comparing object identity is correct and cheap.
- **RETURN still calls `findItem` first**, so an unknown ID reports "item X not found" rather than "X is not on loan".
- `loans.remove(itemId)` both ends the loan and tells us whether there was one: it returns `null` if the item wasn't on loan.

## Step 10: ADVANCE, due days, fines

- **Time is just an `int`** (`Library.today`, starting at 0). Only `Library.advance` changes it. The program never reads the system clock, so every run of the same input gives the same output.
- **The fine maths lives in `Loan`**, because a loan has all the facts it needs: due day and fine rate.
  - `daysLate(today) = max(0, today - dueDay)`. The `max` gives "no fine if returned on or before the due day".
  - `fine(today) = daysLate(today) × finePerDay`.
  - Both take `today` as a parameter instead of storing it, so the same loan can answer for any day. RETURN uses this, and so does OVERDUE ("fine owed so far").
- **Checked by hand:**
  - Returned on day 14, due on day 14 → `OK fine 0`.
  - Magazine returned on day 20, due on day 14 → 6 days × ₹1 → `OK fine 6`.
- `Library.returnItem` now returns the fine (`int`). It doesn't return the `Loan`, because the caller only needs the amount.

## Step 11: Input validation

The brief assumes well-formed input, but the TAs overrode this: invalid input is checked in evaluation. Every malformed line throws `InvalidInputException`, and every reused ID throws `DuplicateIdException`. Both extend `LibraryException`, so the same single `catch` in `run()` prints them as `ERROR <message>`. Nothing new was added to the error handling path.

| Input | Output | Where |
|---|---|---|
| `FOO`, or the right name in the wrong case (`borrow`) | `ERROR unknown command FOO` | `execute`, `default:` branch |
| A line starting with `\|` | `ERROR missing command` | `execute`, `case "":` |
| Too few or too many fields | `ERROR BORROW expects 2 fields: memberId\|itemId` | `fields(...)` |
| A field that is empty or only spaces | `ERROR title cannot be empty` | `fields(...)` |
| `ADVANCE` with a non-number, decimal, negative or too-large value | `ERROR days must be a non-negative number` | `parseDays` |
| Member type other than `STUDENT`, `STAFF` or `FACULTY` (exact case) | `ERROR unknown member type ADMIN` | `createMember` |
| ADD_ with an ID already used | `ERROR duplicate id B1` | `Library.addItem` / `addMember` |

Decisions behind the table:

- **`fields(parts, "id", "title", "author")`** does all the count and empty checks in one place. The field names give both the error message and the documentation of each command. A new command needs one `fields(...)` call to be fully validated.
- **Validation order:** command name first, then field count, then empty fields, then field content (number, member type), then the library rules.
- **Command names and member types are case-sensitive**, because the brief spells them in capitals. To accept lowercase, use `command.toUpperCase()` and `type.toUpperCase()`.
- **`ADVANCE|0` is allowed** (it's harmless and prints the current day). Negative days are rejected, because time only moves forward.
- **Integer overflow** (`ADVANCE|99999999999`) is caught, because `Integer.parseInt` throws `NumberFormatException` for it, the same as for `abc`.
- **Item and member IDs are separate namespaces.** A member `B1` and a book `B1` can both exist, because the commands always say which kind of ID they expect. Two *items* can't share an ID, even across types (a book and a magazine), because they share the one catalogue map.
- **The duplicate check lives in `Library`**, not `CommandProcessor`. "IDs are unique" is a rule about the library's data, and only the library can see the existing IDs.
- **Why validate in `CommandProcessor`?** It is the boundary where text becomes objects. Everything behind it (`Library`, `Loan`, the item and member classes) can assume clean values.
- **Blank lines are still skipped** (see step 8).

## Step 12: SEARCH, LOANS, OVERDUE

- **Each report is a `Comparator`**, built with `Comparator.comparing(...)` and `.thenComparing(...)` for the tie-break:

  | Report | Sort | Code |
  |---|---|---|
  | SEARCH | title, then ID | `comparing(LibraryItem::getTitle).thenComparing(LibraryItem::getId)` |
  | LOANS | due day, then item ID | `comparingInt(Loan::getDueDay).thenComparing(Library::itemIdOf)` |
  | OVERDUE | days late (most first), then item ID | `comparingInt(loan -> loan.daysLate(today)).reversed().thenComparing(...)` |
  | INVENTORY | item ID | `comparing(LibraryItem::getId)` |

- **The brief gives no tie-break for SEARCH.** Two items can share a title, so the ID is added to keep the output deterministic.
- **The `.reversed()` in OVERDUE flips only the days-late part.** It applies to the comparator built so far, which is just the days-late one. The item-ID tie-break added afterwards stays ascending.
- **Titles are compared exactly as written** (`String.compareTo`), so capital letters sort before lowercase. For a case-insensitive order, use `comparing(LibraryItem::getTitle, String.CASE_INSENSITIVE_ORDER)`.
- **Matching is case-insensitive.** `matchesTitle` lowercases both strings with `Locale.ROOT`, so the result doesn't depend on the computer's language setting.
- **`Library` returns objects; `CommandProcessor` formats them.**
  - `Library.loansOf` returns `List<Loan>`, and `CommandProcessor.loans` turns each into `itemId|title|dueDay`.
  - Changing an output format never touches the rules.
- **OVERDUE computes the fine "so far"** with `loan.fine(today)`. It reuses the method RETURN uses, so the two can never disagree.
- **`LOANS` for an unknown member** throws `MemberNotFoundException` (from `findMember`). A known member with no loans prints `NONE`.
- **`tests/sample.in` and `tests/sample.expected`** were copied from section 9 of the brief. `java -cp out Main < tests/sample.in | diff - tests/sample.expected` prints nothing, so the output matches exactly.

## Step 13: Tests and CI

- **Each test is a pair**, `tests/<name>.in` and `tests/<name>.expected`. `./run_tests.sh`:
  - compiles everything (warnings count as errors);
  - runs each `.in` through `java -cp out Main`;
  - diffs the output against the matching `.expected`;
  - exits non-zero if anything differs.
- **The expected outputs were worked out by hand from the rules**, not copied from the program's output. A test that only repeats what the program already prints can't catch a bug. Writing them this way caught a mistake in one expected file (a day-0 student loan is due on day 14, not day 0).

| Test | What it proves |
|---|---|
| `sample` | The brief's section 9 session, character for character |
| `errors` | Every section 7 error message, at least once |
| `limits` | Student, staff and faculty each hit their own limit (3, 4, 5), with loan periods 14, 21, 30. Returning an item frees a slot. |
| `fines` | Return on the due day means fine 0; fines for books (₹2) and magazines (₹1); OVERDUE "fine so far"; `NONE` for empty reports; case-insensitive search |
| `sorting` | Every tie-break: LOANS same due day → item ID; OVERDUE same days late → item ID; SEARCH same title → ID; string ID order (`B10` before `B2`) |
| `invalid_input` | Every validation message from step 11. Bad lines change nothing, so the final INVENTORY shows only the valid commands' effect. |

- **CI.** `.github/workflows/ci.yml` runs `./run_tests.sh` on GitHub Actions (Java 17) for every push and pull request. A red badge on the README means some commit broke a test.
- **Java 17 compatibility.** The code compiles with `--release 17`, so it runs on any JDK from 17 upwards. Lab machines may have an older JDK than the one this was developed on.

## Step 14: Reservations (bonus)

- **Storage.** `Library.reservations` is a `Map<String, List<Member>>`, keyed by item ID and kept in the order members reserved.
  - It's a `List`, not a `Queue`, because the rules can skip a member in the middle (someone at their limit) and still keep them waiting. A plain queue only lets you take from the front.
  - An empty list is removed from the map, so the map only holds items somebody is actually waiting for.
- **RESERVE check order:**
  1. member exists
  2. item exists
  3. item is borrowable (`cannot be borrowed`, the same error as BORROW, as section 8 requires)
  4. item is on loan (otherwise `<id> is available`)
  5. the member isn't the current holder (`already has`)
  6. the member isn't already queued (`already reserved`)
- **RETURN returns a `ReturnResult`**, not just the fine. A return can now also start a new loan, and the command needs both facts to print `OK fine 12, issued to T1 due on day 41`. A small result object is clearer than returning two values some other way, such as an array or extra getters on `Library`.
- **Auto-issue** (`issueToNextInQueue`):
  - It walks the queue in order and lends the item to the first member with `countLoans(member) < member.getMaxItems()`.
  - Members who are skipped stay in place.
  - The new loan starts *today*, so its due day is `today + loanPeriod`.
  - If nobody qualifies, the item becomes AVAILABLE and the queue is kept for the next return.
- **One lending path.** `lend(...)` is shared by BORROW and auto-issue, so a loan is always created and recorded the same way.
- **The brief leaves one case open:** a skipped member could later BORROW the item directly while still in its queue. Without handling, the item would be auto-issued straight back to them when they return it. **Decision: borrowing an item cancels your own reservation for it** (`cancelReservation` in `borrow`). `tests/reservations_skip` covers this.
- **Changing the queue while looping over it.** `issueToNextInQueue` removes from the list inside a for-each loop and returns immediately afterwards. Continuing the loop after a removal would throw `ConcurrentModificationException`.
- **The class diagram was updated** to match the final code: `ReturnResult`, the reservations field, `reserve()`, the private helpers, and the real `CommandProcessor` method names.
