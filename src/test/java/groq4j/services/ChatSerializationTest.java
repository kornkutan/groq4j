package groq4j.services;

import groq4j.builders.ChatCompletionRequestBuilder;
import groq4j.enums.ReasoningEffort;
import groq4j.enums.ReasoningFormat;
import groq4j.enums.ServiceTier;
import groq4j.models.chat.ChatCompletionChunk;
import groq4j.models.chat.ChatCompletionRequest;
import groq4j.models.chat.ChatCompletionResponse;
import groq4j.utils.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Chat request building and response parsing")
class ChatSerializationTest {

    private final ChatServiceImpl service = new ChatServiceImpl("test-key");

    @Test
    void buildsBodyWithNewOptionalFields() {
        ChatCompletionRequest request = ChatCompletionRequestBuilder.create()
            .model("openai/gpt-oss-120b")
            .userMessage("hi")
            .reasoningEffort(ReasoningEffort.HIGH)
            .serviceTier(ServiceTier.PERFORMANCE)
            .includeReasoning(true)
            .disableToolValidation(true)
            .metadata(Map.of("trace", "abc"))
            .maxCompletionTokens(256)
            .temperature(0.3)
            .build();

        Map<String, Object> body = service.buildRequestBody(request, false);

        assertEquals("openai/gpt-oss-120b", body.get("model"));
        assertEquals("high", body.get("reasoning_effort"));
        assertEquals("performance", body.get("service_tier"));
        assertEquals(Boolean.TRUE, body.get("include_reasoning"));
        assertEquals(Boolean.TRUE, body.get("disable_tool_validation"));
        assertEquals(Map.of("trace", "abc"), body.get("metadata"));
        assertEquals(256, body.get("max_completion_tokens"));
        assertEquals(0.3, body.get("temperature"));
        assertFalse(body.containsKey("stream"), "stream flag must be absent for blocking calls");
        assertFalse(body.containsKey("citation_options"));
    }

    @Test
    void refusesIncludeReasoningWithReasoningFormat() {
        var builder = ChatCompletionRequestBuilder.create()
            .model("openai/gpt-oss-20b")
            .userMessage("hi")
            .includeReasoning(true);

        assertThrows(IllegalArgumentException.class, () -> builder.reasoningFormat(ReasoningFormat.PARSED).build());
    }

    @Test
    void addsStreamFlagAndOptions() {
        ChatCompletionRequest request = ChatCompletionRequestBuilder.create()
            .model("openai/gpt-oss-20b")
            .userMessage("hi")
            .build();

        Map<String, Object> body = service.buildRequestBody(request, true);

        assertEquals(Boolean.TRUE, body.get("stream"));
        assertEquals(Map.of("include_usage", true), body.get("stream_options"));
    }

    @Test
    void parsesDocumentsIntoSingleTextSource() {
        ChatCompletionRequest request = ChatCompletionRequestBuilder.create()
            .model("openai/gpt-oss-20b")
            .userMessage("summary?")
            .addTextDocument("doc-1", "Groq builds LPUs.")
            .build();

        Map<String, Object> body = service.buildRequestBody(request, false);
        List<Object> documents = JsonUtils.asList(body.get("documents"));

        assertEquals(1, documents.size());
        Map<String, Object> document = JsonUtils.asMap(documents.getFirst());
        assertEquals("doc-1", document.get("id"));
        assertEquals("Groq builds LPUs.", JsonUtils.node(document, "source.text"));
        assertEquals("text", JsonUtils.node(document, "source.type"));
    }

