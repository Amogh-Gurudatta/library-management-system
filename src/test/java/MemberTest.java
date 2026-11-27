import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MemberTest {
    @ParameterizedTest(name = "{0}: {1} items, {2} days")
    @CsvSource({"STUDENT,3,14", "STAFF,4,21", "FACULTY,5,30"})
    void eachMemberTypeHasItsOwnLimitAndLoanPeriod(String type, int maxItems, int loanPeriod) {
        Member member = switch (type) {
            case "STUDENT" -> new Student("X", "Name");
            case "STAFF" -> new Staff("X", "Name");
            default -> new Faculty("X", "Name");
        };
        assertEquals(maxItems, member.getMaxItems());
        assertEquals(loanPeriod, member.getLoanPeriod());
    }
}
