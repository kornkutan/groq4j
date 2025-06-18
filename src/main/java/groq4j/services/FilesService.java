package groq4j.services;

import groq4j.builders.FileUploadRequestBuilder.FileUploadRequest;
import groq4j.enums.FilePurpose;
import groq4j.models.files.FileDeleteResponse;
import groq4j.models.files.FileListResponse;
import groq4j.models.files.FileObject;

/**
 * Service interface for file operations with the Groq API.
 * 
 * <p>Provides functionality for:
 * - File upload (primarily for batch processing)
 * - File listing and retrieval
 * - File deletion
 * - File content download
 * 
 * <p><strong>Usage Examples:</strong>
 * <pre>{@code
 * // Create service
 * FilesService service = FilesServiceImpl.create(apiKey);
 * 
 * // Upload a batch file
 * FileObject file = service.uploadFile(jsonlData, "batch.jsonl", FilePurpose.BATCH);
 * 
 * // List all files
 * FileListResponse files = service.listFiles();
 * 
 * // Retrieve specific file
 * FileObject file = service.retrieveFile("file-abc123");
 * 
 * // Download file content
 * byte[] content = service.downloadFileContent("file-abc123");
 * 
 * // Delete file
 * FileDeleteResponse response = service.deleteFile("file-abc123");
 * }</pre>
 */
public interface FilesService {
    
    /**
     * Uploads a file to the Groq API.
     * 
     * @param fileData the file data as byte array
     * @param filename the name of the file
     * @param purpose the intended purpose of the file
     * @return file object containing the uploaded file metadata
     * @throws groq4j.exceptions.GroqApiException if the API request fails
     * @throws groq4j.exceptions.GroqValidationException if the file parameters are invalid
     */
    FileObject uploadFile(byte[] fileData, String filename, FilePurpose purpose);
    
    /**
     * Uploads a file using a FileUploadRequest from the builder.
     * 
     * @param request the file upload request containing file data and metadata
     * @return file object containing the uploaded file metadata
     * @throws groq4j.exceptions.GroqApiException if the API request fails
     * @throws groq4j.exceptions.GroqValidationException if the file parameters are invalid
     */
    FileObject uploadFile(FileUploadRequest request);
    
    /**
     * Lists all files that have been uploaded.
     * 
     * @return response containing the list of files and metadata
     * @throws groq4j.exceptions.GroqApiException if the API request fails
     */
    FileListResponse listFiles();
    
    /**
     * Retrieves information about a specific file.
     * 
     * @param fileId the unique identifier of the file
     * @return file object containing the file metadata
     * @throws groq4j.exceptions.GroqApiException if the API request fails or file not found
     * @throws groq4j.exceptions.GroqValidationException if the file ID is invalid
     */
    FileObject retrieveFile(String fileId);
    
    /**
     * Deletes a file from the Groq API.
     * 
     * @param fileId the unique identifier of the file to delete
     * @return response indicating whether the deletion was successful
     * @throws groq4j.exceptions.GroqApiException if the API request fails
     * @throws groq4j.exceptions.GroqValidationException if the file ID is invalid
     */
    FileDeleteResponse deleteFile(String fileId);
    
    /**
     * Downloads the content of a file.
     * 
     * @param fileId the unique identifier of the file
     * @return byte array containing the file content
     * @throws groq4j.exceptions.GroqApiException if the API request fails or file not found
     * @throws groq4j.exceptions.GroqValidationException if the file ID is invalid
     */
    byte[] downloadFileContent(String fileId);
    
    /**
     * Convenience method for uploading a batch file.
     * 
     * @param jsonlData the JSONL file data as byte array
     * @param filename the name of the file (should end with .jsonl)
     * @return file object containing the uploaded file metadata
     * @throws groq4j.exceptions.GroqApiException if the API request fails
     * @throws groq4j.exceptions.GroqValidationException if the file parameters are invalid
     */
    default FileObject uploadBatchFile(byte[] jsonlData, String filename) {
        return uploadFile(jsonlData, filename, FilePurpose.BATCH);
    }
    
    /**
     * Convenience method for checking if a file exists.
     * 
     * @param fileId the unique identifier of the file
     * @return true if the file exists, false otherwise
     */
    default boolean fileExists(String fileId) {
        try {
            retrieveFile(fileId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Convenience method for getting file size without downloading content.
     * 
     * @param fileId the unique identifier of the file
     * @return file size in bytes, or -1 if file not found
     */
    default long getFileSize(String fileId) {
        try {
            FileObject file = retrieveFile(fileId);
            return file.bytes();
        } catch (Exception e) {
            return -1;
        }
    }
}