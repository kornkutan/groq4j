package groq4j.services;

import groq4j.builders.ChatCompletionRequestBuilder;
import groq4j.builders.EmbeddingRequestBuilder;
import groq4j.builders.RerankRequestBuilder;
import groq4j.builders.ResponseRequestBuilder;
import groq4j.builders.SpeechRequestBuilder;
import groq4j.builders.TranscriptionRequestBuilder;
import groq4j.builders.TranslationRequestBuilder;
import groq4j.enums.AudioFormat;
import groq4j.enums.ResponseFormat;
import groq4j.enums.SampleRate;
import groq4j.enums.TimestampGranularity;
import groq4j.exceptions.GroqSerializationException;
import groq4j.models.chat.SearchSettings;

import java.util.List;
import java.util.Optional;
import groq4j.support.FakeGroqServer;
import groq4j.support.FakeServerTestBase;
import groq4j.utils.HttpUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Feeds the parsers payloads missing optional fields and non-JSON bodies, so every fallback
 * branch and every serialization-failure path runs.
 */
@DisplayName("Parsers handle degraded payloads")
class ServiceParseEdgeSpecTest extends FakeServerTestBase {

    private ChatServiceImpl chat() {
        return new ChatServiceImpl(HttpUtils.createHttpClient(), "k", server.baseUrl());
    }

    @Test
    void nonJsonRepliesBecomeSerializationErrors() {
        server.route("/chat/completions", request -> FakeGroqServer.Reply.json("<html>nope</html>"));
        server.route("/embeddings", request -> FakeGroqServer.Reply.json("<html>nope</html>"));
        server.route("/reranking", request -> FakeGroqServer.Reply.json("<html>nope</html>"));
        server.route("/models", request -> FakeGroqServer.Reply.json("<html>nope</html>"));
        server.route("/models/", request -> FakeGroqServer.Reply.json("<html>nope</html>"));
        server.route("/files", request -> FakeGroqServer.Reply.json("<html>nope</html>"));

        var http = HttpUtils.createHttpClient();
        String base = server.baseUrl();

        assertThrows(GroqSerializationException.class,
            () -> chat().createCompletion(ChatCompletionRequestBuilder.create("m").userMessage("hi").build()));
        assertThrows(GroqSerializationException.class,
            () -> new EmbeddingsServiceImpl(http, "k", base).embed("m", "x"));
        assertThrows(GroqSerializationException.class,
            () -> new RerankingServiceImpl(http, "k", base)
                .rerank(RerankRequestBuilder.create().query("q").documents("d").build()));
        assertThrows(GroqSerializationException.class,
            () -> new ModelsServiceImpl(http, "k", base).listModels());
        assertThrows(GroqSerializationException.class,
            () -> new ModelsServiceImpl(http, "k", base).retrieveModel("m"));
        assertThrows(GroqSerializationException.class,
            () -> new FilesServiceImpl(http, "k", base).listFiles());
    }

    @Test
    void responsesRejectsNonJsonReplies() {
        server.route("/responses", request -> FakeGroqServer.Reply.json("nope"));

        assertThrows(GroqSerializationException.class,
            () -> new ResponsesServiceImpl(HttpUtils.createHttpClient(), "k", server.baseUrl())
                .createResponse(ResponseRequestBuilder.create("m").input("hi").build()));
    }

    @Test
    void batchesRejectNonJsonReplies() {
        server.route("/batches", request -> FakeGroqServer.Reply.json("nope"));

        assertThrows(GroqSerializationException.class,
            () -> new BatchServiceImpl(HttpUtils.createHttpClient(), "k", server.baseUrl()).listBatches());
    }

    @Test
    void audioRejectsNonJsonReplies() {
        server.route("/audio/transcriptions", request -> FakeGroqServer.Reply.json("nope"));
        server.route("/audio/translations", request -> FakeGroqServer.Reply.json("nope"));

        var service = new AudioServiceImpl(HttpUtils.createHttpClient(), "k", server.baseUrl());
        byte[] audio = {1, 2, 3};

        assertThrows(GroqSerializationException.class, () -> service.createTranscription(
            TranscriptionRequestBuilder.withFile("whisper-large-v3", audio).build()));
        assertThrows(GroqSerializationException.class, () -> service.createTranslation(
            TranslationRequestBuilder.withFile("whisper-large-v3", audio).build()));
    }

