import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Input validation: each malformed line must produce its exact error and change nothing. */
class CommandProcessorTest {
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = ';', value = {
        "FOO|1;ERROR unknown command FOO",
        "borrow|S1|B1;ERROR unknown command borrow",
        "|B1;ERROR missing command",
        "ADD_BOOK|B1|T;ERROR ADD_BOOK expects 3 fields: id|title|author",
        "ADD_BOOK|B1|T|A|X;ERROR ADD_BOOK expects 3 fields: id|title|author",
        "ADD_BOOK|B1||A;ERROR title cannot be empty",
        "ADD_BOOK|   |T|A;ERROR id cannot be empty",
        "ADD_MEMBER|S1|N|ADMIN;ERROR unknown member type ADMIN",
        "ADD_MEMBER|S1|N|student;ERROR unknown member type student",
        "ADVANCE|abc;ERROR days must be a non-negative number",
        "ADVANCE|-3;ERROR days must be a non-negative number",
        "ADVANCE|99999999999;ERROR days must be a non-negative number",
        "ADVANCE|2.5;ERROR days must be a non-negative number",
        "ADVANCE|;ERROR days cannot be empty",
        "RETURN;ERROR RETURN expects 1 field: itemId",
        "INVENTORY|x;ERROR INVENTORY expects no fields",
        "BORROW|S1;ERROR BORROW expects 2 fields: memberId|itemId",
    })
    void malformedLineGivesItsErrorMessage(String line, String error) {
        assertEquals(error + "\n", GoldenTest.run(line + "\n"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = ';', value = {"ADVANCE|0;OK today is day 0", "ADVANCE|7;OK today is day 7"})
    void validAdvanceIsAccepted(String line, String reply) {
        assertEquals(reply + "\n", GoldenTest.run(line + "\n"));
    }

    @org.junit.jupiter.api.Test
    void blankLinesAreSkippedSilently() {
        assertEquals("OK\n", GoldenTest.run("\n   \nADD_BOOK|B1|T|A\n\n"));
    }

    @org.junit.jupiter.api.Test
    void duplicateIdIsReportedAndTheFirstItemIsKept() {
        assertEquals("OK\nERROR duplicate id B1\nB1|BOOK|AVAILABLE\n",
                GoldenTest.run("ADD_BOOK|B1|T|A\nADD_MAGAZINE|B1|M|i\nINVENTORY\n"));
    }
}
