package groq4j.utils;

import groq4j.exceptions.GroqValidationException;

import java.util.Map;

public final class FileUtils {
    private FileUtils() {
        // Prevent instantiation
    }

    private static final Map<String, String> AUDIO_MIME_TYPES = Map.of(
        "flac", "audio/flac",
        "mp3", "audio/mpeg",
        "mp4", "audio/mp4",
        "mpeg", "audio/mpeg",
        "mpga", "audio/mpeg",
        "m4a", "audio/mp4",
        "ogg", "audio/ogg",
        "wav", "audio/wav",
        "webm", "audio/webm"
    );

    private static final Map<String, String> SUPPORTED_AUDIO_EXTENSIONS = Map.of(
        "audio/flac", ".flac",
        "audio/mpeg", ".mp3",
        "audio/mp4", ".mp4",
        "audio/ogg", ".ogg",
        "audio/wav", ".wav",
        "audio/webm", ".webm"
    );

    public static void validateAudioFile(byte[] fileData, String filename) {
        ValidationUtils.validateFileSize(fileData);
        
        if (filename != null && !filename.isEmpty()) {
            String extension = getFileExtension(filename).toLowerCase();
            if (!AUDIO_MIME_TYPES.containsKey(extension)) {
                throw GroqValidationException.invalidFieldValue("filename", filename, 
                    "Unsupported audio format. Supported formats: " + String.join(", ", AUDIO_MIME_TYPES.keySet()));
            }
        }
    }

    public static void validateJsonlFile(byte[] fileData, String filename) {
        ValidationUtils.validateFileSize(fileData);
        
        if (filename != null && !filename.isEmpty()) {
            String extension = getFileExtension(filename).toLowerCase();
            if (!"jsonl".equals(extension)) {
                throw GroqValidationException.invalidFieldValue("filename", filename, 
                    "File must have .jsonl extension for batch processing");
            }
        }
        
        // Basic validation that content looks like JSONL
        String content = new String(fileData);
        if (content.trim().isEmpty()) {
            throw GroqValidationException.invalidFieldValue("fileContent", "empty", "File cannot be empty");
        }
        
        // Check if each line looks like JSON (basic validation)
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty() && (!line.startsWith("{") || !line.endsWith("}"))) {
                throw GroqValidationException.invalidFieldValue("fileContent", "line " + (i + 1), 
                    "Each line must be a valid JSON object");
            }
        }
    }

    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        
        return filename.substring(lastDotIndex + 1);
    }

    public static String guessMimeType(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "application/octet-stream";
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        return AUDIO_MIME_TYPES.getOrDefault(extension, "application/octet-stream");
    }

    public static String generateFilename(String baseName, String extension) {
        if (baseName == null || baseName.isEmpty()) {
            baseName = "file";
        }
        
        if (extension == null || extension.isEmpty()) {
            return baseName;
        }
        
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        
        return baseName + extension;
    }

    public static boolean isAudioFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        return AUDIO_MIME_TYPES.containsKey(extension);
    }

    public static boolean isJsonlFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        return "jsonl".equals(extension);
    }

    public static long calculateFileSizeInMB(byte[] fileData) {
        return fileData.length / (1024 * 1024);
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        
        long kb = bytes / 1024;
        if (kb < 1024) {
            return kb + " KB";
        }
        
        long mb = kb / 1024;
        if (mb < 1024) {
            return mb + " MB";
        }
        
        long gb = mb / 1024;
        return gb + " GB";
    }

    /**
     * Validates batch file format and content.
     * This is an alias for validateJsonlFile for consistency with builder usage.
     *
     * @param fileData the file data
     * @param filename the filename
     * @throws GroqValidationException if the file is invalid
     */
    public static void validateBatchFile(byte[] fileData, String filename) {
        validateJsonlFile(fileData, filename);
    }

    /**
     * Validates file size.
     * This is an alias for ValidationUtils.validateFileSize for consistency.
     *
     * @param fileData the file data
     * @throws GroqValidationException if the file is too large
     */
    public static void validateFileSize(byte[] fileData) {
        ValidationUtils.validateFileSize(fileData);
    }
}