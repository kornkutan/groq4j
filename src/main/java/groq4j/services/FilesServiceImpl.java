package groq4j.services;

import groq4j.builders.FileUploadRequestBuilder.FileUploadRequest;
import groq4j.enums.FilePurpose;
import groq4j.enums.HttpMethod;
import groq4j.models.files.FileDeleteResponse;
import groq4j.models.files.FileListResponse;
import groq4j.models.files.FileObject;
import groq4j.utils.Constants;
import groq4j.utils.HttpUtils;
import groq4j.utils.JsonUtils;
import groq4j.utils.UrlUtils;
import groq4j.utils.ValidationUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of FilesService that communicates with the Groq API.
 * 
 * <p>Provides file management functionality including upload, list, retrieve,
 * delete, and download operations using HTTP/2 client for efficient communication.
 * 
 * <p><strong>Usage Examples:</strong>
 * <pre>{@code
 * // Create service
 * FilesService service = FilesServiceImpl.create(apiKey);
 * 
 * // Upload a file
 * FileObject file = service.uploadFile(data, "batch.jsonl", FilePurpose.BATCH);
 * 
 * // List files
 * FileListResponse files = service.listFiles();
 * 
 * // Delete file
 * FileDeleteResponse response = service.deleteFile(file.id());
 * }</pre>
 */
public class FilesServiceImpl implements FilesService {
    private final HttpClient httpClient;
    private final String apiKey;
    private final boolean ownsHttpClient;

    /**
     * Creates FilesService with default HttpClient (convenient for quick usage).
     * Uses sensible defaults but less flexible than providing your own HttpClient.
     */
    public FilesServiceImpl(String apiKey) {
        this.httpClient = HttpUtils.createHttpClient();
        this.apiKey = apiKey;
        this.ownsHttpClient = true;
        
        ValidationUtils.validateApiKey(apiKey);
    }

    /**
     * Creates FilesService with custom HttpClient (recommended for robust production deployments).
     * Allows full control over HTTP configuration and enables testing with mocked clients.
     */
    public FilesServiceImpl(HttpClient httpClient, String apiKey) {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        this.ownsHttpClient = false;

        ValidationUtils.validateApiKey(apiKey);
    }

    /**
     * Static factory method for creating FilesService with custom HttpClient.
     * Recommended for production use and testing.
     */
    public static FilesService create(HttpClient httpClient, String apiKey) {
        return new FilesServiceImpl(httpClient, apiKey);
    }

    /**
     * Static factory method for creating FilesService with default HttpClient.
     * Convenient for quick usage and prototyping.
     */
    public static FilesService create(String apiKey) {
        return new FilesServiceImpl(apiKey);
    }

    @Override
    public FileObject uploadFile(byte[] fileData, String filename, FilePurpose purpose) {
        ValidationUtils.requireNonNull(fileData, "fileData");
        ValidationUtils.requireNonEmpty(filename, "filename");
        ValidationUtils.requireNonNull(purpose, "purpose");
        ValidationUtils.validateFileSize(fileData);

        String url = UrlUtils.buildFilesUrl(Constants.BASE_URL);
        HttpRequest request = HttpUtils.createFileUploadRequest(
            url, 
            apiKey, 
            fileData, 
            filename, 
            purpose.getValue()
        );

        CompletableFuture<String> future = HttpUtils.executeRequest(httpClient, request);
        String responseJson = future.join();

        return parseFileObject(responseJson);
    }

    @Override
    public FileObject uploadFile(FileUploadRequest request) {
        ValidationUtils.requireNonNull(request, "request");
        
        return uploadFile(
            request.fileData(),
            request.filename(),
            request.purpose()
        );
    }

    @Override
    public FileListResponse listFiles() {
        String url = UrlUtils.buildFilesUrl(Constants.BASE_URL);
        HttpRequest request = HttpUtils.createJsonRequest(url, apiKey, HttpMethod.GET, null);

        CompletableFuture<String> future = HttpUtils.executeRequest(httpClient, request);
        String responseJson = future.join();

        return parseFileListResponse(responseJson);
    }

    @Override
    public FileObject retrieveFile(String fileId) {
        ValidationUtils.requireNonEmpty(fileId, "fileId");

        String url = UrlUtils.buildFileUrl(Constants.BASE_URL, fileId);
        HttpRequest request = HttpUtils.createJsonRequest(url, apiKey, HttpMethod.GET, null);

        CompletableFuture<String> future = HttpUtils.executeRequest(httpClient, request);
        String responseJson = future.join();

        return parseFileObject(responseJson);
    }

    @Override
    public FileDeleteResponse deleteFile(String fileId) {
        ValidationUtils.requireNonEmpty(fileId, "fileId");

        String url = UrlUtils.buildFileUrl(Constants.BASE_URL, fileId);
        HttpRequest request = HttpUtils.createJsonRequest(url, apiKey, HttpMethod.DELETE, null);

        CompletableFuture<String> future = HttpUtils.executeRequest(httpClient, request);
        String responseJson = future.join();

        return parseFileDeleteResponse(responseJson);
    }

