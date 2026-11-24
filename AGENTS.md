# Agent Notes

Java 21 command-line library management system (college capstone). Default package, no dependencies.

- **Build and test:** `./run_tests.sh`. It must print `0 failed` before you commit.
- **Run:** `javac --release 21 -d out src/*.java && java -cp out Main < tests/sample.in`

Hard rules:
- Output must match the spec character for character (`docs/capstone_problem_statement.pdf`, plus the validation rules in `docs/DEVELOPER.md`).
- Never use the system clock. Time is `Library.today`, advanced only by `ADVANCE`.
- Business rules go in `Library` or the model classes. Parsing, validation and formatting go in `CommandProcessor`. Exceptions are caught only in `CommandProcessor.run`.
- No branching on type-name strings. Use overridden methods or `instanceof Borrowable`.
- Write new test `.expected` files by hand from the rules, not from program output.
- Commit messages are plain with no attribution trailers. Keep `docs/DESIGN.md` and `docs/diagram/class_diagram.mmd` in sync with code changes.

Full architecture, invariants, check orders and recipes: [docs/DEVELOPER.md](docs/DEVELOPER.md).
