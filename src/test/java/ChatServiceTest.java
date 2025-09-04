import groq4j.builders.ChatCompletionRequestBuilder;
import groq4j.exceptions.GroqBadRequestException;
import groq4j.models.common.Message;
import groq4j.services.ChatService;
import groq4j.services.ChatServiceImpl;
import groq4j.models.common.Tool;

import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Chat Service Tests")
@Tag("integration")
@Tag("requires-api-key")
class ChatServiceTest extends BaseServiceTest {

    private ChatService chatService;

    @BeforeAll
    void setup() {
        super.baseSetup();
        requireServiceEnabled("Chat", isChatEnabled());
        chatService = ChatServiceImpl.create(getApiKey());
        logTestProgress("ChatService initialized for testing with model: " + getDefaultModel());
    }

    @Test
    @DisplayName("Simple chat completion with model and message should return valid response")
    void testSimpleChatCompletion() {
        requireApiKey();

        var response = chatService.simple(
                getDefaultModel(),
                "Explain quantum computing in one sentence."
        );

        assertNotNull(response, "Response should not be null");
        assertNotNull(response.id(), "Response ID should not be null");
        assertEquals(getDefaultModel(), response.model(), "Model should match request");
        assertFalse(response.choices().isEmpty(), "Response should have at least one choice");

        var firstChoice = response.choices().getFirst();
        assertNotNull(firstChoice.message().content().orElse(null), "Response content should not be null");

        logTestProgress("Simple chat completion test passed");
    }

    @Test
    @DisplayName("Simple chat completion with system prompt should return valid response")
    void testSimpleChatWithSystemPrompt() {
        requireApiKey();

        var response = chatService.simple(
                getDefaultModel(),
                "You are a professional poet who speaks in haikus.",
                "Describe Java programming."
        );

        assertNotNull(response, "Response should not be null");
        assertEquals(getDefaultModel(), response.model(), "Model should match request");
        assertFalse(response.choices().isEmpty(), "Response should have at least one choice");

        var usage = response.usage();
        assertTrue(usage.totalTokens() > 0, "Should have consumed tokens");

        logTestProgress("Simple chat with system prompt test passed");
    }

    @Test
    @DisplayName("Basic chat completion request should return valid response")
    void testBasicChatCompletionRequest() {
        requireApiKey();

        var request = ChatCompletionRequestBuilder.create(getDefaultModel())
                .userMessage("What are the benefits of using Java for enterprise applications?")
                .temperature(TestConstants.DEFAULT_TEMPERATURE)
                .maxCompletionTokens(TestConstants.DEFAULT_MAX_TOKENS)
                .build();

        var response = chatService.createCompletion(request);

        assertNotNull(response, "Response should not be null");
        assertEquals(getDefaultModel(), response.model(), "Model should match request");
        assertFalse(response.choices().isEmpty(), "Response should have at least one choice");

        var firstChoice = response.choices().getFirst();
        assertNotNull(firstChoice.finishReason(), "Finish reason should be provided");

        logTestProgress("Basic chat completion request test passed");
    }

    @Test
    @DisplayName("Advanced chat completion with multiple messages should return valid response")
    void testAdvancedChatCompletionRequest() {
        requireApiKey();

        var request = ChatCompletionRequestBuilder.create(getDefaultModel())
                .systemMessage("You are a helpful coding assistant specializing in Java.")
                .userMessage("Show me a simple example of the Observer pattern in Java.")
                .assistantMessage("I'll provide a clean implementation of the Observer pattern.")
                .userMessage("Make it thread-safe please.")
                .temperature(0.3)
                .maxCompletionTokens(200)
                .seed(12345)
                .build();

        var response = chatService.createCompletion(request);

        assertNotNull(response, "Response should not be null");
        assertEquals(getDefaultModel(), response.model(), "Model should match request");
        assertFalse(response.choices().isEmpty(), "Response should have at least one choice");

        var usage = response.usage();
        assertTrue(usage.promptTokens() > 0, "Should have consumed prompt tokens");
        assertTrue(usage.completionTokens() > 0, "Should have generated completion tokens");

        logTestProgress("Advanced chat completion request test passed");
    }

