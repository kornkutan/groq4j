package groq4j.enums;

/** Whether the model should annotate document and search citations in its answer. */
public enum CitationOptions {
    ENABLED("enabled"),
    DISABLED("disabled");

    private final String value;

    CitationOptions(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CitationOptions fromValue(String value) {
        for (CitationOptions option : values()) {
            if (option.value.equals(value)) {
                return option;
            }
        }
        throw new IllegalArgumentException("Unknown CitationOptions: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
