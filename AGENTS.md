# Agent Notes

Java 21 command-line library management system (college capstone). Default package, no dependencies.

- **Build and test:** `./mvnw verify` (or `make test`). It must end with `BUILD SUCCESS` before you commit.
- **Shortcuts:** `make` (compile), `make run INPUT=file`, `make test`, `make clean`, `make help`.
- **Run:** `./mvnw -q -DskipTests package && java -jar target/library-management-system.jar < src/test/resources/golden/sample.in`
- **Layout:** code in `src/main/java`, JUnit tests in `src/test/java`, whole-program `.in`/`.expected` pairs in `src/test/resources/golden` (picked up automatically by `GoldenTest`).

Hard rules:
- Output must match the spec character for character (`docs/capstone_problem_statement.pdf`, plus the validation rules in `docs/DEVELOPER.md`).
- Never use the system clock. Time is `Library.today`, advanced only by `ADVANCE`.
- Business rules go in `Library` or the model classes. Parsing, validation and formatting go in `CommandProcessor`. Exceptions are caught only in `CommandProcessor.run`.
- No branching on type-name strings. Use overridden methods or `instanceof Borrowable`.
- Write new test `.expected` files by hand from the rules, not from program output.
- Commit messages are plain with no attribution trailers. Keep `docs/DESIGN.md` and `docs/diagram/class_diagram.mmd` in sync with code changes.

Full architecture, invariants, check orders and recipes: [docs/DEVELOPER.md](docs/DEVELOPER.md).
