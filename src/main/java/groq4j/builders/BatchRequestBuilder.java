package groq4j.builders;

import groq4j.models.batch.BatchRequest;
import groq4j.utils.ValidationUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Builder for creating BatchRequest objects with a fluent API.
 * 
 * This builder provides a convenient way to construct batch processing requests
 * with validation and sensible defaults. It supports method chaining for easy
 * configuration of all batch parameters.
 * 
 * Example usage:
 * <pre>{@code
 * // Simple batch request
 * BatchRequest request = BatchRequestBuilder.create("file-123")
 *     .completionWindow("24h")
 *     .build();
 * 
 * // Batch request with metadata
 * BatchRequest request = BatchRequestBuilder.create("file-123")
 *     .completionWindow("48h")
 *     .metadata("project", "experimental")
 *     .metadata("priority", "high")
 *     .build();
 * }</pre>
 * 
 * @see BatchRequest
 */
public class BatchRequestBuilder {
    
    private String inputFileId;
    private String endpoint = "/v1/chat/completions"; // Default endpoint
    private String completionWindow;
    private Map<String, String> metadata = new HashMap<>();
    
    private BatchRequestBuilder() {
        // Private constructor - use create() method
    }
    
    /**
     * Creates a new BatchRequestBuilder with the specified input file ID.
     * 
     * @param inputFileId the ID of the uploaded input file containing batch requests
     * @return new BatchRequestBuilder instance
     * @throws IllegalArgumentException if inputFileId is null or empty
     */
    public static BatchRequestBuilder create(String inputFileId) {
        ValidationUtils.requireNonEmpty(inputFileId, "inputFileId");
        BatchRequestBuilder builder = new BatchRequestBuilder();
        builder.inputFileId = inputFileId;
        return builder;
    }
    
    /**
     * Sets the API endpoint for the batch processing.
     * 
     * Note: Currently only "/v1/chat/completions" is supported by the Groq API.
     * 
     * @param endpoint the API endpoint to use
     * @return this builder for method chaining
     * @throws IllegalArgumentException if endpoint is null, empty, or unsupported
     */
    public BatchRequestBuilder endpoint(String endpoint) {
        ValidationUtils.requireNonEmpty(endpoint, "endpoint");
        ValidationUtils.validateBatchEndpoint(endpoint);
        this.endpoint = endpoint;
        return this;
    }
    
    /**
     * Sets the completion window for the batch processing.
     * 
     * The completion window specifies the time frame within which the batch
     * should be processed. Valid formats are:
     * - "24h" for 24 hours
     * - "7d" for 7 days
     * - Any number followed by 'h' (hours) or 'd' (days)
     * 
     * @param completionWindow the completion window (e.g., "24h", "7d")
     * @return this builder for method chaining
     * @throws IllegalArgumentException if completionWindow is invalid
     */
    public BatchRequestBuilder completionWindow(String completionWindow) {
        ValidationUtils.requireNonEmpty(completionWindow, "completionWindow");
        ValidationUtils.validateBatchCompletionWindow(completionWindow);
        this.completionWindow = completionWindow;
        return this;
    }
    
    /**
     * Adds a metadata key-value pair to the batch request.
     * 
     * Metadata can be used to organize and track batches. Multiple metadata
     * entries can be added by calling this method multiple times.
     * 
     * @param key the metadata key
     * @param value the metadata value
     * @return this builder for method chaining
     * @throws IllegalArgumentException if key or value is null or empty
     */
    public BatchRequestBuilder metadata(String key, String value) {
        ValidationUtils.requireNonEmpty(key, "metadata key");
        ValidationUtils.requireNonEmpty(value, "metadata value");
        this.metadata.put(key, value);
        return this;
    }
    
    /**
     * Sets all metadata at once, replacing any previously set metadata.
     * 
     * @param metadata a map of metadata key-value pairs
     * @return this builder for method chaining
     * @throws IllegalArgumentException if metadata is null
     */
    public BatchRequestBuilder metadata(Map<String, String> metadata) {
        ValidationUtils.requireNonNull(metadata, "metadata");
        this.metadata = new HashMap<>(metadata);
        return this;
    }
    
    /**
     * Clears all metadata from the batch request.
     * 
     * @return this builder for method chaining
     */
    public BatchRequestBuilder clearMetadata() {
        this.metadata.clear();
        return this;
    }
    
    /**
     * Sets the completion window to 24 hours (convenience method).
     * 
     * @return this builder for method chaining
     */
    public BatchRequestBuilder in24Hours() {
        return completionWindow("24h");
    }
    
    /**
     * Sets the completion window to 48 hours (convenience method).
     * 
     * @return this builder for method chaining
     */
    public BatchRequestBuilder in48Hours() {
        return completionWindow("48h");
    }
    
    /**
     * Sets the completion window to 7 days (convenience method).
     * 
     * @return this builder for method chaining
     */
    public BatchRequestBuilder in7Days() {
        return completionWindow("7d");
    }
    
    /**
     * Sets a "priority" metadata field (convenience method).
     * 
     * @param priority the priority level (e.g., "high", "medium", "low")
     * @return this builder for method chaining
     */
    public BatchRequestBuilder priority(String priority) {
        return metadata("priority", priority);
    }
    
    /**
     * Sets a "project" metadata field (convenience method).
     * 
     * @param project the project name or identifier
     * @return this builder for method chaining
     */
    public BatchRequestBuilder project(String project) {
        return metadata("project", project);
    }
    
    /**
     * Sets a "description" metadata field (convenience method).
     * 
     * @param description a description of the batch
     * @return this builder for method chaining
     */
    public BatchRequestBuilder description(String description) {
        return metadata("description", description);
    }
    
    /**
     * Builds and validates the BatchRequest.
     * 
     * @return a new BatchRequest instance
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    public BatchRequest build() {
        if (completionWindow == null) {
            throw new IllegalArgumentException("completionWindow is required");
        }
        
        Optional<Map<String, String>> metadataOptional = metadata.isEmpty() 
            ? Optional.empty() 
            : Optional.of(new HashMap<>(metadata));
        
        return new BatchRequest(
            inputFileId,
            endpoint,
            completionWindow,
            metadataOptional
        );
    }
    
    /**
     * Gets the current input file ID.
     * 
     * @return the input file ID
     */
    public String getInputFileId() {
        return inputFileId;
    }
    
    /**
     * Gets the current endpoint.
     * 
     * @return the endpoint
     */
    public String getEndpoint() {
        return endpoint;
    }
    
    /**
     * Gets the current completion window.
     * 
     * @return the completion window, or null if not set
     */
    public String getCompletionWindow() {
        return completionWindow;
    }
    
    /**
     * Gets a copy of the current metadata.
     * 
     * @return a copy of the metadata map
     */
    public Map<String, String> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    /**
     * Checks if metadata has been set.
     * 
     * @return true if metadata is not empty
     */
    public boolean hasMetadata() {
        return !metadata.isEmpty();
    }
}