package groq4j.builders;

import groq4j.enums.FilePurpose;
import groq4j.utils.FileUtils;
import groq4j.utils.ValidationUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Builder for creating file upload requests with fluent API.
 * 
 * <p>Supports file uploads from byte arrays, file paths, or existing files
 * with validation and convenience methods for common file types.
 * 
 * <p><strong>Basic Usage:</strong>
 * <pre>{@code
 * // Upload from byte array
 * var request = FileUploadRequestBuilder.create(fileData, "data.jsonl")
 *     .purpose(FilePurpose.BATCH)
 *     .build();
 * 
 * // Upload from file path
 * var request = FileUploadRequestBuilder.fromFile("/path/to/file.jsonl")
 *     .batchPurpose()
 *     .build();
 * 
 * // Convenience methods for batch files
 * var request = FileUploadRequestBuilder.createBatchFile(data, "batch.jsonl")
 *     .build();
 * }</pre>
 */
public class FileUploadRequestBuilder {
    private byte[] fileData;
    private String filename;
    private Optional<FilePurpose> purpose = Optional.empty();
    
    private FileUploadRequestBuilder() {
        // Private constructor - use static factory methods
    }
    
    /**
     * Creates a new builder for file upload.
     * 
     * @param fileData the file data as byte array (required)
     * @param filename the filename (required)
     * @return new builder instance
     * @throws IllegalArgumentException if fileData is null or empty, or filename is invalid
     */
    public static FileUploadRequestBuilder create(byte[] fileData, String filename) {
        ValidationUtils.requireNonNull(fileData, "File data cannot be null");
        ValidationUtils.requireNonEmpty(filename, "Filename cannot be null or empty");
        
        if (fileData.length == 0) {
            throw new IllegalArgumentException("File data cannot be empty");
        }
        
        var builder = new FileUploadRequestBuilder();
        builder.fileData = fileData.clone(); // Defensive copy
        builder.filename = filename;
        return builder;
    }
    
    /**
     * Creates a new builder from a file path.
     * 
     * @param filePath the path to the file to upload
     * @return new builder instance
     * @throws IllegalArgumentException if the file path is invalid
     * @throws RuntimeException if the file cannot be read
     */
    public static FileUploadRequestBuilder fromFile(String filePath) {
        ValidationUtils.requireNonEmpty(filePath, "File path cannot be null or empty");
        
        try {
            Path path = Path.of(filePath);
            byte[] data = Files.readAllBytes(path);
            String filename = path.getFileName().toString();
            return create(data, filename);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + filePath, e);
        }
    }
    
    /**
     * Creates a new builder from a Path object.
     * 
     * @param path the Path to the file to upload
     * @return new builder instance
     * @throws IllegalArgumentException if the path is invalid
     * @throws RuntimeException if the file cannot be read
     */
    public static FileUploadRequestBuilder fromPath(Path path) {
        ValidationUtils.requireNonNull(path, "Path cannot be null");
        
        try {
            byte[] data = Files.readAllBytes(path);
            String filename = path.getFileName().toString();
            return create(data, filename);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + path, e);
        }
    }
    
    /**
     * Convenience method to create a batch file upload request.
     * 
     * @param fileData the JSONL file data as byte array
     * @param filename the filename (should end with .jsonl)
     * @return new builder instance with BATCH purpose pre-set
     */
    public static FileUploadRequestBuilder createBatchFile(byte[] fileData, String filename) {
        return create(fileData, filename).purpose(FilePurpose.BATCH);
    }
    
    /**
     * Convenience method to create a batch file upload from a file path.
     * 
     * @param filePath the path to the JSONL file
     * @return new builder instance with BATCH purpose pre-set
     */
    public static FileUploadRequestBuilder createBatchFileFromPath(String filePath) {
        return fromFile(filePath).purpose(FilePurpose.BATCH);
    }
    
    /**
     * Sets the file purpose.
     * 
     * @param purpose the intended purpose of the file
     * @return this builder for method chaining
     */
    public FileUploadRequestBuilder purpose(FilePurpose purpose) {
        this.purpose = Optional.ofNullable(purpose);
        return this;
    }
    
    /**
     * Convenience method to set the purpose to BATCH.
     * 
     * @return this builder for method chaining
     */
    public FileUploadRequestBuilder batchPurpose() {
        return purpose(FilePurpose.BATCH);
    }
    
    /**
     * Validates the current configuration.
     * 
     * @throws IllegalArgumentException if the configuration is invalid
     */
    private void validate() {
        // Validate file data
        if (fileData == null || fileData.length == 0) {
            throw new IllegalArgumentException("File data cannot be null or empty");
        }
        
        // Validate filename
        ValidationUtils.requireNonEmpty(filename, "Filename cannot be null or empty");
        
        // Validate file size (100MB limit per Groq API)
        FileUtils.validateFileSize(fileData);
        
        // Purpose-specific validations
        if (purpose.isPresent()) {
            FilePurpose filePurpose = purpose.get();
            
            if (filePurpose == FilePurpose.BATCH) {
                // Validate JSONL format for batch files
                if (!filename.toLowerCase().endsWith(".jsonl")) {
                    throw new IllegalArgumentException("Batch files must have .jsonl extension, got: " + filename);
                }
                
                // Validate JSONL content
                FileUtils.validateBatchFile(fileData, filename);
            }
        }
    }
    
    /**
     * Builds the file upload parameters.
     * 
     * @return a FileUploadRequest containing the validated parameters
     * @throws IllegalArgumentException if the configuration is invalid
     */
    public FileUploadRequest build() {
        validate();
        
        return new FileUploadRequest(
            fileData.clone(), // Defensive copy
            filename,
            purpose.orElse(FilePurpose.BATCH) // Default to BATCH if not specified
        );
    }
    
    /**
     * Gets the current file data (defensive copy).
     * 
     * @return copy of the file data
     */
    public byte[] getFileData() {
        return fileData != null ? fileData.clone() : null;
    }
    
    /**
     * Gets the current filename.
     * 
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }
    
    /**
     * Gets the current purpose.
     * 
     * @return the file purpose, or empty if not set
     */
    public Optional<FilePurpose> getPurpose() {
        return purpose;
    }
    
    /**
     * Gets the file size in bytes.
     * 
     * @return the file size, or 0 if no file data
     */
    public long getFileSize() {
        return fileData != null ? fileData.length : 0;
    }
    
    /**
     * Returns a human-readable representation of the file size.
     * 
     * @return formatted file size (e.g., "1.5 MB", "256 KB")
     */
    public String getDisplaySize() {
        long bytes = getFileSize();
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
     * Record representing the file upload parameters.
     */
    public record FileUploadRequest(
        byte[] fileData,
        String filename,
        FilePurpose purpose
    ) {
        
        /**
         * Gets the file size in bytes.
         * 
         * @return the file size
         */
        public long getFileSize() {
            return fileData != null ? fileData.length : 0;
        }
        
        /**
         * Returns a human-readable representation of the file size.
         * 
         * @return formatted file size
         */
        public String getDisplaySize() {
            long bytes = getFileSize();
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
         * Checks if this is a batch file.
         * 
         * @return true if the purpose is BATCH
         */
        public boolean isBatchFile() {
            return purpose == FilePurpose.BATCH;
        }
        
        /**
         * Returns a defensive copy of the file data.
         * 
         * @return copy of the file data
         */
        public byte[] getFileDataCopy() {
            return fileData != null ? fileData.clone() : new byte[0];
        }
    }
}