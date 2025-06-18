package groq4j.models.batch;

import groq4j.enums.BatchStatus;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Response record for listing batch processing jobs.
 * 
 * Contains a list of batch jobs with utility methods for filtering and analyzing
 * the batches by status, completion state, and other criteria.
 * 
 * @param object Always "list" for list responses
 * @param data List of BatchResponse objects representing the batches
 * @param hasMore Whether there are more batches available (pagination)
 * 
 * @see <a href="https://console.groq.com/docs/batch">Groq Batch API Documentation</a>
 */
public record BatchListResponse(
    String object,
    List<BatchResponse> data,
    boolean hasMore
) {
    public BatchListResponse {
        if (object == null || object.trim().isEmpty()) {
            throw new IllegalArgumentException("object cannot be null or empty");
        }
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }
    }
    
    /**
     * Gets the total number of batches in this response.
     * 
     * @return number of batches
     */
    public int getBatchCount() {
        return data.size();
    }
    
    /**
     * Checks if the batch list is empty.
     * 
     * @return true if no batches are present
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }
    
    /**
     * Gets all batches with the specified status.
     * 
     * @param status the status to filter by
     * @return list of batches with the specified status
     */
    public List<BatchResponse> getBatchesByStatus(BatchStatus status) {
        return data.stream()
                  .filter(batch -> batch.status() == status)
                  .collect(Collectors.toList());
    }
    
    /**
     * Gets all batches that are currently in progress.
     * 
     * @return list of batches with IN_PROGRESS status
     */
    public List<BatchResponse> getInProgressBatches() {
        return getBatchesByStatus(BatchStatus.IN_PROGRESS);
    }
    
    /**
     * Gets all batches that have completed successfully.
     * 
     * @return list of batches with COMPLETED status
     */
    public List<BatchResponse> getCompletedBatches() {
        return getBatchesByStatus(BatchStatus.COMPLETED);
    }
    
    /**
     * Gets all batches that have failed.
     * 
     * @return list of batches with FAILED status
     */
    public List<BatchResponse> getFailedBatches() {
        return getBatchesByStatus(BatchStatus.FAILED);
    }
    
    /**
     * Gets all batches that have been cancelled.
     * 
     * @return list of batches with CANCELLED status
     */
    public List<BatchResponse> getCancelledBatches() {
        return getBatchesByStatus(BatchStatus.CANCELLED);
    }
    
    /**
     * Gets all batches that have expired.
     * 
     * @return list of batches with EXPIRED status
     */
    public List<BatchResponse> getExpiredBatches() {
        return getBatchesByStatus(BatchStatus.EXPIRED);
    }
    
    /**
     * Gets all batches that are in terminal states (completed, failed, cancelled, expired).
     * 
     * @return list of batches in terminal states
     */
    public List<BatchResponse> getTerminalBatches() {
        return data.stream()
                  .filter(BatchResponse::isTerminal)
                  .collect(Collectors.toList());
    }
    
    /**
     * Gets all batches that can be cancelled.
     * 
     * @return list of cancellable batches
     */
    public List<BatchResponse> getCancellableBatches() {
        return data.stream()
                  .filter(BatchResponse::isCancellable)
                  .collect(Collectors.toList());
    }
    
    /**
     * Gets all batches that have output data available.
     * 
     * @return list of batches with output files
     */
    public List<BatchResponse> getBatchesWithOutput() {
        return data.stream()
                  .filter(BatchResponse::hasOutput)
                  .collect(Collectors.toList());
    }
    
    /**
     * Gets all batches that have error data available.
     * 
     * @return list of batches with error files
     */
    public List<BatchResponse> getBatchesWithErrors() {
        return data.stream()
                  .filter(BatchResponse::hasErrors)
                  .collect(Collectors.toList());
    }
    
    /**
     * Gets all batch IDs as a list.
     * 
     * @return list of batch IDs
     */
    public List<String> getBatchIds() {
        return data.stream()
                  .map(BatchResponse::id)
                  .collect(Collectors.toList());
    }
    
    /**
     * Finds a batch by its ID.
     * 
     * @param batchId the ID to search for
     * @return the batch with the specified ID
     * @throws IllegalArgumentException if the batch is not found
     */
    public BatchResponse findBatch(String batchId) {
        return data.stream()
                  .filter(batch -> batch.id().equals(batchId))
                  .findFirst()
                  .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));
    }
    
    /**
     * Checks if a batch with the specified ID exists in this list.
     * 
     * @param batchId the ID to check for
     * @return true if the batch exists, false otherwise
     */
    public boolean hasBatch(String batchId) {
        return data.stream()
                  .anyMatch(batch -> batch.id().equals(batchId));
    }
    
    /**
     * Gets statistics about batch statuses.
     * 
     * @return a map of status to count
     */
    public java.util.Map<BatchStatus, Long> getStatusStatistics() {
        return data.stream()
                  .collect(Collectors.groupingBy(
                      BatchResponse::status,
                      Collectors.counting()
                  ));
    }
    
    /**
     * Calculates the overall completion rate across all batches.
     * 
     * @return average completion percentage (0.0 to 100.0)
     */
    public double getOverallCompletionRate() {
        if (data.isEmpty()) {
            return 0.0;
        }
        
        return data.stream()
                  .mapToDouble(BatchResponse::getCompletionPercentage)
                  .average()
                  .orElse(0.0);
    }
    
    /**
     * Gets the most recently created batch.
     * 
     * @return the batch with the latest createdAt timestamp
     * @throws IllegalStateException if no batches are present
     */
    public BatchResponse getMostRecentBatch() {
        return data.stream()
                  .max((a, b) -> Long.compare(a.createdAt(), b.createdAt()))
                  .orElseThrow(() -> new IllegalStateException("No batches available"));
    }
    
    /**
     * Gets the oldest batch.
     * 
     * @return the batch with the earliest createdAt timestamp
     * @throws IllegalStateException if no batches are present
     */
    public BatchResponse getOldestBatch() {
        return data.stream()
                  .min((a, b) -> Long.compare(a.createdAt(), b.createdAt()))
                  .orElseThrow(() -> new IllegalStateException("No batches available"));
    }
    
    /**
     * Creates a simple factory method for testing purposes.
     * 
     * @param batches list of batches
     * @return BatchListResponse with default values
     */
    public static BatchListResponse of(List<BatchResponse> batches) {
        return new BatchListResponse("list", batches, false);
    }
    
    /**
     * Creates a factory method with pagination support.
     * 
     * @param batches list of batches
     * @param hasMore whether there are more batches available
     * @return BatchListResponse with pagination info
     */
    public static BatchListResponse of(List<BatchResponse> batches, boolean hasMore) {
        return new BatchListResponse("list", batches, hasMore);
    }
}