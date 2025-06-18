package groq4j.models.files;

import groq4j.enums.FilePurpose;
import java.time.Instant;
import java.util.Optional;

/**
 * Represents a file object in the Groq API.
 * 
 * <p>This record contains all the metadata associated with a file uploaded to Groq,
 * including its unique identifier, size, creation time, and purpose.
 * 
 * <p><strong>Usage Examples:</strong>
 * <pre>{@code
 * // File information
 * FileObject file = FileObject.of(
 *     "file-abc123",
 *     "file", 
 *     1024L,
 *     Instant.now().getEpochSecond(),
 *     "batch_data.jsonl",
 *     FilePurpose.BATCH
 * );
 * 
 * // Check file properties
 * boolean isBatchFile = file.isBatchFile();
 * String displaySize = file.getDisplaySize();
 * boolean isLarge = file.isLargeFile();
 * }</pre>
 */
public record FileObject(
    /**
     * The file identifier, which can be referenced in the API endpoints.
     */
    String id,
    
    /**
     * The object type, which is always "file".
     */
    String object,
    
    /**
     * The size of the file, in bytes.
     */
    long bytes,
    
    /**
     * The Unix timestamp (in seconds) for when the file was created.
     */
    long createdAt,
    
    /**
     * The name of the file.
     */
    String filename,
    
    /**
     * The intended purpose of the file.
     * 
     * @see FilePurpose
     */
    FilePurpose purpose
) {
    
    /**
     * Creates a new FileObject with all required fields.
     *
     * @param id the file identifier
     * @param object the object type (should be "file")
     * @param bytes the file size in bytes
     * @param createdAt the creation timestamp
     * @param filename the original filename
     * @param purpose the file purpose
     * @return a new FileObject instance
     */
    public static FileObject of(String id, String object, long bytes, long createdAt, 
                               String filename, FilePurpose purpose) {
        return new FileObject(id, object, bytes, createdAt, filename, purpose);
    }
    
    /**
     * Returns the creation time as an Instant.
     *
     * @return the creation time as an Instant
     */
    public Instant getCreationTime() {
        return Instant.ofEpochSecond(createdAt);
    }
    
    /**
     * Returns a human-readable representation of the file size.
     *
     * @return formatted file size (e.g., "1.5 MB", "256 KB")
     */
    public String getDisplaySize() {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * Checks if this is a batch processing file.
     *
     * @return true if the purpose is BATCH
     */
    public boolean isBatchFile() {
        return purpose == FilePurpose.BATCH;
    }
    
    /**
     * Checks if this is considered a large file (> 10 MB).
     *
     * @return true if the file is larger than 10 MB
     */
    public boolean isLargeFile() {
        return bytes > 10 * 1024 * 1024; // 10 MB
    }
    
    /**
     * Returns the file extension from the filename.
     *
     * @return the file extension (without the dot), or empty if no extension
     */
    public Optional<String> getFileExtension() {
        if (filename == null || filename.isEmpty()) {
            return Optional.empty();
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return Optional.empty();
        }
        
        return Optional.of(filename.substring(lastDotIndex + 1).toLowerCase());
    }
    
    /**
     * Checks if this file appears to be a JSONL file based on its extension.
     *
     * @return true if the file has a .jsonl extension
     */
    public boolean isJsonlFile() {
        return getFileExtension()
            .map(ext -> "jsonl".equals(ext))
            .orElse(false);
    }
    
    /**
     * Returns the age of the file in seconds.
     *
     * @return the number of seconds since the file was created
     */
    public long getAgeInSeconds() {
        return Instant.now().getEpochSecond() - createdAt;
    }
    
    /**
     * Returns the age of the file in a human-readable format.
     *
     * @return formatted age (e.g., "2 hours ago", "3 days ago")
     */
    public String getDisplayAge() {
        long ageSeconds = getAgeInSeconds();
        
        if (ageSeconds < 60) {
            return ageSeconds + " seconds ago";
        } else if (ageSeconds < 3600) {
            long minutes = ageSeconds / 60;
            return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        } else if (ageSeconds < 86400) {
            long hours = ageSeconds / 3600;
            return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        } else {
            long days = ageSeconds / 86400;
            return days + " day" + (days == 1 ? "" : "s") + " ago";
        }
    }
}