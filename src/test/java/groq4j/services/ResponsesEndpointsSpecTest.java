package groq4j.services;

import groq4j.builders.EmbeddingRequestBuilder;
import groq4j.builders.RerankRequestBuilder;
import groq4j.builders.ResponseRequestBuilder;
import groq4j.enums.EmbeddingEncodingFormat;
import groq4j.models.embeddings.EmbeddingResponse;
import groq4j.models.reranking.RerankResponse;
import groq4j.models.responses.ResponseObject;
import groq4j.models.responses.ResponseStreamEvent;
import groq4j.support.FakeGroqServer;
import groq4j.support.FakeServerTestBase;
import groq4j.support.GroqFixtures;
import groq4j.utils.HttpUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Responses, embeddings and reranking against a fake Groq server")
class ResponsesEndpointsSpecTest extends FakeServerTestBase {

    private ResponsesService responses() {
        return new ResponsesServiceImpl(HttpUtils.createHttpClient(), "test-key", server.baseUrl());
    }

    private EmbeddingsService embeddings() {
        return new EmbeddingsServiceImpl(HttpUtils.createHttpClient(), "test-key", server.baseUrl());
    }

    private RerankingService reranking() {
        return new RerankingServiceImpl(HttpUtils.createHttpClient(), "test-key", server.baseUrl());
    }

    @Test
    void createsResponseAndReadsTextAndReasoning() {
        var request = ResponseRequestBuilder.create("openai/gpt-oss-20b")
            .input("say pong")
            .instructions("be brief")
            .metadata("suite", "unit")
            .maxOutputTokens(64)
            .build();

        ResponseObject response = responses().createResponse(request);

        assertEquals("resp_fixture", response.id());
        assertEquals("completed", response.status());
        assertEquals("pong", response.getOutputText());
        assertEquals("short answer", response.getReasoningText());
        assertEquals(24, response.usage().orElseThrow().totalTokens());
        assertEquals(7, response.usage().orElseThrow().reasoningTokens());
        assertFalse(response.hasError());
        assertEquals("unit", response.metadata().get("suite"));

        String body = server.requestAt("/openai/v1/responses").body();
        assertTrue(body.contains("\"input\":\"say pong\""));
        assertTrue(body.contains("\"instructions\":\"be brief\""));
        assertTrue(body.contains("\"max_output_tokens\":64"));
        assertFalse(body.contains("\"stream\":true"));
    }

    @Test
    void createsResponseFromMessageItems() {
        var request = ResponseRequestBuilder.create("openai/gpt-oss-20b")
            .systemMessage("be brief")
            .userMessage("say pong")
            .build();

        responses().createResponse(request);

        String body = server.requestAt("/openai/v1/responses").body();
        assertTrue(body.contains("\"input\":[{"));
        assertTrue(body.contains("\"role\":\"system\""));
        assertTrue(body.contains("\"role\":\"user\""));
    }

    @Test
    void streamsResponseEvents() {
        var request = ResponseRequestBuilder.create("openai/gpt-oss-20b").input("say pong").build();

        List<ResponseStreamEvent> events;
        try (var stream = responses().streamResponse(request)) {
            events = stream.toList();
        }

        assertEquals(4, events.size());
        assertEquals("response.created", events.getFirst().type());
        assertEquals("pong", events.stream()
            .filter(ResponseStreamEvent::isTextDelta)
            .map(ResponseStreamEvent::delta)
            .reduce("", String::concat));
        assertTrue(events.getLast().isCompleted());
        assertTrue(events.getLast().isTerminal());
        assertTrue(server.requestAt("/openai/v1/responses").body().contains("\"stream\":true"));
    }

    @Test
    void parsesFunctionCallsFromResponses() {
        server.route("/responses", request -> FakeGroqServer.Reply.json(GroqFixtures.RESPONSE_WITH_TOOL_CALL));

        var response = responses().createResponse(
            ResponseRequestBuilder.create("openai/gpt-oss-20b").input("weather?").build());

        assertEquals(1, response.getFunctionCalls().size());
        assertEquals("get_weather", response.getFunctionCalls().getFirst().name().orElse(null));
        assertEquals("", response.getOutputText());
    }

    @Test
    void createsEmbeddingsForOneAndManyInputs() {
        EmbeddingResponse many = embeddings().createEmbeddings(
            EmbeddingRequestBuilder.create().input(List.of("first", "second")).build());
        EmbeddingResponse single = embeddings().embed("nomic-embed-text-v1_5", "only");

        assertEquals(2, many.data().size());
        assertEquals(3, many.getFirst().dimensions());
        assertEquals(0.22, many.getFirst().vector().orElseThrow()[1], 1e-9);
        assertEquals(6, many.totalTokens());
        assertEquals("list", single.object());

        String body = server.requestAt("/openai/v1/embeddings").body();
        assertTrue(body.contains("\"input\":\"only\""), "single input must be sent as a string: " + body);
    }

    @Test
    void sendsBase64EncodingFormat() {
        embeddings().createEmbeddings(EmbeddingRequestBuilder.create().input("x").base64Encoding().build());

        String body = server.requestAt("/openai/v1/embeddings").body();
        assertEquals(EmbeddingEncodingFormat.BASE64, EmbeddingEncodingFormat.fromValue("base64"));
        assertTrue(body.contains("\"encoding_format\":\"base64\""));
    }

    @Test
    void reranksDocuments() {
        RerankResponse response = reranking().rerank(
            RerankRequestBuilder.create()
                .query("what is an LPU?")
                .documents(List.of("LPUs are deterministic chips.", "Bread needs yeast."))
                .instruction("score relevance")
                .build());

        assertEquals(2, response.results().size());
        assertEquals(0.91, response.results().getFirst().score(), 1e-9);
        assertEquals("LPUs are deterministic chips.", response.results().getFirst().document());

        String body = server.requestAt("/openai/v1/reranking").body();
        assertTrue(body.contains("\"docs\":["));
        assertTrue(body.contains("\"instruction\":\"score relevance\""));
    }
}
