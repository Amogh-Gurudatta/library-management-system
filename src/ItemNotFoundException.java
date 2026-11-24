/** Thrown when an item ID is not in the catalogue. */
public class ItemNotFoundException extends LibraryException {
    public ItemNotFoundException(String itemId) {
        super("item " + itemId + " not found");
    }
}
