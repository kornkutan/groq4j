package groq4j.services;

import groq4j.exceptions.GroqNetworkException;
import groq4j.exceptions.GroqSerializationException;
import groq4j.exceptions.GroqValidationException;
import groq4j.support.FakeGroqServer;
import groq4j.support.FakeServerTestBase;
import groq4j.utils.HttpUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the parts of every service that the happy path tests cannot reach: convenience
 * constructors, factories, unparseable payloads, connection failures and local validation.
 */
@DisplayName("Service construction, dead connections and malformed payloads")
class ServiceEdgeSpecTest extends FakeServerTestBase {

    private static final String UNREACHABLE = "http://127.0.0.1:1/openai/v1";

    @Test
    void everyConvenienceFactoryCanBeCalled() {
        assertNotNull(ChatServiceImpl.create("k"));
        assertNotNull(ChatServiceImpl.create(HttpUtils.createHttpClient(), "k"));
        assertNotNull(ResponsesServiceImpl.create("k"));
        assertNotNull(ResponsesServiceImpl.create(HttpUtils.createHttpClient(), "k"));
        assertNotNull(EmbeddingsServiceImpl.create("k"));
        assertNotNull(EmbeddingsServiceImpl.create(HttpUtils.createHttpClient(), "k"));
        assertNotNull(RerankingServiceImpl.create("k"));
        assertNotNull(RerankingServiceImpl.create(HttpUtils.createHttpClient(), "k"));
        assertNotNull(AudioServiceImpl.create("k"));
        assertNotNull(AudioServiceImpl.create(HttpUtils.createHttpClient(), "k"));
        assertNotNull(ModelsServiceImpl.create("k"));
        assertNotNull(ModelsServiceImpl.create(HttpUtils.createHttpClient(), "k"));
        assertNotNull(BatchServiceImpl.create("k"));
        assertNotNull(BatchServiceImpl.create(HttpUtils.createHttpClient(), "k"));
        assertNotNull(FilesServiceImpl.create("k"));
        assertNotNull(FilesServiceImpl.create(HttpUtils.createHttpClient(), "k"));

        assertNotNull(new ChatServiceImpl("k"));
        assertNotNull(new ResponsesServiceImpl("k"));
        assertNotNull(new EmbeddingsServiceImpl("k"));
        assertNotNull(new RerankingServiceImpl("k"));
        assertNotNull(new AudioServiceImpl("k"));
        assertNotNull(new ModelsServiceImpl("k"));
        assertNotNull(new BatchServiceImpl("k"));
        assertNotNull(new FilesServiceImpl("k"));
    }

    @Test
    void everyServiceRejectsAnEmptyKey() {
        HttpClient http = HttpUtils.createHttpClient();
        String base = server.baseUrl();

        assertThrows(GroqValidationException.class, () -> new ChatServiceImpl(http, "", base));
        assertThrows(GroqValidationException.class, () -> new ResponsesServiceImpl(http, "", base));
        assertThrows(GroqValidationException.class, () -> new EmbeddingsServiceImpl(http, "", base));
        assertThrows(GroqValidationException.class, () -> new RerankingServiceImpl(http, "", base));
        assertThrows(GroqValidationException.class, () -> new AudioServiceImpl(http, "", base));
        assertThrows(GroqValidationException.class, () -> new ModelsServiceImpl(http, "", base));
        assertThrows(GroqValidationException.class, () -> new BatchServiceImpl(http, "", base));
        assertThrows(GroqValidationException.class, () -> new FilesServiceImpl(http, "", base));
    }

    @Test
    void everyServiceRejectsANullHttpClient() {
        String base = server.baseUrl();

        assertThrows(GroqValidationException.class, () -> new ChatServiceImpl(null, "k", base));
        assertThrows(GroqValidationException.class, () -> new ResponsesServiceImpl(null, "k", base));
        assertThrows(GroqValidationException.class, () -> new EmbeddingsServiceImpl(null, "k", base));
        assertThrows(GroqValidationException.class, () -> new RerankingServiceImpl(null, "k", base));
        assertThrows(GroqValidationException.class, () -> new AudioServiceImpl(null, "k", base));
        assertThrows(GroqValidationException.class, () -> new ModelsServiceImpl(null, "k", base));
        assertThrows(GroqValidationException.class, () -> new BatchServiceImpl(null, "k", base));
        assertThrows(GroqValidationException.class, () -> new FilesServiceImpl(null, "k", base));
    }

