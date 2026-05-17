package org.presencejs.presencejs.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public interface PresenceJSEvents {
    EventGroup GROUP = EventGroup.of("PresenceJSEvents");
    EventHandler BUILD = GROUP.client("build", () -> PresenceBuildEventJS.class);
    EventHandler READY = GROUP.client("ready", () -> PresenceConnectionEventJS.class);
    EventHandler DISCONNECTED = GROUP.client("disconnected", () -> PresenceConnectionEventJS.class);
    EventHandler JOIN = GROUP.client("join", () -> PresenceConnectionEventJS.class);
    EventHandler SPECTATE = GROUP.client("spectate", () -> PresenceConnectionEventJS.class);
    EventHandler JOIN_REQUEST = GROUP.client("joinRequest", () -> PresenceJoinRequestEventJS.class);
    EventHandler WEBHOOK_RESPONSE = GROUP.client("webhookResponse", () -> PresenceWebhookResponseEventJS.class);
    EventHandler WEBHOOK_ERROR = GROUP.client("webhookError", () -> PresenceWebhookErrorEventJS.class);
}


