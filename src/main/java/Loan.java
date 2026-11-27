/** One item lent to one member. Knows its own due day and fine rate. */
public class Loan {
    private final LibraryItem item;
    private final Member member;
    private final int borrowDay;
    private final int dueDay;
    private final int finePerDay;

    /** The fine rate is copied from the Borrowable so Loan never needs a cast later. */
    public Loan(LibraryItem item, Borrowable borrowable, Member member, int borrowDay) {
        this.item = item;
        this.member = member;
        this.borrowDay = borrowDay;
        this.dueDay = borrowDay + member.getLoanPeriod();
        this.finePerDay = borrowable.getFinePerDay();
    }

    public LibraryItem getItem() {
        return item;
    }

    public Member getMember() {
        return member;
    }

    public int getBorrowDay() {
        return borrowDay;
    }

    public int getDueDay() {
        return dueDay;
    }

    public int getFinePerDay() {
        return finePerDay;
    }

    /** Days past the due day as of {@code today}; 0 if not yet late. */
    public int daysLate(int today) {
        return Math.max(0, today - dueDay);
    }

    /** Fine owed as of {@code today}: days late times the item's fine rate. */
    public int fine(int today) {
        return daysLate(today) * finePerDay;
    }
}
