import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ItemTest {
    @Test
    void bookIsBorrowableAndFinesTwoRupeesPerDay() {
        Book book = new Book("B1", "Algorithms", "Cormen");
        assertInstanceOf(Borrowable.class, book);
        assertEquals(2, book.getFinePerDay());
        assertEquals("BOOK", book.getTypeName());
    }

    @Test
    void magazineIsBorrowableAndFinesOneRupeePerDay() {
        Magazine magazine = new Magazine("M1", "Resonance", "Oct 2026");
        assertInstanceOf(Borrowable.class, magazine);
        assertEquals(1, magazine.getFinePerDay());
        assertEquals("MAGAZINE", magazine.getTypeName());
    }

    @Test
    void referenceBookIsNotBorrowable() {
        LibraryItem reference = new ReferenceBook("R1", "Mahabharata", "Vyasa");
        assertFalse(reference instanceof Borrowable);
        assertEquals("REFERENCE", reference.getTypeName());
    }

    @Test
    void titleMatchingIgnoresCaseAndLooksInsideTheTitle() {
        LibraryItem book = new Book("B1", "Introduction to Algorithms", "Cormen");
        assertTrue(book.matchesTitle("ALGO"));
        assertTrue(book.matchesTitle("to al"));
        assertFalse(book.matchesTitle("compilers"));
    }
}
