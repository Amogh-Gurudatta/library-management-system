import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

class InteractiveShellTest {
    private static final String BANNER = "Library Management System\nType HELP for the command list, EXIT to quit.\n";

    private static String shell(String input, boolean color) {
        Library library = new Library();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new InteractiveShell(library, new CommandProcessor(library), color).run(new Scanner(input), out);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void showsBannerAndPromptWithTheCurrentDay() {
        assertEquals(BANNER + "library (day 0)> OK today is day 5\nlibrary (day 5)> ",
                shell("ADVANCE|5\n", false).replaceFirst("\n$", ""));
    }

    @Test
    void exitStopsAndLaterLinesAreNotRun() {
        String out = shell("EXIT\nADD_BOOK|B1|T|A\n", false);
        assertEquals(BANNER + "library (day 0)> ", out);
    }

    @Test
    void helpListsTheCommandsWithoutRunningAnything() {
        String out = shell("help\nquit\n", false);
        assertTrue(out.contains("BORROW|memberId|itemId"));
        assertTrue(out.contains("OVERDUE"));
        assertFalse(out.contains("ERROR"));
    }

    @Test
    void reportsAreShownAsAlignedTables() {
        String out = shell("ADD_BOOK|B1|Intro|Cormen\nADD_MEMBER|S1|Sam|STUDENT\nBORROW|S1|B1\nINVENTORY\nexit\n", false);
        assertTrue(out.contains("ITEM  TYPE  STATUS\n----  ----  ------------\nB1    BOOK  ON_LOAN (S1)\n"), out);
    }

    @Test
    void emptyReportsSayNone() {
        assertTrue(shell("OVERDUE\nexit\n", false).contains("(none)\n"));
    }

    @Test
    void errorsKeepTheirExactMessage() {
        assertTrue(shell("BORROW|S1|B1\nFOO\nexit\n", false)
                .contains("ERROR member S1 not found\nlibrary (day 0)> ERROR unknown command FOO\n"));
    }

    @Test
    void endOfInputExitsCleanly() {
        assertEquals(BANNER + "library (day 0)> \n", shell("", false));
    }

    @Test
    void colourIsOnlyAddedWhenAskedFor() {
        assertFalse(shell("FOO\nexit\n", false).contains("\u001B"));
        assertTrue(shell("FOO\nexit\n", true).contains("\u001B[31mERROR unknown command FOO\u001B[0m"));
    }
}
