package groq4j.services;

import groq4j.enums.HttpMethod;
import groq4j.exceptions.GroqSerializationException;
import groq4j.models.responses.ResponseInputItem;
import groq4j.models.responses.ResponseObject;
import groq4j.models.responses.ResponseOutputItem;
import groq4j.models.responses.ResponseRequest;
import groq4j.models.responses.ResponseStreamEvent;
import groq4j.models.responses.ResponseTool;
import groq4j.models.responses.ResponseUsage;
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

/** Implementation of the Responses API. */
public class ResponsesServiceImpl implements ResponsesService {
    private final HttpClient httpClient;
    private final String apiKey;

    public ResponsesServiceImpl(String apiKey) {
        this.httpClient = HttpUtils.createHttpClient();
        this.apiKey = apiKey;
        ValidationUtils.validateApiKey(apiKey);
    }

    public ResponsesServiceImpl(HttpClient httpClient, String apiKey) {
        ValidationUtils.requireNonNull(httpClient, "httpClient");
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        ValidationUtils.validateApiKey(apiKey);
    }

    public static ResponsesService create(String apiKey) {
        return new ResponsesServiceImpl(apiKey);
    }

    public static ResponsesService create(HttpClient httpClient, String apiKey) {
        return new ResponsesServiceImpl(httpClient, apiKey);
    }

    @Override
    public ResponseObject createResponse(ResponseRequest request) {
        ValidationUtils.requireNonNull(request, "request");
        String body = JsonUtils.toJsonString(buildRequestBody(request, false));
        var httpRequest = HttpUtils.createJsonRequest(
            UrlUtils.buildResponsesUrl(Constants.BASE_URL), apiKey, HttpMethod.POST, body);

        return HttpUtils.await(HttpUtils.executeRequest(httpClient, httpRequest)
                .thenApply(this::parseResponse));
    }

    @Override
    public Stream<ResponseStreamEvent> streamResponse(ResponseRequest request) {
        ValidationUtils.requireNonNull(request, "request");
        String body = JsonUtils.toJsonString(buildRequestBody(request, true));
        var httpRequest = HttpUtils.createStreamingRequest(
            UrlUtils.buildResponsesUrl(Constants.BASE_URL), apiKey, body);

        return SseUtils.payloads(HttpUtils.openEventStream(httpClient, httpRequest))
            .map(this::parseEvent);
    }

    Map<String, Object> buildRequestBody(ResponseRequest request, boolean stream) {
        var body = new LinkedHashMap<String, Object>();
        body.put("model", request.model());

        if (request.inputText().isPresent()) {
            body.put("input", request.inputText().get());
        } else {
            List<Map<String, Object>> items = new ArrayList<>();
            for (ResponseInputItem item : request.inputItems()) {
                items.add(item.toMap());
            }
            body.put("input", items);
        }

        request.instructions().ifPresent(v -> body.put("instructions", v));
        request.maxOutputTokens().ifPresent(v -> body.put("max_output_tokens", v));
        request.metadata().ifPresent(v -> body.put("metadata", v));
        request.parallelToolCalls().ifPresent(v -> body.put("parallel_tool_calls", v));
        request.reasoningEffort().ifPresent(v -> body.put("reasoning", Map.of("effort", v.getValue())));
        request.serviceTier().ifPresent(v -> body.put("service_tier", v.getValue()));
        request.temperature().ifPresent(v -> body.put("temperature", v));
        request.textFormat().ifPresent(v -> body.put("text", Map.of("format", v.toMap())));
        request.toolChoice().ifPresent(v -> body.put("tool_choice", v));
        request.topP().ifPresent(v -> body.put("top_p", v));
        request.truncation().ifPresent(v -> body.put("truncation", v));
        request.user().ifPresent(v -> body.put("user", v));

        if (!request.tools().isEmpty()) {
            body.put("tools", request.tools().stream().map(ResponseTool::toMap).toList());
        }
        if (stream) {
            body.put("stream", true);
        }

        return body;
    }

    ResponseObject parseResponse(String json) {
        try {
            Map<String, Object> root = JsonUtils.parseObject(json);
            List<ResponseOutputItem> output = new ArrayList<>();
            for (Object raw : JsonUtils.asList(root.get("output"))) {
                output.add(ResponseOutputItem.fromMap(JsonUtils.asMap(raw)));
            }

            Optional<ResponseUsage> usage = Optional.empty();
            if (root.get("usage") instanceof Map<?, ?> rawUsage) {
                Map<String, Object> u = JsonUtils.asMap(rawUsage);
                Map<String, Object> inputDetails = JsonUtils.asMap(u.get("input_tokens_details"));
                Map<String, Object> outputDetails = JsonUtils.asMap(u.get("output_tokens_details"));
                usage = Optional.of(new ResponseUsage(
                    intValue(u.get("input_tokens")),
                    intValue(u.get("output_tokens")),
                    intValue(u.get("total_tokens")),
                    intValue(inputDetails.get("cached_tokens")),
                    intValue(outputDetails.get("reasoning_tokens"))));
            }

            Optional<String> errorMessage = Optional.empty();
            if (root.get("error") instanceof Map<?, ?> rawError) {
                errorMessage = Optional.ofNullable(JsonUtils.stringValue(JsonUtils.asMap(rawError).get("message")));
            }

            return new ResponseObject(
                JsonUtils.stringValue(root.get("id")),
                JsonUtils.stringValue(root.get("object")),
                JsonUtils.stringValue(root.get("status")),
                longValue(root.get("created_at")),
                JsonUtils.stringValue(root.get("model")),
                output,
                usage,
                errorMessage,
                Optional.ofNullable(JsonUtils.stringValue(root.get("instructions"))),
                JsonUtils.asMap(root.get("metadata")),
                root);
        } catch (GroqSerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new GroqSerializationException("Failed to parse response: " + e.getMessage(), e);
        }
    }

    ResponseStreamEvent parseEvent(String json) {
        try {
            Map<String, Object> root = JsonUtils.parseObject(json);
            String type = JsonUtils.stringValue(root.get("type"));
            String delta = JsonUtils.stringValue(root.get("delta"));
            return new ResponseStreamEvent(type, delta, root);
        } catch (Exception e) {
            throw new GroqSerializationException("Failed to parse stream event: " + e.getMessage(), e);
        }
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
