package groq4j.exceptions;

public class GroqUnprocessableEntityException extends GroqApiException {

    public GroqUnprocessableEntityException(String message, String errorType, String errorCode) {
        super(message, 422, errorType, errorCode);
    }

    public GroqUnprocessableEntityException(String message) {
        super(message, 422, "invalid_request_error", "unprocessable_entity");
    }
}
