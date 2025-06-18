package groq4j.utils;

import groq4j.enums.HttpMethod;
import groq4j.exceptions.GroqApiException;
import groq4j.exceptions.GroqAuthenticationException;
import groq4j.exceptions.GroqBadRequestException;
import groq4j.exceptions.GroqNetworkException;
import groq4j.exceptions.GroqRateLimitException;
import groq4j.exceptions.GroqServerException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class HttpUtils {
    private HttpUtils() {
        // Prevent instantiation
    }

    public static HttpClient createHttpClient() {
        return HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(Constants.DEFAULT_CONNECT_TIMEOUT))
            .build();
    }

    public static HttpRequest.Builder createRequestBuilder(String url, String apiKey) {
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(Constants.DEFAULT_READ_TIMEOUT))
            .header(Constants.AUTHORIZATION_HEADER, Constants.BEARER_PREFIX + apiKey)
            .header(Constants.USER_AGENT_HEADER, Constants.USER_AGENT);
    }

    public static HttpRequest createJsonRequest(String url, String apiKey, HttpMethod method, String jsonBody) {
        var builder = createRequestBuilder(url, apiKey)
            .header(Constants.CONTENT_TYPE_HEADER, Constants.APPLICATION_JSON);

        return switch (method) {
            case GET -> builder.GET().build();
            case POST -> builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8)).build();
            case DELETE -> builder.DELETE().build();
        };
    }

    public static HttpRequest createMultipartRequest(String url, String apiKey, Map<String, Object> formData) {
        String boundary = "----groq4j-boundary-" + System.currentTimeMillis();
        byte[] multipartBody = buildMultipartBodyBytes(formData, boundary);
        
        return createRequestBuilder(url, apiKey)
            .header(Constants.CONTENT_TYPE_HEADER, Constants.MULTIPART_FORM_DATA + "; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
            .build();
    }

    /**
     * Creates a multipart request for file uploads with explicit filename and content type.
     */
    public static HttpRequest createFileUploadRequest(String url, String apiKey, byte[] fileData, 
                                                    String filename, String purpose) {
        String boundary = "----groq4j-boundary-" + System.currentTimeMillis();
        byte[] multipartBody = buildFileUploadBodyBytes(fileData, filename, purpose, boundary);
        
        return createRequestBuilder(url, apiKey)
            .header(Constants.CONTENT_TYPE_HEADER, Constants.MULTIPART_FORM_DATA + "; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
            .build();
    }

    private static byte[] buildMultipartBodyBytes(Map<String, Object> formData, String boundary) {
        var baos = new java.io.ByteArrayOutputStream();
        
        try {
            for (var entry : formData.entrySet()) {
                baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                
                if (entry.getValue() instanceof byte[] fileData) {
                    // For file data, determine proper filename and content type
                    String filename = "audio.wav"; // Default filename
                    String contentType = "audio/wav"; // Default content type

                    baos.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
                    baos.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                    baos.write(fileData);
                } else {
                    baos.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                    baos.write(entry.getValue().toString().getBytes(StandardCharsets.UTF_8));
                }
                baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }
            baos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to build multipart body", e);
        }
        
        return baos.toByteArray();
    }

    private static byte[] buildFileUploadBodyBytes(byte[] fileData, String filename, String purpose, String boundary) {
        var baos = new java.io.ByteArrayOutputStream();
        
        try {
            baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));

            String contentType = determineContentType(filename);
            baos.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write(fileData);
            baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
            
            // Add purpose field
            baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write(("Content-Disposition: form-data; name=\"purpose\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write(purpose.getBytes(StandardCharsets.UTF_8));
            baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
            baos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to build file upload body", e);
        }
        
        return baos.toByteArray();
    }

    private static String determineContentType(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "application/octet-stream";
        }
        
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "jsonl" -> "application/jsonl";
            case "json" -> "application/json";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            default -> "application/octet-stream";
        };
    }

    public static CompletableFuture<String> executeRequest(HttpClient client, HttpRequest request) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                handleHttpErrors(response);
                return response.body();
            })
            .exceptionally(throwable -> {
                if (throwable.getCause() instanceof GroqApiException) {
                    throw (GroqApiException) throwable.getCause();
                }
                throw GroqNetworkException.connectionFailed("Request failed", throwable);
            });
    }

    public static CompletableFuture<byte[]> executeRequestForBytes(HttpClient client, HttpRequest request) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
            .thenApply(response -> {
                handleHttpErrors(response);
                return response.body();
            })
            .exceptionally(throwable -> {
                if (throwable.getCause() instanceof GroqApiException) {
                    throw (GroqApiException) throwable.getCause();
                }
                throw GroqNetworkException.connectionFailed("Request failed", throwable);
            });
    }

    public static void handleHttpErrors(HttpResponse<?> response) {
        int statusCode = response.statusCode();
        String body = response.body().toString();
        
        if (statusCode >= 200 && statusCode < 300) {
            return; // Success
        }
        
        // Parse error details from the response body
        String errorMessage = extractErrorMessage(body);
        String errorType = extractErrorType(body);
        String errorCode = extractErrorCode(body);
        
        switch (statusCode) {
            case 400 -> throw new GroqBadRequestException(errorMessage, errorType, errorCode);
            case 401 -> throw new GroqAuthenticationException(errorMessage, errorType, errorCode);
            case 429 -> {
                Integer retryAfter = extractRetryAfter(response);
                throw new GroqRateLimitException(errorMessage, retryAfter);
            }
            case 500, 502, 503, 504 -> throw new GroqServerException(errorMessage, statusCode, errorType, errorCode);
            default -> throw new GroqApiException(errorMessage, statusCode, errorType, errorCode);
        }
    }

    private static String extractErrorMessage(String responseBody) {
        try {
            String message = JsonUtils.extractStringValue(responseBody, "error.message");
            return message != null ? message : "Unknown error occurred";
        } catch (Exception e) {
            return "Failed to parse error message from response";
        }
    }

    private static String extractErrorType(String responseBody) {
        try {
            return JsonUtils.extractStringValue(responseBody, "error.type");
        } catch (Exception e) {
            return "unknown_error";
        }
    }

    private static String extractErrorCode(String responseBody) {
        try {
            return JsonUtils.extractStringValue(responseBody, "error.code");
        } catch (Exception e) {
            return "unknown_code";
        }
    }

    private static Integer extractRetryAfter(HttpResponse<?> response) {
        return response.headers().firstValue("Retry-After")
            .map(value -> {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return null;
                }
            })
            .orElse(null);
    }

    public static String buildUrl(String baseUrl, String endpoint) {
        return baseUrl + endpoint;
    }

    public static String buildUrl(String baseUrl, String endpoint, String pathParam) {
        return baseUrl + endpoint + "/" + pathParam;
    }
}