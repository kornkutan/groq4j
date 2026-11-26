package groq4j.services;

import groq4j.builders.FileUploadRequestBuilder.FileUploadRequest;
import groq4j.enums.FilePurpose;
import groq4j.enums.HttpMethod;
import groq4j.exceptions.GroqSerializationException;
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
import java.util.Map;
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
    private final String baseUrl;
    private final boolean ownsHttpClient;

    /**
     * Creates FilesService with default HttpClient (convenient for quick usage).
     * Uses sensible defaults but less flexible than providing your own HttpClient.
     */
    public FilesServiceImpl(String apiKey) {
        this(HttpUtils.createHttpClient(), apiKey, Constants.BASE_URL);
    }

    /**
     * Creates FilesService with custom HttpClient (recommended for robust production deployments).
     * Allows full control over HTTP configuration and enables testing with mocked clients.
     */
    public FilesServiceImpl(HttpClient httpClient, String apiKey) {
        this(httpClient, apiKey, Constants.BASE_URL);
    }

    public FilesServiceImpl(HttpClient httpClient, String apiKey, String baseUrl) {
        ValidationUtils.requireNonNull(httpClient, "httpClient");
        ValidationUtils.requireNonNull(baseUrl, "baseUrl");
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        this.ownsHttpClient = false;

        ValidationUtils.validateApiKey(apiKey);
        this.baseUrl = baseUrl;
    }

    /**
     * Static factory method for creating FilesService with custom HttpClient.
     * Recommended for production use and testing.
     */
    public static FilesService create(HttpClient httpClient, String apiKey) {
        return new FilesServiceImpl(httpClient, apiKey);
    }

    public static FilesService create(HttpClient httpClient, String apiKey, String baseUrl) {
        return new FilesServiceImpl(httpClient, apiKey, baseUrl);
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

        String url = UrlUtils.buildFilesUrl(baseUrl);
        HttpRequest request = HttpUtils.createFileUploadRequest(
            url, 
            apiKey, 
            fileData, 
            filename, 
            purpose.getValue()
        );

        CompletableFuture<String> future = HttpUtils.executeRequest(httpClient, request);
        String responseJson = HttpUtils.await(future);

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
        String url = UrlUtils.buildFilesUrl(baseUrl);
        HttpRequest request = HttpUtils.createJsonRequest(url, apiKey, HttpMethod.GET, null);

        CompletableFuture<String> future = HttpUtils.executeRequest(httpClient, request);
        String responseJson = HttpUtils.await(future);

        return parseFileListResponse(responseJson);
    }

    @Override
    public FileObject retrieveFile(String fileId) {
        ValidationUtils.requireNonEmpty(fileId, "fileId");

        String url = UrlUtils.buildFileUrl(baseUrl, fileId);
        HttpRequest request = HttpUtils.createJsonRequest(url, apiKey, HttpMethod.GET, null);

        CompletableFuture<String> future = HttpUtils.executeRequest(httpClient, request);
        String responseJson = HttpUtils.await(future);

        return parseFileObject(responseJson);
    }

    @Override
    public FileDeleteResponse deleteFile(String fileId) {
        ValidationUtils.requireNonEmpty(fileId, "fileId");

        String url = UrlUtils.buildFileUrl(baseUrl, fileId);
        HttpRequest request = HttpUtils.createJsonRequest(url, apiKey, HttpMethod.DELETE, null);

        CompletableFuture<String> future = HttpUtils.executeRequest(httpClient, request);
        String responseJson = HttpUtils.await(future);

        return parseFileDeleteResponse(responseJson);
    }

    @Override
    public byte[] downloadFileContent(String fileId) {
        ValidationUtils.requireNonEmpty(fileId, "fileId");

        String url = UrlUtils.buildFileContentUrl(baseUrl, fileId);
        HttpRequest request = HttpUtils.createJsonRequest(url, apiKey, HttpMethod.GET, null);

        CompletableFuture<byte[]> future = HttpUtils.executeRequestForBytes(httpClient, request);
        return HttpUtils.await(future);
    }

    // Private parsing methods

    private FileObject parseFileObject(String responseJson) {
        Map<String, Object> root = JsonUtils.parseObject(responseJson);
        String id = JsonUtils.stringValue(root.get("id"));
        if (id == null) {
            throw new GroqSerializationException("Invalid file response: missing 'id' field");
        }

        String object = JsonUtils.stringValue(root.get("object"));
        Double bytes = JsonUtils.doubleValue(root.get("bytes"));
        Double createdAt = JsonUtils.doubleValue(root.get("created_at"));
        String filename = JsonUtils.stringValue(root.get("filename"));

        FilePurpose purpose;
        try {
            purpose = FilePurpose.fromValue(JsonUtils.stringValue(root.get("purpose")));
        } catch (IllegalArgumentException e) {
            purpose = FilePurpose.BATCH;
        }

        return FileObject.of(id,
            object == null ? "file" : object,
            bytes == null ? 0L : bytes.longValue(),
            createdAt == null ? System.currentTimeMillis() / 1000 : createdAt.longValue(),
            filename == null ? "unknown" : filename,
            purpose);
    }

    private FileListResponse parseFileListResponse(String responseJson) {
        Map<String, Object> root = JsonUtils.parseObject(responseJson);
        String object = JsonUtils.stringValue(root.get("object"));

        List<FileObject> files = new ArrayList<>();
        for (Object raw : JsonUtils.asList(root.get("data"))) {
            files.add(parseFileObject(JsonUtils.toJsonString(JsonUtils.asMap(raw))));
        }

        return FileListResponse.of(object == null ? "list" : object, files);
    }

    private FileDeleteResponse parseFileDeleteResponse(String responseJson) {
        Map<String, Object> root = JsonUtils.parseObject(responseJson);
        String id = JsonUtils.stringValue(root.get("id"));
        if (id == null) {
            throw new GroqSerializationException("Invalid delete response: missing 'id' field");
        }

        String object = JsonUtils.stringValue(root.get("object"));
        Object deleted = root.get("deleted");

        return FileDeleteResponse.of(id,
            object == null ? "file" : object,
            deleted instanceof Boolean flag && flag);
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