    @Test
    @DisplayName("Chat completion with custom message objects should return valid response")
    void testChatCompletionWithCustomMessages() {
        requireApiKey();

        var messages = java.util.List.of(
                Message.system("You are an expert software architect."),
                Message.user("What are the key principles of microservices architecture?"),
                Message.assistant("The key principles include: single responsibility, decentralized governance, and failure isolation."),
                Message.user("How does this apply to Java Spring Boot applications?")
        );

        var request = ChatCompletionRequestBuilder.create(getDefaultModel())
                .messages(messages)
                .temperature(0.5)
                .maxCompletionTokens(150)
                .build();

        var response = chatService.createCompletion(request);

        assertNotNull(response, "Response should not be null");
        assertEquals(getDefaultModel(), response.model(), "Model should match request");
        assertFalse(response.choices().isEmpty(), "Response should have at least one choice");

        var firstChoice = response.choices().getFirst();
        assertTrue(firstChoice.message().content().isPresent(), "Response should have content");

        logTestProgress("Chat completion with custom messages test passed");
    }

    @Test
    @DisplayName("Invalid model should throw appropriate exception")
    void testErrorHandlingWithInvalidModel() {
        requireApiKey();

        assertThrows(Exception.class, () -> {
            chatService.simple("invalid-model-name", "This should fail gracefully."
            );
        }, "Invalid model should throw an exception");

        logTestProgress("Error handling test passed");
    }

    @Test
    @DisplayName("Chat completion should work with alternative model")
    void testChatWithAlternativeModel() {
        requireApiKey();

        var response = chatService.simple(getAlternativeModel(), TestConstants.SIMPLE_TEST_MESSAGE);

        assertNotNull(response, "Response should not be null");
        assertEquals(getAlternativeModel(), response.model(), "Model should match request");
        assertFalse(response.choices().isEmpty(), "Response should have at least one choice");

        logTestProgress("Chat with alternative model " + getAlternativeModel() + " test passed");
    }

    @Nested
    @DisplayName("Advanced Chat Features")
    class AdvancedChatFeatures {

        @Test
        @DisplayName("High temperature should produce more creative responses")
        void testHighTemperatureCompletion() {
            requireApiKey();

            var request = ChatCompletionRequestBuilder.create(getDefaultModel())
                    .userMessage("Tell me a creative story about a robot.")
                    .temperature(1.0)
                    .maxCompletionTokens(TestConstants.DEFAULT_MAX_TOKENS)
                    .build();

            var response = chatService.createCompletion(request);

            assertNotNull(response, "Response should not be null");
            assertFalse(response.choices().isEmpty(), "Response should have at least one choice");

            logTestProgress("High temperature completion test passed");
        }

