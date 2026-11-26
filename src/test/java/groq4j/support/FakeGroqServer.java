package groq4j.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A local HTTP server that answers like the Groq API. Every request is recorded, so a test can
 * assert on the method, path, headers and body the client actually sent.
 */
public final class FakeGroqServer implements AutoCloseable {

    /** One request the client made. */
    public record Recorded(String method, String path, Map<String, String> headers, String body) {

        public String header(String name) {
            for (var entry : headers.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    return entry.getValue();
                }
            }
            return null;
        }

        public boolean hasHeader(String name) {
            return header(name) != null;
        }
    }

    /** One canned response. */
    public record Reply(int status, String contentType, String body) {

        public static Reply json(String body) {
            return new Reply(200, "application/json", body);
        }

        public static Reply json(int status, String body) {
            return new Reply(status, "application/json", body);
        }

        public static Reply events(String body) {
            return new Reply(200, "text/event-stream", body);
        }

        public static Reply audio(byte[] data) {
            return new Reply(200, "audio/wav", new String(data, StandardCharsets.ISO_8859_1));
        }

        public static Reply error(int status, String message, String type, String code) {
            return json(status, """
                {"error":{"message":"%s","type":"%s","code":"%s"}}""".formatted(message, type, code));
        }
    }

    private final HttpServer server;
    private final List<Recorded> recorded = Collections.synchronizedList(new ArrayList<>());
    /** Answers one request. */
    public interface Handler {
        Reply handle(Recorded request);
    }

    private final Map<String, Handler> routes = new LinkedHashMap<>();
    private final List<byte[]> rawBodies = Collections.synchronizedList(new ArrayList<>());

    public FakeGroqServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot start the fake Groq server", e);
        }
        server.createContext("/", this::handle);
        server.setExecutor(java.util.concurrent.Executors.newSingleThreadExecutor(runnable -> {
            var thread = new Thread(runnable, "fake-groq");
            thread.setDaemon(true);
            return thread;
        }));
    }

    /** Answers every documented endpoint with canned Groq shaped JSON. */
    public FakeGroqServer withDefaultRoutes() {
        route("/models", request -> Reply.json(GroqFixtures.MODELS));
        route("/models/", request -> Reply.json(GroqFixtures.model(request.path().substring(request.path().lastIndexOf('/') + 1))));
        route("/chat/completions", replyFor(GroqFixtures.CHAT_COMPLETION, GroqFixtures.chatStream()));
        route("/responses", replyFor(GroqFixtures.RESPONSE, GroqFixtures.responsesStream()));
        route("/embeddings", request -> Reply.json(GroqFixtures.EMBEDDINGS));
        route("/reranking", request -> Reply.json(GroqFixtures.RERANKING));
        route("/audio/transcriptions", request -> Reply.json(GroqFixtures.TRANSCRIPTION));
        route("/audio/translations", request -> Reply.json(GroqFixtures.TRANSLATION));
        route("/audio/speech", request -> new Reply(200, "audio/wav",
            GroqFixtures.SPEECH_BYTES));
        route("/files", request -> "POST".equals(request.method())
            ? Reply.json(GroqFixtures.FILE)
            : Reply.json(GroqFixtures.FILE_LIST));
        route("/files/", request -> request.path().endsWith("/content")
            ? new Reply(200, "application/jsonl", GroqFixtures.FILE_CONTENT)
            : ("DELETE".equals(request.method())
                ? Reply.json(GroqFixtures.FILE_DELETE)
                : Reply.json(GroqFixtures.FILE)));
        route("/batches", request -> "POST".equals(request.method())
            ? Reply.json(GroqFixtures.BATCH)
            : Reply.json(GroqFixtures.BATCH_LIST));
        route("/batches/", request -> request.path().endsWith("/cancel")
            ? Reply.json(GroqFixtures.batch("cancelled"))
            : Reply.json(GroqFixtures.BATCH));
        return this;
    }

    public FakeGroqServer route(String pathSuffix, Handler handler) {
        routes.put(pathSuffix, handler);
        return this;
    }

    public void reset() {
        recorded.clear();
        rawBodies.clear();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/openai/v1";
    }

    public String rootUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public List<Recorded> requests() {
        synchronized (recorded) {
            return List.copyOf(recorded);
        }
    }

    public Optional<Recorded> lastRequest() {
        synchronized (recorded) {
            return recorded.isEmpty() ? Optional.empty() : Optional.of(recorded.getLast());
        }
    }

    public Recorded requestTo(String pathSuffix) {
        return requests().stream()
            .filter(request -> request.path().endsWith(pathSuffix))
            .reduce((first, second) -> second)
            .orElseThrow(() -> new AssertionError("no request to " + pathSuffix));
    }

    public Recorded requestAt(String exactPath) {
        return requests().stream()
            .filter(request -> request.path().equals(exactPath))
            .reduce((first, second) -> second)
            .orElseThrow(() -> new AssertionError("no request to " + exactPath + ", saw " + paths()));
    }

    public java.util.List<String> paths() {
        return requests().stream().map(Recorded::path).toList();
    }

    public void start() {
        server.start();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private Handler replyFor(String blocking, String streaming) {
        return request -> request.body().contains("\"stream\":true")
            ? Reply.events(streaming)
            : Reply.json(blocking);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        Map<String, String> headers = new LinkedHashMap<>();
        exchange.getRequestHeaders().forEach((name, values) -> headers.put(name, String.join(",", values)));

        var request = new Recorded(method, path, headers, body);
        recorded.add(request);

        Handler handler = routes.entrySet().stream()
            .filter(entry -> path.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .reduce((first, second) -> second)
            .orElse(null);

        Reply reply = handler == null
            ? Reply.error(404, "no fake route for " + path, "not_found_error", "not_found")
            : handler.handle(request);

        byte[] payload = reply.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", reply.contentType());
        exchange.sendResponseHeaders(reply.status(), payload.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
        }
    }
}
