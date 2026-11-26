package groq4j.services;

import groq4j.builders.ChatCompletionRequestBuilder;
import groq4j.builders.EmbeddingRequestBuilder;
import groq4j.builders.RerankRequestBuilder;
import groq4j.builders.ResponseRequestBuilder;
import groq4j.builders.SpeechRequestBuilder;
import groq4j.builders.TranscriptionRequestBuilder;
import groq4j.builders.TranslationRequestBuilder;
import groq4j.enums.ReasoningFormat;
import groq4j.enums.ResponseFormat;
import groq4j.enums.ServiceTier;
import groq4j.enums.TtsVoice;
import groq4j.exceptions.GroqSerializationException;
import groq4j.models.batch.BatchRequest;
import groq4j.models.chat.ChatCompletionRequest;
import groq4j.models.chat.ChatCompletionResponse;
import groq4j.models.chat.ToolChoice;
import groq4j.models.common.Message;
import groq4j.models.common.Tool;
import groq4j.models.responses.ResponseObject;
import groq4j.models.responses.ResponseStreamEvent;
import groq4j.models.responses.ResponseTextFormat;
import groq4j.support.FakeGroqServer;
import groq4j.support.FakeServerTestBase;
import groq4j.utils.HttpUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives the request builders and parsers through every branch the happy path misses: all
 * optional parameters set, every optional response field absent, and unparseable replies.
 */
@DisplayName("Optional request fields and degraded responses")
class ServiceBranchSpecTest extends FakeServerTestBase {

    private HttpClient http() {
        return HttpUtils.createHttpClient();
    }

    private String base() {
        return server.baseUrl();
    }

    @Test
    void everyThreeArgumentFactoryWorks() {
        var client = http();
        assertNotNull(ChatServiceImpl.create(client, "k", base()));
        assertNotNull(ResponsesServiceImpl.create(client, "k", base()));
        assertNotNull(EmbeddingsServiceImpl.create(client, "k", base()));
        assertNotNull(RerankingServiceImpl.create(client, "k", base()));
        assertNotNull(AudioServiceImpl.create(client, "k", base()));
        assertNotNull(ModelsServiceImpl.create(client, "k", base()));
        assertNotNull(BatchServiceImpl.create(client, "k", base()));
        assertNotNull(FilesServiceImpl.create(client, "k", base()));
    }

    @Test
    void chatSendsSearchSettingsToolsAndCitations() {
        ChatCompletionRequest request = ChatCompletionRequestBuilder.create("openai/gpt-oss-120b")
            .userMessage("search and cite")
            .searchSettings(groq4j.models.chat.SearchSettings.includeDomains("groq.com")
                .withCountry("thailand"))
            .addTool(Tool.function("get_weather", "weather", java.util.Optional.of(Map.of("type", "object"))))
            .toolChoice(ToolChoice.function("get_weather"))
            .citationOptions(groq4j.enums.CitationOptions.ENABLED)
            .addTextDocument("doc-1", "Groq builds LPUs.")
            .disableToolValidation(true)
            .build();

        new ChatServiceImpl(http(), "k", base()).createCompletion(request);

        String body = server.requestAt("/openai/v1/chat/completions").body();
        assertTrue(body.contains("\"search_settings\":{\"include_domains\":[\"groq.com\"],\"country\":\"thailand\""), body);
        assertTrue(body.contains("\"tool_choice\":"), body);
        assertTrue(body.contains("\"function\":{\"name\":\"get_weather\"}"), body);
        assertTrue(body.contains("\"citation_options\":\"enabled\""), body);
        assertTrue(body.contains("\"documents\":"), body);
        assertTrue(body.contains("\"disable_tool_validation\":true"));
    }

    @Test
    void chatSendsNonFunctionToolChoiceAndReasoningFormat() {
        ChatCompletionRequest request = ChatCompletionRequestBuilder.create("openai/gpt-oss-120b")
            .userMessage("hi")
            .toolChoice(ToolChoice.auto())
            .reasoningFormat(ReasoningFormat.HIDDEN)
            .responseFormat(groq4j.models.chat.JsonSchemaResponseFormat.jsonSchema("name", "desc", Map.of("type", "object")))
            .logitBias(Map.of("123", 4))
            .stop(List.of("\n"))
            .build();

        new ChatServiceImpl(http(), "k", base()).createCompletion(request);

        String body = server.requestAt("/openai/v1/chat/completions").body();
        assertTrue(body.contains("\"tool_choice\":\"auto\""));
        assertTrue(body.contains("\"reasoning_format\":\"hidden\""));
        assertTrue(body.contains("\"response_format\":"));
        assertTrue(body.contains("\"logit_bias\":{\"123\":4}"));
    }

