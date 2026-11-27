/** Implemented by items that may be lent out. The item decides its own fine rate. */
public interface Borrowable {
    /** Fine charged for each day the item is returned late, in rupees. */
    int getFinePerDay();
}
