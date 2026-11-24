# Design and Implementation Decisions

A guide to *why* the code is shaped the way it is.
Each section names the class that owns a rule, so you can find it quickly.

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