    @Test
    void chatSerializesEverySearchSettingAndSkipsEmptyXGroq() {
        server.route("/chat/completions", request -> FakeGroqServer.Reply.json("""
            {"id":"x","object":"chat.completion","model":"m","created":1,
             "choices":[{"index":0,"message":{"role":"assistant","content":"ok"},"finish_reason":"stop"}],
             "x_groq":{"seed":7},
             "usage":{"prompt_tokens":"not-a-number","completion_tokens":null,"total_tokens":3}}"""));

        var request = ChatCompletionRequestBuilder.create("m")
            .userMessage("hi")
            .searchSettings(new SearchSettings(Optional.empty(),
                Optional.of(List.of("spam.com")), Optional.of(true), Optional.empty()))
            .build();

        var response = chat().createCompletion(request);

        String body = server.requestAt("/openai/v1/chat/completions").body();
        assertTrue(body.contains("\"exclude_domains\":[\"spam.com\"]"), body);
        assertTrue(body.contains("\"include_images\":true"), body);
        assertTrue(response.xGroq().isEmpty());
        var usage = response.usage().orElseThrow();
        assertEquals(0, usage.promptTokens());
        assertEquals(0, usage.completionTokens());
        assertEquals(3, usage.totalTokens());
    }

    @Test
    void chatParsesSegmentsWithMissingNumbers() {
        server.route("/audio/transcriptions", request -> FakeGroqServer.Reply.json("""
            {"text":"hi","x_groq":{"id":"req"},
             "segments":[{"text":"hi","tokens":[1,null,2]}],
             "words":[{"word":"hi"}]}"""));

        var response = new AudioServiceImpl(HttpUtils.createHttpClient(), "k", server.baseUrl())
            .createTranscription(TranscriptionRequestBuilder
                .withFile("whisper-large-v3", new byte[] {1}).build());

        var segment = response.segments().getFirst();
        assertEquals(0, segment.id());
        assertEquals("hi", segment.text());
        assertEquals(2, segment.tokens().size());
        assertEquals(1, segment.tokens().getFirst());
        assertEquals(2, segment.tokens().getLast());

        var word = response.words().getFirst();
        assertEquals("hi", word.word());
        assertEquals(0.0, word.start());
        assertEquals(0.0, word.end());
    }

    @Test
    void transcriptionSendsEveryOptionalFieldOnUrlInput() {
        var request = TranscriptionRequestBuilder
            .withUrl("whisper-large-v3", "https://example.com/a.mp3")
            .language("en")
            .prompt("names")
            .responseFormat(ResponseFormat.JSON)
            .temperature(0.2)
            .timestampGranularities(List.of(TimestampGranularity.WORD, TimestampGranularity.SEGMENT))
            .build();

        new AudioServiceImpl(HttpUtils.createHttpClient(), "k", server.baseUrl())
            .createTranscription(request);

        String body = server.requestAt("/openai/v1/audio/transcriptions").body();
        assertTrue(body.contains("\"url\":\"https://example.com/a.mp3\""), body);
        assertTrue(body.contains("\"language\":\"en\""));
        assertTrue(body.contains("\"prompt\":\"names\""));
        assertTrue(body.contains("\"temperature\":0.2"));
        assertTrue(body.contains("\"timestamp_granularities\":[\"word\",\"segment\"]"));
    }

    @Test
    void translationFormSendsEveryOptionalField() {
        var request = TranslationRequestBuilder
            .withFile("whisper-large-v3", "RIFFdata".getBytes(), "clip.wav")
            .prompt("names")
            .temperature(0.4)
            .build();

        new AudioServiceImpl(HttpUtils.createHttpClient(), "k", server.baseUrl())
            .createTranslation(request);

        String body = server.requestAt("/openai/v1/audio/translations").body();
        assertTrue(body.contains("name=\"prompt\""), body);
        assertTrue(body.contains("name=\"temperature\""));
        assertTrue(body.contains("name=\"file\"; filename=\"clip.wav\""));
    }

    @Test
    void translationUrlSendsPromptAndTemperature() {
        var request = TranslationRequestBuilder
            .withUrl("whisper-large-v3", "https://example.com/a.mp3")
            .prompt("names")
            .responseFormat(ResponseFormat.JSON)
            .temperature(0.4)
            .build();

        new AudioServiceImpl(HttpUtils.createHttpClient(), "k", server.baseUrl())
            .createTranslation(request);

        String body = server.requestAt("/openai/v1/audio/translations").body();
        assertTrue(body.contains("\"url\":\"https://example.com/a.mp3\""), body);
        assertTrue(body.contains("\"prompt\":\"names\""));
        assertTrue(body.contains("\"temperature\":0.4"));
    }

