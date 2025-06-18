package groq4j.services;

import groq4j.builders.ChatCompletionRequestBuilder;
import groq4j.enums.HttpMethod;
import groq4j.models.chat.ChatCompletionRequest;
import groq4j.models.chat.ChatCompletionResponse;
import groq4j.models.common.Message;
import groq4j.utils.Constants;
import groq4j.utils.HttpUtils;
import groq4j.utils.ResponseParser;
import groq4j.utils.UrlUtils;
import groq4j.utils.ValidationUtils;

import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of ChatService that communicates with the Groq API.
 * 
 * Uses HTTP/2 client for efficient communication and provides both simple
 * and advanced chat completion functionality.
 */
public class ChatServiceImpl implements ChatService {
    private final HttpClient httpClient;
    private final String apiKey;
    private final boolean ownsHttpClient;

    /**
     * Creates ChatService with default HttpClient (convenient for quick usage).
     * Uses sensible defaults but less flexible than providing your own HttpClient.
     */
    public ChatServiceImpl(String apiKey) {
        this.httpClient = HttpUtils.createHttpClient();
        this.apiKey = apiKey;
        this.ownsHttpClient = true;
        
        ValidationUtils.validateApiKey(apiKey);
    }

    /**
     * Creates ChatService with custom HttpClient (recommended for robust production deployments).
     * Allows full control over HTTP configuration and enables testing with mocked clients.
     */
    public ChatServiceImpl(HttpClient httpClient, String apiKey) {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        this.ownsHttpClient = false;

        ValidationUtils.validateApiKey(apiKey);
    }

    /**
     * Static factory method for creating ChatService with custom HttpClient.
     * Recommended for production use and testing.
     */
    public static ChatService create(HttpClient httpClient, String apiKey) {
        return new ChatServiceImpl(httpClient, apiKey);
    }
    
    /**
     * Static factory method for creating ChatService with default settings.
     * Convenient for quick prototyping and simple use cases.
     */
    public static ChatService create(String apiKey) {
        return new ChatServiceImpl(apiKey);
    }
    
    @Override
    public ChatCompletionResponse createCompletion(ChatCompletionRequest request) {
        ValidationUtils.requireNonNull(request, "request");
        
        String requestJson = buildChatCompletionRequestJson(request);
        
        String url = UrlUtils.buildChatCompletionsUrl(Constants.BASE_URL);
        var httpRequest = HttpUtils.createJsonRequest(url, apiKey, HttpMethod.POST, requestJson);
        
        return HttpUtils.executeRequest(httpClient, httpRequest)
            .thenApply(this::parseChatCompletionResponse)
            .join(); // Block for synchronous response
    }
    
    @Override
    public ChatCompletionResponse simple(String model, String message) {
        ValidationUtils.validateModel(model);
        ValidationUtils.requireNonNull(message, "message");
        
        var request = ChatCompletionRequestBuilder.create(model)
            .userMessage(message)
            .build();
            
        return createCompletion(request);
    }
    
    @Override
    public ChatCompletionResponse simple(String model, String systemPrompt, String userMessage) {
        ValidationUtils.validateModel(model);
        ValidationUtils.requireNonNull(systemPrompt, "systemPrompt");
        ValidationUtils.requireNonNull(userMessage, "userMessage");
        
        var request = ChatCompletionRequestBuilder.create(model)
            .systemMessage(systemPrompt)
            .userMessage(userMessage)
            .build();
            
        return createCompletion(request);
    }
    
