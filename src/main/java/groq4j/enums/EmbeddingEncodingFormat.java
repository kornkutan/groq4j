package groq4j.enums;

public enum EmbeddingEncodingFormat {
    FLOAT("float"),
    BASE64("base64");

    private final String value;

    EmbeddingEncodingFormat(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static EmbeddingEncodingFormat fromValue(String value) {
        for (EmbeddingEncodingFormat format : values()) {
            if (format.value.equals(value)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown EmbeddingEncodingFormat: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
