package groq4j.utils;

public final class Constants {
    private Constants() {
        // Prevent instantiation
    }

    // API Configuration
    /**
     * API root. Override with {@code -Dgroq4j.baseUrl=...} to point at a proxy or a test server.
     */
    public static final String BASE_URL = System.getProperty("groq4j.baseUrl", "https://api.groq.com/openai/v1");
    public static final String USER_AGENT = "groq4j/1.1.0";
    public static final String VERSION = "1.1.0";

    // Endpoints
    public static final String CHAT_COMPLETIONS = "/chat/completions";
    public static final String RESPONSES = "/responses";

    public static final String TTS_MODEL_ENGLISH = "canopylabs/orpheus-v1-english";
    public static final String TTS_MODEL_ARABIC_SAUDI = "canopylabs/orpheus-arabic-saudi";
    public static final int MAX_TTS_INPUT_CHARS = 200;
    public static final String EMBEDDINGS = "/embeddings";
    public static final String RERANKING = "/reranking";
    public static final String AUDIO_TRANSCRIPTIONS = "/audio/transcriptions";
    public static final String AUDIO_TRANSLATIONS = "/audio/translations";
    public static final String AUDIO_SPEECH = "/audio/speech";
    public static final String MODELS = "/models";
    public static final String BATCHES = "/batches";
    public static final String BATCH_CANCEL = "/cancel";
    public static final String FILES = "/files";
    public static final String FILE_CONTENT = "/content";

    // Endpoints accepted inside a batch JSONL file
    public static final String BATCH_ENDPOINT_CHAT = "/v1/chat/completions";
    public static final String BATCH_ENDPOINT_TRANSCRIPTIONS = "/v1/audio/transcriptions";
    public static final String BATCH_ENDPOINT_TRANSLATIONS = "/v1/audio/translations";

    // Headers
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String ACCEPT_HEADER = "Accept";
    public static final String USER_AGENT_HEADER = "User-Agent";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String RETRY_AFTER_HEADER = "Retry-After";

    // Content Types
    public static final String APPLICATION_JSON = "application/json";
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String TEXT_EVENT_STREAM = "text/event-stream";
    public static final String SSE_DATA_PREFIX = "data:";
    public static final String SSE_DONE = "[DONE]";

    // Default Values
    public static final double DEFAULT_TEMPERATURE = 1.0;
    public static final double DEFAULT_TOP_P = 1.0;
    public static final int DEFAULT_N = 1;
    public static final String DEFAULT_RESPONSE_FORMAT = "json";
    public static final int DEFAULT_SAMPLE_RATE = 24000;
    public static final double DEFAULT_SPEED = 1.0;

    // Limits
    public static final int MAX_FUNCTION_COUNT = 128;
    public static final int MAX_STOP_SEQUENCES = 4;
    public static final int MAX_IDENTIFIER_LENGTH = 64;
    public static final long MAX_AUDIO_FILE_SIZE_BYTES = 100L * 1024 * 1024;
    public static final long MAX_BATCH_FILE_SIZE_BYTES = 200L * 1024 * 1024;
    public static final int MAX_BATCH_FILE_LINES = 50_000;
    public static final int MAX_BATCH_STATUS_LOOKUPS = 200;

    // Timeouts (in seconds)
    public static final int DEFAULT_CONNECT_TIMEOUT = 30;
    public static final int DEFAULT_READ_TIMEOUT = 120;
    public static final int DEFAULT_STREAM_READ_TIMEOUT = 900;

    // Validation Ranges
    public static final double MIN_TEMPERATURE = 0.0;
    public static final double MAX_TEMPERATURE = 2.0;
    public static final double MIN_TOP_P = 0.0;
    public static final double MAX_TOP_P = 1.0;
    public static final double MIN_FREQUENCY_PENALTY = -2.0;
    public static final double MAX_FREQUENCY_PENALTY = 2.0;
    public static final double MIN_PRESENCE_PENALTY = -2.0;
    public static final double MAX_PRESENCE_PENALTY = 2.0;
    public static final double MIN_AUDIO_TEMPERATURE = 0.0;
    public static final double MAX_AUDIO_TEMPERATURE = 1.0;
    public static final double MIN_SPEED = 0.5;
    public static final double MAX_SPEED = 5.0;
    public static final int MIN_TOP_LOGPROBS = 0;
    public static final int MAX_TOP_LOGPROBS = 20;
}
