package groq4j.enums;

public enum ServiceTier {
    AUTO("auto"),
    ON_DEMAND("on_demand"),
    FLEX("flex");

    private final String value;

    ServiceTier(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ServiceTier fromValue(String value) {
        for (ServiceTier tier : values()) {
            if (tier.value.equals(value)) {
                return tier;
            }
        }
        throw new IllegalArgumentException("Unknown ServiceTier: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}