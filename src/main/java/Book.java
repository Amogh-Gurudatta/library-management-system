/** A borrowable book. Fine: 2 rupees per day late. */
public class Book extends LibraryItem implements Borrowable {
    private final String author;

    public Book(String id, String title, String author) {
        super(id, title);
        this.author = author;
    }

    public String getAuthor() {
        return author;
    }

    @Override
    public String getTypeName() {
        return "BOOK";
    }

    @Override
    public int getFinePerDay() {
        return 2;
    }
}
