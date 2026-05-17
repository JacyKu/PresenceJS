package org.presencejs.presencejs.client;

public class DiscordWebhookException extends RuntimeException {
    private final DiscordWebhookRequest request;
    private final DiscordWebhookResponse response;

    public DiscordWebhookException(String message, DiscordWebhookRequest request) {
        super(message);
        this.request = request == null ? null : request.copy();
        this.response = null;
    }

    public DiscordWebhookException(String message, Throwable cause, DiscordWebhookRequest request) {
        super(message, cause);
        this.request = request == null ? null : request.copy();
        this.response = null;
    }

    public DiscordWebhookException(String message, DiscordWebhookRequest request, DiscordWebhookResponse response) {
        super(message);
        this.request = request == null ? null : request.copy();
        this.response = response;
    }

    public DiscordWebhookRequest getRequest() {
        return request == null ? null : request.copy();
    }

    public DiscordWebhookResponse getResponse() {
        return response;
    }
}