package org.presencejs.presencejs.kubejs;

import dev.latvian.mods.kubejs.script.ScriptType;
import org.presencejs.presencejs.client.PresenceActivity;
import org.presencejs.presencejs.client.PresenceContext;
import org.presencejs.presencejs.client.PresenceDiscordUser;

public final class PresenceKubeJSBridge {
    private PresenceKubeJSBridge() {
    }

    public static void postBuild(PresenceActivity activity, PresenceContext context) {
        PresenceJSEvents.BUILD.post(ScriptType.CLIENT, new PresenceBuildEventJS(activity, context));
    }

    public static void postReady(PresenceDiscordUser user, String discordBuild) {
        PresenceJSEvents.READY.post(
                ScriptType.CLIENT,
                new PresenceConnectionEventJS("ready", "Connected to Discord", user, discordBuild, null));
    }

    public static void postDisconnected(String message) {
        PresenceJSEvents.DISCONNECTED.post(
                ScriptType.CLIENT,
                new PresenceConnectionEventJS("disconnected", message, null, null, null));
    }

    public static void postJoin(String secret) {
        PresenceJSEvents.JOIN.post(
                ScriptType.CLIENT,
                new PresenceConnectionEventJS("join", "Discord join callback", null, null, secret));
    }

    public static void postSpectate(String secret) {
        PresenceJSEvents.SPECTATE.post(
                ScriptType.CLIENT,
                new PresenceConnectionEventJS("spectate", "Discord spectate callback", null, null, secret));
    }

    public static void postJoinRequest(
            PresenceDiscordUser user,
            String secret,
            Runnable approve,
            Runnable deny,
            Runnable ignore) {
        PresenceJSEvents.JOIN_REQUEST.post(
                ScriptType.CLIENT,
                new PresenceJoinRequestEventJS(user, secret, approve, deny, ignore));
    }
}

