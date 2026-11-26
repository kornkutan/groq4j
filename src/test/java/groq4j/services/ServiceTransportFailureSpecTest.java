package groq4j.services;

import groq4j.builders.ChatCompletionRequestBuilder;
import groq4j.builders.EmbeddingRequestBuilder;
import groq4j.builders.RerankRequestBuilder;
import groq4j.builders.ResponseRequestBuilder;
import groq4j.builders.SpeechRequestBuilder;
import groq4j.builders.TranscriptionRequestBuilder;
import groq4j.builders.TranslationRequestBuilder;
import groq4j.GroqApiClientImpl;
import groq4j.enums.FilePurpose;
import groq4j.exceptions.GroqApiException;
import groq4j.exceptions.GroqNetworkException;
import groq4j.models.batch.BatchRequest;
import groq4j.models.chat.ChatCompletionRequest;
import groq4j.models.chat.ChatCompletionResponse;
import groq4j.models.chat.ChatCompletionChunk;
import groq4j.models.responses.ResponseObject;
import groq4j.models.responses.ResponseRequest;
import groq4j.models.responses.ResponseStreamEvent;
import groq4j.utils.HttpUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Calls every method on every service against a port that nothing listens on, so each method's
 * transport-failure branch runs and surfaces as a typed exception instead of a raw JDK one.
 */
@DisplayName("Every service method reports transport failures")
class ServiceTransportFailureSpecTest {

    private final HttpClient http = HttpUtils.createHttpClient();

    private static String deadBaseUrl() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return "http://127.0.0.1:" + socket.getLocalPort() + "/openai/v1";
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String base() {
        return deadBaseUrl();
    }

    private static void assertTransportFailure(Class<? extends GroqApiException> expected, Runnable call) {
        assertThrows(expected, call::run);
    }

    private static void assertTransportFailure(Class<? extends GroqApiException> expected,
                                               Supplier<?> call) {
        assertThrows(expected, () -> {
            Object result = call.get();
            if (result instanceof Stream<?> stream) {
                stream.toList();
            }
        });
    }

    @Test
    void chatMethodsReportNetworkFailures() {
        ChatCompletionRequest request = ChatCompletionRequestBuilder.create("m").userMessage("hi").build();
        var service = ChatServiceImpl.create(http, "k", base());

        assertTransportFailure(GroqNetworkException.class, () -> service.createCompletion(request));
        assertTransportFailure(GroqNetworkException.class, () -> service.streamCompletion(request));
        assertTransportFailure(GroqNetworkException.class, () -> service.simple("m", "hi"));
        assertTransportFailure(GroqNetworkException.class, () -> service.simple("m", "sys", "hi"));
    }

    @Test
    void responsesMethodsReportNetworkFailures() {
        ResponseRequest request = ResponseRequestBuilder.create("m").input("hi").build();
        var service = ResponsesServiceImpl.create(http, "k", base());

        assertTransportFailure(GroqNetworkException.class, () -> service.createResponse(request));
        assertTransportFailure(GroqNetworkException.class, () -> service.streamResponse(request));
    }

    @Test
    void embeddingAndRerankMethodsReportNetworkFailures() {
        var embeddings = EmbeddingsServiceImpl.create(http, "k", base());
        var reranking = RerankingServiceImpl.create(http, "k", base());

        assertTransportFailure(GroqNetworkException.class,
            () -> embeddings.createEmbeddings(EmbeddingRequestBuilder.create().input("x").build()));
        assertTransportFailure(GroqNetworkException.class, () -> embeddings.embed("m", "x"));

        assertTransportFailure(GroqNetworkException.class,
            () -> reranking.rerank(RerankRequestBuilder.create().query("q").documents("d").build()));
    }

