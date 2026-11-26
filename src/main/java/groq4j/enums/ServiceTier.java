package groq4j.enums;

/**
 * Processing tier for a request.
 *
 * <ul>
 *   <li>{@code auto} picks the highest tier available within your rate limits.</li>
 *   <li>{@code on_demand} is the default shared tier.</li>
 *   <li>{@code flex} succeeds or fails fast, and can return status 498 when at capacity.</li>
 *   <li>{@code performance} is the provisioned throughput tier.</li>
 * </ul>
 */
public enum ServiceTier {
    AUTO("auto"),
    ON_DEMAND("on_demand"),
    FLEX("flex"),
    PERFORMANCE("performance"),
    DEFAULT("default");

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
