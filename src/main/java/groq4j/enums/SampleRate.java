package groq4j.enums;

public enum SampleRate {
    RATE_8000(8000),
    RATE_16000(16000),
    RATE_22050(22050),
    RATE_24000(24000),
    RATE_32000(32000),
    RATE_44100(44100),
    RATE_48000(48000);

    private final int value;

    SampleRate(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static SampleRate fromValue(int value) {
        for (SampleRate rate : values()) {
            if (rate.value == value) {
                return rate;
            }
        }
        throw new IllegalArgumentException("Unknown SampleRate: " + value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}