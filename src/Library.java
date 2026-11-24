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
