import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Reads one command per line, runs it against the Library, and prints the result. */
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
        String[] f = line.split("\\|", -1);
        for (int i = 0; i < f.length; i++) {
            f[i] = f[i].trim();
        }
        switch (f[0]) {
            case "ADD_BOOK":
                library.addItem(new Book(f[1], f[2], f[3]));
                return List.of("OK");
            case "ADD_MAGAZINE":
                library.addItem(new Magazine(f[1], f[2], f[3]));
                return List.of("OK");
            case "ADD_REFERENCE":
                library.addItem(new ReferenceBook(f[1], f[2], f[3]));
                return List.of("OK");
            case "ADD_MEMBER":
                library.addMember(createMember(f[1], f[2], f[3]));
                return List.of("OK");
            case "BORROW": {
                Loan loan = library.borrow(f[1], f[2]);
                return List.of("OK due on day " + loan.getDueDay());
            }
            case "RETURN":
                return List.of("OK fine " + library.returnItem(f[1]));
            case "ADVANCE":
                return List.of("OK today is day " + library.advance(Integer.parseInt(f[1])));
            case "INVENTORY":
                return inventory();
            default:
                throw new InvalidInputException("unknown command " + f[0]);
        }
    }

    /** Turns the type text into the right Member subclass. The only place member types are named. */
    private Member createMember(String id, String name, String type) throws InvalidInputException {
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
