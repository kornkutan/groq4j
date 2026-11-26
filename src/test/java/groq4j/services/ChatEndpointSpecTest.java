package groq4j.services;

import groq4j.builders.ChatCompletionRequestBuilder;
import groq4j.enums.ReasoningEffort;
import groq4j.models.chat.ChatCompletionChunk;
import groq4j.models.chat.ChatCompletionResponse;
import groq4j.support.FakeGroqServer;
import groq4j.support.FakeServerTestBase;
import groq4j.support.GroqFixtures;
import groq4j.utils.HttpUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Chat completions against a fake Groq server")
class ChatEndpointSpecTest extends FakeServerTestBase {

    private ChatService chat() {
        return new ChatServiceImpl(HttpUtils.createHttpClient(), "test-key", server.baseUrl());
    }

    @Test
    void sendsBlockingCompletionAndParsesEverything() {
        ChatCompletionResponse response = chat().simple("openai/gpt-oss-20b", "say pong");

        assertEquals("chatcmpl-fixture", response.id());
        assertEquals("pong", response.getFirstContent());
        assertEquals("because tests", response.choices().getFirst().message().reasoning().orElse(null));
        assertEquals(16, response.usage().orElseThrow().totalTokens());
        assertEquals(6, response.usage().orElseThrow().cachedTokens().orElse(0));
        assertEquals("req_fixture", response.getRequestId());
        assertFalse(response.hasToolCalls());

        var request = server.requestAt("/openai/v1/chat/completions");
        assertTrue(request.body().contains("\"model\":\"openai/gpt-oss-20b\""));
        assertTrue(request.body().contains("\"role\":\"user\""));
        assertFalse(request.body().contains("\"stream\":true"));
    }

    @Test
    void sendsSystemAndUserPrompt() {
        chat().simple("openai/gpt-oss-20b", "be terse", "say pong");

        String body = server.requestAt("/openai/v1/chat/completions").body();
        assertTrue(body.contains("\"role\":\"system\""));
        assertTrue(body.contains("be terse"));
        assertTrue(body.contains("say pong"));
    }

    @Test
    void streamsChunksAndJoinsThem() {
        var request = ChatCompletionRequestBuilder.create("openai/gpt-oss-20b")
            .userMessage("count")
            .build();

        List<ChatCompletionChunk> chunks;
        try (var stream = chat().streamCompletion(request)) {
            chunks = stream.toList();
        }

        assertEquals(3, chunks.size());
        assertEquals("assistant", chunks.getFirst().choices().getFirst().delta().role().orElse(null));
        assertEquals("", chunks.getFirst().getContent());
        assertEquals("popong", chunks.get(1).getContent());
        assertTrue(chunks.getLast().isFinal());
        assertEquals(5, chunks.getLast().usage().orElseThrow().totalTokens());
        assertEquals("req_stream", chunks.getLast().xGroq().orElseThrow().id());

        var httpRequest = server.requestAt("/openai/v1/chat/completions");
        assertTrue(httpRequest.body().contains("\"stream\":true"));
        assertTrue(httpRequest.body().contains("\"include_usage\":true"));
    }

    @Test
    void parsesToolCallResponses() {
        server.route("/chat/completions", request -> FakeGroqServer.Reply.json(GroqFixtures.TOOL_CALL_COMPLETION));

        var response = chat().simple("openai/gpt-oss-20b", "weather?");

        assertTrue(response.hasToolCalls());
        var call = response.choices().getFirst().message().toolCalls().orElseThrow().getFirst();
        assertEquals("call_1", call.id());
        assertEquals("get_weather", call.function().name());
        assertEquals("{\"city\":\"Bangkok\"}", call.function().arguments());
        assertEquals("tool_calls", response.choices().getFirst().finishReason());
    }

    @Test
    void sendsEveryDocumentedChatParameter() {
        var request = ChatCompletionRequestBuilder.create("openai/gpt-oss-120b")
            .systemMessage("be terse")
            .userMessage("hi")
            .frequencyPenalty(0.1)
            .presencePenalty(0.2)
            .maxCompletionTokens(64)
            .n(1)
            .parallelToolCalls(true)
            .reasoningEffort(ReasoningEffort.LOW)
            .seed(7)
            .stop("\n\n")
            .temperature(0.4)
            .topP(0.9)
            .topLogprobs(2)
            .logprobs(true)
            .user("korn")
            .metadata("trace", "abc")
            .build();

        chat().createCompletion(request);

        String body = server.requestAt("/openai/v1/chat/completions").body();
        for (String expected : List.of("frequency_penalty", "presence_penalty", "max_completion_tokens",
                "\"n\":1", "parallel_tool_calls", "\"reasoning_effort\":\"low\"", "\"seed\":7",
                "\"stop\":", "temperature", "top_p", "top_logprobs", "\"logprobs\":true",
                "\"user\":\"korn\"", "\"metadata\":{\"trace\":\"abc\"}")) {
            assertTrue(body.contains(expected), "missing " + expected + " in " + body);
        }
    }

    @Test
    void surfacesApiErrorsFromTheRealWire() {
        server.route("/chat/completions", request -> FakeGroqServer.Reply.error(
            429, "slow down", "rate_limit_error", "rate_limit_exceeded"));

        var thrown = assertThrows(groq4j.exceptions.GroqRateLimitException.class,
            () -> chat().simple("openai/gpt-oss-20b", "hi"));

        assertEquals(429, thrown.getStatusCode());
        assertEquals("rate_limit_exceeded", thrown.getErrorCode());
        assertEquals("slow down", thrown.getMessage());
    }
}
