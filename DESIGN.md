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
