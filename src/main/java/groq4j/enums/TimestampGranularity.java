package groq4j.enums;

public enum TimestampGranularity {
    WORD("word"),
    SEGMENT("segment");

    private final String value;

    TimestampGranularity(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TimestampGranularity fromValue(String value) {
        for (TimestampGranularity granularity : values()) {
            if (granularity.value.equals(value)) {
                return granularity;
            }
        }
        throw new IllegalArgumentException("Unknown TimestampGranularity: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}