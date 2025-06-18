package groq4j.enums;

public enum FilePurpose {
    BATCH("batch");

    private final String value;

    FilePurpose(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static FilePurpose fromValue(String value) {
        for (FilePurpose purpose : values()) {
            if (purpose.value.equals(value)) {
                return purpose;
            }
        }
        throw new IllegalArgumentException("Unknown FilePurpose: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}