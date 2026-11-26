package groq4j.utils;

import com.sun.net.httpserver.HttpServer;
import groq4j.enums.HttpMethod;
import groq4j.exceptions.GroqApiException;
import groq4j.exceptions.GroqForbiddenException;
import groq4j.exceptions.GroqNotFoundException;
import groq4j.exceptions.GroqRateLimitException;
import groq4j.exceptions.GroqUnprocessableEntityException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HTTP transport: multipart bodies, error mapping, streaming")
class HttpTransportTest {

    private HttpServer server;
    private HttpClient client;
    private final AtomicReference<String> lastContentType = new AtomicReference<>();
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastAuthorization = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String responseBody = "{\"ok\":true}";
    private volatile String responseContentType = "application/json";

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            lastAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

            byte[] payload = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", responseContentType);
            exchange.sendResponseHeaders(status, payload.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(payload);
            }
        });
        server.start();
        client = HttpUtils.createHttpClient();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/openai/v1/test";
    }

    @Test
    void sendsJsonWithAuthHeader() {
        var request = HttpUtils.createJsonRequest(url(), "key-123", HttpMethod.POST, "{\"a\":1}");

        String response = HttpUtils.await(HttpUtils.executeRequest(client, request));

        assertEquals("{\"ok\":true}", response);
        assertEquals("Bearer key-123", lastAuthorization.get());
        assertTrue(lastContentType.get().startsWith("application/json"));
    }

    @Test
    void buildsMultipartWithRepeatedArrayFieldsAndFile() {
        var form = Map.<String, Object>of(
            "model", "whisper-large-v3",
            "timestamp_granularities[]", List.of("word", "segment"),
            "file", new HttpUtils.FormFile(new byte[] {1, 2, 3}, "clip.wav", "audio/wav"));

        var request = HttpUtils.createMultipartRequest(url(), "key-123", form);
        HttpUtils.await(HttpUtils.executeRequest(client, request));

        String body = lastBody.get();
        assertTrue(lastContentType.get().startsWith("multipart/form-data; boundary="));
        assertTrue(body.contains("name=\"model\""));
        assertTrue(body.contains("whisper-large-v3"));
        assertEquals(2, body.split("name=\"timestamp_granularities\\[\\]\"", -1).length - 1);
        assertTrue(body.contains("filename=\"clip.wav\""));
        assertTrue(body.contains("Content-Type: audio/wav"));
    }

    @Test
    void mapsErrorStatusesToExceptions() {
        status = 403;
        responseBody = "{\"error\":{\"message\":\"denied\",\"type\":\"forbidden_error\",\"code\":\"no_access\"}}";
        var request = HttpUtils.createJsonRequest(url(), "key-123", HttpMethod.GET, null);

        assertThrows(GroqForbiddenException.class, () -> HttpUtils.await(HttpUtils.executeRequest(client, request)));
    }

    @Test
    void mapsNotFound() {
        status = 404;
        responseBody = "{\"error\":{\"message\":\"missing\",\"type\":\"not_found_error\",\"code\":\"not_found\"}}";
        var request = HttpUtils.createJsonRequest(url(), "key-123", HttpMethod.GET, null);

        assertThrows(GroqNotFoundException.class, () -> HttpUtils.await(HttpUtils.executeRequest(client, request)));
    }

    @Test
    void mapsUnprocessableEntity() {
        status = 422;
        responseBody = "{\"error\":{\"message\":\"bad\",\"type\":\"invalid_request_error\",\"code\":\"invalid\"}}";
        var request = HttpUtils.createJsonRequest(url(), "key-123", HttpMethod.POST, "{}");

        assertThrows(GroqUnprocessableEntityException.class,
            () -> HttpUtils.await(HttpUtils.executeRequest(client, request)));
    }

    @Test
    void mapsRateLimitAndKeepsGroqApiExceptionAsCause() {
        status = 429;
        responseBody = "{\"error\":{\"message\":\"slow down\",\"type\":\"rate_limit_error\",\"code\":\"rate_limit_exceeded\"}}";
        var request = HttpUtils.createJsonRequest(url(), "key-123", HttpMethod.POST, "{}");

        var thrown = assertThrows(GroqRateLimitException.class,
            () -> HttpUtils.await(HttpUtils.executeRequest(client, request)));
        assertEquals(429, thrown.getStatusCode());
        assertInstanceOf(GroqApiException.class, thrown);
    }

    @Test
    void mapsPayloadTooLarge() {
        status = 413;
        responseBody = "{\"error\":{\"message\":\"too big\",\"type\":\"invalid_request_error\",\"code\":\"too_large\"}}";
        var request = HttpUtils.createJsonRequest(url(), "key-123", HttpMethod.POST, "{}");

        var thrown = assertThrows(groq4j.exceptions.GroqRequestTooLargeException.class,
            () -> HttpUtils.await(HttpUtils.executeRequest(client, request)));
        assertEquals(413, thrown.getStatusCode());
    }

    @Test
    void mapsFailedDependency() {
        status = 424;
        responseBody = "{\"error\":{\"message\":\"mcp auth failed\",\"type\":\"dependency_error\",\"code\":\"mcp\"}}";
        var request = HttpUtils.createJsonRequest(url(), "key-123", HttpMethod.POST, "{}");

        var thrown = assertThrows(groq4j.exceptions.GroqFailedDependencyException.class,
            () -> HttpUtils.await(HttpUtils.executeRequest(client, request)));
        assertEquals(424, thrown.getStatusCode());
    }

    @Test
    void streamsEventsUntilDone() {
        responseContentType = "text/event-stream";
        responseBody = """
            data: {"id":"1","choices":[{"index":0,"delta":{"content":"Hel"}}]}

            data: {"id":"1","choices":[{"index":0,"delta":{"content":"lo"}}]}

            data: [DONE]

            """;
        var request = HttpUtils.createStreamingRequest(url(), "key-123", "{\"stream\":true}");

        List<String> payloads = SseUtils.payloads(HttpUtils.openEventStream(client, request)).toList();

        assertEquals(2, payloads.size());
        assertTrue(payloads.getFirst().contains("Hel"));
        assertTrue(lastBody.get().contains("\"stream\":true"));
    }

    @Test
    void streamsBinaryResponses() {
        responseContentType = "audio/wav";
        responseBody = "RIFFdata";
        var request = HttpUtils.createJsonRequest(url(), "key-123", HttpMethod.POST, "{}");

        byte[] audio = HttpUtils.await(HttpUtils.executeRequestForBytes(client, request));

        assertEquals("RIFFdata", new String(audio, StandardCharsets.UTF_8));
    }
}