    @Test
    void speechSendsFormatSampleRateAndSpeed() {
        var request = SpeechRequestBuilder.create()
            .input("hi")
            .voice("troy")
            .responseFormat(AudioFormat.MP3)
            .sampleRate(SampleRate.RATE_44100)
            .speed(1.5)
            .build();

        new AudioServiceImpl(HttpUtils.createHttpClient(), "k", server.baseUrl()).createSpeech(request);

        String body = server.requestAt("/openai/v1/audio/speech").body();
        assertTrue(body.contains("\"response_format\":\"mp3\""), body);
        assertTrue(body.contains("\"sample_rate\":44100"));
        assertTrue(body.contains("\"speed\":1.5"));
    }

    @Test
    void embeddingsParsesVectorsWithNullEntries() {
        server.route("/embeddings", request -> FakeGroqServer.Reply.json("""
            {"object":"list","model":"m",
             "data":[{"index":0,"embedding":[0.1,null,0.3]}],
             "usage":{"total_tokens":null}}"""));

        var response = new EmbeddingsServiceImpl(HttpUtils.createHttpClient(), "k", server.baseUrl())
            .createEmbeddings(EmbeddingRequestBuilder.create().input("x").user("korn").build());

        assertArrayEquals(new double[] {0.1, 0.0, 0.3}, response.getFirst().vector().orElseThrow());
        assertEquals(0, response.promptTokens());
        assertEquals(0, response.totalTokens());
        assertTrue(server.requestAt("/openai/v1/embeddings").body().contains("\"user\":\"korn\""));
    }

    @Test
    void xGroqWithoutIdIsIgnored() {
        server.route("/audio/transcriptions", request -> FakeGroqServer.Reply.json(
            "{\"text\":\"hi\",\"x_groq\":{\"seed\":1}}"));

        var response = new AudioServiceImpl(HttpUtils.createHttpClient(), "k", server.baseUrl())
            .createTranscription(TranscriptionRequestBuilder
                .withFile("whisper-large-v3", new byte[] {1}).build());

        assertTrue(response.xGroq().isEmpty());
    }

    @Test
    void stringTimestampsAndMissingListFieldsAreTolerated() {
        server.route("/chat/completions", request -> FakeGroqServer.Reply.json("""
            {"id":"x","object":"chat.completion","model":"m","created":"1700000000",
             "choices":[{"index":0,"message":{"role":"assistant","content":"ok"},"finish_reason":"stop"}]}"""));
        server.route("/files", request -> FakeGroqServer.Reply.json("{\"data\":[{\"id\":\"file-1\"}]}"));

        var response = chat().createCompletion(
            ChatCompletionRequestBuilder.create("m").userMessage("hi").build());
        assertEquals(1700000000L, response.created());

        assertEquals("list", new FilesServiceImpl(HttpUtils.createHttpClient(), "k", server.baseUrl())
            .listFiles().object());
    }

    @Test
    void batchListToleratesStringCountsAndNullMetadataValues() {
        server.route("/batches", request -> FakeGroqServer.Reply.json("""
            {"object":"list","data":[{"id":"batch_1","object":"batch","status":"completed",
              "endpoint":"/v1/chat/completions","input_file_id":"file-1","completion_window":"24h",
              "metadata":{"trace":"x","empty":null},
              "request_counts":{"total":"5","completed":3}}]}"""));

        var list = new BatchServiceImpl(HttpUtils.createHttpClient(), "k", server.baseUrl()).listBatches();
        var batch = list.data().getFirst();

        assertEquals(5, batch.requestCounts().total());
        assertEquals(3, batch.requestCounts().completed());
        assertEquals(0, batch.requestCounts().failed());
        assertEquals("x", batch.metadata().orElseThrow().get("trace"));
        assertNull(batch.metadata().orElseThrow().get("empty"));
        assertEquals(0L, batch.createdAt());
        assertFalse(list.hasMore());
    }

    @Test
    void unknownServiceTierAndMissingFingerprintAreTolerated() {
        server.route("/chat/completions", request -> FakeGroqServer.Reply.json("""
            {"id":"x","object":"chat.completion","model":"m","created":1,
             "choices":[{"index":0,"message":{"role":"assistant","content":"ok"},"finish_reason":"stop"}]}"""));

        var response = chat().createCompletion(ChatCompletionRequestBuilder.create("m").userMessage("hi").build());

        assertTrue(response.serviceTier().isEmpty());
        assertTrue(response.xGroq().isEmpty());
        assertTrue(response.systemFingerprint().isEmpty());
        assertFalse(response.hasToolCalls());
    }
}
