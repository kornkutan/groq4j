package groq4j.services;

import groq4j.builders.ChatCompletionRequestBuilder;
import groq4j.enums.HttpMethod;
import groq4j.enums.ServiceTier;
import groq4j.exceptions.GroqSerializationException;
import groq4j.models.chat.Annotation;
import groq4j.models.chat.ChatCompletionChunk;
import groq4j.models.chat.ChatCompletionDocument;
import groq4j.models.chat.ChatCompletionRequest;
import groq4j.models.chat.ChatCompletionResponse;
import groq4j.models.chat.Choice;
import groq4j.models.chat.ExecutedTool;
import groq4j.models.chat.McpToolList;
import groq4j.models.chat.ChunkChoice;
import groq4j.models.chat.Delta;
import groq4j.models.chat.LogProbs;
import groq4j.models.chat.SearchSettings;
import groq4j.models.chat.ToolCallDelta;
import groq4j.models.chat.ToolChoice;
import groq4j.models.common.Message;
import groq4j.models.common.Tool;
import groq4j.models.common.Usage;
import groq4j.models.common.XGroq;
import groq4j.utils.Constants;
import groq4j.utils.HttpUtils;
import groq4j.utils.JsonUtils;
import groq4j.utils.SseUtils;
import groq4j.utils.UrlUtils;
import groq4j.utils.ValidationUtils;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/** Chat completions over the Groq OpenAI compatible endpoint. */
public class ChatServiceImpl implements ChatService {
    private final HttpClient httpClient;
    private final String apiKey;

    public ChatServiceImpl(String apiKey) {
        this.httpClient = HttpUtils.createHttpClient();
        this.apiKey = apiKey;
        ValidationUtils.validateApiKey(apiKey);
    }

    public ChatServiceImpl(HttpClient httpClient, String apiKey) {
        ValidationUtils.requireNonNull(httpClient, "httpClient");
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        ValidationUtils.validateApiKey(apiKey);
    }

    public static ChatService create(HttpClient httpClient, String apiKey) {
        return new ChatServiceImpl(httpClient, apiKey);
    }

    public static ChatService create(String apiKey) {
        return new ChatServiceImpl(apiKey);
    }

    @Override
    public ChatCompletionResponse createCompletion(ChatCompletionRequest request) {
        ValidationUtils.requireNonNull(request, "request");
        String body = JsonUtils.toJsonString(buildRequestBody(request, false));
        var httpRequest = HttpUtils.createJsonRequest(
            UrlUtils.buildChatCompletionsUrl(Constants.BASE_URL), apiKey, HttpMethod.POST, body);

        return HttpUtils.await(HttpUtils.executeRequest(httpClient, httpRequest)
                .thenApply(this::parseCompletionResponse));
    }

    @Override
    public Stream<ChatCompletionChunk> streamCompletion(ChatCompletionRequest request) {
        ValidationUtils.requireNonNull(request, "request");
        String body = JsonUtils.toJsonString(buildRequestBody(request, true));
        var httpRequest = HttpUtils.createStreamingRequest(
            UrlUtils.buildChatCompletionsUrl(Constants.BASE_URL), apiKey, body);

        return SseUtils.payloads(HttpUtils.openEventStream(httpClient, httpRequest))
            .map(this::parseChunk);
    }

    @Override
    public ChatCompletionResponse simple(String model, String message) {
        ValidationUtils.validateModel(model);
        ValidationUtils.requireNonNull(message, "message");

        return createCompletion(ChatCompletionRequestBuilder.create(model)
            .userMessage(message)
            .build());
    }

    @Override
    public ChatCompletionResponse simple(String model, String systemPrompt, String userMessage) {
        ValidationUtils.validateModel(model);
        ValidationUtils.requireNonNull(systemPrompt, "systemPrompt");
        ValidationUtils.requireNonNull(userMessage, "userMessage");

        return createCompletion(ChatCompletionRequestBuilder.create(model)
            .systemMessage(systemPrompt)
            .userMessage(userMessage)
            .build());
    }

