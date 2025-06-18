import groq4j.builders.ChatCompletionRequestBuilder;
import groq4j.enums.MessageRole;
import groq4j.models.common.Message;
import groq4j.services.ChatService;
import groq4j.services.ChatServiceImpl;
import groq4j.models.chat.ChatCompletionResponse;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
        
        // Test with invalid model (should handle gracefully)
        assertThrows(Exception.class, () -> {
            chatService.simple(
                    "invalid-model-name",
                    "This should fail gracefully."
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
    }
}