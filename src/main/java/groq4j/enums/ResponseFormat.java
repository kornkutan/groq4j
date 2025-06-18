package groq4j.enums;

public enum ResponseFormat {
    // Simple string formats (for audio APIs)
    JSON("json"),
    TEXT("text"), 
    VERBOSE_JSON("verbose_json"),
    
    // Object formats (for chat API - will be serialized as objects)
    JSON_OBJECT("json_object"),
    JSON_SCHEMA("json_schema");

    private final String value;

    ResponseFormat(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ResponseFormat fromValue(String value) {
        for (ResponseFormat format : values()) {
            if (format.value.equals(value)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown ResponseFormat: " + value);
    }

    public boolean isSimpleString() {
        return this == JSON || this == TEXT || this == VERBOSE_JSON;
    }

    public boolean requiresObject() {
        return this == JSON_OBJECT || this == JSON_SCHEMA;
    }

    @Override
    public String toString() {
        return value;
    }
}