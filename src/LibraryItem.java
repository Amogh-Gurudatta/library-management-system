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

    /** The TYPE column in reports: BOOK, MAGAZINE or REFERENCE. */
    public abstract String getTypeName();
}
