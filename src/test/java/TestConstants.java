/**
 * Shared test data and configuration constants for all test classes.
 * Contains common test models, messages, and configuration values.
 * 
 * NOTE: Most configuration values are now loaded from application.properties
 * via BaseServiceTest. These constants serve as defaults and static values.
 */
public final class TestConstants {
    
    // Private constructor to prevent instantiation
    private TestConstants() {}
    
    // Default test models (fallback values - prefer configuration from BaseServiceTest)
    public static final String DEFAULT_MODEL = "llama-3.1-8b-instant";
    public static final String ALTERNATIVE_MODEL = "llama-3.1-70b-versatile";
    public static final String LARGE_MODEL = "llama-3.2-90b-text-preview";
    public static final String AUDIO_TRANSCRIPTION_MODEL = "whisper-large-v3";
    
    // Test messages (static content)
    public static final String SIMPLE_TEST_MESSAGE = "Hello, this is a test message.";
    public static final String COMPLEX_TEST_MESSAGE = "Can you explain the concept of machine learning in simple terms?";
    public static final String MATH_TEST_MESSAGE = "What is 2 + 2?";
    
    // Test parameters (static defaults)
    public static final double DEFAULT_TEMPERATURE = 0.7;
    public static final int DEFAULT_MAX_TOKENS = 100;
    public static final int LARGE_MAX_TOKENS = 1000;
    
    // Batch test constants
    public static final String BATCH_DESCRIPTION = "Test batch processing";
    public static final String BATCH_ENDPOINT = "/v1/chat/completions";
    
    // File test constants
    public static final String TEST_FILE_NAME = "test-file.txt";
    public static final String TEST_FILE_CONTENT = "This is test file content for groq4j testing.";
    
    // Expected response patterns
    public static final String EXPECTED_RESPONSE_PATTERN = ".*[a-zA-Z]+.*"; // Any response with letters
    
    // Common JSON request templates
    public static final String BASIC_CHAT_REQUEST_TEMPLATE = """
        {
            "model": "%s",
            "messages": [
                {
                    "role": "user",
                    "content": "%s"
                }
            ],
            "temperature": %.1f,
            "max_tokens": %d
        }
        """;
    
    // Premium feature error codes (for tests expecting failures)
    public static final int PREMIUM_FEATURE_ERROR_CODE = 403;
    public static final String PREMIUM_FEATURE_ERROR_MESSAGE = "premium feature";
    
    // Test utility methods
    public static String formatChatRequest(String model, String message, double temperature, int maxTokens) {
        return String.format(BASIC_CHAT_REQUEST_TEMPLATE, model, message, temperature, maxTokens);
    }
}