import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Optional friendly front end for people typing at a terminal (started with {@code -i}).
 * It adds a prompt, colours, a help screen and aligned tables, but never changes what a command does:
 * every command still goes through {@link CommandProcessor#handle}. The plain, graded mode does not use it.
 */
public class InteractiveShell {
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String CYAN = "\u001B[36m";

    /** Column titles for each report command. */
    private static final Map<String, List<String>> REPORT_HEADERS = Map.of(
            "SEARCH", List.of("ID", "TYPE", "TITLE"),
            "LOANS", List.of("ITEM", "TITLE", "DUE DAY"),
            "OVERDUE", List.of("ITEM", "MEMBER", "DAYS LATE", "FINE"),
            "INVENTORY", List.of("ITEM", "TYPE", "STATUS"));

    static final String HELP = String.join("\n",
            "Commands (fields are separated by |):",
            "  ADD_BOOK|id|title|author         add a book",
            "  ADD_MAGAZINE|id|title|issue      add a magazine",
            "  ADD_REFERENCE|id|title|author    add a reference book (library use only)",
            "  ADD_MEMBER|id|name|type          type is STUDENT, STAFF or FACULTY",
            "  BORROW|memberId|itemId           lend an item",
            "  RETURN|itemId                    take an item back",
            "  RESERVE|memberId|itemId          join the waiting list for an item on loan",
            "  ADVANCE|days                     move the clock forward",
            "  SEARCH|text                      find items by title",
            "  LOANS|memberId                   what a member has out",
            "  OVERDUE                          loans past their due day",
            "  INVENTORY                        every item and its status",
            "Shell: HELP or ?  show this list,  EXIT or QUIT  leave (Ctrl+D also works).");

    private final Library library;
    private final CommandProcessor processor;
    private final boolean color;

    public InteractiveShell(Library library, CommandProcessor processor, boolean color) {
        this.library = library;
        this.processor = processor;
        this.color = color;
    }

    public void run(Scanner in, PrintStream out) {
        out.println(style(BOLD, "Library Management System"));
        out.println("Type HELP for the command list, EXIT to quit.");
        while (true) {
            out.print(style(CYAN, "library (day " + library.getToday() + ")> "));
            out.flush();
            if (!in.hasNextLine()) {
                out.println();
                return;
            }
            String line = in.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            String word = line.toUpperCase();
            if (word.equals("EXIT") || word.equals("QUIT")) {
                return;
            }
            if (word.equals("HELP") || word.equals("?")) {
                out.println(HELP);
                continue;
            }
            print(out, line, processor.handle(line));
        }
    }

    private void print(PrintStream out, String line, List<String> lines) {
        String command = line.split("\\|", 2)[0].trim();
        List<String> headers = REPORT_HEADERS.get(command);
        if (headers == null || lines.isEmpty() || lines.get(0).startsWith("ERROR")) {
            for (String text : lines) {
                out.println(colourise(text));
            }
        } else if (lines.get(0).equals("NONE")) {
            out.println(style(DIM, "(none)"));
        } else {
            printTable(out, command, headers, lines);
        }
    }

    private String colourise(String text) {
        if (text.startsWith("OK")) {
            return style(GREEN, text);
        }
        return text.startsWith("ERROR") ? style(RED, text) : text;
    }

    private void printTable(PrintStream out, String command, List<String> headers, List<String> lines) {
        List<List<String>> rows = new ArrayList<>();
        for (String line : lines) {
            rows.add(cells(command, line));
        }
        int[] widths = new int[headers.size()];
        for (int c = 0; c < widths.length; c++) {
            widths[c] = headers.get(c).length();
            for (List<String> row : rows) {
                widths[c] = Math.max(widths[c], row.get(c).length());
            }
        }
        out.println(style(BOLD, formatRow(headers, widths)));
        List<String> rule = new ArrayList<>();
        for (int width : widths) {
            rule.add("-".repeat(width));
        }
        out.println(style(DIM, formatRow(rule, widths)));
        for (List<String> row : rows) {
            out.println(formatRow(row, widths));
        }
    }

    /** Splits a report line into its cells. INVENTORY's "ON_LOAN|S1" becomes one cell: "ON_LOAN (S1)". */
    private static List<String> cells(String command, String line) {
        String[] parts = line.split("\\|", -1);
        List<String> cells = new ArrayList<>(List.of(parts));
        if (command.equals("INVENTORY") && cells.size() == 4) {
            cells = new ArrayList<>(List.of(parts[0], parts[1], parts[2] + " (" + parts[3] + ")"));
        }
        return cells;
    }

    private static String formatRow(List<String> row, int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < widths.length; c++) {
            sb.append(String.format("%-" + widths[c] + "s", row.get(c)));
            if (c < widths.length - 1) {
                sb.append("  ");
            }
        }
        return sb.toString().stripTrailing();
    }

    private String style(String code, String text) {
        return color ? code + text + RESET : text;
    }
}
