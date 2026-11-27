import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LoanTest {
    private final Book book = new Book("B1", "Algorithms", "Cormen");
    private final Student student = new Student("S1", "Ananya");

    @Test
    void dueDayIsBorrowDayPlusLoanPeriod() {
        assertEquals(14, new Loan(book, book, student, 0).getDueDay());
        assertEquals(24, new Loan(book, book, student, 10).getDueDay());
    }

    @Test
    void noFineOnOrBeforeTheDueDay() {
        Loan loan = new Loan(book, book, student, 0);
        assertEquals(0, loan.fine(0));
        assertEquals(0, loan.fine(13));
        assertEquals(0, loan.fine(14));
        assertEquals(0, loan.daysLate(14));
    }

    @Test
    void fineIsDaysLateTimesTheItemsRate() {
        Loan bookLoan = new Loan(book, book, student, 0);
        assertEquals(1, bookLoan.daysLate(15));
        assertEquals(12, bookLoan.fine(20));

        Magazine magazine = new Magazine("M1", "Resonance", "Oct 2026");
        assertEquals(6, new Loan(magazine, magazine, student, 0).fine(20));
    }
}