    @Test
    void chatParsesLogprobsSeedAndUnknownTier() {
        server.route("/chat/completions", request -> FakeGroqServer.Reply.json("""
            {"id":"x","object":"chat.completion","created":1700000000,"model":"m",
             "service_tier":"does_not_exist",
             "choices":[{"index":0,"finish_reason":"stop",
               "logprobs":{"content":[{"token":"po","logprob":-0.1,"bytes":[112],"top_logprobs":[]}],
                           "refusal":null},
               "message":{"role":"assistant","content":"po"}}],
             "x_groq":{"id":"req","seed":42}}"""));

        ChatCompletionResponse response = new ChatServiceImpl(http(), "k", base()).simple("m", "hi");

        assertTrue(response.serviceTier().isEmpty());
        assertEquals(42, response.xGroq().orElseThrow().seed().orElseThrow());
        assertEquals("po", response.choices().getFirst().logprobs().orElseThrow()
            .content().getFirst().token());
        assertTrue(response.usage().isEmpty());
    }

    @Test
    void chatRejectsAResponseWithoutId() {
        server.route("/chat/completions", request -> FakeGroqServer.Reply.json(
            "{\"object\":\"chat.completion\",\"model\":\"m\",\"choices\":[],\"created\":1}"));

        assertThrows(GroqSerializationException.class,
            () -> new ChatServiceImpl(http(), "k", base()).simple("m", "hi"));
    }

    @Test
    void chatStreamRejectsMalformedChunks() {
        server.route("/chat/completions", request -> FakeGroqServer.Reply.events(
            "data: not-json\n\ndata: [DONE]\n\n"));

        var service = new ChatServiceImpl(http(), "k", base());
        var request = ChatCompletionRequestBuilder.create("m").userMessage("hi").build();

        assertThrows(GroqSerializationException.class, () -> {
            try (var stream = service.streamCompletion(request)) {
                stream.toList();
            }
        });
    }

    @Test
    void responsesSendsEveryOptionalField() {
        var request = ResponseRequestBuilder.create("openai/gpt-oss-120b")
            .userMessage("hi")
            .reasoningEffort(groq4j.enums.ReasoningEffort.MEDIUM)
            .serviceTier(ServiceTier.FLEX)
            .temperature(0.5)
            .topP(0.8)
            .truncation("auto")
            .user("korn")
            .parallelToolCalls(false)
            .textFormat(ResponseTextFormat.jsonSchema("city", Map.of("type", "object")))
            .toolChoice(groq4j.models.responses.ResponseTool.function("f", "d", Map.of("type", "object")))
            .addTool(groq4j.models.responses.ResponseTool.function("g", "d2", Map.of("type", "object")))
            .build();

        new ResponsesServiceImpl(http(), "k", base()).createResponse(request);

        String body = server.requestAt("/openai/v1/responses").body();
        assertTrue(body.contains("\"reasoning\":{\"effort\":\"medium\"}"));
        assertTrue(body.contains("\"service_tier\":\"flex\""));
        assertTrue(body.contains("\"truncation\":\"auto\""));
        assertTrue(body.contains("\"text\":{\"format\":"));
        assertTrue(body.contains("\"tool_choice\":"));
        assertTrue(body.contains("\"tools\":["));
        assertTrue(body.contains("\"parallel_tool_calls\":false"));
    }

    @Test
    void responsesHandlesMissingOptionalFields() {
        server.route("/responses", request -> FakeGroqServer.Reply.json("""
            {"id":"resp_min","object":"response","status":"incomplete","model":"m","created_at":1,
             "output":[]}"""));

        ResponseObject response = new ResponsesServiceImpl(http(), "k", base())
            .createResponse(ResponseRequestBuilder.create("m").input("x").build());

        assertTrue(response.usage().isEmpty());
        assertTrue(response.instructions().isEmpty());
        assertTrue(response.metadata().isEmpty());
        assertEquals("", response.getOutputText());
        assertTrue(response.getFunctionCalls().isEmpty());
        assertFalse(response.isCompleted());
    }

    @Test
    void responsesStreamRejectsMalformedEvents() {
        server.route("/responses", request -> FakeGroqServer.Reply.events("data: {broken\n\ndata: [DONE]\n\n"));

        var service = new ResponsesServiceImpl(http(), "k", base());
        var request = ResponseRequestBuilder.create("m").input("x").build();

        assertThrows(GroqSerializationException.class, () -> {
            try (var stream = service.streamResponse(request)) {
                stream.toList();
            }
        });
    }

