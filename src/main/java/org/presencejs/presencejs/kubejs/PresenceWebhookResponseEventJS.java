package org.presencejs.presencejs.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;
import org.presencejs.presencejs.client.DiscordWebhookRequest;
import org.presencejs.presencejs.client.DiscordWebhookResponse;

public class PresenceWebhookResponseEventJS extends EventJS {
    private final long requestId;
    private final DiscordWebhookRequest request;
    private final DiscordWebhookResponse response;

    public PresenceWebhookResponseEventJS() {
        this(0L, null, null);
    }

    public PresenceWebhookResponseEventJS(long requestId, DiscordWebhookRequest request, DiscordWebhookResponse response) {
        this.requestId = requestId;
        this.request = request == null ? null : request.copy();
        this.response = response;
    }

    public long getRequestId() {
        return requestId;
    }

    public DiscordWebhookRequest getRequest() {
        return request == null ? null : request.copy();
    }

    public DiscordWebhookResponse getResponse() {
        return response;
    }
}