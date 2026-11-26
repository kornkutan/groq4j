package groq4j.models.chat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * One part of a multimodal message. Text parts use {@code type=text}, image parts use
 * {@code type=image_url} with either a public URL or a {@code data:} base64 payload.
 */
public record ContentPart(
    String type,
    Optional<String> text,
    Optional<String> imageUrl,
    Optional<String> detail
) {
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE_URL = "image_url";

    public static ContentPart text(String text) {
        return new ContentPart(TYPE_TEXT, Optional.of(text), Optional.empty(), Optional.empty());
    }

    public static ContentPart image(String imageUrl) {
        return new ContentPart(TYPE_IMAGE_URL, Optional.empty(), Optional.of(imageUrl), Optional.empty());
    }

    public static ContentPart image(String imageUrl, String detail) {
        return new ContentPart(TYPE_IMAGE_URL, Optional.empty(), Optional.of(imageUrl), Optional.of(detail));
    }

    public boolean isImage() {
        return TYPE_IMAGE_URL.equals(type);
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("type", type);
        text.ifPresent(value -> map.put("text", value));
        imageUrl.ifPresent(url -> {
            var image = new LinkedHashMap<String, Object>();
            image.put("url", url);
            detail.ifPresent(d -> image.put("detail", d));
            map.put("image_url", image);
        });
        return map;
    }
}
