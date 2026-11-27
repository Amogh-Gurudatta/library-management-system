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

    private static final String SETUP = "ADD_BOOK|B1|Intro|Cormen\nADD_MAGAZINE|M1|Wired|Oct\nADD_MEMBER|S1|Sam|STUDENT\n"
            + "BORROW|S1|B1\nBORROW|S1|M1\n";

    @Test
    void searchLoansAndOverdueTablesHaveTheirHeadersAndRows() {
        String out = shell(SETUP + "ADVANCE|20\nSEARCH|intro\nLOANS|S1\nOVERDUE\nexit\n", false);
        assertTrue(out.contains("ID  TYPE  TITLE\n--  ----  -----\nB1  BOOK  Intro\n"), out);
        assertTrue(out.contains("ITEM  TITLE  DUE DAY\n----  -----  -------\nB1    Intro  14\nM1    Wired  14\n"), out);
        assertTrue(out.contains("ITEM  MEMBER  DAYS LATE  FINE\n----  ------  ---------  ----\n"
                + "B1    S1      6          12\nM1    S1      6          6\n"), out);
    }

    /** The shell must never change what a command does: same script, same report lines as plain mode. */
    @Test
    void shellAndPlainModeAgreeOnEveryCommand() {
        String script = SETUP + "ADD_BOOK|B1|dup|x\nBORROW|S1|B9\nADVANCE|3\nRETURN|M1\nINVENTORY\nFOO\n";
        String plain = GoldenTest.run(script);
        String viaShell = shell(script + "exit\n", false)
                .replaceAll("library \\(day \\d+\\)> ", "");
        for (String line : plain.split("\n")) {
            if (line.startsWith("OK") || line.startsWith("ERROR")) {
                assertTrue(viaShell.contains(line + "\n"), "missing '" + line + "' in:\n" + viaShell);
            }
        }
        assertTrue(viaShell.contains("ERROR duplicate id B1\n"));
        assertTrue(viaShell.contains("OK today is day 3\n"));
    }

    @Test
    void shellKeepsTheLibraryStateBetweenLines() {
        assertTrue(shell("ADD_BOOK|B1|T|A\nADD_BOOK|B1|T|A\nexit\n", false).contains("ERROR duplicate id B1"));
    }

    @Test
    void aFailingReportCommandPrintsItsErrorNotATable() {
        String out = shell("LOANS|X1\nSEARCH|\nexit\n", false);
        assertTrue(out.contains("ERROR member X1 not found\n"), out);
        assertTrue(out.contains("ERROR text cannot be empty\n"), out);
        assertFalse(out.contains("---"), out);
    }

    @Test
    void reportCommandsWithPaddingAroundTheNameStillGetTables() {
        String out = shell("ADD_BOOK|B1|Intro|Cormen\n  SEARCH |intro\nexit\n", false);
        assertTrue(out.contains("ID  TYPE  TITLE\n"), out);
    }

    @Test
    void questionMarkShowsHelpAndBlankLinesJustReprompt() {
        String out = shell("?\n\n   \nexit\n", false);
        assertTrue(out.contains(InteractiveShell.HELP), out);
        assertEquals(BANNER + "library (day 0)> " + InteractiveShell.HELP + "\nlibrary (day 0)> library (day 0)> library (day 0)> ", out);
    }

    @Test
    void successLinesAreGreenWhenColourIsOn() {
        assertTrue(shell("ADD_BOOK|B1|T|A\nexit\n", true).contains("\u001B[32mOK\u001B[0m"));
    }
}
