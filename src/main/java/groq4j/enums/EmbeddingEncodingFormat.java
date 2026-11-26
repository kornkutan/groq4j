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

    @Override
    public String toString() {
        return value;
    }
}
