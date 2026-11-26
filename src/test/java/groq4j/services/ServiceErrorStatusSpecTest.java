package groq4j.services;

import groq4j.builders.ChatCompletionRequestBuilder;
import groq4j.builders.EmbeddingRequestBuilder;
import groq4j.builders.RerankRequestBuilder;
import groq4j.builders.ResponseRequestBuilder;
import groq4j.builders.SpeechRequestBuilder;
import groq4j.builders.TranscriptionRequestBuilder;
import groq4j.builders.TranslationRequestBuilder;
import groq4j.enums.AudioFormat;
import groq4j.exceptions.GroqBadRequestException;
import groq4j.exceptions.GroqNotFoundException;
import groq4j.models.batch.BatchRequest;
import groq4j.support.FakeGroqServer;
import groq4j.support.FakeServerTestBase;
import groq4j.utils.HttpUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sends an error reply to every endpoint and checks the documented status-to-exception map holds
 * on each service method, not just on the transport helper.
 */
@DisplayName("Every endpoint maps API errors to typed exceptions")
class ServiceErrorStatusSpecTest extends FakeServerTestBase {

    private static FakeGroqServer.Handler badRequest() {
        return request -> FakeGroqServer.Reply.error(400, "bad request", "invalid_request_error", "bad_body");
    }

    private void failEverythingWith400() {
        for (String path : new String[] {"/chat/completions", "/responses", "/embeddings", "/reranking",
            "/models", "/models/", "/files", "/files/", "/batches", "/batches/",
            "/audio/transcriptions", "/audio/translations", "/audio/speech"}) {
            server.route(path, badRequest());
        }
    }

    @Test
    void chatSurfacesBadRequests() {
        failEverythingWith400();
        var service = new ChatServiceImpl(HttpUtils.createHttpClient(), "k", server.baseUrl());
        var request = ChatCompletionRequestBuilder.create("m").userMessage("hi").build();

        var error = assertThrows(GroqBadRequestException.class, () -> service.createCompletion(request));
        assertEquals("invalid_request_error", error.getErrorType());
        assertEquals("bad_body", error.getErrorCode());
        assertThrows(GroqBadRequestException.class, () -> service.simple("m", "hi"));
    }

    @Test
    void chatStreamSurfacesBadRequests() {
        failEverythingWith400();
        var service = new ChatServiceImpl(HttpUtils.createHttpClient(), "k", server.baseUrl());
        var request = ChatCompletionRequestBuilder.create("m").userMessage("hi").build();

        assertThrows(GroqBadRequestException.class, () -> {
            try (var stream = service.streamCompletion(request)) {
                stream.toList();
            }
        });
    }

    @Test
    void responsesSurfaceBadRequests() {
        failEverythingWith400();
        var service = new ResponsesServiceImpl(HttpUtils.createHttpClient(), "k", server.baseUrl());
        var request = ResponseRequestBuilder.create("m").input("hi").build();

        assertThrows(GroqBadRequestException.class, () -> service.createResponse(request));
        assertThrows(GroqBadRequestException.class, () -> {
            try (var stream = service.streamResponse(request)) {
                stream.toList();
            }
        });
    }

    @Test
    void embeddingsAndRerankingSurfaceBadRequests() {
        failEverythingWith400();
        var http = HttpUtils.createHttpClient();
        String base = server.baseUrl();

        assertThrows(GroqBadRequestException.class,
            () -> new EmbeddingsServiceImpl(http, "k", base).embed("m", "x"));
        assertThrows(GroqBadRequestException.class,
            () -> new EmbeddingsServiceImpl(http, "k", base)
                .createEmbeddings(EmbeddingRequestBuilder.create().input("x").build()));
        assertThrows(GroqBadRequestException.class,
            () -> new RerankingServiceImpl(http, "k", base)
                .rerank(RerankRequestBuilder.create().query("q").documents("d").build()));
    }

    @Test
    void audioSurfaceBadRequests() {
        failEverythingWith400();
        var service = new AudioServiceImpl(HttpUtils.createHttpClient(), "k", server.baseUrl());
        byte[] audio = {1, 2, 3};

        assertThrows(GroqBadRequestException.class, () -> service.createTranscription(
            TranscriptionRequestBuilder.withFile("whisper-large-v3", audio).build()));
        assertThrows(GroqBadRequestException.class, () -> service.createTranslation(
            TranslationRequestBuilder.withFile("whisper-large-v3", audio).build()));
        assertThrows(GroqBadRequestException.class, () -> service.createSpeech(
            SpeechRequestBuilder.create().input("hi").voice("troy").responseFormat(AudioFormat.WAV).build()));
    }

    @Test
    void modelsSurfaceNotFound() {
        server.route("/models/", request -> FakeGroqServer.Reply.error(404, "no such model",
            "not_found_error", "model_not_found"));

        assertThrows(GroqNotFoundException.class,
            () -> new ModelsServiceImpl(HttpUtils.createHttpClient(), "k", server.baseUrl())
                .retrieveModel("missing"));
    }

    @Test
    void filesSurfaceErrors() {
        failEverythingWith400();
        var service = new FilesServiceImpl(HttpUtils.createHttpClient(), "k", server.baseUrl());
        byte[] content = "{}".getBytes();

        assertThrows(GroqBadRequestException.class,
            () -> service.uploadFile(content, "a.jsonl", groq4j.enums.FilePurpose.BATCH));
        assertThrows(GroqBadRequestException.class, service::listFiles);
        assertThrows(GroqBadRequestException.class, () -> service.retrieveFile("file-1"));
        assertThrows(GroqBadRequestException.class, () -> service.deleteFile("file-1"));
        assertThrows(GroqBadRequestException.class, () -> service.downloadFileContent("file-1"));
    }

    @Test
    void batchesSurfaceErrors() {
        failEverythingWith400();
        var service = new BatchServiceImpl(HttpUtils.createHttpClient(), "k", server.baseUrl());
        var request = new BatchRequest("file-1", "/v1/chat/completions", "24h", Optional.empty());

        assertThrows(GroqBadRequestException.class, () -> service.createBatch(request));
        assertThrows(GroqBadRequestException.class, service::listBatches);
        assertThrows(GroqBadRequestException.class, () -> service.retrieveBatch("batch_1"));
        assertThrows(GroqBadRequestException.class, () -> service.cancelBatch("batch_1"));
    }
}
