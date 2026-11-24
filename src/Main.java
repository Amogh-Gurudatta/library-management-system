/** Step 7 hand test: one of each item and member type, printing their rules. */
public class Main {
    public static void main(String[] args) {
        LibraryItem[] items = {
            new Book("B1", "Introduction to Algorithms", "Cormen"),
            new Magazine("M1", "Resonance", "Oct 2026"),
            new ReferenceBook("R1", "Mahabharata", "Vyasa"),
        };
        for (LibraryItem item : items) {
            String fine = (item instanceof Borrowable b) ? "fine " + b.getFinePerDay() + "/day" : "not borrowable";
            System.out.println(item.getId() + " " + item.getTypeName() + " " + fine);
        }

        Member[] members = {
            new Student("S1", "Ananya Rao"),
            new Staff("T1", "Ravi Kumar"),
            new Faculty("F1", "Meera Iyer"),
        };
        for (Member m : members) {
            System.out.println(m.getId() + " max " + m.getMaxItems() + " period " + m.getLoanPeriod());
        }
    }
}
