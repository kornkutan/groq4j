package groq4j.services;

import groq4j.builders.EmbeddingRequestBuilder;
import groq4j.builders.RerankRequestBuilder;
import groq4j.builders.ResponseRequestBuilder;
import groq4j.models.embeddings.EmbeddingResponse;
import groq4j.models.reranking.RerankResponse;
import groq4j.models.responses.ResponseObject;
import groq4j.models.responses.ResponseStreamEvent;
import groq4j.utils.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Responses, embeddings and reranking parsing")
class EndpointParsingTest {

    private final ResponsesServiceImpl responses = new ResponsesServiceImpl("test-key");
    private final EmbeddingsServiceImpl embeddings = new EmbeddingsServiceImpl("test-key");
    private final RerankingServiceImpl reranking = new RerankingServiceImpl("test-key");

    @Test
    void buildsResponsesBodyFromTextInput() {
        var request = ResponseRequestBuilder.create("openai/gpt-oss-20b")
            .input("Explain LPUs in one line")
            .instructions("Be brief")
            .maxOutputTokens(120)
            .temperature(0.2)
            .build();

        Map<String, Object> body = responses.buildRequestBody(request, false);

        assertEquals("openai/gpt-oss-20b", body.get("model"));
        assertEquals("Explain LPUs in one line", body.get("input"));
        assertEquals("Be brief", body.get("instructions"));
        assertEquals(120, body.get("max_output_tokens"));
        assertFalse(body.containsKey("stream"));
    }

    @Test
    void buildsResponsesBodyWithToolsAndJsonFormat() {
        var request = ResponseRequestBuilder.create("openai/gpt-oss-20b")
            .userMessage("weather in Bangkok?")
            .addTool(groq4j.models.responses.ResponseTool.function("get_weather", "Get weather", Map.of(
                "type", "object",
                "properties", Map.of("city", Map.of("type", "string")),
                "required", List.of("city"))))
            .textFormat(groq4j.models.responses.ResponseTextFormat.jsonObject())
            .build();

        Map<String, Object> body = responses.buildRequestBody(request, true);
        List<Object> input = JsonUtils.asList(body.get("input"));
        List<Object> tools = JsonUtils.asList(body.get("tools"));

        assertEquals(Boolean.TRUE, body.get("stream"));
        assertEquals(1, input.size());
        assertEquals("user", JsonUtils.node(input.getFirst(), "role"));
        assertEquals(1, tools.size());
        assertEquals("function", JsonUtils.node(tools.getFirst(), "type"));
        assertEquals("get_weather", JsonUtils.node(tools.getFirst(), "name"));
        assertEquals("json_object", JsonUtils.node(body, "text.format.type"));
    }

    @Test
    void parsesResponseObject() {
        String json = """
            {"id":"resp_1","object":"response","created_at":1700000000,"status":"completed",
             "model":"openai/gpt-oss-20b","instructions":"be brief","metadata":{"k":"v"},
             "output":[{"type":"message","id":"msg_1","role":"assistant","status":"completed",
               "content":[{"type":"output_text","text":"An LPU is a deterministic chip.","annotations":[]}]}],
             "usage":{"input_tokens":20,"output_tokens":10,"total_tokens":30,
               "input_tokens_details":{"cached_tokens":0},
               "output_tokens_details":{"reasoning_tokens":4}}}
            """;

        ResponseObject response = responses.parseResponse(json);

        assertEquals("resp_1", response.id());
        assertEquals("completed", response.status());
        assertEquals("v", response.metadata().get("k"));
        assertEquals("An LPU is a deterministic chip.", response.getOutputText());
        assertEquals(30, response.usage().orElseThrow().totalTokens());
        assertEquals(4, response.usage().orElseThrow().reasoningTokens());
        assertFalse(response.hasError());
    }

    @Test
    void parsesResponseError() {
        String json = """
            {"id":"resp_2","object":"response","status":"failed","model":"m","created_at":1,
             "output":[],"error":{"code":"server_error","message":"boom"}}
            """;

        ResponseObject response = responses.parseResponse(json);

        assertTrue(response.hasError());
        assertEquals("boom", response.errorMessage().orElseThrow());
    }

    @Test
    void parsesStreamEvents() {
        ResponseStreamEvent delta = responses.parseEvent(
            "{\"type\":\"response.output_text.delta\",\"delta\":\"Hel\",\"item_id\":\"msg_1\"}");
        ResponseStreamEvent done = responses.parseEvent(
            "{\"type\":\"response.completed\",\"response\":{\"id\":\"resp_1\"}}");

        assertEquals("response.output_text.delta", delta.type());
        assertEquals("Hel", delta.delta());
        assertTrue(delta.isTextDelta());
        assertTrue(done.isTerminal());
    }

    @Test
    void buildsEmbeddingsBodyAndParsesVectors() {
        var request = EmbeddingRequestBuilder.create()
            .input(List.of("first", "second"))
            .build();

        assertEquals("nomic-embed-text-v1_5", request.model());

        EmbeddingResponse parsed = embeddings.parseResponse("""
            {"object":"list","model":"nomic-embed-text-v1_5",
             "data":[{"object":"embedding","index":0,"embedding":[0.1,0.2,0.3]},
                     {"object":"embedding","index":1,"embedding":[0.4,0.5,0.6]}],
             "usage":{"prompt_tokens":4,"total_tokens":4}}
            """);

        assertEquals(2, parsed.data().size());
        assertEquals(3, parsed.getFirst().dimensions());
        assertEquals(0.2, parsed.getFirst().vector().orElseThrow()[1], 1e-9);
        assertEquals(4, parsed.totalTokens());
    }

    @Test
    void parsesRerankingResults() {
        var request = RerankRequestBuilder.create()
            .query("what is an LPU?")
            .documents("LPUs are deterministic.", "Nothing to do with chips.")
            .build();

        assertEquals("qwen3-reranker-4b", request.model());
        assertEquals(2, request.documents().size());

        RerankResponse parsed = reranking.parseResponse("""
            {"results":[{"index":0,"doc":"LPUs are deterministic.","score":0.93},
                        {"index":1,"doc":"Nothing to do with chips.","score":0.02}]}
            """);

        assertEquals(2, parsed.results().size());
        assertEquals(0.93, parsed.results().getFirst().score(), 1e-9);
        assertEquals("LPUs are deterministic.", parsed.results().getFirst().document());
    }
}
