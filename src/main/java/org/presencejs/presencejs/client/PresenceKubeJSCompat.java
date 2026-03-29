package org.presencejs.presencejs.client;

import java.lang.reflect.Method;
import net.minecraftforge.fml.ModList;
import org.presencejs.presencejs.Presencejs;

public final class PresenceKubeJSCompat {
    private static final String BRIDGE_CLASS_NAME = "org.presencejs.presencejs.kubejs.PresenceKubeJSBridge";
    private static boolean loadAttempted;
    private static Method postBuildMethod;
    private static Method postReadyMethod;
    private static Method postDisconnectedMethod;
    private static Method postJoinMethod;
    private static Method postSpectateMethod;
    private static Method postJoinRequestMethod;

    private PresenceKubeJSCompat() {
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded("kubejs") && loadBridge();
    }

    public static void postBuild(PresenceActivity activity, PresenceContext context) {
        invoke(postBuildMethod, activity, context);
    }

    public static void postReady(PresenceDiscordUser user, String discordBuild) {
        invoke(postReadyMethod, user, discordBuild);
    }

    public static void postDisconnected(String message) {
        invoke(postDisconnectedMethod, message);
    }

    public static void postJoin(String secret) {
        invoke(postJoinMethod, secret);
    }

    public static void postSpectate(String secret) {
        invoke(postSpectateMethod, secret);
    }

    public static void postJoinRequest(
            PresenceDiscordUser user,
            String secret,
            Runnable approve,
            Runnable deny,
            Runnable ignore) {
        invoke(postJoinRequestMethod, user, secret, approve, deny, ignore);
    }

    private static boolean loadBridge() {
        if (loadAttempted) {
            return postBuildMethod != null;
        }

        loadAttempted = true;
        try {
            Class<?> bridgeClass = Class.forName(BRIDGE_CLASS_NAME);
            postBuildMethod = bridgeClass.getMethod("postBuild", PresenceActivity.class, PresenceContext.class);
            postReadyMethod = bridgeClass.getMethod("postReady", PresenceDiscordUser.class, String.class);
            postDisconnectedMethod = bridgeClass.getMethod("postDisconnected", String.class);
            postJoinMethod = bridgeClass.getMethod("postJoin", String.class);
            postSpectateMethod = bridgeClass.getMethod("postSpectate", String.class);
            postJoinRequestMethod = bridgeClass.getMethod(
                    "postJoinRequest",
                    PresenceDiscordUser.class,
                    String.class,
                    Runnable.class,
                    Runnable.class,
                    Runnable.class);
            return true;
        } catch (Throwable throwable) {
            Presencejs.LOGGER.error("Failed to initialize KubeJS compatibility bridge", throwable);
            postBuildMethod = null;
            return false;
        }
    }

    private static void invoke(Method method, Object... arguments) {
        if (method == null && !isAvailable()) {
            return;
        }

        try {
            method.invoke(null, arguments);
        } catch (Throwable throwable) {
            Presencejs.LOGGER.error("Failed to post PresenceJS KubeJS event", throwable);
        }
    }
}

