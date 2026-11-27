/** Thrown when an ADD_ command reuses an existing item or member ID. */
public class DuplicateIdException extends LibraryException {
    public DuplicateIdException(String id) {
        super("duplicate id " + id);
    }
}
