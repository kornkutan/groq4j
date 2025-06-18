package groq4j.models.batch;

import java.util.Map;
import java.util.Optional;

/**
 * Request record for creating a batch processing job.
 * 
 * Represents a request to create a batch of API calls that will be processed asynchronously.
 * All batch requests must specify an input file ID and endpoint, with optional completion window
 * and metadata.
 * 
 * @param inputFileId The ID of an uploaded file containing the batch requests in JSONL format
 * @param endpoint The API endpoint to use for all requests in the batch (currently only "/v1/chat/completions")
 * @param completionWindow The time frame within which the batch should be processed (e.g., "24h")
 * @param metadata Optional metadata to attach to the batch for organization and tracking
 * 
 * @see <a href="https://console.groq.com/docs/batch">Groq Batch API Documentation</a>
 */
public record BatchRequest(
    String inputFileId,
    String endpoint,
    String completionWindow,
    Optional<Map<String, String>> metadata
) {
    public BatchRequest {
        if (inputFileId == null || inputFileId.trim().isEmpty()) {
            throw new IllegalArgumentException("inputFileId cannot be null or empty");
        }
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("endpoint cannot be null or empty");
        }
        if (!"/v1/chat/completions".equals(endpoint)) {
            throw new IllegalArgumentException("Only '/v1/chat/completions' endpoint is currently supported");
        }
        if (completionWindow == null || completionWindow.trim().isEmpty()) {
            throw new IllegalArgumentException("completionWindow cannot be null or empty");
        }
        if (!completionWindow.matches("^\\d+[hd]$")) {
            throw new IllegalArgumentException("completionWindow must be in format like '24h' or '7d'");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("metadata cannot be null (use Optional.empty() for no metadata)");
        }
    }
    
    /**
     * Creates a simple batch request with just the required fields.
     * 
     * @param inputFileId The ID of the uploaded input file
     * @param completionWindow The completion time window (e.g., "24h")
     * @return BatchRequest with default endpoint and no metadata
     */
    public static BatchRequest simple(String inputFileId, String completionWindow) {
        return new BatchRequest(
            inputFileId,
            "/v1/chat/completions",
            completionWindow,
            Optional.empty()
        );
    }
    
    /**
     * Creates a batch request with metadata.
     * 
     * @param inputFileId The ID of the uploaded input file
     * @param completionWindow The completion time window (e.g., "24h")
     * @param metadata Custom metadata for the batch
     * @return BatchRequest with metadata
     */
    public static BatchRequest withMetadata(String inputFileId, String completionWindow, Map<String, String> metadata) {
        return new BatchRequest(
            inputFileId,
            "/v1/chat/completions",
            completionWindow,
            Optional.of(metadata)
        );
    }
    
    /**
     * Checks if this batch request has metadata.
     * 
     * @return true if metadata is present, false otherwise
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
     * Validates the completion window format.
     * 
     * @return true if the completion window is valid
     */
    public boolean isValidCompletionWindow() {
        return completionWindow.matches("^\\d+[hd]$");
    }
    
    /**
     * Extracts the numeric value from the completion window.
     * 
     * @return numeric value (e.g., 24 for "24h")
     */
    public int getCompletionWindowValue() {
        return Integer.parseInt(completionWindow.replaceAll("[hd]", ""));
    }
    
    /**
     * Gets the time unit from the completion window.
     * 
     * @return "h" for hours or "d" for days
     */
    public String getCompletionWindowUnit() {
        return completionWindow.substring(completionWindow.length() - 1);
    }
}