    @Test
    void responsesEventWithoutDeltaHasNoText() {
        server.route("/responses", request -> FakeGroqServer.Reply.events(
            "data: {\"type\":\"response.created\",\"response\":{\"id\":\"r\"}}\n\ndata: [DONE]\n\n"));

        var service = new ResponsesServiceImpl(http(), "k", base());
        var request = ResponseRequestBuilder.create("m").input("x").build();

        try (var stream = service.streamResponse(request)) {
            ResponseStreamEvent event = stream.findFirst().orElseThrow();
            assertEquals("response.created", event.type());
            assertNull(event.delta());
            assertFalse(event.isFailed());
        }
    }

    @Test
    void embeddingsHandlesBase64AndMissingUsage() {
        server.route("/embeddings", request -> FakeGroqServer.Reply.json("""
            {"object":"list","model":"nomic-embed-text-v1_5",
             "data":[{"object":"embedding","index":0,"embedding":"c29tZWJhc2U2NA=="}]}"""));

        var response = new EmbeddingsServiceImpl(http(), "k", base()).embed("m", "x");

        assertEquals("c29tZWJhc2U2NA==", response.getFirst().base64().orElseThrow());
        assertEquals(0, response.getFirst().dimensions());
        assertTrue(response.getFirst().vector().isEmpty());
        assertEquals(0, response.totalTokens());
    }

    @Test
    void embeddingsHandlesEmptyData() {
        server.route("/embeddings", request -> FakeGroqServer.Reply.json(
            "{\"object\":\"list\",\"model\":\"m\",\"data\":[],\"usage\":{\"prompt_tokens\":0,\"total_tokens\":0}}"));

        var response = new EmbeddingsServiceImpl(http(), "k", base()).embed("m", "x");

        assertNull(response.getFirst());
    }

    @Test
    void rerankingDefaultsMissingFields() {
        server.route("/reranking", request -> FakeGroqServer.Reply.json("{\"results\":[{}]}"));

        var response = new RerankingServiceImpl(http(), "k", base()).rerank(
            RerankRequestBuilder.create().query("q").documents("d").build());

        assertEquals(1, response.results().size());
        assertNull(response.results().getFirst().document());
        assertEquals(0.0, response.results().getFirst().score());
    }

    @Test
    void audioHandlesMinimalRequestsAndResponses() {
        server.route("/audio/transcriptions", request -> FakeGroqServer.Reply.json("{\"text\":\"hi\"}"));
        server.route("/audio/translations", request -> FakeGroqServer.Reply.json("{\"text\":\"hi\"}"));

        var service = new AudioServiceImpl(http(), "k", base());

        var transcription = service.createTranscription(
            TranscriptionRequestBuilder.withFile("whisper-large-v3", "RIFF".getBytes()).build());
        var translation = service.createTranslation(
            TranslationRequestBuilder.withFile("whisper-large-v3", "RIFF".getBytes()).build());

        assertEquals("hi", transcription.text());
        assertTrue(transcription.xGroq().isEmpty());
        assertFalse(transcription.hasTimestamps());
        assertFalse(transcription.hasGroqMetadata());
        assertEquals("hi", translation.text());
        assertTrue(translation.language().isEmpty());
        assertTrue(translation.isEmpty() == false);

        var request = server.requestAt("/openai/v1/audio/transcriptions");
        assertTrue(request.body().contains("filename=\"audio.wav\""), "default filename when none given");
        assertFalse(request.body().contains("timestamp_granularities"));
    }

    @Test
    void audioSpeechWithoutOptionalFields() {
        new AudioServiceImpl(http(), "k", base()).createSpeech(
            SpeechRequestBuilder.create().input("hello").voice(TtsVoice.HANNAH).build());

        String body = server.requestAt("/openai/v1/audio/speech").body();
        assertFalse(body.contains("response_format"), "server default wav is used when unset");
        assertFalse(body.contains("speed"));
        assertFalse(body.contains("sample_rate"));
    }

    @Test
    void filesParseDegradedPayloads() {
        server.route("/files", request -> FakeGroqServer.Reply.json("""
            {"object":"list","data":[{"id":"file-x"}]}"""));

        var list = new FilesServiceImpl(http(), "k", base()).listFiles();

        assertEquals(1, list.getTotalCount());
        var file = list.data().getFirst();
        assertEquals("file", file.object());
        assertEquals(0, file.bytes());
        assertEquals("unknown", file.filename());
        assertEquals(groq4j.enums.FilePurpose.BATCH, file.purpose());
    }