    private String buildChatCompletionRequestJson(ChatCompletionRequest request) {
        var requestMap = new HashMap<String, Object>();
        
        // Required fields
        requestMap.put("model", request.model());
        requestMap.put("messages", request.messages().stream()
            .map(Message::toMap)
            .toList());
        
        // Optional fields
        request.frequencyPenalty().ifPresent(fp -> requestMap.put("frequency_penalty", fp));
        request.logitBias().ifPresent(lb -> requestMap.put("logit_bias", lb));
        request.logprobs().ifPresent(lp -> requestMap.put("logprobs", lp));
        request.maxCompletionTokens().ifPresent(mct -> requestMap.put("max_completion_tokens", mct));
        request.n().ifPresent(n -> requestMap.put("n", n));
        request.parallelToolCalls().ifPresent(ptc -> requestMap.put("parallel_tool_calls", ptc));
        request.presencePenalty().ifPresent(pp -> requestMap.put("presence_penalty", pp));
        request.reasoningEffort().ifPresent(re -> requestMap.put("reasoning_effort", re.getValue()));
        request.reasoningFormat().ifPresent(rf -> requestMap.put("reasoning_format", rf.getValue()));
        request.responseFormat().ifPresent(rf -> requestMap.put("response_format", rf.toMap()));
        request.searchSettings().ifPresent(ss -> requestMap.put("search_settings", searchSettingsToMap(ss)));
        request.seed().ifPresent(s -> requestMap.put("seed", s));
        request.serviceTier().ifPresent(st -> requestMap.put("service_tier", st.getValue()));
        request.stop().ifPresent(stop -> requestMap.put("stop", stop));
        request.temperature().ifPresent(t -> requestMap.put("temperature", t));
        request.toolChoice().ifPresent(tc -> requestMap.put("tool_choice", toolChoiceToMap(tc)));
        request.tools().ifPresent(tools -> requestMap.put("tools", tools.stream()
            .map(tool -> tool.toMap())
            .toList()));
        request.topLogprobs().ifPresent(tlp -> requestMap.put("top_logprobs", tlp));
        request.topP().ifPresent(tp -> requestMap.put("top_p", tp));
        request.user().ifPresent(u -> requestMap.put("user", u));
        
        return groq4j.utils.JsonUtils.toJsonString(requestMap);
    }
    
    private Map<String, Object> searchSettingsToMap(groq4j.models.chat.SearchSettings searchSettings) {
        var map = new HashMap<String, Object>();
        searchSettings.includeDomains().ifPresent(domains -> map.put("include_domains", domains));
        searchSettings.excludeDomains().ifPresent(domains -> map.put("exclude_domains", domains));
        searchSettings.includeImages().ifPresent(images -> map.put("include_images", images));
        searchSettings.maxResults().ifPresent(max -> map.put("max_results", max));
        searchSettings.region().ifPresent(region -> map.put("region", region));
        searchSettings.timeRange().ifPresent(range -> map.put("time_range", range));
        searchSettings.safeSearch().ifPresent(safe -> map.put("safe_search", safe));
        return map;
    }
    
    private Map<String, Object> toolChoiceToMap(groq4j.models.chat.ToolChoice toolChoice) {
        if (toolChoice.isNone() || toolChoice.isAuto() || toolChoice.isRequired()) {
            return Map.of("type", toolChoice.type().getValue());
        } else if (toolChoice.isSpecificFunction()) {
            return Map.of(
                "type", "function",
                "function", Map.of("name", toolChoice.getFunctionName())
            );
        }
        throw new IllegalStateException("Unknown tool choice type: " + toolChoice.type());
    }
    
    private ChatCompletionResponse parseChatCompletionResponse(String responseJson) {
        try {
            String id = ResponseParser.getRequiredString(responseJson, "id");
            String object = ResponseParser.getRequiredString(responseJson, "object");
            long created = ResponseParser.getRequiredLong(responseJson, "created");
            String model = ResponseParser.getRequiredString(responseJson, "model");
            
            // Parse choices array manually since getRequiredArray doesn't exist
            var choices = parseChoicesArray(responseJson);

            var usage = parseUsage(responseJson);
            
            var systemFingerprint = ResponseParser.getOptionalString(responseJson, "system_fingerprint");
            var xGroq = ResponseParser.getOptionalString(responseJson, "x_groq")
                .map(xGroqJson -> new groq4j.models.common.XGroq(
                    ResponseParser.getRequiredString(xGroqJson, "id")
                ));
            
            return new ChatCompletionResponse(id, object, created, model, choices, usage, systemFingerprint, xGroq);
            
        } catch (Exception e) {
            throw new groq4j.exceptions.GroqSerializationException("Failed to parse chat completion response: " + e.getMessage(), e);
        }
    }
    
