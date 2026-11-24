import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Reads one command per line, validates it, runs it against the Library and prints the result.
 * All text parsing and output formatting lives here; Library never sees raw input.
 */
public class CommandProcessor {
    private final Library library;

    public CommandProcessor(Library library) {
        this.library = library;
    }

    /** The command loop. This is the one place where LibraryExceptions are caught. */
    public void run(Scanner in, PrintStream out) {
        while (in.hasNextLine()) {
            String line = in.nextLine();
            if (line.isBlank()) {
                continue;
            }
            try {
                for (String result : execute(line)) {
                    out.println(result);
                }
            } catch (LibraryException e) {
                out.println("ERROR " + e.getMessage());
            }
        }
    }

    /** Runs one command line and returns the lines to print. */
    private List<String> execute(String line) throws LibraryException {
        String[] parts = line.split("\\|", -1);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        String command = parts[0];
        String[] a;
        switch (command) {
            case "ADD_BOOK":
                a = fields(parts, "id", "title", "author");
                library.addItem(new Book(a[0], a[1], a[2]));
                return List.of("OK");
            case "ADD_MAGAZINE":
                a = fields(parts, "id", "title", "issue");
                library.addItem(new Magazine(a[0], a[1], a[2]));
                return List.of("OK");
            case "ADD_REFERENCE":
                a = fields(parts, "id", "title", "author");
                library.addItem(new ReferenceBook(a[0], a[1], a[2]));
                return List.of("OK");
            case "ADD_MEMBER":
                a = fields(parts, "id", "name", "type");
                library.addMember(createMember(a[0], a[1], a[2]));
                return List.of("OK");
            case "BORROW": {
                a = fields(parts, "memberId", "itemId");
                Loan loan = library.borrow(a[0], a[1]);
                return List.of("OK due on day " + loan.getDueDay());
            }
            case "RETURN":
                a = fields(parts, "itemId");
                return List.of("OK fine " + library.returnItem(a[0]));
            case "ADVANCE":
                a = fields(parts, "days");
                return List.of("OK today is day " + library.advance(parseDays(a[0])));
            case "INVENTORY":
                fields(parts);
                return inventory();
            case "":
                throw new InvalidInputException("missing command");
            default:
                throw new InvalidInputException("unknown command " + command);
        }
    }

    /**
     * Checks that the command has exactly the named fields and none is empty,
     * then returns them without the command name.
     */
    private static String[] fields(String[] parts, String... names) throws InvalidInputException {
        if (parts.length - 1 != names.length) {
            String usage = names.length == 0 ? "no fields" : names.length + (names.length == 1 ? " field: " : " fields: ")
                    + String.join("|", names);
            throw new InvalidInputException(parts[0] + " expects " + usage);
        }
        String[] values = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            if (parts[i + 1].isEmpty()) {
                throw new InvalidInputException(names[i] + " cannot be empty");
            }
            values[i] = parts[i + 1];
        }
        return values;
    }

    /** Parses the ADVANCE argument: a whole number, zero or more. */
    private static int parseDays(String text) throws InvalidInputException {
        int days;
        try {
            days = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new InvalidInputException("days must be a non-negative number");
        }
        if (days < 0) {
            throw new InvalidInputException("days must be a non-negative number");
        }
        return days;
    }

    /** Turns the type text into the right Member subclass. The only place member types are named. */
    private static Member createMember(String id, String name, String type) throws InvalidInputException {
        switch (type) {
            case "STUDENT":
                return new Student(id, name);
            case "STAFF":
                return new Staff(id, name);
            case "FACULTY":
                return new Faculty(id, name);
            default:
                throw new InvalidInputException("unknown member type " + type);
        }
    }

    private List<String> inventory() {
        List<String> lines = new ArrayList<>();
        for (LibraryItem item : library.inventory()) {
            lines.add(item.getId() + "|" + item.getTypeName() + "|" + statusOf(item));
        }
        return orNone(lines);
    }

    private String statusOf(LibraryItem item) {
        if (!(item instanceof Borrowable)) {
            return "NOT_BORROWABLE";
        }
        Loan loan = library.findLoan(item.getId());
        return loan == null ? "AVAILABLE" : "ON_LOAN|" + loan.getMember().getId();
    }

    /** A report with no rows prints a single NONE line. */
    private static List<String> orNone(List<String> lines) {
        return lines.isEmpty() ? List.of("NONE") : lines;
    }
}
