package groq4j.enums;

public enum HttpMethod {
    GET("GET"),
    POST("POST"),
    DELETE("DELETE");

    private final String value;

    HttpMethod(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static HttpMethod fromValue(String value) {
        for (HttpMethod method : values()) {
            if (method.value.equals(value)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown HttpMethod: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}