    @Test
    void filesHandleEmptyListsAndMinimalDeletes() {
        server.route("/files", request -> FakeGroqServer.Reply.json("{\"object\":\"list\",\"data\":[]}"));
        server.route("/files/", request -> FakeGroqServer.Reply.json("{\"id\":\"file-1\"}"));

        var service = new FilesServiceImpl(http(), "k", base());

        assertTrue(service.listFiles().isEmpty());
        var deleted = service.deleteFile("file-1");
        assertFalse(deleted.deleted());
        assertEquals("file", deleted.object());
    }

    @Test
    void filesRejectPayloadsWithoutId() {
        server.route("/files", request -> FakeGroqServer.Reply.json("{\"object\":\"list\",\"data\":[{\"filename\":\"x\"}]}"));
        server.route("/files/", request -> FakeGroqServer.Reply.json("{\"object\":\"file\"}"));

        var service = new FilesServiceImpl(http(), "k", base());

        assertThrows(GroqSerializationException.class, service::listFiles);
        assertThrows(GroqSerializationException.class, () -> service.retrieveFile("file-1"));
        assertThrows(GroqSerializationException.class, () -> service.deleteFile("file-1"));
    }

    @Test
    void filesExposeTheirOwnState() {
        var service = new FilesServiceImpl(http(), "k", base());

        assertNotNull(service.getHttpClient());
        assertFalse(service.ownsHttpClient());
        assertEquals("***", service.getMaskedApiKey());
        assertEquals("test***-key", new FilesServiceImpl("test-key").getMaskedApiKey().substring(0, 7) + "-key");
    }

    @Test
    void batchesWithoutMetadataOrCounts() {
        server.route("/batches", request -> FakeGroqServer.Reply.json("""
            {"id":"batch_min","object":"batch","endpoint":"/v1/chat/completions",
             "input_file_id":"file-1","completion_window":"24h","status":"completed",
             "created_at":1700000000,"expires_at":1700086400,"metadata":null}"""));

        var service = new BatchServiceImpl(http(), "k", base());
        var created = service.createBatch(new BatchRequest("file-1", "/v1/chat/completions", "24h",
            java.util.Optional.empty()));

        assertEquals("batch_min", created.id());
        assertEquals(0, created.requestCounts().total());
        assertTrue(created.outputFileId().isEmpty());
        assertTrue(created.cancellingAt().isEmpty());
        assertFalse(server.requestAt("/openai/v1/batches").body().contains("\"metadata\":{\""));
    }

    @Test
    void batchesHandleEmptyListAndMissingId() {
        server.route("/batches", request -> FakeGroqServer.Reply.json("{\"object\":\"list\",\"data\":[]}"));
        var service = new BatchServiceImpl(http(), "k", base());
        assertTrue(service.listBatches().isEmpty());

        server.route("/batches/batch_1", request -> FakeGroqServer.Reply.json(
            "{\"object\":\"batch\",\"status\":\"completed\"}"));
        assertThrows(GroqSerializationException.class, () -> service.retrieveBatch("batch_1"));
    }

    @Test
    void modelsRejectPayloadWithoutId() {
        server.route("/models", request -> FakeGroqServer.Reply.json("{\"object\":\"list\",\"data\":[{}]}"));
        assertThrows(GroqSerializationException.class,
            () -> new ModelsServiceImpl(http(), "k", base()).listModels());
    }

    @Test
    void messagesWithImagePartsAreSerialized() {
        var request = ChatCompletionRequestBuilder.create("m")
            .addMessage(Message.userWithImages("what is this?", "https://example.com/a.png"))
            .build();

        new ChatServiceImpl(http(), "k", base()).createCompletion(request);

        String body = server.requestAt("/openai/v1/chat/completions").body();
        assertTrue(body.contains("\"type\":\"image_url\""));
        assertTrue(body.contains("https://example.com/a.png"));
    }

    @Test
    void transcriptionWithExplicitFormatAndNoGranularity() {
        new AudioServiceImpl(http(), "k", base()).createTranscription(
            TranscriptionRequestBuilder.withFile("whisper-large-v3", new byte[] {1, 2}, "a.mp3")
                .responseFormat(ResponseFormat.TEXT)
                .build());

        String body = server.requestAt("/openai/v1/audio/transcriptions").body();
        assertTrue(body.contains("Content-Type: audio/mpeg"));
        assertTrue(body.contains("name=\"response_format\""));
    }

    @Test
    void embeddingBuilderRejectsEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> EmbeddingRequestBuilder.create().build());
        assertThrows(IllegalArgumentException.class, () -> RerankRequestBuilder.create().build());
        assertNotNull(ResponseStreamEvent.class);
    }
}
