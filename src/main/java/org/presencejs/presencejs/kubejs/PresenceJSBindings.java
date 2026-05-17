package org.presencejs.presencejs.kubejs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.presencejs.presencejs.client.DiscordWebhookMessage;
import org.presencejs.presencejs.client.DiscordRpcService;
import org.presencejs.presencejs.client.DiscordWebhookService;
import org.presencejs.presencejs.client.PresenceActivity;
import org.presencejs.presencejs.client.PresenceContext;
import org.presencejs.presencejs.client.PresenceDiscordUser;

public final class PresenceJSBindings {
    public static final PresenceJSBindings INSTANCE = new PresenceJSBindings();

    private PresenceJSBindings() {
    }

    public PresenceActivity activity() {
        return new PresenceActivity();
    }

    public PresenceActivity.Button button(String label, String url) {
        return new PresenceActivity.Button(label, url);
    }

    public PresenceActivity.Image image(String key, String text) {
        PresenceActivity.Image image = new PresenceActivity.Image();
        image.setKey(key);
        image.setText(text);
        return image;
    }

    public JsonObject jsonObject() {
        return new JsonObject();
    }

    public JsonArray jsonArray() {
        return new JsonArray();
    }

    public JsonElement json(String json) {
        return JsonParser.parseString(json);
    }

    public DiscordWebhookMessage webhookMessage() {
        return new DiscordWebhookMessage();
    }

    public DiscordWebhookMessage.Embed webhookEmbed() {
        return new DiscordWebhookMessage.Embed();
    }

    public DiscordWebhookMessage.EmbedField webhookField(String name, String value) {
        return new DiscordWebhookMessage.EmbedField(name, value);
    }

    public DiscordWebhookService webhooks() {
        return DiscordWebhookService.get();
    }

    public PresenceContext getContext() {
        return DiscordRpcService.get().getLastContext();
    }

    public PresenceActivity getBaseActivity() {
        return DiscordRpcService.get().getBaseActivity();
    }

    public void setBaseActivity(PresenceActivity activity) {
        DiscordRpcService.get().setBaseActivity(activity);
    }

    public void clearBaseActivity() {
        DiscordRpcService.get().clearBaseActivity();
    }

    public PresenceActivity getLastSentActivity() {
        return DiscordRpcService.get().getLastSentActivity();
    }

    public PresenceDiscordUser getCurrentDiscordUser() {
        return DiscordRpcService.get().getCurrentDiscordUser();
    }

    public String getConnectionState() {
        return DiscordRpcService.get().getConnectionState().name();
    }

    public String getConnectionMessage() {
        return DiscordRpcService.get().getConnectionMessage();
    }

    public boolean isConnected() {
        return DiscordRpcService.get().isConnected();
    }

    public boolean isEnabled() {
        return DiscordRpcService.get().isRuntimeEnabled();
    }

    public void setEnabled(boolean enabled) {
        DiscordRpcService.get().setRuntimeEnabled(enabled);
    }

    public void refresh() {
        DiscordRpcService.get().requestRefresh();
    }

    public void disconnect() {
        DiscordRpcService.get().disconnect();
    }
}

