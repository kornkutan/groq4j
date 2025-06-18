package groq4j.enums;

public enum ToolChoiceType {
    NONE("none"),
    AUTO("auto"),
    REQUIRED("required"),
    FUNCTION("function");

    private final String value;

    ToolChoiceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ToolChoiceType fromValue(String value) {
        for (ToolChoiceType choice : values()) {
            if (choice.value.equals(value)) {
                return choice;
            }
        }
        throw new IllegalArgumentException("Unknown ToolChoiceType: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}