/** Thrown when returning an item that is not currently on loan. */
public class ItemNotOnLoanException extends LibraryException {
    public ItemNotOnLoanException(String itemId) {
        super(itemId + " is not on loan");
    }
}