    private groq4j.models.chat.Choice parseChoice(String choiceJson) {
        int index = ResponseParser.getRequiredInt(choiceJson, "index");
        var message = parseMessage(ResponseParser.getRequiredString(choiceJson, "message"));
        var logprobs = ResponseParser.getOptionalString(choiceJson, "logprobs")
            .map(this::parseLogProbs);
        String finishReason = ResponseParser.getRequiredString(choiceJson, "finish_reason");
        
        return new groq4j.models.chat.Choice(index, message, logprobs, finishReason);
    }
    
    private Message parseMessage(String messageJson) {
        return Message.fromJson(messageJson, "");
    }
    
    private groq4j.models.chat.LogProbs parseLogProbs(String logprobsJson) {
        // TODO: Implement LogProbs parsing when needed
        return null;
    }
    
    private java.util.List<groq4j.models.chat.Choice> parseChoicesArray(String responseJson) {
        var choices = new java.util.ArrayList<groq4j.models.chat.Choice>();
        
        // FXIME: For now, implement a simple manual parser for the first choice
        // This is a temporary solution until we have proper JSON parsing
        try {
            // Find the choices array
            String choicesMarker = "\"choices\":[";
            int choicesStart = responseJson.indexOf(choicesMarker);
            if (choicesStart == -1) {
                return choices;
            }
            
            // Extract the first choice object
            int firstChoiceStart = responseJson.indexOf("{", choicesStart + choicesMarker.length());
            if (firstChoiceStart == -1) {
                return choices;
            }
            
            // Find the end of the first choice (look for the closing brace)
            int braceCount = 1;
            int firstChoiceEnd = firstChoiceStart + 1;
            while (firstChoiceEnd < responseJson.length() && braceCount > 0) {
                char c = responseJson.charAt(firstChoiceEnd);
                if (c == '{') braceCount++;
                else if (c == '}') braceCount--;
                firstChoiceEnd++;
            }
            
            String firstChoiceJson = responseJson.substring(firstChoiceStart, firstChoiceEnd);
            // Parse the basic fields from the choice
            int index = 0; // Default to 0
            String content = extractSimpleStringValue(firstChoiceJson, "content");
            String finishReason = extractSimpleStringValue(firstChoiceJson, "finish_reason");
            
            if (content != null && finishReason != null) {
                var message = Message.assistant(content);
                choices.add(new groq4j.models.chat.Choice(index, message, java.util.Optional.empty(), finishReason));
            }
            
        } catch (Exception e) {
            // Log error but don't fail completely
        }
        
        return choices;
    }
    
    private String extractSimpleStringValue(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return null;
        
        int valueStart = start + pattern.length();
        int valueEnd = json.indexOf("\"", valueStart);
        if (valueEnd == -1) return null;
        
        return json.substring(valueStart, valueEnd);
    }
    
    private groq4j.models.common.Usage parseUsage(String responseJson) {
        double queueTime = ResponseParser.getOptionalDouble(responseJson, "usage.queue_time").orElse(0.0);
        int promptTokens = ResponseParser.getRequiredInt(responseJson, "usage.prompt_tokens");
        double promptTime = ResponseParser.getOptionalDouble(responseJson, "usage.prompt_time").orElse(0.0);
        int completionTokens = ResponseParser.getRequiredInt(responseJson, "usage.completion_tokens");
        double completionTime = ResponseParser.getOptionalDouble(responseJson, "usage.completion_time").orElse(0.0);
        int totalTokens = ResponseParser.getRequiredInt(responseJson, "usage.total_tokens");
        double totalTime = ResponseParser.getOptionalDouble(responseJson, "usage.total_time").orElse(0.0);
        
        return new groq4j.models.common.Usage(
            queueTime, promptTokens, promptTime, completionTokens, 
            completionTime, totalTokens, totalTime
        );
    }
}