package groq4j.models.common;

import java.util.Optional;

public record XGroq(String id) {
    public static Optional<XGroq> fromJson(String json, String path) {
        return groq4j.utils.ResponseParser.getOptionalString(json, path + ".id")
            .map(XGroq::new);
    }

    public static Optional<XGroq> fromJson(String json) {
        return fromJson(json, "x_groq");
    }
}