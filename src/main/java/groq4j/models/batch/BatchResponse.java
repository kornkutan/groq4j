package groq4j.models.batch;

import groq4j.enums.BatchStatus;
import groq4j.models.common.RequestCounts;

import java.util.Map;
import java.util.Optional;

/**
 * Response record representing a batch processing job.
 * 
 * Contains all information about a batch job including its current status, progress,
 * and associated file IDs for input and output data.
 * 
 * @param id Unique identifier for the batch
 * @param object Always "batch" for batch objects
 * @param endpoint The API endpoint used for this batch
 * @param errors Optional error information if the batch failed
 * @param inputFileId The ID of the input file containing the batch requests
 * @param completionWindow The time frame for batch processing
 * @param status Current status of the batch processing
 * @param outputFileId Optional ID of the output file (available when completed)
 * @param errorFileId Optional ID of the error file (available if there were errors)
 * @param createdAt Unix timestamp when the batch was created
 * @param inProgressAt Optional timestamp when batch processing started
 * @param expiresAt Unix timestamp when the batch will expire
 * @param finalizingAt Optional timestamp when batch entered finalizing state
 * @param completedAt Optional timestamp when batch completed
 * @param failedAt Optional timestamp when batch failed
 * @param expiredAt Optional timestamp when batch expired
 * @param cancellingAt Optional timestamp when batch cancellation started
 * @param cancelledAt Optional timestamp when batch was cancelled
 * @param requestCounts Statistics about the requests in this batch
 * @param metadata Optional metadata associated with the batch
 * 
 * @see <a href="https://console.groq.com/docs/batch">Groq Batch API Documentation</a>
 */
public record BatchResponse(
    String id,
    String object,
    String endpoint,
    Optional<Object> errors,
    String inputFileId,
    String completionWindow,
    BatchStatus status,
    Optional<String> outputFileId,
    Optional<String> errorFileId,
    long createdAt,
    Optional<Long> inProgressAt,
    long expiresAt,
    Optional<Long> finalizingAt,
    Optional<Long> completedAt,
    Optional<Long> failedAt,
    Optional<Long> expiredAt,
    Optional<Long> cancellingAt,
    Optional<Long> cancelledAt,
    RequestCounts requestCounts,
    Optional<Map<String, String>> metadata
) {
    public BatchResponse {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("id cannot be null or empty");
        }
        if (object == null || object.trim().isEmpty()) {
            throw new IllegalArgumentException("object cannot be null or empty");
        }
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("endpoint cannot be null or empty");
        }
        if (inputFileId == null || inputFileId.trim().isEmpty()) {
            throw new IllegalArgumentException("inputFileId cannot be null or empty");
        }
        if (completionWindow == null || completionWindow.trim().isEmpty()) {
            throw new IllegalArgumentException("completionWindow cannot be null or empty");
        }
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        if (requestCounts == null) {
            throw new IllegalArgumentException("requestCounts cannot be null");
        }
        if (createdAt < 0) {
            throw new IllegalArgumentException("createdAt cannot be negative");
        }
        if (expiresAt < 0) {
            throw new IllegalArgumentException("expiresAt cannot be negative");
        }
    }
    
    /**
     * Checks if the batch is currently in progress.
     * 
     * @return true if status is IN_PROGRESS
     */
    public boolean isInProgress() {
        return status == BatchStatus.IN_PROGRESS;
    }
    
    /**
     * Checks if the batch has completed successfully.
     * 
     * @return true if status is COMPLETED
     */
    public boolean isCompleted() {
        return status == BatchStatus.COMPLETED;
    }
    
    /**
     * Checks if the batch has failed.
     * 
     * @return true if status is FAILED
     */
    public boolean isFailed() {
        return status == BatchStatus.FAILED;
    }
    
    /**
     * Checks if the batch has been cancelled.
     * 
     * @return true if status is CANCELLED
     */
    public boolean isCancelled() {
        return status == BatchStatus.CANCELLED;
    }
    
    /**
     * Checks if the batch has expired.
     * 
     * @return true if status is EXPIRED
     */
    public boolean isExpired() {
        return status == BatchStatus.EXPIRED;
    }
    
    /**
     * Checks if the batch is in a terminal state (completed, failed, cancelled, or expired).
     * 
     * @return true if the batch is in a terminal state
     */
    public boolean isTerminal() {
        return isCompleted() || isFailed() || isCancelled() || isExpired();
    }
    
    /**
     * Checks if the batch can be cancelled.
     * 
     * @return true if the batch is in a cancellable state
     */
    public boolean isCancellable() {
        return status == BatchStatus.VALIDATING || status == BatchStatus.IN_PROGRESS;
    }
    
    /**
     * Checks if the batch has output data available.
     * 
     * @return true if outputFileId is present
     */
    public boolean hasOutput() {
        return outputFileId.isPresent();
    }
    
    /**
     * Checks if the batch has error data available.
     * 
     * @return true if errorFileId is present
     */
    public boolean hasErrors() {
        return errorFileId.isPresent();
    }
    
    /**
     * Checks if the batch has metadata.
     * 
     * @return true if metadata is present and not empty
     */
    public boolean hasMetadata() {
        return metadata.isPresent() && !metadata.get().isEmpty();
    }
    
    /**
     * Gets the metadata map, or empty map if no metadata.
     * 
     * @return metadata map or empty map
     */
    public Map<String, String> getMetadataOrEmpty() {
        return metadata.orElse(Map.of());
    }
    
    /**
     * Calculates the completion percentage based on request counts.
     * 
     * @return completion percentage (0.0 to 100.0)
     */
    public double getCompletionPercentage() {
        int total = requestCounts.total();
        if (total == 0) {
            return 0.0;
        }
        int completed = requestCounts.completed();
        return (completed * 100.0) / total;
    }
    
    /**
     * Gets the duration in milliseconds since the batch was created.
     * 
     * @return duration in milliseconds
     */
    public long getDurationSinceCreated() {
        return System.currentTimeMillis() - (createdAt * 1000);
    }
    
    /**
     * Gets the time remaining until expiration in milliseconds.
     * 
     * @return time remaining in milliseconds, or 0 if already expired
     */
    public long getTimeUntilExpiration() {
        long expirationTime = expiresAt * 1000;
        long currentTime = System.currentTimeMillis();
        return Math.max(0, expirationTime - currentTime);
    }
    
    /**
     * Formats the batch status as a human-readable string.
     * 
     * @return formatted status string
     */
    public String getFormattedStatus() {
        return switch (status) {
            case VALIDATING -> "Validating";
            case IN_PROGRESS -> "In Progress";
            case FINALIZING -> "Finalizing";
            case COMPLETED -> "Completed";
            case FAILED -> "Failed";
            case EXPIRED -> "Expired";
            case CANCELLING -> "Cancelling";
            case CANCELLED -> "Cancelled";
        };
    }
    
    /**
     * Creates a simple factory method for testing purposes.
     * 
     * @param id batch ID
     * @param status batch status
     * @param inputFileId input file ID
     * @param requestCounts request statistics
     * @return simplified BatchResponse
     */
    public static BatchResponse simple(String id, BatchStatus status, String inputFileId, RequestCounts requestCounts) {
        long now = System.currentTimeMillis() / 1000;
        return new BatchResponse(
            id,
            "batch",
            "/v1/chat/completions",
            Optional.empty(),
            inputFileId,
            "24h",
            status,
            Optional.empty(),
            Optional.empty(),
            now,
            Optional.empty(),
            now + 86400, // 24 hours later
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            requestCounts,
            Optional.empty()
        );
    }
}