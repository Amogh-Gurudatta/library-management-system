import java.util.Locale;

/** Base class for everything in the catalogue. */
public abstract class LibraryItem {
    private final String id;
    private final String title;

    protected LibraryItem(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    /** True if the title contains {@code text}, ignoring case. Used by SEARCH. */
    public boolean matchesTitle(String text) {
        return title.toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT));
    }

    /** The TYPE column in reports: BOOK, MAGAZINE or REFERENCE. */
    public abstract String getTypeName();
}