    @Override
    public byte[] downloadFileContent(String fileId) {
        ValidationUtils.requireNonEmpty(fileId, "fileId");

        String url = UrlUtils.buildFileContentUrl(Constants.BASE_URL, fileId);
        HttpRequest request = HttpUtils.createJsonRequest(url, apiKey, HttpMethod.GET, null);

        CompletableFuture<byte[]> future = HttpUtils.executeRequestForBytes(httpClient, request);
        return future.join();
    }

    // Private parsing methods

    private FileObject parseFileObject(String responseJson) {
        String id = JsonUtils.extractStringValue(responseJson, "id");
        String object = JsonUtils.extractStringValue(responseJson, "object");
        Long bytes = JsonUtils.extractLongValue(responseJson, "bytes");
        Long createdAt = JsonUtils.extractLongValue(responseJson, "created_at");
        String filename = JsonUtils.extractStringValue(responseJson, "filename");
        String purposeValue = JsonUtils.extractStringValue(responseJson, "purpose");

        // Handle missing or null values gracefully
        if (id == null) {
            throw new RuntimeException("Invalid file response: missing 'id' field");
        }
        if (bytes == null) {
            bytes = 0L;
        }
        if (createdAt == null) {
            createdAt = System.currentTimeMillis() / 1000;
        }
        if (object == null) {
            object = "file";
        }
        if (filename == null) {
            filename = "unknown";
        }

        FilePurpose purpose;
        try {
            purpose = purposeValue != null ? FilePurpose.fromValue(purposeValue) : FilePurpose.BATCH;
        } catch (IllegalArgumentException e) {
            purpose = FilePurpose.BATCH; // Default fallback
        }

        return FileObject.of(id, object, bytes, createdAt, filename, purpose);
    }

    private FileListResponse parseFileListResponse(String responseJson) {
        String object = JsonUtils.extractStringValue(responseJson, "object");
        if (object == null) {
            object = "list";
        }

        List<FileObject> files = new ArrayList<>();
        
        // Try to extract the data array as a string
        String dataArrayJson = JsonUtils.extractArrayValue(responseJson, "data");
        
        if (dataArrayJson != null && !dataArrayJson.equals("[]")) {
            // Remove outer brackets
            String arrayContent = dataArrayJson.substring(1, dataArrayJson.length() - 1).trim();
            
            if (!arrayContent.isEmpty()) {
                // Simple parsing - split by objects
                String[] fileJsons = splitJsonObjects(arrayContent);
                
                for (String fileJson : fileJsons) {
                    try {
                        // Wrap in braces if not already wrapped
                        String completeJson = fileJson.trim();
                        if (!completeJson.startsWith("{")) {
                            completeJson = "{" + completeJson;
                        }
                        if (!completeJson.endsWith("}")) {
                            completeJson = completeJson + "}";
                        }
                        
                        FileObject file = parseFileObject(completeJson);
                        files.add(file);
                    } catch (Exception e) {
                        // Skip malformed file entries but continue processing
                        System.err.println("Warning: Failed to parse file entry: " + e.getMessage());
                    }
                }
            }
        }

        return FileListResponse.of(object, files);
    }
    
    private String[] splitJsonObjects(String arrayContent) {
        if (arrayContent.trim().isEmpty()) {
            return new String[0];
        }
        
        List<String> objects = new ArrayList<>();
        int start = 0;
        int braceCount = 0;
        boolean inString = false;
        
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            
            if (!inString) {
                if (c == '"') {
                    inString = true;
                } else if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        // End of an object
                        objects.add(arrayContent.substring(start, i + 1).trim());
                        start = i + 1;
                        // Skip comma if present
                        while (start < arrayContent.length() && 
                               (arrayContent.charAt(start) == ',' || Character.isWhitespace(arrayContent.charAt(start)))) {
                            start++;
                        }
                        i = start - 1; // Will be incremented by the loop
                    }
                }
            } else {
                if (c == '"' && (i == 0 || arrayContent.charAt(i - 1) != '\\')) {
                    inString = false;
                }
            }
        }
        
        return objects.toArray(new String[0]);
    }

    private FileDeleteResponse parseFileDeleteResponse(String responseJson) {
        String id = JsonUtils.extractStringValue(responseJson, "id");
        String object = JsonUtils.extractStringValue(responseJson, "object");
        Boolean deleted = JsonUtils.extractBooleanValue(responseJson, "deleted");

        // Handle missing values
        if (id == null) {
            throw new RuntimeException("Invalid delete response: missing 'id' field");
        }
        if (object == null) {
            object = "file";
        }
        if (deleted == null) {
            deleted = false;
        }

        return FileDeleteResponse.of(id, object, deleted);
    }

    /**
     * Gets the HTTP client used by this service.
     * Useful for testing and debugging purposes.
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Gets the API key used by this service.
     * Returns a masked version for security.
     */
    public String getMaskedApiKey() {
        if (apiKey == null || apiKey.length() < 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "***" + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * Checks if this service instance owns its HTTP client.
     * If true, the client was created internally and uses default settings.
     */
    public boolean ownsHttpClient() {
        return ownsHttpClient;
    }
}