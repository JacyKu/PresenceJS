package org.presencejs.presencejs.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

public final class DiscordWebhookFilePart {
    private final String filename;
    private final byte[] data;
    private final String contentType;

    public DiscordWebhookFilePart(String filename, byte[] data) {
        this(filename, data, "application/octet-stream");
    }

    public DiscordWebhookFilePart(String filename, byte[] data, String contentType) {
        this.filename = requireNonBlank(filename, "filename");
        this.data = data == null ? new byte[0] : Arrays.copyOf(data, data.length);
        this.contentType = isBlank(contentType) ? "application/octet-stream" : contentType.trim();
    }

    public DiscordWebhookFilePart(DiscordWebhookFilePart other) {
        this(other.filename, other.data, other.contentType);
    }

    public static DiscordWebhookFilePart fromPath(String path) {
        Path filePath = requirePath(path);
        return fromPath(filePath, filePath.getFileName().toString(), probeContentType(filePath));
    }

    public static DiscordWebhookFilePart fromPath(String path, String filename) {
        Path filePath = requirePath(path);
        return fromPath(filePath, filename, probeContentType(filePath));
    }

    public static DiscordWebhookFilePart fromPath(String path, String filename, String contentType) {
        return fromPath(requirePath(path), filename, contentType);
    }

    public static DiscordWebhookFilePart fromText(String filename, String content) {
        return fromText(filename, content, "text/plain; charset=UTF-8");
    }

    public static DiscordWebhookFilePart fromText(String filename, String content, String contentType) {
        return new DiscordWebhookFilePart(
                filename,
                content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8),
                contentType);
    }

    private static DiscordWebhookFilePart fromPath(Path path, String filename, String contentType) {
        try {
            return new DiscordWebhookFilePart(filename, Files.readAllBytes(path), contentType);
        } catch (IOException exception) {
            throw new DiscordWebhookException("Failed to read webhook upload file: " + path, exception, null);
        }
    }

    private static String probeContentType(Path path) {
        try {
            String detected = Files.probeContentType(path);
            return isBlank(detected) ? "application/octet-stream" : detected;
        } catch (IOException ignored) {
            return "application/octet-stream";
        }
    }

    private static Path requirePath(String path) {
        if (isBlank(path)) {
            throw new IllegalArgumentException("path must not be blank");
        }

        Path resolved = Path.of(path);
        if (!Files.exists(resolved) || !Files.isRegularFile(resolved)) {
            throw new IllegalArgumentException("Webhook file does not exist: " + path);
        }
        return resolved;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public DiscordWebhookFilePart copy() {
        return new DiscordWebhookFilePart(this);
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DiscordWebhookFilePart that)) {
            return false;
        }
        return Objects.equals(filename, that.filename)
                && Arrays.equals(data, that.data)
                && Objects.equals(contentType, that.contentType);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(filename, contentType);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}