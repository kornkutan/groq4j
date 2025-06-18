package groq4j.enums;

public enum FinishReason {
    STOP("stop"),
    LENGTH("length"),
    CONTENT_FILTER("content_filter"),
    TOOL_CALLS("tool_calls");

    private final String value;

    FinishReason(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static FinishReason fromValue(String value) {
        for (FinishReason reason : values()) {
            if (reason.value.equals(value)) {
                return reason;
            }
        }
        throw new IllegalArgumentException("Unknown FinishReason: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}