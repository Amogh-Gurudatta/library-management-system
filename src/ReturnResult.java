/** What happened when an item came back: the fine, and the new loan if it went straight to a waiting member. */
public class ReturnResult {
    private final int fine;
    private final Loan reissuedLoan;

    public ReturnResult(int fine, Loan reissuedLoan) {
        this.fine = fine;
        this.reissuedLoan = reissuedLoan;
    }

    public int getFine() {
        return fine;
    }

    /** The loan to the next member in the reservation queue, or null if nobody was waiting. */
    public Loan getReissuedLoan() {
        return reissuedLoan;
    }
}
