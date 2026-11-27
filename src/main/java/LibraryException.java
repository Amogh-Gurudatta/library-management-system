/** Parent of every error the program reports. Caught once, in the command loop. */
public abstract class LibraryException extends Exception {
    protected LibraryException(String message) {
        super(message);
    }
}
