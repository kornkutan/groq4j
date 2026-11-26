package groq4j.utils;

import groq4j.enums.HttpMethod;
import groq4j.exceptions.GroqApiException;
import groq4j.exceptions.GroqFailedDependencyException;
import groq4j.exceptions.GroqAuthenticationException;
import groq4j.exceptions.GroqBadRequestException;
import groq4j.exceptions.GroqForbiddenException;
import groq4j.exceptions.GroqNetworkException;
import groq4j.exceptions.GroqNotFoundException;
import groq4j.exceptions.GroqRateLimitException;
import groq4j.exceptions.GroqRequestTooLargeException;
import groq4j.exceptions.GroqServerException;
import groq4j.exceptions.GroqUnprocessableEntityException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

public final class HttpUtils {
    private HttpUtils() {
        // Prevent instantiation
    }

    /** A binary form part with an explicit filename and content type. */
    public record FormFile(byte[] data, String filename, String contentType) {
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

    public static HttpRequest createStreamingRequest(String url, String apiKey, String jsonBody) {
        return createRequestBuilder(url, apiKey)
            .timeout(Duration.ofSeconds(Constants.DEFAULT_STREAM_READ_TIMEOUT))
            .header(Constants.CONTENT_TYPE_HEADER, Constants.APPLICATION_JSON)
            .header(Constants.ACCEPT_HEADER, Constants.TEXT_EVENT_STREAM)
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
            .build();
    }

    public static HttpRequest createMultipartRequest(String url, String apiKey, Map<String, Object> formData) {
        String boundary = "----groq4j-boundary-" + System.nanoTime();
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
        String boundary = "----groq4j-boundary-" + System.nanoTime();
        byte[] multipartBody = buildFileUploadBodyBytes(fileData, filename, purpose, boundary);

        return createRequestBuilder(url, apiKey)
            .header(Constants.CONTENT_TYPE_HEADER, Constants.MULTIPART_FORM_DATA + "; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
            .build();
    }

    private static byte[] buildMultipartBodyBytes(Map<String, Object> formData, String boundary) {
        var baos = new ByteArrayOutputStream();

        try {
            for (var entry : formData.entrySet()) {
                Object value = entry.getValue();
                if (value == null) {
                    continue;
                }
                if (value instanceof FormFile file) {
                    writeFilePart(baos, entry.getKey(), file, boundary);
                } else if (value instanceof byte[] data) {
                    writeFilePart(baos, entry.getKey(),
                        new FormFile(data, entry.getKey(), "application/octet-stream"), boundary);
                } else if (value instanceof Iterable<?> values) {
                    for (Object element : values) {
                        if (element != null) {
                            writeTextPart(baos, entry.getKey(), element.toString(), boundary);
                        }
                    }
                } else {
                    writeTextPart(baos, entry.getKey(), value.toString(), boundary);
                }
            }
            baos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to build multipart body", e);
        }

        return baos.toByteArray();
    }

    private static void writeTextPart(ByteArrayOutputStream baos, String name, String value, String boundary)
            throws IOException {
        baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write(value.getBytes(StandardCharsets.UTF_8));
        baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static void writeFilePart(ByteArrayOutputStream baos, String name, FormFile file, String boundary)
            throws IOException {
        baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\""
            + file.filename() + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Type: " + file.contentType() + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write(file.data());
        baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] buildFileUploadBodyBytes(byte[] fileData, String filename, String purpose, String boundary) {
        var baos = new ByteArrayOutputStream();

        try {
            writeFilePart(baos, "file", new FormFile(fileData, filename, determineContentType(filename)), boundary);
            writeTextPart(baos, "purpose", purpose, boundary);
            baos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to build file upload body", e);
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

    /**
     * Waits for a request and rethrows the real failure. {@link CompletableFuture#join()} wraps
     * everything in a {@link java.util.concurrent.CompletionException}, which hides the typed
     * exceptions this library promises to throw.
     */
    public static <T> T await(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }
    }

    /**
     * Blocking send that maps transport problems to {@link GroqNetworkException} and HTTP error
     * statuses to their typed exceptions.
     */
    public static String sendAndReadBody(HttpClient client, HttpRequest request) {
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw GroqNetworkException.connectionFailed("Request interrupted", e);
        } catch (IOException e) {
            throw GroqNetworkException.connectionFailed("Request failed", e);
        }
        handleHttpErrors(response);
        return response.body();
    }

    public static CompletableFuture<String> executeRequest(HttpClient client, HttpRequest request) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                handleHttpErrors(response);
                return response.body();
            })
            .exceptionally(throwable -> {
                if (throwable.getCause() instanceof GroqApiException apiException) {
                    throw apiException;
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
                if (throwable.getCause() instanceof GroqApiException apiException) {
                    throw apiException;
                }
                throw GroqNetworkException.connectionFailed("Request failed", throwable);
            });
    }

    /**
     * Opens a server-sent events response and returns its raw lines. The caller is responsible
     * for closing the stream, which closes the underlying connection.
     */
    public static Stream<String> openEventStream(HttpClient client, HttpRequest request) {
        try {
            HttpResponse<Stream<String>> response = client.send(request, HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String body = response.body().reduce("", (a, b) -> a + b);
                throw toException(response.statusCode(), body, response);
            }
            return response.body();
        } catch (IOException e) {
            throw GroqNetworkException.connectionFailed("Stream request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw GroqNetworkException.connectionFailed("Stream request interrupted", e);
        }
    }

    public static void handleHttpErrors(HttpResponse<?> response) {
        int statusCode = response.statusCode();
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }
        Object body = response.body();
        throw toException(statusCode, body == null ? "" : body.toString(), response);
    }

    private static GroqApiException toException(int statusCode, String body, HttpResponse<?> response) {
        String errorMessage = extractErrorMessage(body);
        String errorType = extractErrorType(body);
        String errorCode = extractErrorCode(body);

        return switch (statusCode) {
            case 400 -> new GroqBadRequestException(errorMessage, errorType, errorCode);
            case 401 -> new GroqAuthenticationException(errorMessage, errorType, errorCode);
            case 403 -> new GroqForbiddenException(errorMessage, errorType, errorCode);
            case 404 -> new GroqNotFoundException(errorMessage, errorType, errorCode);
            case 413 -> new GroqRequestTooLargeException(errorMessage, errorType, errorCode);
            case 422 -> new GroqUnprocessableEntityException(errorMessage, errorType, errorCode);
            case 424 -> new GroqFailedDependencyException(errorMessage, errorType, errorCode);
            case 429 -> new GroqRateLimitException(
                errorMessage, errorType, errorCode, extractRetryAfter(response));
            case 498 -> new GroqApiException(
                "Flex tier capacity exceeded: " + errorMessage, statusCode, errorType, errorCode);
            case 499 -> new GroqApiException(
                "Request cancelled: " + errorMessage, statusCode, errorType, errorCode);
            case 500, 502, 503, 504 -> new GroqServerException(errorMessage, statusCode, errorType, errorCode);
            default -> new GroqApiException(errorMessage, statusCode, errorType, errorCode);
        };
    }

    private static String extractErrorMessage(String responseBody) {
        try {
            String message = JsonUtils.extractStringValue(responseBody, "error.message");
            return message != null ? message : responseBody;
        } catch (Exception e) {
            return responseBody.isEmpty() ? "Unknown error occurred" : responseBody;
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
        return response.headers().firstValue(Constants.RETRY_AFTER_HEADER)
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