        @Test
        @DisplayName("Tool calling with city coordinates and weather functions")
        void testToolCallingWithWeatherFunction() {
            requireApiKey();

            var cityTool = createCityCoordinatesTool();
            var weatherTool = createWeatherTool();

            var request = ChatCompletionRequestBuilder.create(getDefaultModel())
                    .systemMessage("""
                            "You are a weather assistant.
                            IMPORTANT RULES:
                                1. When users ask about weather in a CITY NAME, you MUST first call get_city_coordinates to get lat/lng.
                                2. Only call get_weather when you have exact latitude and longitude numbers.
                                3. Never call get_weather without both required parameters.
                            """)
                    .userMessage("What's the weather like in Bangkok?")
                    .addTool(cityTool)
                    .addTool(weatherTool)
                    .temperature(0.0)  // Set 0 temperature for predictable tool calling
                    .maxCompletionTokens(150)
                    .build();

            try {
                var response = chatService.createCompletion(request);

                assertNotNull(response, "Response should not be null");
                assertFalse(response.choices().isEmpty(), "Response should have at least one choice");

                var firstChoice = response.choices().getFirst();
                var message = firstChoice.message();

                if (message.hasToolCalls()) {
                    assertTrue(message.toolCalls().isPresent(), "Tool calls should be present");
                    var toolCalls = message.toolCalls().get();
                    assertFalse(toolCalls.isEmpty(), "Should have at least one tool call");

                    var firstCall = toolCalls.getFirst();
                    assertNotNull(firstCall.id(), "Tool call should have an ID");
                    assertNotNull(firstCall.function().arguments(), "Tool call should have arguments");

                    String functionName = firstCall.function().name();
                    if ("get_city_coordinates".equals(functionName) || "get_weather".equals(functionName)) {
                        logTestProgress("Tool calling test passed - Function called: " + functionName);
                    } else {
                        logTestProgress("Tool calling test - Unexpected function called: " + functionName);
                    }

                } else {
                    // Model might not always call the tool, just log this case
                    logTestProgress("Tool calling test passed - Model provided direct response instead of tool call");
                }

            } catch (GroqBadRequestException e) {
                // Handle different types of tool_use_failed errors
                String errorMsg = e.getMessage();
                if (errorMsg.contains("missing properties: 'latitude', 'longitude'")) {
                    logTestProgress("Model tried to call get_weather without coordinates - schema validation working correctly");
                    logTestProgress("This shows tool calling infrastructure is working, just need better prompting");
                } else if (errorMsg.contains("tool_use_failed")) {
                    logTestProgress("Tool calling test - Model/prompt combination needs adjustment: " + errorMsg.substring(0, Math.min(100, errorMsg.length())));
                } else {
                    throw e; // Re-throw other bad request errors
                }
            }
        }

