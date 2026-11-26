package groq4j.builders;

import groq4j.enums.ReasoningEffort;
import groq4j.enums.ServiceTier;
import groq4j.models.responses.ResponseInputItem;
import groq4j.models.responses.ResponseRequest;
import groq4j.models.responses.ResponseTextFormat;
import groq4j.models.responses.ResponseTool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Fluent builder for {@link ResponseRequest}. */
public class ResponseRequestBuilder {
    private String model;
    private String inputText;
    private final List<ResponseInputItem> inputItems = new ArrayList<>();
    private String instructions;
    private Integer maxOutputTokens;
    private Map<String, String> metadata;
    private Boolean parallelToolCalls;
    private ReasoningEffort reasoningEffort;
    private ServiceTier serviceTier;
    private Double temperature;
    private ResponseTextFormat textFormat;
    private Object toolChoice;
    private final List<ResponseTool> tools = new ArrayList<>();
    private Double topP;
    private String truncation;
    private String user;

    public static ResponseRequestBuilder create(String model) {
        return new ResponseRequestBuilder().model(model);
    }

    public ResponseRequestBuilder model(String model) {
        this.model = model;
        return this;
    }

    public ResponseRequestBuilder input(String input) {
        this.inputText = input;
        return this;
    }

    public ResponseRequestBuilder instructions(String instructions) {
        this.instructions = instructions;
        return this;
    }

    public ResponseRequestBuilder addItem(ResponseInputItem item) {
        this.inputItems.add(item);
        return this;
    }

    public ResponseRequestBuilder userMessage(String text) {
        return addItem(ResponseInputItem.message("user", text));
    }

    public ResponseRequestBuilder systemMessage(String text) {
        return addItem(ResponseInputItem.message("system", text));
    }

    public ResponseRequestBuilder assistantMessage(String text) {
        return addItem(ResponseInputItem.message("assistant", text));
    }

    public ResponseRequestBuilder functionCallOutput(String callId, String output) {
        return addItem(ResponseInputItem.functionCallOutput(callId, output));
    }

    public ResponseRequestBuilder maxOutputTokens(int maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
        return this;
    }

    public ResponseRequestBuilder metadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    public ResponseRequestBuilder metadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }

    public ResponseRequestBuilder parallelToolCalls(boolean parallelToolCalls) {
        this.parallelToolCalls = parallelToolCalls;
        return this;
    }

    public ResponseRequestBuilder reasoningEffort(ReasoningEffort reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
        return this;
    }

    public ResponseRequestBuilder serviceTier(ServiceTier serviceTier) {
        this.serviceTier = serviceTier;
        return this;
    }

    public ResponseRequestBuilder temperature(double temperature) {
        this.temperature = temperature;
        return this;
    }

    public ResponseRequestBuilder textFormat(ResponseTextFormat textFormat) {
        this.textFormat = textFormat;
        return this;
    }

    public ResponseRequestBuilder jsonSchema(String name, Map<String, Object> schema) {
        return textFormat(ResponseTextFormat.jsonSchema(name, schema));
    }

    public ResponseRequestBuilder toolChoice(String toolChoice) {
        this.toolChoice = toolChoice;
        return this;
    }

    public ResponseRequestBuilder toolChoice(ResponseTool tool) {
        this.toolChoice = Map.of("type", "function", "name", tool.name());
        return this;
    }

    public ResponseRequestBuilder addTool(ResponseTool tool) {
        this.tools.add(tool);
        return this;
    }

    public ResponseRequestBuilder topP(double topP) {
        this.topP = topP;
        return this;
    }

    public ResponseRequestBuilder truncation(String truncation) {
        this.truncation = truncation;
        return this;
    }

    public ResponseRequestBuilder user(String user) {
        this.user = user;
        return this;
    }

    public ResponseRequest build() {
        return new ResponseRequest(
            model,
            Optional.ofNullable(inputText),
            List.copyOf(inputItems),
            Optional.ofNullable(instructions),
            Optional.ofNullable(maxOutputTokens),
            Optional.ofNullable(metadata),
            Optional.ofNullable(parallelToolCalls),
            Optional.ofNullable(reasoningEffort),
            Optional.ofNullable(serviceTier),
            Optional.ofNullable(temperature),
            Optional.ofNullable(textFormat),
            Optional.ofNullable(toolChoice),
            List.copyOf(tools),
            Optional.ofNullable(topP),
            Optional.ofNullable(truncation),
            Optional.ofNullable(user));
    }
}
