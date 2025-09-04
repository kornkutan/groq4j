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
        try {
            // Parse index
            int index = extractIntValue(choiceJson, "index");
            
            // Parse finish reason
            String finishReason = extractSimpleStringValue(choiceJson, "finish_reason");
            if (finishReason == null) {
                return null;
            }
            
            // Parse message object
            var message = parseMessageFromChoice(choiceJson);
            if (message == null) {
                return null;
            }
            
            // LogProbs are optional for now
            var logprobs = java.util.Optional.<groq4j.models.chat.LogProbs>empty();
            
            return new groq4j.models.chat.Choice(index, message, logprobs, finishReason);
            
        } catch (Exception e) {
            System.err.println("Warning: Failed to parse choice: " + e.getMessage());
            return null;
        }
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
        
        try {
            // Find the choices array
            String choicesMarker = "\"choices\":[";
            int choicesStart = responseJson.indexOf(choicesMarker);
            if (choicesStart == -1) {
                return choices;
            }
            
            // Extract all choice objects from the array
            int arrayStart = choicesStart + choicesMarker.length();
            int arrayEnd = findMatchingBracket(responseJson, arrayStart - 1, '[', ']');
            
            if (arrayEnd == -1) {
                return choices;
            }
            
            String choicesArray = responseJson.substring(arrayStart, arrayEnd);
            
            // Parse each choice object
            int pos = 0;
            while (pos < choicesArray.length()) {
                int choiceStart = choicesArray.indexOf("{", pos);
                if (choiceStart == -1) break;
                
                int choiceEnd = findMatchingBracket(choicesArray, choiceStart, '{', '}');
                if (choiceEnd == -1) break;
                
                String choiceJson = choicesArray.substring(choiceStart, choiceEnd + 1);
                groq4j.models.chat.Choice choice = parseChoice(choiceJson);
                if (choice != null) {
                    choices.add(choice);
                }
                
                pos = choiceEnd + 1;
            }
            
        } catch (Exception e) {
            // Log error but don't fail completely
            System.err.println("Warning: Failed to parse choices array: " + e.getMessage());
        }
        
        return choices;
    }
    
    private int findMatchingBracket(String json, int startPos, char openBracket, char closeBracket) {
        int count = 1;
        int pos = startPos + 1;
        boolean inString = false;
        boolean escaped = false;
        
        while (pos < json.length() && count > 0) {
            char c = json.charAt(pos);
            
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"' && !escaped) {
                inString = !inString;
            } else if (!inString) {
                if (c == openBracket) {
                    count++;
                } else if (c == closeBracket) {
                    count--;
                }
            }
            pos++;
        }
        
        return count == 0 ? pos - 1 : -1;
    }
    
    private String extractSimpleStringValue(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) {
            // Try null value pattern
            String nullPattern = "\"" + key + "\":null";
            if (json.indexOf(nullPattern) != -1) {
                return null;
            }
            return null;
        }
        
        int valueStart = start + pattern.length();
        int valueEnd = json.indexOf("\"", valueStart);
        if (valueEnd == -1) return null;
        
        return json.substring(valueStart, valueEnd);
    }
    
    private int extractIntValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return 0;
        
        int valueStart = start + pattern.length();
        int valueEnd = valueStart;
        while (valueEnd < json.length() && Character.isDigit(json.charAt(valueEnd))) {
            valueEnd++;
        }
        
        try {
            return Integer.parseInt(json.substring(valueStart, valueEnd));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private Message parseMessageFromChoice(String choiceJson) {
        // Find the message object
        String messageMarker = "\"message\":{";
        int messageStart = choiceJson.indexOf(messageMarker);
        if (messageStart == -1) {
            return null;
        }
        
        int messageObjStart = messageStart + messageMarker.length() - 1; // Include the opening brace
        int messageObjEnd = findMatchingBracket(choiceJson, messageObjStart, '{', '}');
        if (messageObjEnd == -1) {
            return null;
        }
        
        String messageJson = choiceJson.substring(messageObjStart, messageObjEnd + 1);
        
        // Parse message fields
        String role = extractSimpleStringValue(messageJson, "role");
        String content = extractSimpleStringValue(messageJson, "content");
        
        // Check for tool calls
        java.util.List<groq4j.models.common.ToolCall> toolCalls = parseToolCalls(messageJson);
        
        if (toolCalls.isEmpty()) {
            // Regular message with content
            return Message.assistant(content != null ? content : "");
        } else {
            // Message with tool calls
            if (content != null && !content.isEmpty()) {
                return Message.assistant(content, toolCalls);
            } else {
                return Message.assistantWithToolCalls(toolCalls);
            }
        }
    }
    
    private java.util.List<groq4j.models.common.ToolCall> parseToolCalls(String messageJson) {
        var toolCalls = new java.util.ArrayList<groq4j.models.common.ToolCall>();
        
        try {
            String toolCallsMarker = "\"tool_calls\":[";
            int toolCallsStart = messageJson.indexOf(toolCallsMarker);
            if (toolCallsStart == -1) {
                return toolCalls;
            }
            
            int arrayStart = toolCallsStart + toolCallsMarker.length();
            int arrayEnd = findMatchingBracket(messageJson, arrayStart - 1, '[', ']');
            if (arrayEnd == -1) {
                return toolCalls;
            }
            
            String toolCallsArray = messageJson.substring(arrayStart, arrayEnd);
            
            // Parse each tool call object
            int pos = 0;
            while (pos < toolCallsArray.length()) {
                int callStart = toolCallsArray.indexOf("{", pos);
                if (callStart == -1) break;
                
                int callEnd = findMatchingBracket(toolCallsArray, callStart, '{', '}');
                if (callEnd == -1) break;
                
                String callJson = toolCallsArray.substring(callStart, callEnd + 1);
                groq4j.models.common.ToolCall toolCall = parseToolCall(callJson);
                if (toolCall != null) {
                    toolCalls.add(toolCall);
                }
                
                pos = callEnd + 1;
            }
            
        } catch (Exception e) {
            System.err.println("Warning: Failed to parse tool calls: " + e.getMessage());
        }
        
        return toolCalls;
    }
    
    private groq4j.models.common.ToolCall parseToolCall(String callJson) {
        try {
            String id = extractSimpleStringValue(callJson, "id");
            String type = extractSimpleStringValue(callJson, "type");
            
            if (id == null || type == null) {
                return null;
            }
            
            // Parse function call
            String functionMarker = "\"function\":{";
            int funcStart = callJson.indexOf(functionMarker);
            if (funcStart == -1) {
                return null;
            }
            
            int funcObjStart = funcStart + functionMarker.length() - 1;
            int funcObjEnd = findMatchingBracket(callJson, funcObjStart, '{', '}');
            if (funcObjEnd == -1) {
                return null;
            }
            
            String functionJson = callJson.substring(funcObjStart, funcObjEnd + 1);
            String name = extractSimpleStringValue(functionJson, "name");
            String arguments = extractSimpleStringValue(functionJson, "arguments");
            
            if (name == null || arguments == null) {
                return null;
            }
            
            return groq4j.models.common.ToolCall.function(id, name, arguments);
            
        } catch (Exception e) {
            System.err.println("Warning: Failed to parse tool call: " + e.getMessage());
            return null;
        }
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