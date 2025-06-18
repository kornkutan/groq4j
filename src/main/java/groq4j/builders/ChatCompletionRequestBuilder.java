package groq4j.builders;

import groq4j.enums.MessageRole;
import groq4j.enums.ReasoningEffort;
import groq4j.enums.ReasoningFormat;
import groq4j.enums.ServiceTier;
import groq4j.models.chat.ChatCompletionRequest;
import groq4j.models.chat.JsonSchemaResponseFormat;
import groq4j.models.chat.SearchSettings;
import groq4j.models.chat.ToolChoice;
import groq4j.models.common.Message;
import groq4j.models.common.Tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ChatCompletionRequestBuilder {
    private final List<Message> messages = new ArrayList<>();
    private String model;
    private Double frequencyPenalty;
    private Map<String, Integer> logitBias;
    private Boolean logprobs;
    private Integer maxCompletionTokens;
    private Integer n;
    private Boolean parallelToolCalls;
    private Double presencePenalty;
    private ReasoningEffort reasoningEffort;
    private ReasoningFormat reasoningFormat;
    private JsonSchemaResponseFormat responseFormat;
    private SearchSettings searchSettings;
    private Integer seed;
    private ServiceTier serviceTier;
    private List<String> stop;
    private Double temperature;
    private ToolChoice toolChoice;
    private List<Tool> tools;
    private Integer topLogprobs;
    private Double topP;
    private String user;
    
    public static ChatCompletionRequestBuilder create() {
        return new ChatCompletionRequestBuilder();
    }
    
    public static ChatCompletionRequestBuilder create(String model) {
        return new ChatCompletionRequestBuilder().model(model);
    }
    
    public ChatCompletionRequestBuilder model(String model) {
        this.model = model;
        return this;
    }
    
    public ChatCompletionRequestBuilder addMessage(String role, String content) {
        this.messages.add(Message.of(MessageRole.fromValue(role), content));
        return this;
    }
    
    public ChatCompletionRequestBuilder addMessage(MessageRole role, String content) {
        this.messages.add(Message.of(role, content));
        return this;
    }
    
    public ChatCompletionRequestBuilder addMessage(Message message) {
        this.messages.add(message);
        return this;
    }
    
    public ChatCompletionRequestBuilder messages(List<Message> messages) {
        this.messages.clear();
        if (messages != null) {
            this.messages.addAll(messages);
        }
        return this;
    }
    
    public ChatCompletionRequestBuilder systemMessage(String content) {
        return addMessage(MessageRole.SYSTEM, content);
    }
    
    public ChatCompletionRequestBuilder userMessage(String content) {
        return addMessage(MessageRole.USER, content);
    }
    
    public ChatCompletionRequestBuilder assistantMessage(String content) {
        return addMessage(MessageRole.ASSISTANT, content);
    }
    
    public ChatCompletionRequestBuilder frequencyPenalty(double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
        return this;
    }
    
    public ChatCompletionRequestBuilder logitBias(Map<String, Integer> logitBias) {
        this.logitBias = logitBias;
        return this;
    }
    
    public ChatCompletionRequestBuilder logprobs(boolean logprobs) {
        this.logprobs = logprobs;
        return this;
    }
    
    public ChatCompletionRequestBuilder maxCompletionTokens(int maxCompletionTokens) {
        this.maxCompletionTokens = maxCompletionTokens;
        return this;
    }
    
    public ChatCompletionRequestBuilder n(int n) {
        this.n = n;
        return this;
    }
    
    public ChatCompletionRequestBuilder parallelToolCalls(boolean parallelToolCalls) {
        this.parallelToolCalls = parallelToolCalls;
        return this;
    }
    
    public ChatCompletionRequestBuilder presencePenalty(double presencePenalty) {
        this.presencePenalty = presencePenalty;
        return this;
    }
    
    public ChatCompletionRequestBuilder reasoningEffort(ReasoningEffort reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
        return this;
    }
    
    public ChatCompletionRequestBuilder reasoningFormat(ReasoningFormat reasoningFormat) {
        this.reasoningFormat = reasoningFormat;
        return this;
    }
    
    public ChatCompletionRequestBuilder responseFormat(JsonSchemaResponseFormat responseFormat) {
        this.responseFormat = responseFormat;
        return this;
    }
    
    public ChatCompletionRequestBuilder jsonResponse() {
        return responseFormat(JsonSchemaResponseFormat.jsonObject());
    }
    
    public ChatCompletionRequestBuilder textResponse() {
        return responseFormat(JsonSchemaResponseFormat.text());
    }
    
    public ChatCompletionRequestBuilder jsonSchema(String name, String description, Map<String, Object> schema) {
        return responseFormat(JsonSchemaResponseFormat.jsonSchema(name, description, schema));
    }
    
    public ChatCompletionRequestBuilder strictJsonSchema(String name, String description, Map<String, Object> schema) {
        return responseFormat(JsonSchemaResponseFormat.strictJsonSchema(name, description, schema));
    }
    
    public ChatCompletionRequestBuilder searchSettings(SearchSettings searchSettings) {
        this.searchSettings = searchSettings;
        return this;
    }
    
    public ChatCompletionRequestBuilder seed(int seed) {
        this.seed = seed;
        return this;
    }
    
    public ChatCompletionRequestBuilder serviceTier(ServiceTier serviceTier) {
        this.serviceTier = serviceTier;
        return this;
    }
    
    public ChatCompletionRequestBuilder stop(String... stopSequences) {
        this.stop = List.of(stopSequences);
        return this;
    }
    
    public ChatCompletionRequestBuilder stop(List<String> stop) {
        this.stop = stop;
        return this;
    }
    
    public ChatCompletionRequestBuilder temperature(double temperature) {
        this.temperature = temperature;
        return this;
    }
    
    public ChatCompletionRequestBuilder creative() {
        return temperature(0.9);
    }
    
    public ChatCompletionRequestBuilder balanced() {
        return temperature(0.7);
    }
    
    public ChatCompletionRequestBuilder precise() {
        return temperature(0.1);
    }
    
    public ChatCompletionRequestBuilder toolChoice(ToolChoice toolChoice) {
        this.toolChoice = toolChoice;
        return this;
    }
    
    public ChatCompletionRequestBuilder tools(List<Tool> tools) {
        this.tools = tools;
        return this;
    }
    
    public ChatCompletionRequestBuilder addTool(Tool tool) {
        if (this.tools == null) {
            this.tools = new ArrayList<>();
        }
        this.tools.add(tool);
        return this;
    }
    
    public ChatCompletionRequestBuilder topLogprobs(int topLogprobs) {
        this.topLogprobs = topLogprobs;
        return this;
    }
    
    public ChatCompletionRequestBuilder topP(double topP) {
        this.topP = topP;
        return this;
    }
    
    public ChatCompletionRequestBuilder user(String user) {
        this.user = user;
        return this;
    }
    
    public ChatCompletionRequest build() {
        return new ChatCompletionRequest(
            List.copyOf(messages),
            model,
            Optional.ofNullable(frequencyPenalty),
            Optional.ofNullable(logitBias),
            Optional.ofNullable(logprobs),
            Optional.ofNullable(maxCompletionTokens),
            Optional.ofNullable(n),
            Optional.ofNullable(parallelToolCalls),
            Optional.ofNullable(presencePenalty),
            Optional.ofNullable(reasoningEffort),
            Optional.ofNullable(reasoningFormat),
            Optional.ofNullable(responseFormat),
            Optional.ofNullable(searchSettings),
            Optional.ofNullable(seed),
            Optional.ofNullable(serviceTier),
            Optional.ofNullable(stop),
            Optional.ofNullable(temperature),
            Optional.ofNullable(toolChoice),
            Optional.ofNullable(tools),
            Optional.ofNullable(topLogprobs),
            Optional.ofNullable(topP),
            Optional.ofNullable(user)
        );
    }
}