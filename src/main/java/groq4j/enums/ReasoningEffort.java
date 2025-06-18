package groq4j.enums;

public enum ReasoningEffort {
    NONE("none"),
    DEFAULT("default");

    private final String value;

    ReasoningEffort(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ReasoningEffort fromValue(String value) {
        for (ReasoningEffort effort : values()) {
            if (effort.value.equals(value)) {
                return effort;
            }
        }
        throw new IllegalArgumentException("Unknown ReasoningEffort: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}