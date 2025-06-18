package groq4j.exceptions;

public class GroqSerializationException extends GroqApiException {
    
    public GroqSerializationException(String message) {
        super(message);
    }

    public GroqSerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public static GroqSerializationException jsonParsingError(String message, Throwable cause) {
        return new GroqSerializationException("JSON parsing error: " + message, cause);
    }

    public static GroqSerializationException jsonSerializationError(String message, Throwable cause) {
        return new GroqSerializationException("JSON serialization error: " + message, cause);
    }

    public static GroqSerializationException invalidResponseFormat(String message) {
        return new GroqSerializationException("Invalid response format: " + message);
    }
}