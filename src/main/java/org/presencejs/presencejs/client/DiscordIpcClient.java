package org.presencejs.presencejs.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.presencejs.presencejs.Presencejs;

public final class DiscordIpcClient implements Closeable {
    private static final long CALLBACK_INTERVAL_MILLIS = 500L;
    private static final RpcBindings RPC = RpcBindings.create();

    private final String clientId;
    private final boolean debugLogging;
    private final boolean verboseLogging;
    private final ScheduledExecutorService callbackExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "PresenceJS-DiscordCallbacks");
        thread.setDaemon(true);
        return thread;
    });
    private volatile Object rpc;
    private volatile Listener listener;
    private volatile PresenceDiscordUser currentUser;
    private volatile String discordBuild;
    private volatile boolean closing;

    public DiscordIpcClient(String clientId, boolean debugLogging, boolean verboseLogging) {
        this.clientId = clientId;
        this.debugLogging = debugLogging;
        this.verboseLogging = verboseLogging;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void connect() throws IOException {
        if (rpc != null) {
            throw new IllegalStateException("Discord RPC client is already connected");
        }

        if (!RPC.isAvailable()) {
            throw RPC.createUnavailableException();
        }

        closing = false;
        currentUser = null;
        discordBuild = null;

        try {
            Object newRpc = RPC.newDiscordRpc();
            RPC.init(newRpc, clientId, createEventHandlerProxy(), false);
            rpc = newRpc;
            callbackExecutor.scheduleAtFixedRate(this::runCallbacks, 0L, CALLBACK_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InvocationTargetException exception) {
            throw mapBackendException(exception.getCause());
        } catch (ReflectiveOperationException exception) {
            throw new IOException(firstNonBlank(exception.getMessage(), "Failed to initialize Discord RPC"), exception);
        } catch (Throwable throwable) {
            throw new IOException(firstNonBlank(throwable.getMessage(), "Failed to initialize Discord RPC"), throwable);
        }
    }

    public void sendRichPresence(JsonObject activity) throws IOException {
        Object currentRpc = rpc;
        if (currentRpc == null) {
            throw new IOException("Discord RPC is not connected");
        }

        JsonObject payload = activity == null ? new JsonObject() : activity.deepCopy();
        if (verboseLogging) {
            Presencejs.LOGGER.debug("Discord RPC -> SET_ACTIVITY {}", payload);
        }

        try {
            RPC.updatePresence(currentRpc, toDiscordRichPresence(payload));
        } catch (InvocationTargetException exception) {
            throw mapBackendException(exception.getCause());
        } catch (ReflectiveOperationException exception) {
            throw new IOException(firstNonBlank(exception.getMessage(), "Failed to send Discord Rich Presence update"), exception);
        }
    }

    public void respondToJoinRequest(String userId, ApprovalMode approvalMode) {
        if (debugLogging) {
            Presencejs.LOGGER.debug("Ignoring Discord join request response; the current RPC backend does not expose join-request actions.");
        }
    }

    public PresenceDiscordUser getCurrentUser() {
        return currentUser;
    }

    public String getDiscordBuild() {
        return discordBuild;
    }

    @Override
    public void close() {
        closing = true;
        callbackExecutor.shutdownNow();

        Object currentRpc = rpc;
        rpc = null;
        if (currentRpc == null) {
            return;
        }

        try {
            RPC.updatePresence(currentRpc, null);
        } catch (Throwable ignored) {
        }

        try {
            RPC.shutdown(currentRpc);
        } catch (Throwable ignored) {
        }
    }

    private void runCallbacks() {
        Object currentRpc = rpc;
        if (closing || currentRpc == null) {
            return;
        }

        try {
            RPC.runCallbacks(currentRpc);
        } catch (Throwable throwable) {
            if (!closing) {
                notifyDisconnected(unwrapInvocationThrowable(throwable));
            }
        }
    }

    private Object toDiscordRichPresence(JsonObject activity) throws ReflectiveOperationException {
        Object builder = RPC.newPresenceBuilder();

        String name = getString(activity, "name");
        if (name != null) {
            RPC.builderName(builder, name);
        }

        String state = getString(activity, "state");
        if (state != null) {
            RPC.builderState(builder, state);
        }

        String details = getString(activity, "details");
        if (details != null) {
            RPC.builderDetails(builder, details);
        }

        JsonObject timestamps = getObject(activity, "timestamps");
        Long startTimestamp = getLong(timestamps, "start");
        if (startTimestamp != null) {
            RPC.builderStartTimestamp(builder, startTimestamp.longValue());
        }

        Long endTimestamp = getLong(timestamps, "end");
        if (endTimestamp != null) {
            RPC.builderEndTimestamp(builder, endTimestamp.longValue());
        }

        JsonObject assets = getObject(activity, "assets");
        String largeImage = getString(assets, "large_image");
        if (largeImage != null) {
            RPC.builderLargeImageKey(builder, largeImage);
        }
        String largeText = getString(assets, "large_text");
        if (largeText != null) {
            RPC.builderLargeImageText(builder, largeText);
        }

        String smallImage = getString(assets, "small_image");
        if (smallImage != null) {
            RPC.builderSmallImageKey(builder, smallImage);
        }
        String smallText = getString(assets, "small_text");
        if (smallText != null) {
            RPC.builderSmallImageText(builder, smallText);
        }

        JsonObject party = getObject(activity, "party");
        String partyId = getString(party, "id");
        JsonArray partySize = getArray(party, "size");
        if (partyId != null && partySize != null && partySize.size() >= 2) {
            RPC.builderPartyId(builder, partyId);
            RPC.builderPartySize(builder, partySize.get(0).getAsInt());
            RPC.builderPartyMax(builder, partySize.get(1).getAsInt());
            RPC.builderPrivacy(builder, mapPartyPrivacy(getInt(party, "privacy")));
        }

        JsonObject secrets = getObject(activity, "secrets");
        String matchSecret = getString(secrets, "match");
        if (matchSecret != null) {
            RPC.builderMatchSecret(builder, matchSecret);
        }
        String joinSecret = getString(secrets, "join");
        if (joinSecret != null) {
            RPC.builderJoinSecret(builder, joinSecret);
        }
        String spectateSecret = getString(secrets, "spectate");
        if (spectateSecret != null) {
            RPC.builderSpectateSecret(builder, spectateSecret);
        }

        if (activity.has("instance") && !activity.get("instance").isJsonNull()) {
            RPC.builderInstance(builder, activity.get("instance").getAsBoolean());
        }

        JsonArray buttons = getArray(activity, "buttons");
        if (buttons != null && !buttons.isEmpty()) {
            List<Object> rpcButtons = new ArrayList<>();
            for (JsonElement buttonElement : buttons) {
                if (!buttonElement.isJsonObject()) {
                    continue;
                }

                JsonObject button = buttonElement.getAsJsonObject();
                String label = getString(button, "label");
                String url = getString(button, "url");
                if (label == null || url == null) {
                    continue;
                }

                rpcButtons.add(RPC.newButton(label, url));
                if (rpcButtons.size() >= 2) {
                    break;
                }
            }

            if (!rpcButtons.isEmpty()) {
                RPC.builderButtons(builder, rpcButtons);
            }
        }

        RPC.builderActivityType(builder, mapActivityType(getInt(activity, "type")));
        return RPC.buildPresence(builder);
    }

    private void notifyDisconnected(Throwable throwable) {
        Listener currentListener = listener;
        if (currentListener != null) {
            currentListener.onDisconnect(this, throwable);
        }
    }

    private Object mapActivityType(Integer type) {
        String activityTypeName;
        if (type == null) {
            activityTypeName = "PLAYING";
        } else {
            activityTypeName = switch (type.intValue()) {
                case 1 -> "STREAMING";
                case 2 -> "LISTENING";
                case 3 -> "WATCHING";
                case 4 -> "CUSTOM";
                case 5 -> "COMPETING";
                default -> "PLAYING";
            };
        }

        return RPC.activityType(activityTypeName);
    }

    private Object mapPartyPrivacy(Integer privacy) {
        return RPC.partyPrivacy(privacy != null && privacy.intValue() == 0 ? "PRIVATE" : "PUBLIC");
    }

    private static JsonObject getObject(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        return object.getAsJsonObject(key);
    }

    private static JsonArray getArray(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        return object.getAsJsonArray(key);
    }

    private static String getString(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        String value = object.get(key).getAsString();
        return value == null || value.isBlank() ? null : value;
    }

    private static Integer getInt(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        return Integer.valueOf(object.get(key).getAsInt());
    }

    private static Long getLong(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        return Long.valueOf(object.get(key).getAsLong());
    }

    private static String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    private static String invokeString(Object target, String... methodNames) {
        if (target == null) {
            return null;
        }

        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                Object value = method.invoke(target);
                if (value instanceof String stringValue && !stringValue.isBlank()) {
                    return stringValue;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static boolean invokeBoolean(Object target, String... methodNames) {
        if (target == null) {
            return false;
        }

        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                Object value = method.invoke(target);
                if (value instanceof Boolean booleanValue) {
                    return booleanValue.booleanValue();
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private Object createEventHandlerProxy() {
        InvocationHandler handler = (proxy, method, args) -> handleEventCallback(method, args);
        return Proxy.newProxyInstance(RPC.classLoader, new Class<?>[] {RPC.discordEventHandlerClass}, handler);
    }

    private Object handleEventCallback(Method method, Object[] args) {
        String methodName = method.getName();
        Object[] actualArgs = args == null ? new Object[0] : args;
        if (method.getDeclaringClass() == Object.class) {
            return switch (methodName) {
                case "toString" -> "PresenceJSDiscordEventHandler";
                case "hashCode" -> Integer.valueOf(System.identityHashCode(this));
                case "equals" -> Boolean.valueOf(actualArgs.length > 0 && actualArgs[0] == this);
                default -> null;
            };
        }

        try {
            switch (methodName) {
                case "ready" -> handleReady(actualArgs.length > 0 ? actualArgs[0] : null);
                case "disconnected" -> handleDisconnected(actualArgs.length > 0 ? actualArgs[0] : null, actualArgs.length > 1 ? actualArgs[1] : null);
                case "errored" -> handleErrored(actualArgs.length > 0 ? actualArgs[0] : null, actualArgs.length > 1 ? actualArgs[1] : null);
                case "joinGame" -> handleJoinGame(actualArgs.length > 0 ? actualArgs[0] : null);
                case "spectateGame" -> handleSpectateGame(actualArgs.length > 0 ? actualArgs[0] : null);
                case "joinRequest" -> handleJoinRequest(actualArgs.length > 0 ? actualArgs[0] : null);
                default -> {
                }
            }
        } catch (Throwable throwable) {
            Presencejs.LOGGER.error("Failed to process Discord RPC callback {}", methodName, throwable);
        }

        return null;
    }

    private void handleReady(Object user) {
        currentUser = toPresenceDiscordUser(user);
        discordBuild = null;

        if (verboseLogging) {
            Presencejs.LOGGER.debug("Discord RPC ready: user={}", currentUser == null ? null : currentUser.getEffectiveName());
        }

        Listener currentListener = listener;
        if (currentListener != null) {
            currentListener.onReady(this);
        }
    }

    private void handleDisconnected(Object errorCode, Object message) {
        if (closing) {
            return;
        }

        notifyDisconnected(new IOException(describeError("Discord RPC disconnected", errorCode, message == null ? null : String.valueOf(message))));
    }

    private void handleErrored(Object errorCode, Object message) {
        if (closing) {
            return;
        }

        notifyDisconnected(new IOException(describeError("Discord RPC error", errorCode, message == null ? null : String.valueOf(message))));
    }

    private void handleJoinGame(Object secret) {
        Listener currentListener = listener;
        if (currentListener != null && secret instanceof String joinSecret && !joinSecret.isBlank()) {
            currentListener.onActivityJoin(this, joinSecret);
        }
    }

    private void handleSpectateGame(Object secret) {
        Listener currentListener = listener;
        if (currentListener != null && secret instanceof String spectateSecret && !spectateSecret.isBlank()) {
            currentListener.onActivitySpectate(this, spectateSecret);
        }
    }

    private void handleJoinRequest(Object joinRequest) {
        Listener currentListener = listener;
        if (currentListener == null || joinRequest == null) {
            return;
        }

        try {
            currentListener.onActivityJoinRequest(this, null, toPresenceDiscordUser(RPC.getJoinRequestUser(joinRequest)));
        } catch (ReflectiveOperationException exception) {
            if (debugLogging) {
                Presencejs.LOGGER.debug("Failed to inspect Discord join request payload", exception);
            }
        }
    }

    private PresenceDiscordUser toPresenceDiscordUser(Object user) {
        return new PresenceDiscordUser(
                invokeString(user, "getUsername"),
                invokeString(user, "getGlobalName", "getDisplayName", "getNickname"),
                firstNonBlank(
                        invokeString(user, "getGlobalName", "getDisplayName", "getNickname"),
                        invokeString(user, "getUsername")),
                firstNonBlank(invokeString(user, "getDiscriminator"), "0"),
                invokeString(user, "getId", "getUserId"),
                invokeString(user, "getAvatarUrl", "getAvatar"),
                invokeBoolean(user, "isBot"));
    }

    private IOException mapBackendException(Throwable throwable) {
        Throwable resolved = unwrapInvocationThrowable(throwable);
        if (resolved == null) {
            return new IOException("Failed to initialize Discord RPC");
        }

        String className = resolved.getClass().getName();
        if (className.equals("dev.firstdark.rpc.exceptions.NoDiscordClientException")) {
            return new NoDiscordClientException();
        }
        if (className.equals("dev.firstdark.rpc.exceptions.PipeAccessDenied")) {
            return new IOException(
                    "Discord RPC connection was denied. Check that Discord and Minecraft are running with the same privileges.",
                    resolved);
        }

        return new IOException(firstNonBlank(resolved.getMessage(), "Failed to initialize Discord RPC"), resolved);
    }

    private static Throwable unwrapInvocationThrowable(Throwable throwable) {
        if (throwable instanceof InvocationTargetException invocationTargetException && invocationTargetException.getCause() != null) {
            return invocationTargetException.getCause();
        }
        return throwable;
    }

    private static String describeError(String prefix, Object errorCode, String message) {
        String errorName = null;
        if (errorCode instanceof Enum<?> enumValue) {
            errorName = enumValue.name();
        } else {
            errorName = invokeString(errorCode, "name");
        }

        if (errorCode == null && (message == null || message.isBlank())) {
            return prefix;
        }
        if (errorName == null || errorName.isBlank()) {
            return prefix + ": " + message;
        }
        if (message == null || message.isBlank()) {
            return prefix + " (" + errorName + ")";
        }
        return prefix + " (" + errorName + "): " + message;
    }

    private static final class RpcBindings {
        private final ClassLoader classLoader;
        private final Class<?> discordRpcClass;
        private final Class<?> discordEventHandlerClass;
        private final Class<?> discordRichPresenceClass;
        private final Class<?> builderClass;
        private final Class<?> rpcButtonClass;
        private final Class<?> activityTypeClass;
        private final Class<?> partyPrivacyClass;
        private final Method initMethod;
        private final Method shutdownMethod;
        private final Method updatePresenceMethod;
        private final Method runCallbacksMethod;
        private final Method presenceBuilderFactoryMethod;
        private final Method builderNameMethod;
        private final Method builderStateMethod;
        private final Method builderDetailsMethod;
        private final Method builderStartTimestampMethod;
        private final Method builderEndTimestampMethod;
        private final Method builderLargeImageKeyMethod;
        private final Method builderLargeImageTextMethod;
        private final Method builderSmallImageKeyMethod;
        private final Method builderSmallImageTextMethod;
        private final Method builderPartyIdMethod;
        private final Method builderPartySizeMethod;
        private final Method builderPartyMaxMethod;
        private final Method builderMatchSecretMethod;
        private final Method builderJoinSecretMethod;
        private final Method builderSpectateSecretMethod;
        private final Method builderInstanceMethod;
        private final Method builderPrivacyMethod;
        private final Method builderActivityTypeMethod;
        private final Method builderButtonsMethod;
        private final Method builderBuildMethod;
        private final Method buttonFactoryMethod;
        private final Method activityTypeValueOfMethod;
        private final Method partyPrivacyValueOfMethod;
        private final Method joinRequestUserMethod;
        private final Throwable loadFailure;

        private RpcBindings(ClassLoader classLoader, Throwable loadFailure) {
            this.classLoader = classLoader;
            this.discordRpcClass = null;
            this.discordEventHandlerClass = null;
            this.discordRichPresenceClass = null;
            this.builderClass = null;
            this.rpcButtonClass = null;
            this.activityTypeClass = null;
            this.partyPrivacyClass = null;
            this.initMethod = null;
            this.shutdownMethod = null;
            this.updatePresenceMethod = null;
            this.runCallbacksMethod = null;
            this.presenceBuilderFactoryMethod = null;
            this.builderNameMethod = null;
            this.builderStateMethod = null;
            this.builderDetailsMethod = null;
            this.builderStartTimestampMethod = null;
            this.builderEndTimestampMethod = null;
            this.builderLargeImageKeyMethod = null;
            this.builderLargeImageTextMethod = null;
            this.builderSmallImageKeyMethod = null;
            this.builderSmallImageTextMethod = null;
            this.builderPartyIdMethod = null;
            this.builderPartySizeMethod = null;
            this.builderPartyMaxMethod = null;
            this.builderMatchSecretMethod = null;
            this.builderJoinSecretMethod = null;
            this.builderSpectateSecretMethod = null;
            this.builderInstanceMethod = null;
            this.builderPrivacyMethod = null;
            this.builderActivityTypeMethod = null;
            this.builderButtonsMethod = null;
            this.builderBuildMethod = null;
            this.buttonFactoryMethod = null;
            this.activityTypeValueOfMethod = null;
            this.partyPrivacyValueOfMethod = null;
            this.joinRequestUserMethod = null;
            this.loadFailure = loadFailure;
        }

        private RpcBindings(
                ClassLoader classLoader,
                Class<?> discordRpcClass,
                Class<?> discordEventHandlerClass,
                Class<?> discordRichPresenceClass,
                Class<?> builderClass,
                Class<?> rpcButtonClass,
                Class<?> activityTypeClass,
                Class<?> partyPrivacyClass,
                Method initMethod,
                Method shutdownMethod,
                Method updatePresenceMethod,
                Method runCallbacksMethod,
                Method presenceBuilderFactoryMethod,
                Method builderNameMethod,
                Method builderStateMethod,
                Method builderDetailsMethod,
                Method builderStartTimestampMethod,
                Method builderEndTimestampMethod,
                Method builderLargeImageKeyMethod,
                Method builderLargeImageTextMethod,
                Method builderSmallImageKeyMethod,
                Method builderSmallImageTextMethod,
                Method builderPartyIdMethod,
                Method builderPartySizeMethod,
                Method builderPartyMaxMethod,
                Method builderMatchSecretMethod,
                Method builderJoinSecretMethod,
                Method builderSpectateSecretMethod,
                Method builderInstanceMethod,
                Method builderPrivacyMethod,
                Method builderActivityTypeMethod,
                Method builderButtonsMethod,
                Method builderBuildMethod,
                Method buttonFactoryMethod,
                Method activityTypeValueOfMethod,
                Method partyPrivacyValueOfMethod,
                Method joinRequestUserMethod,
                Throwable loadFailure) {
            this.classLoader = classLoader;
            this.discordRpcClass = discordRpcClass;
            this.discordEventHandlerClass = discordEventHandlerClass;
            this.discordRichPresenceClass = discordRichPresenceClass;
            this.builderClass = builderClass;
            this.rpcButtonClass = rpcButtonClass;
            this.activityTypeClass = activityTypeClass;
            this.partyPrivacyClass = partyPrivacyClass;
            this.initMethod = initMethod;
            this.shutdownMethod = shutdownMethod;
            this.updatePresenceMethod = updatePresenceMethod;
            this.runCallbacksMethod = runCallbacksMethod;
            this.presenceBuilderFactoryMethod = presenceBuilderFactoryMethod;
            this.builderNameMethod = builderNameMethod;
            this.builderStateMethod = builderStateMethod;
            this.builderDetailsMethod = builderDetailsMethod;
            this.builderStartTimestampMethod = builderStartTimestampMethod;
            this.builderEndTimestampMethod = builderEndTimestampMethod;
            this.builderLargeImageKeyMethod = builderLargeImageKeyMethod;
            this.builderLargeImageTextMethod = builderLargeImageTextMethod;
            this.builderSmallImageKeyMethod = builderSmallImageKeyMethod;
            this.builderSmallImageTextMethod = builderSmallImageTextMethod;
            this.builderPartyIdMethod = builderPartyIdMethod;
            this.builderPartySizeMethod = builderPartySizeMethod;
            this.builderPartyMaxMethod = builderPartyMaxMethod;
            this.builderMatchSecretMethod = builderMatchSecretMethod;
            this.builderJoinSecretMethod = builderJoinSecretMethod;
            this.builderSpectateSecretMethod = builderSpectateSecretMethod;
            this.builderInstanceMethod = builderInstanceMethod;
            this.builderPrivacyMethod = builderPrivacyMethod;
            this.builderActivityTypeMethod = builderActivityTypeMethod;
            this.builderButtonsMethod = builderButtonsMethod;
            this.builderBuildMethod = builderBuildMethod;
            this.buttonFactoryMethod = buttonFactoryMethod;
            this.activityTypeValueOfMethod = activityTypeValueOfMethod;
            this.partyPrivacyValueOfMethod = partyPrivacyValueOfMethod;
            this.joinRequestUserMethod = joinRequestUserMethod;
            this.loadFailure = loadFailure;
        }

        private static RpcBindings create() {
            ClassLoader classLoader = chooseClassLoader();
            try {
                Class<?> discordRpcClass = Class.forName("dev.firstdark.rpc.DiscordRpc", false, classLoader);
                Class<?> discordEventHandlerClass = Class.forName("dev.firstdark.rpc.handlers.DiscordEventHandler", false, classLoader);
                Class<?> discordRichPresenceClass = Class.forName("dev.firstdark.rpc.models.DiscordRichPresence", false, classLoader);
                Class<?> builderClass = Class.forName("dev.firstdark.rpc.models.DiscordRichPresence$DiscordRichPresenceBuilder", false, classLoader);
                Class<?> rpcButtonClass = Class.forName("dev.firstdark.rpc.models.DiscordRichPresence$RPCButton", false, classLoader);
                Class<?> activityTypeClass = Class.forName("dev.firstdark.rpc.enums.ActivityType", false, classLoader);
                Class<?> partyPrivacyClass = Class.forName("dev.firstdark.rpc.enums.PartyPrivacy", false, classLoader);
                Class<?> discordJoinRequestClass = Class.forName("dev.firstdark.rpc.models.DiscordJoinRequest", false, classLoader);

                return new RpcBindings(
                        classLoader,
                        discordRpcClass,
                        discordEventHandlerClass,
                        discordRichPresenceClass,
                        builderClass,
                        rpcButtonClass,
                        activityTypeClass,
                        partyPrivacyClass,
                        discordRpcClass.getMethod("init", String.class, discordEventHandlerClass, boolean.class),
                        discordRpcClass.getMethod("shutdown"),
                        discordRpcClass.getMethod("updatePresence", discordRichPresenceClass),
                        discordRpcClass.getMethod("runCallbacks"),
                        discordRichPresenceClass.getMethod("builder"),
                        builderClass.getMethod("name", String.class),
                        builderClass.getMethod("state", String.class),
                        builderClass.getMethod("details", String.class),
                        builderClass.getMethod("startTimestamp", long.class),
                        builderClass.getMethod("endTimestamp", long.class),
                        builderClass.getMethod("largeImageKey", String.class),
                        builderClass.getMethod("largeImageText", String.class),
                        builderClass.getMethod("smallImageKey", String.class),
                        builderClass.getMethod("smallImageText", String.class),
                        builderClass.getMethod("partyId", String.class),
                        builderClass.getMethod("partySize", int.class),
                        builderClass.getMethod("partyMax", int.class),
                        builderClass.getMethod("matchSecret", String.class),
                        builderClass.getMethod("joinSecret", String.class),
                        builderClass.getMethod("spectateSecret", String.class),
                        builderClass.getMethod("instance", boolean.class),
                        builderClass.getMethod("privacy", partyPrivacyClass),
                        builderClass.getMethod("activityType", activityTypeClass),
                        builderClass.getMethod("buttons", java.util.Collection.class),
                        builderClass.getMethod("build"),
                        rpcButtonClass.getMethod("of", String.class, String.class),
                        activityTypeClass.getMethod("valueOf", String.class),
                        partyPrivacyClass.getMethod("valueOf", String.class),
                        discordJoinRequestClass.getMethod("getUser"),
                        null);
            } catch (Throwable throwable) {
                return new RpcBindings(classLoader, throwable);
            }
        }

        private static ClassLoader chooseClassLoader() {
            ClassLoader context = Thread.currentThread().getContextClassLoader();
            if (context != null) {
                return context;
            }

            ClassLoader own = DiscordIpcClient.class.getClassLoader();
            if (own != null) {
                return own;
            }

            return ClassLoader.getSystemClassLoader();
        }

        private boolean isAvailable() {
            return loadFailure == null;
        }

        private IOException createUnavailableException() {
            return new IOException(
                    "Discord RPC backend classes were not found on the runtime classpath. Rebuild the mod and use the bundled output jar.",
                    loadFailure);
        }

        private Object newDiscordRpc() throws ReflectiveOperationException {
            return discordRpcClass.getConstructor().newInstance();
        }

        private void init(Object rpc, String clientId, Object handler, boolean autoRegister)
                throws ReflectiveOperationException {
            initMethod.invoke(rpc, clientId, handler, Boolean.valueOf(autoRegister));
        }

        private void shutdown(Object rpc) throws ReflectiveOperationException {
            shutdownMethod.invoke(rpc);
        }

        private void updatePresence(Object rpc, Object richPresence) throws ReflectiveOperationException {
            updatePresenceMethod.invoke(rpc, richPresence);
        }

        private void runCallbacks(Object rpc) throws ReflectiveOperationException {
            runCallbacksMethod.invoke(rpc);
        }

        private Object newPresenceBuilder() throws ReflectiveOperationException {
            return presenceBuilderFactoryMethod.invoke(null);
        }

        private void builderName(Object builder, String value) throws ReflectiveOperationException {
            builderNameMethod.invoke(builder, value);
        }

        private void builderState(Object builder, String value) throws ReflectiveOperationException {
            builderStateMethod.invoke(builder, value);
        }

        private void builderDetails(Object builder, String value) throws ReflectiveOperationException {
            builderDetailsMethod.invoke(builder, value);
        }

        private void builderStartTimestamp(Object builder, long value) throws ReflectiveOperationException {
            builderStartTimestampMethod.invoke(builder, Long.valueOf(value));
        }

        private void builderEndTimestamp(Object builder, long value) throws ReflectiveOperationException {
            builderEndTimestampMethod.invoke(builder, Long.valueOf(value));
        }

        private void builderLargeImageKey(Object builder, String value) throws ReflectiveOperationException {
            builderLargeImageKeyMethod.invoke(builder, value);
        }

        private void builderLargeImageText(Object builder, String value) throws ReflectiveOperationException {
            builderLargeImageTextMethod.invoke(builder, value);
        }

        private void builderSmallImageKey(Object builder, String value) throws ReflectiveOperationException {
            builderSmallImageKeyMethod.invoke(builder, value);
        }

        private void builderSmallImageText(Object builder, String value) throws ReflectiveOperationException {
            builderSmallImageTextMethod.invoke(builder, value);
        }

        private void builderPartyId(Object builder, String value) throws ReflectiveOperationException {
            builderPartyIdMethod.invoke(builder, value);
        }

        private void builderPartySize(Object builder, int value) throws ReflectiveOperationException {
            builderPartySizeMethod.invoke(builder, Integer.valueOf(value));
        }

        private void builderPartyMax(Object builder, int value) throws ReflectiveOperationException {
            builderPartyMaxMethod.invoke(builder, Integer.valueOf(value));
        }

        private void builderMatchSecret(Object builder, String value) throws ReflectiveOperationException {
            builderMatchSecretMethod.invoke(builder, value);
        }

        private void builderJoinSecret(Object builder, String value) throws ReflectiveOperationException {
            builderJoinSecretMethod.invoke(builder, value);
        }

        private void builderSpectateSecret(Object builder, String value) throws ReflectiveOperationException {
            builderSpectateSecretMethod.invoke(builder, value);
        }

        private void builderInstance(Object builder, boolean value) throws ReflectiveOperationException {
            builderInstanceMethod.invoke(builder, Boolean.valueOf(value));
        }

        private void builderPrivacy(Object builder, Object value) throws ReflectiveOperationException {
            builderPrivacyMethod.invoke(builder, value);
        }

        private void builderActivityType(Object builder, Object value) throws ReflectiveOperationException {
            builderActivityTypeMethod.invoke(builder, value);
        }

        private void builderButtons(Object builder, List<Object> buttons) throws ReflectiveOperationException {
            builderButtonsMethod.invoke(builder, buttons);
        }

        private Object buildPresence(Object builder) throws ReflectiveOperationException {
            return builderBuildMethod.invoke(builder);
        }

        private Object newButton(String label, String url) throws ReflectiveOperationException {
            return buttonFactoryMethod.invoke(null, label, url);
        }

        private Object activityType(String value) {
            return enumValue(activityTypeValueOfMethod, value);
        }

        private Object partyPrivacy(String value) {
            return enumValue(partyPrivacyValueOfMethod, value);
        }

        private Object getJoinRequestUser(Object joinRequest) throws ReflectiveOperationException {
            return joinRequestUserMethod.invoke(joinRequest);
        }

        private static Object enumValue(Method valueOfMethod, String name) {
            try {
                return valueOfMethod.invoke(null, name);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to map Discord RPC enum value " + name, exception);
            }
        }
    }

    public enum ApprovalMode {
        ACCEPT,
        DENY,
        IGNORE
    }

    public interface Listener {
        default void onReady(DiscordIpcClient client) {
        }

        default void onActivityJoin(DiscordIpcClient client, String secret) {
        }

        default void onActivitySpectate(DiscordIpcClient client, String secret) {
        }

        default void onActivityJoinRequest(DiscordIpcClient client, String secret, PresenceDiscordUser user) {
        }

        default void onClose(DiscordIpcClient client, JsonObject payload) {
        }

        default void onDisconnect(DiscordIpcClient client, Throwable throwable) {
        }
    }

    public static final class NoDiscordClientException extends IOException {
        public NoDiscordClientException() {
            super("Discord desktop client was not found");
        }
    }
}












