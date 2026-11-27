import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LibraryTest {
    private Library library;

    @BeforeEach
    void setUp() throws LibraryException {
        library = new Library();
        library.addItem(new Book("B1", "Alpha", "A"));
        library.addItem(new Book("B2", "Beta", "A"));
        library.addItem(new Book("B3", "Gamma", "A"));
        library.addItem(new Book("B4", "Delta", "A"));
        library.addItem(new ReferenceBook("R1", "Atlas", "A"));
        library.addMember(new Student("S1", "Sam"));
        library.addMember(new Student("S2", "Sue"));
        library.addMember(new Staff("T1", "Tara"));
    }

    @Test
    void borrowSetsTheDueDayFromTheMembersLoanPeriod() throws LibraryException {
        assertEquals(14, library.borrow("S1", "B1").getDueDay());
        library.advance(5);
        assertEquals(26, library.borrow("T1", "B2").getDueDay());
    }

    @Test
    void borrowRejectsUnknownIdsInTheDocumentedOrder() {
        assertEquals("member X1 not found",
                assertThrows(MemberNotFoundException.class, () -> library.borrow("X1", "B9")).getMessage());
        assertEquals("item B9 not found",
                assertThrows(ItemNotFoundException.class, () -> library.borrow("S1", "B9")).getMessage());
    }

    @Test
    void referenceBooksCannotBeBorrowed() {
        assertEquals("R1 cannot be borrowed",
                assertThrows(ItemNotAvailableException.class, () -> library.borrow("S1", "R1")).getMessage());
    }

    @Test
    void anItemCanBeOnLoanToOnlyOneMember() throws LibraryException {
        library.borrow("S1", "B1");
        assertEquals("B1 is on loan",
                assertThrows(ItemNotAvailableException.class, () -> library.borrow("S2", "B1")).getMessage());
    }

    @Test
    void borrowingStopsAtTheMembersLimit() throws LibraryException {
        library.borrow("S1", "B1");
        library.borrow("S1", "B2");
        library.borrow("S1", "B3");
        assertEquals("S1 has reached the limit of 3",
                assertThrows(BorrowLimitExceededException.class, () -> library.borrow("S1", "B4")).getMessage());
        library.borrow("T1", "B4");
    }

    @Test
    void returningFreesTheSlotAndReportsTheFine() throws LibraryException {
        library.borrow("S1", "B1");
        library.advance(20);
        assertEquals(12, library.returnItem("B1").getFine());
        assertNull(library.findLoan("B1"));
        assertEquals("B1 is not on loan",
                assertThrows(ItemNotOnLoanException.class, () -> library.returnItem("B1")).getMessage());
    }

    @Test
    void duplicateIdsAreRejectedWithinItemsAndWithinMembers() {
        assertThrows(DuplicateIdException.class, () -> library.addItem(new Magazine("B1", "M", "i")));
        assertThrows(DuplicateIdException.class, () -> library.addMember(new Staff("S1", "Other")));
    }

    @Test
    void overdueListsMostDaysLateFirstThenItemId() throws LibraryException {
        library.borrow("S1", "B2");
        library.advance(1);
        library.borrow("S2", "B1");
        library.borrow("S2", "B3");
        library.advance(20);
        List<String> ids = library.overdueLoans().stream().map(l -> l.getItem().getId()).toList();
        assertEquals(List.of("B2", "B1", "B3"), ids);
    }

    @Test
    void searchSortsByTitleThenId() throws LibraryException {
        library.addItem(new Book("B0", "Alpha", "A"));
        List<String> ids = library.search("A").stream().map(LibraryItem::getId).toList();
        assertEquals(List.of("B0", "B1", "R1", "B2", "B4", "B3"), ids);
    }

    @Test
    void reservingRequiresALoanAndRejectsDuplicates() throws LibraryException {
        assertEquals("B1 is available",
                assertThrows(ReservationNotAllowedException.class, () -> library.reserve("S2", "B1")).getMessage());
        library.borrow("S1", "B1");
        assertEquals("S1 already has B1",
                assertThrows(ReservationNotAllowedException.class, () -> library.reserve("S1", "B1")).getMessage());
        library.reserve("S2", "B1");
        assertEquals("S2 already reserved B1",
                assertThrows(ReservationNotAllowedException.class, () -> library.reserve("S2", "B1")).getMessage());
    }

    @Test
    void returnIssuesToTheFirstQueuedMemberBelowTheirLimit() throws LibraryException {
        library.borrow("S1", "B1");
        library.borrow("S2", "B2");
        library.borrow("S2", "B3");
        library.borrow("S2", "B4");
        library.reserve("S2", "B1");
        library.reserve("T1", "B1");

        ReturnResult result = library.returnItem("B1");

        Loan reissued = result.getReissuedLoan();
        assertEquals("T1", reissued.getMember().getId());
        assertEquals(21, reissued.getDueDay());
    }

    @Test
    void aReturnWithNobodyWaitingMakesTheItemAvailable() throws LibraryException {
        library.borrow("S1", "B1");
        assertNull(library.returnItem("B1").getReissuedLoan());
        assertTrue(library.findLoan("B1") == null);
    }
}