    @Test
    void audioMethodsReportNetworkFailures() {
        var service = AudioServiceImpl.create(http, "k", base());
        byte[] audio = {1, 2, 3};

        assertTransportFailure(GroqNetworkException.class, () -> service.createTranscription(
            TranscriptionRequestBuilder.withFile("whisper-large-v3", audio).build()));
        assertTransportFailure(GroqNetworkException.class, () -> service.createTranslation(
            TranslationRequestBuilder.withFile("whisper-large-v3", audio).build()));
        assertTransportFailure(GroqNetworkException.class, () -> service.createSpeech(
            SpeechRequestBuilder.create().input("hi").voice("troy").build()));
        assertTransportFailure(GroqNetworkException.class,
            () -> service.simpleTranscription("whisper-large-v3", audio, "a.wav"));
        assertTransportFailure(GroqNetworkException.class,
            () -> service.simpleTranslation("whisper-large-v3", audio, "a.wav"));
        assertTransportFailure(GroqNetworkException.class, () -> service.simpleSpeech("m", "hi", "troy"));
    }

    @Test
    void modelMethodsReportNetworkFailures() {
        var service = ModelsServiceImpl.create(http, "k", base());

        assertTransportFailure(GroqNetworkException.class, service::listModels);
        assertTransportFailure(GroqNetworkException.class, () -> service.retrieveModel("llama-3.3-70b-versatile"));
    }

    @Test
    void batchMethodsReportNetworkFailures() {
        var service = BatchServiceImpl.create(http, "k", base());
        var request = new BatchRequest("file-1", "/v1/chat/completions", "24h", Optional.empty());

        assertTransportFailure(GroqNetworkException.class, () -> service.createBatch(request));
        assertTransportFailure(GroqNetworkException.class, service::listBatches);
        assertTransportFailure(GroqNetworkException.class, () -> service.retrieveBatch("batch_1"));
        assertTransportFailure(GroqNetworkException.class, () -> service.cancelBatch("batch_1"));
    }

    @Test
    void fileMethodsReportNetworkFailures() {
        var service = FilesServiceImpl.create(http, "k", base());
        byte[] content = "{}".getBytes();

        assertTransportFailure(GroqNetworkException.class,
            () -> service.uploadFile(content, "a.jsonl", FilePurpose.BATCH));
        assertTransportFailure(GroqNetworkException.class, service::listFiles);
        assertTransportFailure(GroqNetworkException.class, () -> service.retrieveFile("file-1"));
        assertTransportFailure(GroqNetworkException.class, () -> service.deleteFile("file-1"));
        assertTransportFailure(GroqNetworkException.class, () -> service.downloadFileContent("file-1"));
    }

    @Test
    void defaultHelpersPropagateFailures() {
        var service = FilesServiceImpl.create(http, "k", base());
        byte[] content = "{}".getBytes();

        assertTransportFailure(GroqNetworkException.class,
            () -> service.uploadBatchFile(content, "a.jsonl"));
        assertFalse(service.fileExists("file-1"));
        assertEquals(-1, service.getFileSize("file-1"));
    }

    @Test
    void streamCallersSeeTheTypedException() {
        var service = ChatServiceImpl.create(http, "k", base());
        var request = ChatCompletionRequestBuilder.create("m").userMessage("hi").build();

        assertThrows(GroqNetworkException.class, () -> {
            try (Stream<ChatCompletionChunk> stream = service.streamCompletion(request)) {
                stream.toList();
            }
        });
    }

    @Test
    void clientRejectsBlankKeysBeforeAnyRequest() {
        assertThrows(groq4j.exceptions.GroqValidationException.class, () -> ChatServiceImpl.create(" "));
        assertThrows(groq4j.exceptions.GroqValidationException.class, () -> GroqApiClientImpl.create(""));
    }

    @Test
    void typedResultsAreNotProducedOnFailure() {
        var service = ResponsesServiceImpl.create(http, "k", base());
        Supplier<ResponseObject> call = () -> service.createResponse(
            ResponseRequestBuilder.create("m").input("hi").build());

        GroqApiException error = assertThrows(GroqApiException.class, call::get);
        assertInstanceOf(GroqNetworkException.class, error);
    }

    @Test
    void streamEventsAreNotProducedOnFailure() {
        var service = ResponsesServiceImpl.create(http, "k", base());
        Supplier<ResponseStreamEvent> call = () -> service.streamResponse(
            ResponseRequestBuilder.create("m").input("hi").build()).findFirst().orElseThrow();

        assertThrows(GroqNetworkException.class, call::get);
    }
}
