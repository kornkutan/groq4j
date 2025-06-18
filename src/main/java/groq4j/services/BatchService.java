package groq4j.services;

import groq4j.models.batch.BatchRequest;
import groq4j.models.batch.BatchResponse;
import groq4j.models.batch.BatchListResponse;

/**
 * Service interface for interacting with the Groq Batch API.
 * 
 * This service provides methods to create, retrieve, list, and cancel batch processing jobs.
 * Batch processing allows you to send large numbers of API requests asynchronously, which can
 * be more cost-effective and efficient for bulk operations.
 * 
 * The batch API supports the following operations:
 * - Create a new batch from an uploaded file
 * - Retrieve status and details of a specific batch
 * - List all batches with filtering capabilities
 * - Cancel a batch that is in progress
 * 
 * @see <a href="https://console.groq.com/docs/batch">Groq Batch API Documentation</a>
 */
public interface BatchService {
    
    /**
     * Creates a new batch processing job.
     * 
     * This method submits a batch request that references an uploaded file containing
     * multiple API requests in JSONL format. The batch will be processed asynchronously
     * according to the specified completion window.
     * 
     * The input file must contain one JSON object per line, each representing a single
     * API request. Each line should have the format:
     * <pre>{@code
     * {"custom_id": "request-1", "method": "POST", "url": "/v1/chat/completions", "body": {...}}
     * }</pre>
     * 
     * @param request the batch request containing input file ID and processing parameters
     * @return BatchResponse containing the created batch details and status
     * @throws groq4j.exceptions.GroqApiException if the API request fails
     * @throws groq4j.exceptions.GroqAuthenticationException if the API key is invalid
     * @throws groq4j.exceptions.GroqBadRequestException if the request parameters are invalid
     * @throws groq4j.exceptions.GroqNetworkException if there are network connectivity issues
     * @throws IllegalArgumentException if request is null
     * 
     * @see groq4j.builders.BatchRequestBuilder
     * 
     * Example usage:
     * <pre>{@code
     * BatchService service = BatchServiceImpl.create(apiKey);
     * 
     * // Basic form
     * BatchRequest request1 = BatchRequestBuilder.create("file-abc123")
     *     .completionWindow("24h")
     *     .metadata("project", "experiment-1")
     *     .build();
     * 
     * // Convenience form  
     * BatchRequest request2 = BatchRequestBuilder.create("file-def456")
     *     .in24Hours()
     *     .priority("high")
     *     .project("production")
     *     .build();
     * 
     * BatchResponse batch = service.createBatch(request1);
     * System.out.println("Created batch: " + batch.id());
     * }</pre>
     */
    BatchResponse createBatch(BatchRequest request);
    
    /**
     * Retrieves detailed information about a specific batch.
     * 
     * This method returns the current status, progress, and metadata for a batch job.
     * Use this to monitor batch progress and check for completion or errors.
     * 
     * @param batchId the unique identifier of the batch to retrieve
     * @return BatchResponse containing the batch details and current status
     * @throws groq4j.exceptions.GroqApiException if the API request fails
     * @throws groq4j.exceptions.GroqAuthenticationException if the API key is invalid
     * @throws groq4j.exceptions.GroqBadRequestException if the batch ID is not found
     * @throws groq4j.exceptions.GroqNetworkException if there are network connectivity issues
     * @throws IllegalArgumentException if batchId is null or empty
     * 
     * Example usage:
     * <pre>{@code
     * BatchService service = BatchServiceImpl.create(apiKey);
     * BatchResponse batch = service.retrieveBatch("batch_abc123");
     * 
     * System.out.println("Status: " + batch.getFormattedStatus());
     * System.out.println("Progress: " + batch.getCompletionPercentage() + "%");
     * 
     * if (batch.isCompleted() && batch.hasOutput()) {
     *     System.out.println("Output file: " + batch.outputFileId().get());
     * }
     * }</pre>
     */
    BatchResponse retrieveBatch(String batchId);
    
    /**
     * Lists all batch processing jobs.
     * 
     * This method returns a list of all batches associated with your account, including
     * their current status and basic information. The response includes utility methods
     * for filtering batches by status and other criteria.
     * 
     * @return BatchListResponse containing all batches with filtering utilities
     * @throws groq4j.exceptions.GroqApiException if the API request fails
     * @throws groq4j.exceptions.GroqAuthenticationException if the API key is invalid
     * @throws groq4j.exceptions.GroqNetworkException if there are network connectivity issues
     * 
     * @see BatchListResponse#getBatchesByStatus(groq4j.enums.BatchStatus)
     * @see BatchListResponse#getInProgressBatches()
     * @see BatchListResponse#getCompletedBatches()
     * @see BatchListResponse#getFailedBatches()
     * 
     * Example usage:
     * <pre>{@code
     * BatchService service = BatchServiceImpl.create(apiKey);
     * BatchListResponse response = service.listBatches();
     * 
     * System.out.println("Total batches: " + response.getBatchCount());
     * 
     * List<BatchResponse> inProgress = response.getInProgressBatches();
     * System.out.println("In progress: " + inProgress.size());
     * 
     * List<BatchResponse> completed = response.getCompletedBatches();
     * System.out.println("Completed: " + completed.size());
     * 
     * // Check status statistics
     * var stats = response.getStatusStatistics();
     * stats.forEach((status, count) -> 
     *     System.out.println(status + ": " + count));
     * }</pre>
     */
    BatchListResponse listBatches();
    
    /**
     * Cancels a batch processing job that is in progress.
     * 
     * This method attempts to cancel a batch that is currently being processed.
     * Only batches in VALIDATING or IN_PROGRESS status can be cancelled.
     * Once cancelled, the batch cannot be resumed.
     * 
     * @param batchId the unique identifier of the batch to cancel
     * @return BatchResponse containing the updated batch status (typically CANCELLING or CANCELLED)
     * @throws groq4j.exceptions.GroqApiException if the API request fails
     * @throws groq4j.exceptions.GroqAuthenticationException if the API key is invalid
     * @throws groq4j.exceptions.GroqBadRequestException if the batch cannot be cancelled (wrong status)
     * @throws groq4j.exceptions.GroqNetworkException if there are network connectivity issues
     * @throws IllegalArgumentException if batchId is null or empty
     * 
     * Example usage:
     * <pre>{@code
     * BatchService service = BatchServiceImpl.create(apiKey);
     * 
     * // Check if batch can be cancelled
     * BatchResponse batch = service.retrieveBatch("batch_abc123");
     * if (batch.isCancellable()) {
     *     BatchResponse cancelled = service.cancelBatch("batch_abc123");
     *     System.out.println("Batch cancelled: " + cancelled.getFormattedStatus());
     * } else {
     *     System.out.println("Batch cannot be cancelled (status: " + batch.getFormattedStatus() + ")");
     * }
     * }</pre>
     */
    BatchResponse cancelBatch(String batchId);
}