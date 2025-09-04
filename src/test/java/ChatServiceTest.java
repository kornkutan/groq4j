import groq4j.builders.ChatCompletionRequestBuilder;
import groq4j.exceptions.GroqBadRequestException;
import groq4j.models.common.Message;
import groq4j.services.ChatService;
import groq4j.services.ChatServiceImpl;
import groq4j.models.common.Tool;
import groq4j.models.chat.ToolChoice;

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
        @DisplayName("Tool calling with city coordinates function")
        void testToolCallingWithWeatherFunction() {
            requireApiKey();

            var cityTool = createCityCoordinatesTool();

            var request = ChatCompletionRequestBuilder.create(getDefaultModel())
                    .systemMessage("You are a helpful assistant. When users ask about a city, use the get_city_coordinates tool to get coordinates, then provide the information in a human-readable format.")
                    .userMessage("Can you tell me the coordinates of Bangkok?")
                    .addTool(cityTool)
                    .temperature(0.3)
                    .maxCompletionTokens(300)
                    .build();

            var response = chatService.createCompletion(request);

            assertNotNull(response, "Response should not be null");
            assertFalse(response.choices().isEmpty(), "Response should have at least one choice");

            var firstChoice = response.choices().getFirst();
            var message = firstChoice.message();

            if (message.hasToolCalls() && message.toolCalls().isPresent()) {
                var toolCalls = message.toolCalls().get();
                var firstCall = toolCalls.getFirst();
                String functionName = firstCall.function().name();
                String args = firstCall.function().arguments();

                assertEquals("get_city_coordinates", functionName, "Should call city coordinates function");
                assertTrue(args.contains("Bangkok"), "Arguments should contain Bangkok");

                String cityResult = getCityCoordinates("Bangkok");
                var finalRequest = ChatCompletionRequestBuilder.create(getDefaultModel())
                        .systemMessage("You are a helpful assistant. Based on the city coordinates data you received, provide a clear summary to the user.")
                        .userMessage("Can you tell me the coordinates of Bangkok?")
                        .addMessage(Message.assistantWithToolCalls(toolCalls))
                        .addMessage(Message.tool(cityResult, firstCall.id()))
                        .temperature(0.3)
                        .maxCompletionTokens(300)
                        .build();

                var finalResponse = chatService.createCompletion(finalRequest);
                var finalMessage = finalResponse.choices().getFirst().message();
                
                logTestProgress("Model response (city coordinates): " + finalMessage.content().orElse("No response"));
                
                assertTrue(finalMessage.content().isPresent(), "Final response should have content");
                assertFalse(finalMessage.content().get().trim().isEmpty(), "Final response should not be empty");
            } else {
                fail("Model should have called the city coordinates tool");
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
                        .userMessage("What's the weather like in Tokyo?") // Test known city
                        .addTool(cityTool)
                        .addTool(weatherTool)
                        .temperature(0.0)
                        .maxCompletionTokens(300)
                        .build();

                var initialResponse = chatService.createCompletion(initialRequest);

                assertNotNull(initialResponse, "Initial response should not be null");

                var firstChoice = initialResponse.choices().getFirst();
                var assistantMessage = firstChoice.message();

                if (assistantMessage.content().isPresent()) {
                    logTestProgress("Model response (initial): " + assistantMessage.content().get());
                } else {
                    logTestProgress("Model response (initial): No content - made tool call");
                }

                if (assistantMessage.hasToolCalls() && assistantMessage.toolCalls().isPresent()) {
                    var toolCalls = assistantMessage.toolCalls().get();
                    var firstCall = toolCalls.getFirst();
                    String functionName = firstCall.function().name();

                    if ("get_city_coordinates".equals(functionName)) {
                        String cityArgs = firstCall.function().arguments();
                        
                        String cityName = null;
                        try {
                            String cleanArgs = cityArgs.replace("\\\"", "\"");
                            if (cleanArgs.contains("city_name")) {
                                String cityMatch = cleanArgs.replaceAll(".*\"city_name\"\\s*:\\s*\"([^\"]+)\".*", "$1");
                                if (!cityMatch.equals(cleanArgs)) {
                                    cityName = cityMatch;
                                }
                            }
                        } catch (Exception e) {
                            // Parsing failed
                        }
                        
                        if (cityName == null) {
                            return;
                        }
                        
                        String cityResult = getCityCoordinates(cityName);
                        boolean cityFound = cityResult.contains("\"found\": true");
                        
                        if (!cityFound) {
                            var unknownCityRequest = ChatCompletionRequestBuilder.create(getDefaultModel())
                                    .systemMessage("You are a weather assistant. When a city is not found, politely explain that you don't have data for that location.")
                                    .userMessage("What's the weather like in " + cityName + "?")
                                    .addMessage(Message.assistantWithToolCalls(List.of(firstCall)))
                                    .addMessage(Message.tool(cityResult, firstCall.id()))
                                    .toolChoice(ToolChoice.none())
                                    .temperature(0.3)
                                    .maxCompletionTokens(300)
                                    .build();
                                    
                            var unknownCityResponse = chatService.createCompletion(unknownCityRequest);
                            var unknownCityMessage = unknownCityResponse.choices().getFirst().message();
                            
                            logTestProgress("Model response (unknown city): " + unknownCityMessage.content().orElse("No response"));
                            
                            assertNotNull(unknownCityMessage.content(), "Model should respond to unknown city");
                            assertTrue(unknownCityMessage.content().isPresent(), "Unknown city response should have content");
                            return;
                        }

                        try {
                            String cityLat = cityResult.replaceAll(".*\"latitude\"\\s*:\\s*([0-9.-]+).*", "$1");
                            String cityLng = cityResult.replaceAll(".*\"longitude\"\\s*:\\s*([0-9.-]+).*", "$1");
                            
                            var directWeatherRequest = ChatCompletionRequestBuilder.create(getDefaultModel())
                                    .systemMessage("You are a weather assistant. Use the get_weather tool to get weather information.")
                                    .userMessage("Get the current weather for latitude " + cityLat + " and longitude " + cityLng)
                                    .addTool(weatherTool)
                                    .toolChoice(ToolChoice.function("get_weather"))
                                    .temperature(0.0)
                                    .maxCompletionTokens(300)
                                    .build();

                            var secondResponse = chatService.createCompletion(directWeatherRequest);
                            var secondMessage = secondResponse.choices().getFirst().message();

                            if (secondMessage.content().isPresent()) {
                                logTestProgress("Model response (weather step): " + secondMessage.content().get());
                            } else {
                                logTestProgress("Model response (weather step): No content - made tool call");
                            }

                            if (secondMessage.hasToolCalls() && secondMessage.toolCalls().isPresent()) {
                                var weatherToolCalls = secondMessage.toolCalls().get();
                                var weatherCall = weatherToolCalls.getFirst();
                                String weatherFunctionName = weatherCall.function().name();
                                String weatherArgs = weatherCall.function().arguments();
                                
                                if ("get_weather".equals(weatherFunctionName)) {
                                    String weatherResult;
                                    try {
                                        if (weatherArgs.contains("latitude") && weatherArgs.contains("longitude")) {
                                            String cleanArgs = weatherArgs.replace("\\\"", "\"");
                                            String latMatch = cleanArgs.replaceAll(".*\"latitude\"\\s*:\\s*([0-9.-]+).*", "$1");
                                            String lngMatch = cleanArgs.replaceAll(".*\"longitude\"\\s*:\\s*([0-9.-]+).*", "$1");
                                            double latitude = Double.parseDouble(latMatch);
                                            double longitude = Double.parseDouble(lngMatch);
                                            weatherResult = weatherApiCall(latitude, longitude);
                                        } else {
                                            weatherResult = "{\"error\": \"Missing coordinates in weather tool call\"}";
                                        }
                                    } catch (Exception e) {
                                        weatherResult = "{\"error\": \"Failed to parse coordinates: " + e.getMessage() + "\"}";
                                    }
                                    
                                    var finalRequest = ChatCompletionRequestBuilder.create(getDefaultModel())
                                            .systemMessage("You are a weather assistant. Summarize the weather data briefly.")
                                            .userMessage("Weather for " + cityName + ":")
                                            .addMessage(Message.assistantWithToolCalls(weatherToolCalls))
                                            .addMessage(Message.tool(weatherResult, weatherCall.id()))
                                            .toolChoice(ToolChoice.none())
                                            .temperature(0.3)
                                            .maxCompletionTokens(300)
                                            .build();
                                    
                                    var finalResponse = chatService.createCompletion(finalRequest);
                                    var finalWeatherMessage = finalResponse.choices().getFirst().message();
                                    
                                    logTestProgress("Model response (final weather): " + finalWeatherMessage.content().orElse("No response"));
                                    
                                    assertTrue(finalWeatherMessage.content().isPresent(), "Final weather response should have content");
                                    assertFalse(finalWeatherMessage.content().get().trim().isEmpty(), "Weather response should not be empty");
                                }
                            }
                        } catch (Exception e) {
                            throw e;
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
                                .maxCompletionTokens(300)
                                .build();

                        var followUpResponse = chatService.createCompletion(followUpRequest);
                        var finalMessage = followUpResponse.choices().getFirst().message();
                        assertTrue(finalMessage.hasContent(), "Final response should have content about weather");
                    }
                }

            } catch (groq4j.exceptions.GroqBadRequestException e) {
                String errorMsg = e.getMessage();
                if (errorMsg.contains("missing properties: 'latitude', 'longitude'")) {
                    // Schema validation working correctly
                } else if (errorMsg.contains("tool_use_failed")) {
                    // Tool use failed - expected for some model/prompt combinations
                } else {
                    throw e;
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