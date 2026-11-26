package groq4j.enums;

/**
 * Reasoning effort level for reasoning models. Model support is not uniform: values outside a
 * model's supported set are rejected by the API with a 400.
 */
public enum ReasoningEffort {
    NONE("none"),
    DEFAULT("default"),
    MINIMAL("minimal"),
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    XHIGH("xhigh"),
    MAX("max");

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
