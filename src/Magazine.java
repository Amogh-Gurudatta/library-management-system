/** A borrowable magazine issue. Fine: 1 rupee per day late. */
public class Magazine extends LibraryItem implements Borrowable {
    private final String issue;

    public Magazine(String id, String title, String issue) {
        super(id, title);
        this.issue = issue;
    }

    public String getIssue() {
        return issue;
    }

    @Override
    public String getTypeName() {
        return "MAGAZINE";
    }

    @Override
    public int getFinePerDay() {
        return 1;
    }
}