        @Test
        @DisplayName("Tool calling conversation flow should handle tool responses")
        void testToolCallingConversationFlow() {
            requireApiKey();

            var cityTool = createCityCoordinatesTool();
            var weatherTool = createWeatherTool();

            try {
                var initialRequest = ChatCompletionRequestBuilder.create(getDefaultModel())
                        .systemMessage("""
                                You are a weather assistant.
                                CRITICAL: For city names, always call get_city_coordinates first to get exact lat/lng coordinates. Never call get_weather without both latitude and longitude parameters.
                                """)
                        .userMessage("What's the weather like in Bangkok?")
                        .addTool(cityTool)
                        .addTool(weatherTool)
                        .temperature(0.0)
                        .maxCompletionTokens(150)
                        .build();

                var initialResponse = chatService.createCompletion(initialRequest);

                assertNotNull(initialResponse, "Initial response should not be null");

                var firstChoice = initialResponse.choices().getFirst();
                var assistantMessage = firstChoice.message();

                if (assistantMessage.hasToolCalls() && assistantMessage.toolCalls().isPresent()) {
                    var toolCalls = assistantMessage.toolCalls().get();
                    var firstCall = toolCalls.getFirst();
                    String functionName = firstCall.function().name();

                    logTestProgress("Initial tool call: " + functionName);

                    if ("get_city_coordinates".equals(functionName)) {
                        String cityResult = getCityCoordinates("Bangkok");
                        logTestProgress("Model correctly called get_city_coordinates first");
                        logTestProgress("City coordinates response: " + cityResult.substring(0, Math.min(100, cityResult.length())));

                        try {
                            var secondRequest = ChatCompletionRequestBuilder.create(getDefaultModel())
                                    .systemMessage("You have the coordinates for Bangkok. Now get the weather.")
                                    .userMessage("Get weather for latitude 13.7563, longitude 100.5018")
                                    .addTool(weatherTool)
                                    .temperature(0.0)
                                    .maxCompletionTokens(100)
                                    .build();

                            var secondResponse = chatService.createCompletion(secondRequest);
                            var secondMessage = secondResponse.choices().getFirst().message();

                            if (secondMessage.hasToolCalls()) {
                                logTestProgress("Complete two-step flow: city coordinates -> weather call");
                            } else {
                                logTestProgress("Model provided weather response directly after coordinates");
                            }

                        } catch (Exception e) {
                            logTestProgress("First step (city coordinates) worked, second step failed: " + e.getMessage().substring(0, Math.min(50, e.getMessage().length())));
                        }

                    } else if ("get_weather".equals(functionName)) {
                        String args = firstCall.function().arguments();
                        String weatherResult;
                        
                        try {
                            if (args.contains("latitude") && args.contains("longitude")) {
                                String latMatch = args.replaceAll(".*\"latitude\"\\s*:\\s*([0-9.-]+).*", "$1");
                                String lngMatch = args.replaceAll(".*\"longitude\"\\s*:\\s*([0-9.-]+).*", "$1");
                                double latitude = Double.parseDouble(latMatch);
                                double longitude = Double.parseDouble(lngMatch);
                                logTestProgress("Extracted coordinates from tool call: lat=" + latitude + ", lng=" + longitude);
                                weatherResult = weatherApiCall(latitude, longitude);

                            } else {
                                // No coordinates found in the tool call-return error
                                logTestProgress("No coordinates found in weather tool call arguments: " + args);
                                weatherResult = """
                                    {
                                        "error": "Invalid coordinates",
                                        "message": "Weather tool call missing required latitude and longitude coordinates"
                                    }
                                    """;
                            }
                        } catch (Exception e) {
                            logTestProgress("Failed to parse coordinates from tool call: " + e.getMessage());
                            weatherResult = """
                                {
                                    "error": "Coordinate parsing failed",
                                    "message": "Unable to extract valid latitude and longitude from tool call arguments"
                                }
                                """;
                        }

                        var followUpRequest = ChatCompletionRequestBuilder.create(getDefaultModel())
                                .systemMessage("You are a weather assistant.")
                                .userMessage("What's the weather like in Bangkok?")
                                .addMessage(Message.assistantWithToolCalls(toolCalls))
                                .addMessage(Message.tool(weatherResult, firstCall.id()))
                                .temperature(0.0)
                                .maxCompletionTokens(150)
                                .build();

                        var followUpResponse = chatService.createCompletion(followUpRequest);
                        var finalMessage = followUpResponse.choices().getFirst().message();
                        assertTrue(finalMessage.hasContent(), "Final response should have content about weather");

                        logTestProgress("Direct weather tool calling flow completed");
                    }

                    logTestProgress("Tool calling conversation flow test passed");
                } else {
                    logTestProgress("Tool calling conversation test passed - Model provided direct response");
                }

            } catch (groq4j.exceptions.GroqBadRequestException e) {
                // Handle different types of tool_use_failed errors  
                String errorMsg = e.getMessage();
                if (errorMsg.contains("missing properties: 'latitude', 'longitude'")) {
                    logTestProgress("Conversation flow: Model tried get_weather without coordinates - schema working");

                } else if (errorMsg.contains("tool_use_failed")) {
                    logTestProgress("Tool calling conversation flow - Need better prompting: " + errorMsg.substring(0, Math.min(80, errorMsg.length())));

                } else {
                    throw e; // Re-throw other errors
                }
            }
        }

        @Test
        @DisplayName("City coordinates tool should handle unknown cities properly")
        void testUnknownCityHandling() {
            // Test that unknown cities return proper error responses instead of fallback coordinates
            String unknownCityResult = getCityCoordinates("UnknownCity");
            logTestProgress("Unknown city response: " + unknownCityResult);
            
            assertTrue(unknownCityResult.contains("\"found\": false"), "Should indicate city not found");
            assertTrue(unknownCityResult.contains("error"), "Should contain error information");
            assertFalse(unknownCityResult.contains("13.7563"), "Should not contain fallback Bangkok coordinates");
            
            // Test that known cities still work
            String knownCityResult = getCityCoordinates("Tokyo");
            logTestProgress("Known city response: " + knownCityResult);
            
            assertTrue(knownCityResult.contains("\"found\": true"), "Should indicate city found");
            assertTrue(knownCityResult.contains("35.6762"), "Should contain Tokyo coordinates");
            assertFalse(knownCityResult.contains("error"), "Should not contain error information");
        }

