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
    /** Reservation queues keyed by item ID, in the order members reserved. */
    private final Map<String, List<Member>> reservations = new HashMap<>();
    private int today = 0;

    public int getToday() {
        return today;
    }

    /** Adds an item to the catalogue. Item IDs must be unique among items. */
    public void addItem(LibraryItem item) throws DuplicateIdException {
        if (items.containsKey(item.getId())) {
            throw new DuplicateIdException(item.getId());
        }
        items.put(item.getId(), item);
    }

    /** Registers a member. Member IDs must be unique among members. */
    public void addMember(Member member) throws DuplicateIdException {
        if (members.containsKey(member.getId())) {
            throw new DuplicateIdException(member.getId());
        }
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
        cancelReservation(member, itemId);
        return lend(item, borrowable, member);
    }

    /**
     * Takes an item back and computes the fine. If members reserved it, the item goes straight to the
     * first one in the queue who is below their limit; members at their limit are skipped and keep their place.
     */
    public ReturnResult returnItem(String itemId) throws LibraryException {
        LibraryItem item = findItem(itemId);
        Loan loan = loans.remove(itemId);
        if (loan == null) {
            throw new ItemNotOnLoanException(itemId);
        }
        return new ReturnResult(loan.fine(today), issueToNextInQueue(item));
    }

    /**
     * Adds a member to an item's reservation queue. Checks, in order: member exists, item exists,
     * item is borrowable, item is on loan, member does not hold it, member has not already reserved it.
     */
    public void reserve(String memberId, String itemId) throws LibraryException {
        Member member = findMember(memberId);
        LibraryItem item = findItem(itemId);
        if (!(item instanceof Borrowable)) {
            throw ItemNotAvailableException.notBorrowable(itemId);
        }
        Loan loan = loans.get(itemId);
        if (loan == null) {
            throw ReservationNotAllowedException.available(itemId);
        }
        if (loan.getMember() == member) {
            throw ReservationNotAllowedException.alreadyHolds(memberId, itemId);
        }
        List<Member> queue = reservations.computeIfAbsent(itemId, id -> new ArrayList<>());
        if (queue.contains(member)) {
            throw ReservationNotAllowedException.alreadyReserved(memberId, itemId);
        }
        queue.add(member);
    }

    /** Creates and records a loan starting today. Used by BORROW and by the reservation queue. */
    private Loan lend(LibraryItem item, Borrowable borrowable, Member member) {
        Loan loan = new Loan(item, borrowable, member, today);
        loans.put(item.getId(), loan);
        return loan;
    }

    /** Drops a member from an item's queue, e.g. once they borrow it directly. */
    private void cancelReservation(Member member, String itemId) {
        List<Member> queue = reservations.get(itemId);
        if (queue != null) {
            queue.remove(member);
            if (queue.isEmpty()) {
                reservations.remove(itemId);
            }
        }
    }

    /** Lends a just-returned item to the first queued member below their limit. Returns null if nobody qualifies. */
    private Loan issueToNextInQueue(LibraryItem item) {
        List<Member> queue = reservations.get(item.getId());
        if (queue == null || !(item instanceof Borrowable borrowable)) {
            return null;
        }
        for (Member member : queue) {
            if (countLoans(member) < member.getMaxItems()) {
                cancelReservation(member, item.getId());
                return lend(item, borrowable, member);
            }
        }
        return null;
    }

    /** Moves simulated time forward and returns the new day. */
    public int advance(int days) {
        today += days;
        return today;
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

    /** Items whose title contains the text (case-insensitive), sorted by title, then ID. */
    public List<LibraryItem> search(String text) {
        List<LibraryItem> result = new ArrayList<>();
        for (LibraryItem item : items.values()) {
            if (item.matchesTitle(text)) {
                result.add(item);
            }
        }
        result.sort(Comparator.comparing(LibraryItem::getTitle).thenComparing(LibraryItem::getId));
        return result;
    }

    /** A member's current loans, sorted by due day, then item ID. */
    public List<Loan> loansOf(String memberId) throws MemberNotFoundException {
        Member member = findMember(memberId);
        List<Loan> result = new ArrayList<>();
        for (Loan loan : loans.values()) {
            if (loan.getMember() == member) {
                result.add(loan);
            }
        }
        result.sort(Comparator.comparingInt(Loan::getDueDay).thenComparing(Library::itemIdOf));
        return result;
    }

    /** Every loan past its due day, most days late first, then item ID. */
    public List<Loan> overdueLoans() {
        List<Loan> result = new ArrayList<>();
        for (Loan loan : loans.values()) {
            if (loan.daysLate(today) > 0) {
                result.add(loan);
            }
        }
        result.sort(Comparator.comparingInt((Loan loan) -> loan.daysLate(today)).reversed()
                .thenComparing(Library::itemIdOf));
        return result;
    }

    private static String itemIdOf(Loan loan) {
        return loan.getItem().getId();
    }

    /** Every item, sorted by ID. */
    public List<LibraryItem> inventory() {
        List<LibraryItem> result = new ArrayList<>(items.values());
        result.sort(Comparator.comparing(LibraryItem::getId));
        return result;
    }
}
