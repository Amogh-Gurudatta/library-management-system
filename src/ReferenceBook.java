/** A reference book. It does not implement Borrowable, so it can never be lent out. */
public class ReferenceBook extends LibraryItem {
    private final String author;

    public ReferenceBook(String id, String title, String author) {
        super(id, title);
        this.author = author;
    }

    public String getAuthor() {
        return author;
    }

    @Override
    public String getTypeName() {
        return "REFERENCE";
    }
}
