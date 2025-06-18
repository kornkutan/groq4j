package groq4j.models.files;

/**
 * Response object for file deletion operations from the Groq API.
 * 
 * <p>This record contains the result of a file deletion request,
 * including the file ID and deletion status.
 * 
 * <p><strong>Usage Examples:</strong>
 * <pre>{@code
 * // Delete a file
 * FileDeleteResponse response = filesService.deleteFile("file-abc123");
 * 
 * // Check deletion status
 * if (response.isDeleted()) {
 *     System.out.println("File " + response.id() + " was successfully deleted");
 * }
 * 
 * // Get deletion details
 * String fileId = response.id();
 * String objectType = response.object();
 * boolean success = response.deleted();
 * }</pre>
 */
public record FileDeleteResponse(
    /**
     * The file identifier that was deleted.
     */
    String id,
    
    /**
     * The object type, which is always "file".
     */
    String object,
    
    /**
     * Whether the file was successfully deleted.
     */
    boolean deleted
) {
    
    /**
     * Creates a new FileDeleteResponse.
     *
     * @param id the file identifier
     * @param object the object type (should be "file")
     * @param deleted whether the deletion was successful
     * @return a new FileDeleteResponse instance
     */
    public static FileDeleteResponse of(String id, String object, boolean deleted) {
        return new FileDeleteResponse(id, object, deleted);
    }
    
    /**
     * Creates a successful FileDeleteResponse (object will be "file").
     *
     * @param id the file identifier
     * @return a new FileDeleteResponse instance indicating successful deletion
     */
    public static FileDeleteResponse success(String id) {
        return new FileDeleteResponse(id, "file", true);
    }
    
    /**
     * Creates a failed FileDeleteResponse (object will be "file").
     *
     * @param id the file identifier
     * @return a new FileDeleteResponse instance indicating failed deletion
     */
    public static FileDeleteResponse failure(String id) {
        return new FileDeleteResponse(id, "file", false);
    }
    
    /**
     * Checks if the file was successfully deleted.
     *
     * @return true if the file was deleted successfully
     */
    public boolean isDeleted() {
        return deleted;
    }
    
    /**
     * Checks if the deletion failed.
     *
     * @return true if the file deletion failed
     */
    public boolean isFailed() {
        return !deleted;
    }
    
    /**
     * Returns a human-readable status message.
     *
     * @return a status message describing the deletion result
     */
    public String getStatusMessage() {
        if (deleted) {
            return "File '" + id + "' was successfully deleted";
        } else {
            return "Failed to delete file '" + id + "'";
        }
    }
    
    /**
     * Returns a summary of the deletion operation.
     *
     * @return a string summarizing the deletion result
     */
    public String getSummary() {
        return String.format("File deletion: %s [%s]", 
                           id, 
                           deleted ? "SUCCESS" : "FAILED");
    }
}