package org.presencejs.presencejs.client;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class DiscordWebhookRequest {
    private String operation = "request";
    private String method;
    private String path;
    private String authorization;
    private String auditLogReason;
    private final Map<String, String> queryParameters = new LinkedHashMap<>();
    private final Map<String, String> headers = new LinkedHashMap<>();
    private JsonElement jsonBody;
    private final List<DiscordWebhookFilePart> files = new ArrayList<>();

    public DiscordWebhookRequest(String method, String path) {
        setMethod(method);
        setPath(path);
    }

    public DiscordWebhookRequest(DiscordWebhookRequest other) {
        operation = other.operation;
        method = other.method;
        path = other.path;
        authorization = other.authorization;
        auditLogReason = other.auditLogReason;
        queryParameters.putAll(other.queryParameters);
        headers.putAll(other.headers);
        jsonBody = other.jsonBody == null ? null : other.jsonBody.deepCopy();
        other.files.stream().map(DiscordWebhookFilePart::copy).forEach(files::add);
    }

    public static DiscordWebhookRequest of(String method, String path) {
        return new DiscordWebhookRequest(method, path);
    }

    public DiscordWebhookRequest copy() {
        return new DiscordWebhookRequest(this);
    }

    public String getOperation() {
        return operation;
    }

    public DiscordWebhookRequest operation(String operation) {
        if (!isBlank(operation)) {
            this.operation = operation.trim();
        }
        return this;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        if (isBlank(method)) {
            throw new IllegalArgumentException("method must not be blank");
        }
        this.method = method.trim().toUpperCase(Locale.ROOT);
    }

    public DiscordWebhookRequest method(String method) {
        setMethod(method);
        return this;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        if (isBlank(path)) {
            throw new IllegalArgumentException("path must not be blank");
        }

        String trimmed = path.trim();
        if (!trimmed.startsWith("/") && !trimmed.regionMatches(true, 0, "http://", 0, 7)
                && !trimmed.regionMatches(true, 0, "https://", 0, 8)) {
            trimmed = "/" + trimmed;
        }
        this.path = trimmed;
    }

    public DiscordWebhookRequest path(String path) {
        setPath(path);
        return this;
    }

    public String getAuthorization() {
        return authorization;
    }

    public DiscordWebhookRequest authorization(String authorization) {
        this.authorization = isBlank(authorization) ? null : authorization.trim();
        return this;
    }

    public DiscordWebhookRequest botToken(String botToken) {
        if (isBlank(botToken)) {
            authorization = null;
            return this;
        }

        String trimmed = botToken.trim();
        authorization = trimmed.regionMatches(true, 0, "Bot ", 0, 4) ? trimmed : "Bot " + trimmed;
        return this;
    }

    public DiscordWebhookRequest bearerToken(String bearerToken) {
        if (isBlank(bearerToken)) {
            authorization = null;
            return this;
        }

        String trimmed = bearerToken.trim();
        authorization = trimmed.regionMatches(true, 0, "Bearer ", 0, 7) ? trimmed : "Bearer " + trimmed;
        return this;
    }

    public String getAuditLogReason() {
        return auditLogReason;
    }

    public DiscordWebhookRequest auditLogReason(String auditLogReason) {
        this.auditLogReason = isBlank(auditLogReason) ? null : auditLogReason;
        return this;
    }

    public Map<String, String> getQueryParameters() {
        return Map.copyOf(queryParameters);
    }

    public DiscordWebhookRequest query(String name, String value) {
        if (isBlank(name)) {
            throw new IllegalArgumentException("query parameter name must not be blank");
        }

        if (value == null) {
            queryParameters.remove(name);
        } else {
            queryParameters.put(name, value);
        }
        return this;
    }

    public DiscordWebhookRequest waitForResponse(boolean waitForResponse) {
        return query("wait", Boolean.toString(waitForResponse));
    }

    public DiscordWebhookRequest threadId(String threadId) {
        return query("thread_id", threadId);
    }

    public DiscordWebhookRequest withComponents(boolean withComponents) {
        return query("with_components", Boolean.toString(withComponents));
    }

    public Map<String, String> getHeaders() {
        return Map.copyOf(headers);
    }

    public DiscordWebhookRequest header(String name, String value) {
        if (isBlank(name)) {
            throw new IllegalArgumentException("header name must not be blank");
        }

        if (value == null) {
            headers.remove(name);
        } else {
            headers.put(name, value);
        }
        return this;
    }

    public JsonElement getJsonBody() {
        return jsonBody == null ? null : jsonBody.deepCopy();
    }

    public DiscordWebhookRequest json(JsonElement jsonBody) {
        this.jsonBody = jsonBody == null ? null : jsonBody.deepCopy();
        return this;
    }

    public DiscordWebhookRequest message(DiscordWebhookMessage webhookMessage) {
        this.jsonBody = webhookMessage == null ? null : webhookMessage.toJson();
        return this;
    }

    public List<DiscordWebhookFilePart> getFiles() {
        return files.stream().map(DiscordWebhookFilePart::copy).toList();
    }

    public DiscordWebhookRequest addFile(DiscordWebhookFilePart filePart) {
        if (filePart != null) {
            files.add(filePart.copy());
        }
        return this;
    }

    public DiscordWebhookRequest clearFiles() {
        files.clear();
        return this;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DiscordWebhookRequest that)) {
            return false;
        }
        return Objects.equals(operation, that.operation)
                && Objects.equals(method, that.method)
                && Objects.equals(path, that.path)
                && Objects.equals(authorization, that.authorization)
                && Objects.equals(auditLogReason, that.auditLogReason)
                && Objects.equals(queryParameters, that.queryParameters)
                && Objects.equals(headers, that.headers)
                && Objects.equals(jsonBody, that.jsonBody)
                && Objects.equals(files, that.files);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operation, method, path, authorization, auditLogReason, queryParameters, headers, jsonBody, files);
    }
}