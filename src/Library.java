import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Holds the catalogue, the members, the active loans and today's day. Every rule is checked here. */
public class Library {
    private final Map<String, LibraryItem> items = new HashMap<>();
    private final Map<String, Member> members = new HashMap<>();
    /** Active loans keyed by item ID: an item can be on loan to at most one member. */
    private final Map<String, Loan> loans = new HashMap<>();
    private int today = 0;

    public int getToday() {
        return today;
    }

    public void addItem(LibraryItem item) {
        items.put(item.getId(), item);
    }

    public void addMember(Member member) {
        members.put(member.getId(), member);
    }

    /** Looks up an item, or throws if the ID is unknown. */
    public LibraryItem findItem(String itemId) throws ItemNotFoundException {
        LibraryItem item = items.get(itemId);
        if (item == null) {
            throw new ItemNotFoundException(itemId);
        }
        return item;
    }

    /** Looks up a member, or throws if the ID is unknown. */
    public Member findMember(String memberId) throws MemberNotFoundException {
        Member member = members.get(memberId);
        if (member == null) {
            throw new MemberNotFoundException(memberId);
        }
        return member;
    }

    /**
     * Lends an item to a member. Checks, in order: member exists, item exists,
     * item is borrowable, item is not on loan, member is below their limit.
     */
    public Loan borrow(String memberId, String itemId) throws LibraryException {
        Member member = findMember(memberId);
        LibraryItem item = findItem(itemId);
        if (!(item instanceof Borrowable borrowable)) {
            throw ItemNotAvailableException.notBorrowable(itemId);
        }
        if (loans.containsKey(itemId)) {
            throw ItemNotAvailableException.onLoan(itemId);
        }
        if (countLoans(member) >= member.getMaxItems()) {
            throw new BorrowLimitExceededException(memberId, member.getMaxItems());
        }
        Loan loan = new Loan(item, borrowable, member, today);
        loans.put(itemId, loan);
        return loan;
    }

    /** Takes an item back and returns the loan that just ended. */
    public Loan returnItem(String itemId) throws LibraryException {
        findItem(itemId);
        Loan loan = loans.remove(itemId);
        if (loan == null) {
            throw new ItemNotOnLoanException(itemId);
        }
        return loan;
    }

    /** How many items a member currently holds, counted from the active loans. */
    private int countLoans(Member member) {
        int count = 0;
        for (Loan loan : loans.values()) {
            if (loan.getMember() == member) {
                count++;
            }
        }
        return count;
    }

    /** The active loan for an item, or null if the item is not on loan. */
    public Loan findLoan(String itemId) {
        return loans.get(itemId);
    }

    /** Every item, sorted by ID. */
    public List<LibraryItem> inventory() {
        List<LibraryItem> result = new ArrayList<>(items.values());
        result.sort(Comparator.comparing(LibraryItem::getId));
        return result;
    }
}
