/** Thrown when a command line is malformed: unknown command, wrong field count, bad number, and so on. */
public class InvalidInputException extends LibraryException {
    public InvalidInputException(String message) {
        super(message);
    }
}