    @Test
    void reportsDeadConnectionsAsNetworkFailures() {
        HttpClient http = HttpUtils.createHttpClient();

        assertThrows(GroqNetworkException.class,
            () -> new ModelsServiceImpl(http, "k", UNREACHABLE).listModels());
        assertThrows(GroqNetworkException.class,
            () -> new ChatServiceImpl(http, "k", UNREACHABLE).simple("m", "hi"));
        assertThrows(GroqNetworkException.class,
            () -> new BatchServiceImpl(http, "k", UNREACHABLE).listBatches());
        assertThrows(GroqNetworkException.class,
            () -> new FilesServiceImpl(http, "k", UNREACHABLE).listFiles());
        assertThrows(GroqNetworkException.class,
            () -> new ResponsesServiceImpl(http, "k", UNREACHABLE)
                .createResponse(groq4j.builders.ResponseRequestBuilder.create("m").input("x").build()));
    }

    @Test
    void reportsUnparseablePayloads() {
        HttpClient http = HttpUtils.createHttpClient();
        String base = server.baseUrl();

        server.route("/models", request -> FakeGroqServer.Reply.json("not json at all"));
        server.route("/chat/completions", request -> FakeGroqServer.Reply.json("not json at all"));
        server.route("/responses", request -> FakeGroqServer.Reply.json("not json at all"));
        server.route("/embeddings", request -> FakeGroqServer.Reply.json("not json at all"));
        server.route("/reranking", request -> FakeGroqServer.Reply.json("not json at all"));
        server.route("/audio/transcriptions", request -> FakeGroqServer.Reply.json("not json at all"));
        server.route("/audio/translations", request -> FakeGroqServer.Reply.json("not json at all"));
        server.route("/files", request -> FakeGroqServer.Reply.json("not json at all"));
        server.route("/batches", request -> FakeGroqServer.Reply.json("not json at all"));

        assertThrows(GroqSerializationException.class, () -> new ModelsServiceImpl(http, "k", base).listModels());
        assertThrows(GroqSerializationException.class, () -> new ChatServiceImpl(http, "k", base).simple("m", "x"));
        assertThrows(GroqSerializationException.class, () -> new ResponsesServiceImpl(http, "k", base)
            .createResponse(groq4j.builders.ResponseRequestBuilder.create("m").input("x").build()));
        assertThrows(GroqSerializationException.class, () -> new EmbeddingsServiceImpl(http, "k", base)
            .embed("nomic-embed-text-v1_5", "x"));
        assertThrows(GroqSerializationException.class, () -> new RerankingServiceImpl(http, "k", base)
            .rerank(groq4j.builders.RerankRequestBuilder.create().query("q").documents("d").build()));
        assertThrows(GroqSerializationException.class, () -> new AudioServiceImpl(http, "k", base)
            .simpleTranscription("whisper-large-v3", "RIFF".getBytes(), "a.wav"));
        assertThrows(GroqSerializationException.class, () -> new AudioServiceImpl(http, "k", base)
            .simpleTranslation("whisper-large-v3", "RIFF".getBytes(), "a.wav"));
        assertThrows(GroqSerializationException.class, () -> new FilesServiceImpl(http, "k", base).listFiles());
        assertThrows(GroqSerializationException.class, () -> new BatchServiceImpl(http, "k", base).listBatches());
    }

    @Test
    void rejectsNullRequestsBeforeAnyHttpCall() {
        HttpClient http = HttpUtils.createHttpClient();
        String base = server.baseUrl();

        assertThrows(GroqValidationException.class, () -> new ChatServiceImpl(http, "k", base).createCompletion(null));
        assertThrows(GroqValidationException.class, () -> new ChatServiceImpl(http, "k", base).streamCompletion(null));
        assertThrows(GroqValidationException.class, () -> new ResponsesServiceImpl(http, "k", base).createResponse(null));
        assertThrows(GroqValidationException.class, () -> new ResponsesServiceImpl(http, "k", base).streamResponse(null));
        assertThrows(GroqValidationException.class, () -> new EmbeddingsServiceImpl(http, "k", base).createEmbeddings(null));
        assertThrows(GroqValidationException.class, () -> new RerankingServiceImpl(http, "k", base).rerank(null));
        assertThrows(GroqValidationException.class, () -> new AudioServiceImpl(http, "k", base).createTranscription(null));
        assertThrows(GroqValidationException.class, () -> new AudioServiceImpl(http, "k", base).createTranslation(null));
        assertThrows(GroqValidationException.class, () -> new AudioServiceImpl(http, "k", base).createSpeech(null));
        assertThrows(GroqValidationException.class, () -> new BatchServiceImpl(http, "k", base).createBatch(null));

        assertTrue(server.requests().isEmpty(), "no HTTP call may happen for invalid input");
    }
}
