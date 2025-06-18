package groq4j.utils;

public final class Constants {
    private Constants() {
        // Prevent instantiation
    }

    // API Configuration
    public static final String BASE_URL = "https://api.groq.com/openai/v1";
    public static final String USER_AGENT = "groq4j/0.1.0";
    
    // Endpoints
    public static final String CHAT_COMPLETIONS = "/chat/completions";
    public static final String AUDIO_TRANSCRIPTIONS = "/audio/transcriptions";
    public static final String AUDIO_TRANSLATIONS = "/audio/translations";
    public static final String AUDIO_SPEECH = "/audio/speech";
    public static final String MODELS = "/models";
    public static final String BATCHES = "/batches";
    public static final String FILES = "/files";
    
    // Headers
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String USER_AGENT_HEADER = "User-Agent";
    public static final String BEARER_PREFIX = "Bearer ";
    
    // Content Types
    public static final String APPLICATION_JSON = "application/json";
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    
    // Default Values
    public static final double DEFAULT_TEMPERATURE = 1.0;
    public static final double DEFAULT_TOP_P = 1.0;
    public static final int DEFAULT_N = 1;
    public static final String DEFAULT_RESPONSE_FORMAT = "json";
    public static final int DEFAULT_SAMPLE_RATE = 48000;
    public static final double DEFAULT_SPEED = 1.0;
    
    // Limits
    public static final int MAX_TOKENS_LIMIT = 8192;
    public static final int MAX_FUNCTION_COUNT = 128;
    public static final int MAX_STOP_SEQUENCES = 4;
    public static final long MAX_FILE_SIZE_BYTES = 100 * 1024 * 1024; // 100 MB
    
    // Timeouts (in seconds)
    public static final int DEFAULT_CONNECT_TIMEOUT = 30;
    public static final int DEFAULT_READ_TIMEOUT = 120;
    
    // Validation Ranges
    public static final double MIN_TEMPERATURE = 0.0;
    public static final double MAX_TEMPERATURE = 2.0;
    public static final double MIN_TOP_P = 0.0;
    public static final double MAX_TOP_P = 1.0;
    public static final double MIN_FREQUENCY_PENALTY = -2.0;
    public static final double MAX_FREQUENCY_PENALTY = 2.0;
    public static final double MIN_PRESENCE_PENALTY = -2.0;
    public static final double MAX_PRESENCE_PENALTY = 2.0;
    public static final double MIN_SPEED = 0.5;
    public static final double MAX_SPEED = 5.0;
    public static final int MIN_TOP_LOGPROBS = 0;
    public static final int MAX_TOP_LOGPROBS = 20;
}