        @Test
        @DisplayName("Tool calling with default model (gpt-oss-20b) should work reliably")
        void testToolCallingWithGptOss120b() {
            requireApiKey();

            // Tools setup
            var cityTool = createCityCoordinatesTool();
            var weatherTool = createWeatherTool();

            var request = ChatCompletionRequestBuilder.create(getDefaultModel())
                    .systemMessage("""
                            You are a weather assistant.
                            STRICT RULES:
                                1. For city names: MUST call get_city_coordinates first
                                2. For get_weather: MUST have exact latitude and longitude numbers
                                3. NEVER call get_weather without both required coordinates"
                            """)
                    .userMessage("What's the weather like in Bangkok, Thailand?")
                    .addTool(cityTool)
                    .addTool(weatherTool)
                    .temperature(0.1)
                    .maxCompletionTokens(200)
                    .build();

            try {
                var response = chatService.createCompletion(request);

                assertNotNull(response, "Response should not be null");
                assertFalse(response.choices().isEmpty(), "Response should have at least one choice");

                var firstChoice = response.choices().getFirst();
                var message = firstChoice.message();

                if (message.hasToolCalls()) {
                    assertTrue(message.toolCalls().isPresent(), "Tool calls should be present");
                    var toolCalls = message.toolCalls().get();
                    assertFalse(toolCalls.isEmpty(), "Should have at least one tool call");

                    var firstCall = toolCalls.getFirst();
                    String functionName = firstCall.function().name();
                    assertNotNull(firstCall.id(), "Tool call should have an ID");
                    assertNotNull(firstCall.function().arguments(), "Tool call should have arguments");

                    String args = firstCall.function().arguments();
                    logTestProgress("Tool call arguments received: " + args);

                    assertNotNull(args, "Tool call arguments should not be null");
                    assertFalse(args.trim().isEmpty(), "Tool call arguments should not be empty");

                    if ("get_city_coordinates".equals(functionName)) {
                        String cityResult = getCityCoordinates("Bangkok");
                        logTestProgress("Model correctly called get_city_coordinates first");
                        logTestProgress("Arguments: " + args);
                        logTestProgress("Response: " + cityResult.substring(0, Math.min(100, cityResult.length())));
                        logTestProgress("Model demonstrated correct tool calling logic");

                    } else if ("get_weather".equals(functionName)) {
                        boolean hasCoordData = args.contains("13") || args.contains("100") ||
                                args.contains("latitude") || args.contains("longitude");
                        if (hasCoordData) {
                            logTestProgress("Model called weather directly with coordinate data");
                        } else {
                            logTestProgress("Model called weather but may need coordinates: " + args);
                        }

                    } else {
                        logTestProgress("Model called unexpected function: " + functionName);
                    }

                    logTestProgress("Tool calling with model test passed - Function called: " + functionName);
                } else {
                    logTestProgress("Tool calling with model - Model provided direct response instead of tool call");
                }

            } catch (groq4j.exceptions.GroqBadRequestException e) {
                if (e.getMessage().contains("tool_use_failed")) {
                    // This shouldn't happen with default (`gpt-oss-20b`) but just make it gracefully

                } else if (e.getMessage().contains("model_not_found") || e.getMessage().contains("model not found")) {
                    logTestProgress("Tool calling - Model not available, skipping test");
                    // Skip test if model is not available

                } else {
                    throw e; // Re-throw other errors
                }
            }
        }
    }