    Map<String, Object> buildRequestBody(ChatCompletionRequest request, boolean stream) {
        var body = new LinkedHashMap<String, Object>();

        body.put("model", request.model());
        body.put("messages", request.messages().stream().map(Message::toMap).toList());

        request.frequencyPenalty().ifPresent(v -> body.put("frequency_penalty", v));
        request.logitBias().ifPresent(v -> body.put("logit_bias", v));
        request.logprobs().ifPresent(v -> body.put("logprobs", v));
        request.maxCompletionTokens().ifPresent(v -> body.put("max_completion_tokens", v));
        request.n().ifPresent(v -> body.put("n", v));
        request.parallelToolCalls().ifPresent(v -> body.put("parallel_tool_calls", v));
        request.presencePenalty().ifPresent(v -> body.put("presence_penalty", v));
        request.reasoningEffort().ifPresent(v -> body.put("reasoning_effort", v.getValue()));
        request.reasoningFormat().ifPresent(v -> body.put("reasoning_format", v.getValue()));
        request.responseFormat().ifPresent(v -> body.put("response_format", v.toMap()));
        request.searchSettings().ifPresent(v -> body.put("search_settings", searchSettingsToMap(v)));
        request.seed().ifPresent(v -> body.put("seed", v));
        request.serviceTier().ifPresent(v -> body.put("service_tier", v.getValue()));
        request.stop().ifPresent(v -> body.put("stop", v));
        request.temperature().ifPresent(v -> body.put("temperature", v));
        request.toolChoice().ifPresent(v -> body.put("tool_choice", toolChoiceToJson(v)));
        request.tools().ifPresent(v -> body.put("tools", v.stream().map(Tool::toMap).toList()));
        request.topLogprobs().ifPresent(v -> body.put("top_logprobs", v));
        request.topP().ifPresent(v -> body.put("top_p", v));
        request.user().ifPresent(v -> body.put("user", v));
        request.citationOptions().ifPresent(v -> body.put("citation_options", v.getValue()));
        request.documents().ifPresent(v -> body.put("documents", v.stream().map(ChatCompletionDocument::toMap).toList()));
        request.disableToolValidation().ifPresent(v -> body.put("disable_tool_validation", v));
        request.includeReasoning().ifPresent(v -> body.put("include_reasoning", v));
        request.metadata().ifPresent(v -> body.put("metadata", v));

        if (stream) {
            body.put("stream", true);
            body.put("stream_options", Map.of("include_usage", true));
        }

        return body;
    }

    private Map<String, Object> searchSettingsToMap(SearchSettings settings) {
        var map = new LinkedHashMap<String, Object>();
        settings.includeDomains().ifPresent(v -> map.put("include_domains", v));
        settings.excludeDomains().ifPresent(v -> map.put("exclude_domains", v));
        settings.includeImages().ifPresent(v -> map.put("include_images", v));
        settings.country().ifPresent(v -> map.put("country", v));
        return map;
    }

    private Object toolChoiceToJson(ToolChoice toolChoice) {
        if (toolChoice.isSpecificFunction()) {
            return Map.of(
                "type", "function",
                "function", Map.of("name", toolChoice.getFunctionName()));
        }
        return toolChoice.type().getValue();
    }

    ChatCompletionResponse parseCompletionResponse(String json) {
        try {
            Map<String, Object> root = JsonUtils.parseObject(json);
            return new ChatCompletionResponse(
                requiredString(root, "id"),
                requiredString(root, "object"),
                longValue(root.get("created")),
                requiredString(root, "model"),
                parseChoices(root.get("choices")),
                parseUsage(JsonUtils.asMap(root.get("usage"))),
                Optional.ofNullable(JsonUtils.stringValue(root.get("system_fingerprint"))),
                parseXGroq(root.get("x_groq")),
                parseServiceTier(root.get("service_tier")),
                parseExecutedTools(root.get("executed_tools")),
                parseMcpToolLists(root.get("mcp_list_tools")));
        } catch (GroqSerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new GroqSerializationException("Failed to parse chat completion response: " + e.getMessage(), e);
        }
    }

    private List<ExecutedTool> parseExecutedTools(Object rawTools) {
        List<ExecutedTool> tools = new ArrayList<>();
        for (Object raw : JsonUtils.asList(rawTools)) {
            tools.add(ExecutedTool.fromMap(JsonUtils.asMap(raw)));
        }
        return tools;
    }

    private List<McpToolList> parseMcpToolLists(Object rawLists) {
        List<McpToolList> lists = new ArrayList<>();
        for (Object raw : JsonUtils.asList(rawLists)) {
            lists.add(McpToolList.fromMap(JsonUtils.asMap(raw)));
        }
        return lists;
    }

