package groq4j.enums;

/**
 * Enum representing the various states of a batch processing job.
 * 
 * Batch jobs follow a typical lifecycle:
 * 1. VALIDATING → FAILED (if validation fails) OR IN_PROGRESS (if validation succeeds)
 * 2. IN_PROGRESS → FINALIZING → COMPLETED (normal flow)
 * 3. Any state → CANCELLING → CANCELLED (if user cancels)
 * 4. Any state → EXPIRED (if processing window exceeded)
 * 
 * @see <a href="https://console.groq.com/docs/batch">Groq Batch API Documentation</a>
 */
public enum BatchStatus {
    
    /** Batch file is being validated before the batch processing begins */
    VALIDATING("validating"),
    
    /** Batch file has failed the validation process */
    FAILED("failed"),
    
    /** Batch file was successfully validated and the batch is currently being run */
    IN_PROGRESS("in_progress"),
    
    /** Batch has completed and the results are being prepared */
    FINALIZING("finalizing"),
    
    /** Batch has been completed and the results are ready */
    COMPLETED("completed"),
    
    /** Batch was not able to be completed within the processing window */
    EXPIRED("expired"),
    
    /** Batch is being cancelled (may take up to 10 minutes) */
    CANCELLING("cancelling"),
    
    /** Batch was cancelled */
    CANCELLED("cancelled");

    private final String value;

    BatchStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static BatchStatus fromValue(String value) {
        for (BatchStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown BatchStatus: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}