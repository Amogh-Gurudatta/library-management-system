/** Thrown when a member already holds their maximum number of items. */
public class BorrowLimitExceededException extends LibraryException {
    public BorrowLimitExceededException(String memberId, int max) {
        super(memberId + " has reached the limit of " + max);
    }
}
