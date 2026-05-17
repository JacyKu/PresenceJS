package org.presencejs.presencejs.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class DiscordWebhookResponse {
    private final int statusCode;
    private final String body;
    private final JsonElement jsonBody;
    private final Map<String, List<String>> headers;

    public DiscordWebhookResponse(int statusCode, String body, JsonElement jsonBody, Map<String, List<String>> headers) {
        this.statusCode = statusCode;
        this.body = body == null ? "" : body;
        this.jsonBody = jsonBody == null ? null : jsonBody.deepCopy();
        this.headers = copyHeaders(headers);
    }

    public static DiscordWebhookResponse from(HttpResponse<String> response) {
        return new DiscordWebhookResponse(
                response.statusCode(),
                response.body(),
                parseJsonIfPossible(response.body(), response.headers()),
                response.headers().map());
    }

    private static JsonElement parseJsonIfPossible(String body, HttpHeaders headers) {
        if (body == null || body.isBlank()) {
            return null;
        }

        boolean isJson = headers.firstValue("content-type")
                .map(value -> value.toLowerCase(Locale.ROOT).contains("json"))
                .orElse(false);
        String trimmed = body.trim();
        if (!isJson && !(trimmed.startsWith("{") || trimmed.startsWith("["))) {
            return null;
        }

        try {
            return JsonParser.parseString(body);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Map<String, List<String>> copyHeaders(Map<String, List<String>> headers) {
        Map<String, List<String>> copied = new LinkedHashMap<>();
        if (headers == null) {
            return copied;
        }

        headers.forEach((name, values) -> copied.put(name, values == null ? List.of() : List.copyOf(values)));
        return copied;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    public String getBody() {
        return body;
    }

    public JsonElement getJsonBody() {
        return jsonBody == null ? null : jsonBody.deepCopy();
    }

    public Map<String, List<String>> getHeaders() {
        return copyHeaders(headers);
    }

    public String getHeader(String name) {
        if (name == null) {
            return null;
        }

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue().isEmpty() ? null : entry.getValue().get(0);
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DiscordWebhookResponse that)) {
            return false;
        }
        return statusCode == that.statusCode
                && Objects.equals(body, that.body)
                && Objects.equals(jsonBody, that.jsonBody)
                && Objects.equals(headers, that.headers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusCode, body, jsonBody, headers);
    }
}