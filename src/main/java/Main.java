import java.util.Scanner;

/**
 * Entry point. By default it reads commands from standard input until the input ends and prints exactly
 * what the spec requires. With {@code -i} it starts the friendlier interactive shell instead.
 */
public class Main {
    private static final String USAGE = String.join("\n",
            "Usage: java -jar library-management-system.jar [option]",
            "  (no option)         read commands from standard input and print plain results",
            "  -i, --interactive   friendly mode for typing at a terminal (prompt, colours, tables, help)",
            "  -h, --help          show this message");

    public static void main(String[] args) {
        boolean interactive = false;
        for (String arg : args) {
            if (arg.equals("-i") || arg.equals("--interactive")) {
                interactive = true;
            } else if (arg.equals("-h") || arg.equals("--help")) {
                System.out.println(USAGE);
                return;
            } else {
                System.err.println("Unknown option " + arg);
                System.err.println(USAGE);
                System.exit(2);
            }
        }

        Library library = new Library();
        CommandProcessor processor = new CommandProcessor(library);
        Scanner in = new Scanner(System.in);
        if (interactive) {
            boolean color = System.getenv("NO_COLOR") == null;
            new InteractiveShell(library, processor, color).run(in, System.out);
        } else {
            processor.run(in, System.out);
        }
    }
}
