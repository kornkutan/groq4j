import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Base class for all service tests providing common test utilities and API key management.
 * Handles API key configuration from multiple sources and provides shared test setup functionality.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseServiceTest {
    
    protected static final String API_KEY_ENV_VAR = "GROQ_API_KEY";
    protected static final String API_KEY_PROPERTY = "groq.api.key";
    
    protected String apiKey;
    protected Properties testProperties;
    
    // Configuration values loaded from properties
    protected String defaultModel;
    protected String alternativeModel;
    protected String whisperModel;
    protected String synthesisModel;
    protected String synthesisVoice;
    protected int shortTimeout;
    protected int mediumTimeout;
    protected int longTimeout;
    protected String harvardAudioFile;
    protected String frenchAudioFile;
    protected boolean chatEnabled;
    protected boolean modelsEnabled;
    protected boolean audioEnabled;
    protected boolean batchEnabled;
    protected boolean filesEnabled;
    
    @BeforeAll
    void baseSetup() {
        // Load test properties
        loadTestProperties();
        
        // Load configuration values
        loadTestConfiguration();
        
        // Try to get API key from multiple sources (in order of preference):
        // 1. Environment variable
        // 2. application.properties
        apiKey = System.getenv(API_KEY_ENV_VAR);
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = testProperties.getProperty(API_KEY_PROPERTY);
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                System.out.println("Using API key from application.properties");
            }
        }
        
        // Ensure we have a valid API key
        Assumptions.assumeTrue(apiKey != null && !apiKey.trim().isEmpty(), 
            "API key must be provided via environment variable, properties file, or fallback");
        
        logTestProgress("Test configuration loaded: model=" + defaultModel + ", timeouts=" + shortTimeout + "s/" + mediumTimeout + "s/" + longTimeout + "s");
    }
    
    private void loadTestProperties() {
        testProperties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                testProperties.load(input);
            }
        } catch (IOException e) {
            System.out.println("Could not load application.properties: " + e.getMessage());
            testProperties = new Properties(); // Use empty properties as fallback
        }
    }
    
    /**
     * Gets the configured API key for test execution.
     * @return The API key to use for Groq API calls
     */
    protected String getApiKey() {
        return apiKey;
    }
    
    /**
     * Utility method to skip tests if API key is not available.
     * Use this in test methods that require API access.
     */
    protected void requireApiKey() {
        Assumptions.assumeTrue(apiKey != null && !apiKey.trim().isEmpty(), 
            "Test requires valid API key");
    }
    
    /**
     * Loads test configuration values from application.properties.
     * Sets up defaults if properties are not found.
     */
    private void loadTestConfiguration() {
        // Model configuration
        defaultModel = testProperties.getProperty("test.model.default", "llama-3.1-8b-instant");
        alternativeModel = testProperties.getProperty("test.model.alternative", "llama-3.1-70b-versatile");
        whisperModel = testProperties.getProperty("test.model.whisper", "whisper-large-v3");
        synthesisModel = testProperties.getProperty("test.model.synthesis", "playai-tts");
        synthesisVoice = testProperties.getProperty("test.model.synthesis.voice", "Fritz-PlayAI");
        
        // Timeout configuration
        shortTimeout = Integer.parseInt(testProperties.getProperty("test.timeout.short", "10"));
        mediumTimeout = Integer.parseInt(testProperties.getProperty("test.timeout.medium", "30"));
        longTimeout = Integer.parseInt(testProperties.getProperty("test.timeout.long", "60"));
        
        // Audio file configuration
        harvardAudioFile = testProperties.getProperty("test.audio.harvard.file", "harvard.wav");
        frenchAudioFile = testProperties.getProperty("test.audio.french.file", "french_audio.wav");
        
        // Service enablement configuration
        chatEnabled = Boolean.parseBoolean(testProperties.getProperty("test.chat.enabled", "true"));
        modelsEnabled = Boolean.parseBoolean(testProperties.getProperty("test.models.enabled", "true"));
        audioEnabled = Boolean.parseBoolean(testProperties.getProperty("test.audio.enabled", "true"));
        batchEnabled = Boolean.parseBoolean(testProperties.getProperty("test.batch.enabled", "true"));
        filesEnabled = Boolean.parseBoolean(testProperties.getProperty("test.files.enabled", "true"));
    }
    
    /**
     * Configuration getters for test classes to use
     */
    protected String getDefaultModel() { return defaultModel; }
    protected String getAlternativeModel() { return alternativeModel; }
    protected String getWhisperModel() { return whisperModel; }
    protected String getSynthesisModel() { return synthesisModel; }
    protected String getSynthesisVoice() { return synthesisVoice; }
    protected int getShortTimeout() { return shortTimeout; }
    protected int getMediumTimeout() { return mediumTimeout; }
    protected int getLongTimeout() { return longTimeout; }
    protected String getHarvardAudioFile() { return harvardAudioFile; }
    protected String getFrenchAudioFile() { return frenchAudioFile; }
    
    /**
     * Service enablement checks
     */
    protected boolean isChatEnabled() { return chatEnabled; }
    protected boolean isModelsEnabled() { return modelsEnabled; }
    protected boolean isAudioEnabled() { return audioEnabled; }
    protected boolean isBatchEnabled() { return batchEnabled; }
    protected boolean isFilesEnabled() { return filesEnabled; }
    
    /**
     * Skip test if service is disabled in configuration
     */
    protected void requireServiceEnabled(String serviceName, boolean enabled) {
        Assumptions.assumeTrue(enabled, serviceName + " tests are disabled in configuration");
    }
    
    /**
     * Utility method to print test progress for debugging.
     * @param message The message to print
     */
    protected void logTestProgress(String message) {
        System.out.println("[TEST] " + message);
    }
}