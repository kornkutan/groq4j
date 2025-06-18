package groq4j.enums;

public enum ReasoningFormat {
    HIDDEN("hidden"),
    RAW("raw"),
    PARSED("parsed");

    private final String value;

    ReasoningFormat(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ReasoningFormat fromValue(String value) {
        for (ReasoningFormat format : values()) {
            if (format.value.equals(value)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown ReasoningFormat: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}