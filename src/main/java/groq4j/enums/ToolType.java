package groq4j.enums;

public enum ToolType {
    FUNCTION("function");

    private final String value;

    ToolType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ToolType fromValue(String value) {
        for (ToolType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ToolType: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}