    @Test
    void parsesChatCompletionResponse() {
        String json = """
            {"id":"chatcmpl-1","object":"chat.completion","created":1700000000,
             "model":"openai/gpt-oss-20b","service_tier":"default",
             "choices":[{"index":0,"finish_reason":"stop","logprobs":null,
               "message":{"role":"assistant","content":"hello","reasoning":"thinking"}}],
             "usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15,
               "queue_time":0.01,"prompt_time":0.02,"completion_time":0.03,"total_time":0.06,
               "completion_tokens_details":{"reasoning_tokens":2}},
             "x_groq":{"id":"req_1"}}
            """;

        ChatCompletionResponse response = service.parseCompletionResponse(json);

        assertEquals("chatcmpl-1", response.id());
        assertEquals("openai/gpt-oss-20b", response.model());
        assertEquals(ServiceTier.DEFAULT, response.serviceTier().orElse(null));
        assertEquals("req_1", response.getRequestId());
        assertEquals("hello", response.choices().getFirst().message().content().orElse(null));
        assertEquals(15, response.usage().orElseThrow().totalTokens());
        assertEquals(2, response.usage().orElseThrow().reasoningTokens().orElse(0));
        assertFalse(response.hasToolCalls());
    }

    @Test
    void parsesExecutedToolsAndMcpLists() {
        String json = """
            {"id":"chatcmpl-3","object":"chat.completion","created":1700000000,
             "model":"openai/gpt-oss-120b","service_tier":"on_demand",
             "choices":[{"index":0,"finish_reason":"stop","message":{"role":"assistant","content":"x"}}],
             "executed_tools":[{"index":0,"type":"browser_search","arguments":"q=lpU",
               "output":"LPU is a chip","search_results":{"results":[{"title":"Groq","url":"https://groq.com"}]}}],
             "mcp_list_tools":[{"id":"list_1","server_label":"docs","tools":[
               {"name":"lookup","description":"Look up a doc","input_schema":{"type":"object"}}]}],
             "usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}}
            """;

        ChatCompletionResponse response = service.parseCompletionResponse(json);

        assertTrue(response.hasExecutedTools());
        assertEquals("browser_search", response.executedTools().getFirst().type());
        assertEquals("LPU is a chip", response.executedTools().getFirst().output().orElseThrow());
        assertEquals("docs", response.mcpToolLists().getFirst().serverLabel().orElseThrow());
        assertEquals("lookup", response.mcpToolLists().getFirst().tools().getFirst().name());
    }

    @Test
    void parsesStreamingChunk() {
        String json = """
            {"id":"chatcmpl-2","object":"chat.completion.chunk","created":1700000000,
             "model":"openai/gpt-oss-20b",
             "choices":[{"index":0,"delta":{"content":"Hel","role":"assistant"},"finish_reason":null}]}
            """;

        ChatCompletionChunk chunk = service.parseChunk(json);

        assertEquals("chatcmpl-2", chunk.id());
        assertEquals("Hel", chunk.choices().getFirst().delta().content().orElse(null));
        assertFalse(chunk.hasToolCalls());
        assertFalse(chunk.isFinal());
    }

    @Test
    void parsesFinalChunkWithUsage() {
        String json = """
            {"id":"chatcmpl-2","object":"chat.completion.chunk","created":1700000000,
             "model":"openai/gpt-oss-20b","choices":[{"index":0,"delta":{},"finish_reason":"stop"}],
             "usage":{"prompt_tokens":3,"completion_tokens":1,"total_tokens":4},
             "x_groq":{"id":"req_2"}}
            """;

        ChatCompletionChunk chunk = service.parseChunk(json);

        assertTrue(chunk.isFinal());
        assertEquals("stop", chunk.choices().getFirst().finishReason().orElse(null));
        assertEquals(4, chunk.usage().orElseThrow().totalTokens());
        assertEquals("req_2", chunk.xGroq().orElseThrow().id());
    }

    @Test
    void parsesToolCallDelta() {
        String json = """
            {"id":"c","object":"chat.completion.chunk","created":1,"model":"m",
             "choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"id":"call_1","type":"function",
               "function":{"name":"get_weather","arguments":"{\\"ci"}}]},"finish_reason":null}]}
            """;

        ChatCompletionChunk chunk = service.parseChunk(json);

        assertTrue(chunk.hasToolCalls());
        var delta = chunk.choices().getFirst().delta().toolCalls().getFirst();
        assertEquals("call_1", delta.id().orElse(null));
        assertEquals("get_weather", delta.functionName().orElse(null));
        assertEquals("{\"ci", delta.arguments().orElse(null));
    }
}
