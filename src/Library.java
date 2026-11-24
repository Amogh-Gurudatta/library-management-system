import java.util.HashMap;
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
}