    ChatCompletionChunk parseChunk(String json) {
        try {
            Map<String, Object> root = JsonUtils.parseObject(json);
            List<ChunkChoice> choices = new ArrayList<>();
            for (Object raw : JsonUtils.asList(root.get("choices"))) {
                Map<String, Object> choice = JsonUtils.asMap(raw);
                choices.add(new ChunkChoice(
                    intValue(choice.get("index")),
                    Delta.fromMap(JsonUtils.asMap(choice.get("delta"))),
                    Optional.ofNullable(JsonUtils.stringValue(choice.get("finish_reason")))));
            }
            return new ChatCompletionChunk(
                JsonUtils.stringValue(root.get("id")),
                JsonUtils.stringValue(root.get("object")),
                longValue(root.get("created")),
                JsonUtils.stringValue(root.get("model")),
                choices,
                Optional.ofNullable(JsonUtils.stringValue(root.get("system_fingerprint"))),
                parseUsage(JsonUtils.asMap(root.get("usage"))),
                parseXGroq(root.get("x_groq")),
                parseServiceTier(root.get("service_tier")));
        } catch (Exception e) {
            throw new GroqSerializationException("Failed to parse streamed chunk: " + e.getMessage(), e);
        }
    }

    private List<Choice> parseChoices(Object rawChoices) {
        List<Choice> choices = new ArrayList<>();
        for (Object raw : JsonUtils.asList(rawChoices)) {
            Map<String, Object> choice = JsonUtils.asMap(raw);
            choices.add(new Choice(
                intValue(choice.get("index")),
                Message.fromMap(JsonUtils.asMap(choice.get("message"))),
                Optional.ofNullable(choice.get("logprobs"))
                    .filter(value -> value instanceof Map<?, ?>)
                    .map(value -> LogProbs.fromMap(JsonUtils.asMap(value))),
                JsonUtils.stringValue(choice.get("finish_reason"))));
        }
        return choices;
    }

    private Optional<Usage> parseUsage(Map<String, Object> usage) {
        if (usage.isEmpty()) {
            return Optional.empty();
        }
        Map<String, Object> promptDetails = JsonUtils.asMap(usage.get("prompt_tokens_details"));
        Map<String, Object> completionDetails = JsonUtils.asMap(usage.get("completion_tokens_details"));
        return Optional.of(new Usage(
            doubleValue(usage.get("queue_time")),
            intValue(usage.get("prompt_tokens")),
            doubleValue(usage.get("prompt_time")),
            intValue(usage.get("completion_tokens")),
            doubleValue(usage.get("completion_time")),
            intValue(usage.get("total_tokens")),
            doubleValue(usage.get("total_time")),
            Optional.ofNullable(promptDetails.get("cached_tokens")).map(JsonUtils::doubleValue).map(Double::intValue),
            Optional.ofNullable(completionDetails.get("reasoning_tokens")).map(JsonUtils::doubleValue).map(Double::intValue)));
    }

    private Optional<XGroq> parseXGroq(Object rawXGroq) {
        if (!(rawXGroq instanceof Map<?, ?>)) {
            return Optional.empty();
        }
        Map<String, Object> xGroq = JsonUtils.asMap(rawXGroq);
        String id = JsonUtils.stringValue(xGroq.get("id"));
        if (id == null) {
            return Optional.empty();
        }
        return Optional.of(new XGroq(id,
            Optional.ofNullable(xGroq.get("seed")).map(JsonUtils::doubleValue).map(Double::intValue)));
    }

    private Optional<ServiceTier> parseServiceTier(Object value) {
        String tier = JsonUtils.stringValue(value);
        if (tier == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(ServiceTier.fromValue(tier));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String requiredString(Map<String, Object> map, String key) {
        String value = JsonUtils.stringValue(map.get(key));
        if (value == null) {
            throw GroqSerializationException.invalidResponseFormat("Required field missing: " + key);
        }
        return value;
    }

    private double doubleValue(Object value) {
        Double number = JsonUtils.doubleValue(value);
        return number == null ? 0.0 : number;
    }

    private int intValue(Object value) {
        Double number = JsonUtils.doubleValue(value);
        return number == null ? 0 : number.intValue();
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        Double number = JsonUtils.doubleValue(value);
        return number == null ? 0L : number.longValue();
    }
}
