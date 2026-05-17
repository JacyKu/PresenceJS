package org.presencejs.presencejs.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;
import org.presencejs.presencejs.client.DiscordWebhookRequest;

public class PresenceWebhookErrorEventJS extends EventJS {
    private final long requestId;
    private final DiscordWebhookRequest request;
    private final String message;
    private final String errorType;

    public PresenceWebhookErrorEventJS() {
        this(0L, null, null, null);
    }

    public PresenceWebhookErrorEventJS(long requestId, DiscordWebhookRequest request, String message, String errorType) {
        this.requestId = requestId;
        this.request = request == null ? null : request.copy();
        this.message = message;
        this.errorType = errorType;
    }

    public long getRequestId() {
        return requestId;
    }

    public DiscordWebhookRequest getRequest() {
        return request == null ? null : request.copy();
    }

    public String getMessage() {
        return message;
    }

    public String getErrorType() {
        return errorType;
    }
}