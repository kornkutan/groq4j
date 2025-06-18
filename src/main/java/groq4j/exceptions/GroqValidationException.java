package groq4j.exceptions;

public class GroqValidationException extends GroqApiException {
    
    public GroqValidationException(String message) {
        super(message);
    }

    public GroqValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public static GroqValidationException requiredFieldMissing(String fieldName) {
        return new GroqValidationException("Required field missing: " + fieldName);
    }

    public static GroqValidationException invalidFieldValue(String fieldName, Object value, String reason) {
        return new GroqValidationException(
            String.format("Invalid value for field '%s': %s. Reason: %s", fieldName, value, reason)
        );
    }

    public static GroqValidationException fieldOutOfRange(String fieldName, Object value, Object min, Object max) {
        return new GroqValidationException(
            String.format("Field '%s' value %s is out of range [%s, %s]", fieldName, value, min, max)
        );
    }
}