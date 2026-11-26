package groq4j.enums;

import java.util.List;

/**
 * Voices hosted by Groq's Orpheus text-to-speech models. The English model supports vocal
 * direction tags in the input, for example {@code "[cheerful] hello"}.
 */
public enum TtsVoice {
    AUTUMN("autumn", true),
    DIANA("diana", true),
    HANNAH("hannah", true),
    AUSTIN("austin", true),
    DANIEL("daniel", true),
    TROY("troy", true),
    ABDULLAH("abdullah", false),
    FAHAD("fahad", false),
    SULTAN("sultan", false),
    LULWA("lulwa", false),
    NOURA("noura", false),
    AISHA("aisha", false);

    private final String value;
    private final boolean english;

    TtsVoice(String value, boolean english) {
        this.value = value;
        this.english = english;
    }

    public String getValue() {
        return value;
    }

    public boolean isEnglish() {
        return english;
    }

    public static List<TtsVoice> englishVoices() {
        return List.of(AUTUMN, DIANA, HANNAH, AUSTIN, DANIEL, TROY);
    }

    public static List<TtsVoice> arabicVoices() {
        return List.of(ABDULLAH, FAHAD, SULTAN, LULWA, NOURA, AISHA);
    }
}
