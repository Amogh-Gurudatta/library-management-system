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
