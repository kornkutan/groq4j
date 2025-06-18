package groq4j.enums;

public enum AudioFormat {
    FLAC("flac"),
    MP3("mp3"),
    MULAW("mulaw"),
    OGG("ogg"),
    WAV("wav");

    private final String value;

    AudioFormat(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AudioFormat fromValue(String value) {
        for (AudioFormat format : values()) {
            if (format.value.equals(value)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown AudioFormat: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}