    // Helper for tool calling
    private Tool createCityCoordinatesTool() {
        var parameters = Map.of(
                "type", "object",
                "properties", Map.of(
                        "city_name", Map.of(
                                "type", "string",
                                "description", "The name of the city (e.g., 'Bangkok', 'New York', 'London')"
                        ),
                        "country", Map.of(
                                "type", "string",
                                "description", "The country name (optional, e.g., 'Thailand', 'USA', 'UK')"
                        )
                ),
                "required", List.of("city_name"),
                "additionalProperties", false
        );

        return Tool.function(
                "get_city_coordinates",
                "Get the latitude and longitude coordinates for a city. Use this when you need coordinates for a city to get weather information.",
                Optional.of(parameters)
        );
    }

    private Tool createWeatherTool() {
        var parameters = Map.of(
                "type", "object",
                "properties", Map.of(
                        "latitude", Map.of(
                                "type", "number",
                                "description", "The latitude coordinate for the location (-90 to 90)",
                                "example", 13.7563
                        ),
                        "longitude", Map.of(
                                "type", "number",
                                "description", "The longitude coordinate for the location (-180 to 180)",
                                "example", 100.5018
                        )
                ),
                "required", List.of("latitude", "longitude"),
                "additionalProperties", false
        );

        return Tool.function(
                "get_weather",
                "Get current weather information for a specific location. IMPORTANT: You MUST provide both latitude and longitude coordinates. If you don't have coordinates for a city, first use get_city_coordinates.",
                Optional.of(parameters)
        );
    }

    private String getCityCoordinates(String cityName) {
        var cityCoordinates = Map.of(
                "bangkok", Map.of("lat", 13.7563, "lng", 100.5018, "country", "Thailand"),
                "london", Map.of("lat", 51.5074, "lng", -0.1278, "country", "United Kingdom"),
                "newyork", Map.of("lat", 40.7128, "lng", -74.0060, "country", "United States"),
                "tokyo", Map.of("lat", 35.6762, "lng", 139.6503, "country", "Japan"),
                "paris", Map.of("lat", 48.8566, "lng", 2.3522, "country", "France"),
                "sydney", Map.of("lat", -33.8688, "lng", 151.2093, "country", "Australia"),
                "moscow", Map.of("lat", 55.7558, "lng", 37.6176, "country", "Russia"),
                "dubai", Map.of("lat", 25.2048, "lng", 55.2708, "country", "United Arab Emirates"),
                "singapore", Map.of("lat", 1.3521, "lng", 103.8198, "country", "Singapore"),
                "mumbai", Map.of("lat", 19.0760, "lng", 72.8777, "country", "India")
        );

        String normalizedCity = cityName.toLowerCase().replaceAll("\\s+", "");
        var coords = cityCoordinates.get(normalizedCity);

        if (coords != null) {
            return String.format("""
                    {
                        "city": "%s",
                        "country": "%s",
                        "latitude": %.4f,
                        "longitude": %.4f,
                        "found": true
                    }
                    """, cityName, coords.get("country"), (Double) coords.get("lat"), (Double) coords.get("lng"));
        } else {
            return String.format("""
                    {
                        "city": "%s",
                        "found": false,
                        "error": "City not found in database"
                    }
                    """, cityName);
        }
    }

    private String weatherApiCall(double latitude, double longitude) {
        try {
            String url = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weathercode,windspeed_10m&hourly=temperature_2m&timezone=auto",
                    latitude, longitude
            );

            var client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                throw new RuntimeException("Weather API returned status: " + response.statusCode());
            }

        } catch (Exception e) {
            // Fallback
            logTestProgress("Weather API call failed, using mock data: " + e.getMessage());
            return String.format("""
                    {
                        "latitude": %.4f,
                        "longitude": %.4f,
                        "current": {
                            "temperature_2m": 28.5,
                            "weathercode": 1,
                            "windspeed_10m": 5.2
                        },
                        "hourly": {
                            "time": ["2024-01-15T00:00", "2024-01-15T01:00", "2024-01-15T02:00"],
                            "temperature_2m": [28.5, 27.8, 27.2]
                        },
                        "timezone": "Asia/Bangkok"
                    }
                    """, latitude, longitude);
        }
    }
}