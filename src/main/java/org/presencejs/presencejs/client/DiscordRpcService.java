package org.presencejs.presencejs.client;

import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.presencejs.presencejs.Config;
import org.presencejs.presencejs.Presencejs;

public final class DiscordRpcService {
    private static final DiscordRpcService INSTANCE = new DiscordRpcService();

    private final Queue<Runnable> mainThreadActions = new ConcurrentLinkedQueue<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "PresenceJS-DiscordRPC");
        thread.setDaemon(true);
        return thread;
    });
    private boolean bootstrapped;
    private boolean runtimeEnabled = true;
    private boolean dirty = true;
    private long tickCounter;
    private long lastSendTick = -1L;
    private long nextReconnectTick;
    private long sessionStartEpochSecond = Instant.now().getEpochSecond();
    private long worldStartEpochSecond = sessionStartEpochSecond;
    private String lastContextToken = "";
    private volatile String activeClientId;
    private volatile DiscordIpcClient client;
    private volatile boolean connectInFlight;
    private volatile boolean readyForUpdates;
    private volatile boolean sendInFlight;
    private PresenceConnectionState connectionState = PresenceConnectionState.IDLE;
    private String connectionMessage = "Waiting for the client to start";
    private PresenceActivity baseActivityOverride;
    private PresenceActivity lastSentActivity;
    private PresenceActivity pendingActivity;
    private PresenceContext lastContext;
    private PresenceDiscordUser currentDiscordUser;
    private boolean loggedSuccessfulPresenceUpdate;
    private boolean warnedMissingClientId;
    private boolean warnedInvalidClientId;
    private boolean warnedRegistrationUnsupported;

    private DiscordRpcService() {
    }

    public static DiscordRpcService get() {
        return INSTANCE;
    }

    public void bootstrap() {
        if (bootstrapped) {
            return;
        }

        bootstrapped = true;
        sessionStartEpochSecond = Instant.now().getEpochSecond();
        worldStartEpochSecond = sessionStartEpochSecond;
        dirty = true;
        Runtime.getRuntime().addShutdownHook(new Thread(this::closeForShutdown, "PresenceJS Shutdown"));
    }

    public void tick() {
        if (!bootstrapped) {
            bootstrap();
        }

        tickCounter++;
        drainMainThreadActions();

        PresenceContext context = PresenceContext.capture(sessionStartEpochSecond, worldStartEpochSecond);
        if (handleContextTransition(context)) {
            context = PresenceContext.capture(sessionStartEpochSecond, worldStartEpochSecond);
        }
        lastContext = context;

        if (!Config.enabled || !runtimeEnabled) {
            disconnectClient(PresenceConnectionState.DISABLED, "Presence disabled", false);
            return;
        }

        PresenceActivity activity = buildPresence(context);
        if (activity == null || !activity.isEnabled()) {
            disconnectClient(PresenceConnectionState.IDLE, "Presence disabled by script", false);
            return;
        }

        String resolvedClientId = activity.resolveClientId(Config.clientId);
        if (isBlank(resolvedClientId)) {
            connectionState = PresenceConnectionState.IDLE;
            connectionMessage = "Waiting for a Discord application ID";
            if (!warnedMissingClientId) {
                warnedMissingClientId = true;
                Presencejs.LOGGER.warn("PresenceJS is enabled, but no Discord application ID is configured yet.");
            }
            disconnectClient(PresenceConnectionState.IDLE, connectionMessage, false);
            return;
        }

        warnedMissingClientId = false;
        if (!ensureConnected(resolvedClientId, activity.resolveSteamId(Config.steamId))) {
            return;
        }

        boolean needsUpdate = dirty
                || lastSendTick < 0L
                || tickCounter - lastSendTick >= Math.max(1, Config.updateIntervalTicks)
                || !activity.equals(lastSentActivity);

        if (needsUpdate) {
            pendingActivity = activity.copy();
        }

        scheduleSendIfPossible();
    }

    private boolean handleContextTransition(PresenceContext context) {
        boolean worldStartChanged = false;
        if (context.isInWorld()) {
            String previousMode = lastContext == null ? null : lastContext.getChangeToken();
            if (lastContext == null || !lastContext.isInWorld()) {
                worldStartEpochSecond = context.getNowEpochSecond();
                worldStartChanged = true;
            } else if (!Objects.equals(previousMode, context.getChangeToken())
                    && (!Objects.equals(lastContext.getServerAddress(), context.getServerAddress())
                    || !Objects.equals(lastContext.getWorldName(), context.getWorldName()))) {
                worldStartEpochSecond = context.getNowEpochSecond();
                worldStartChanged = true;
            }
        }

        if (!Objects.equals(lastContextToken, context.getChangeToken())) {
            dirty = true;
            lastContextToken = context.getChangeToken();
        }

        return worldStartChanged;
    }

    private PresenceActivity buildPresence(PresenceContext context) {
        PresenceActivity activity = createAutomaticActivity(context);
        if (baseActivityOverride != null) {
            if (baseActivityOverride.isReplaceDefaults()) {
                activity = baseActivityOverride.copy();
            } else {
                activity.merge(baseActivityOverride);
            }
        }

        if (PresenceKubeJSCompat.isAvailable()) {
            PresenceKubeJSCompat.postBuild(activity, context);
        }

        return activity;
    }

    private PresenceActivity createAutomaticActivity(PresenceContext context) {
        PresenceActivity activity = new PresenceActivity();
        activity.setClientId(Config.clientId);
        activity.setSteamId(Config.steamId);
        activity.setActivityType(Config.defaultActivityType);

        if (!isBlank(Config.defaultLargeImageKey)) {
            activity.setLargeImage(Config.defaultLargeImageKey, firstNonBlank(Config.defaultLargeImageText, "PresenceJS"));
        }
        if (!isBlank(Config.defaultSmallImageKey)) {
            activity.setSmallImage(Config.defaultSmallImageKey, Config.defaultSmallImageText);
        }

        if (context.isInWorld()) {
            activity.setDetails(context.isSingleplayer() ? Config.singleplayerDetails : Config.multiplayerDetails);
            String locationState = context.isSingleplayer()
                    ? firstNonBlank(context.getWorldName(), context.getDimensionId())
                    : firstNonBlank(context.getServerName(), context.getServerAddress());
            String state = appendSection(locationState, context.getDimensionId());
            activity.setState(firstNonBlank(state, context.getBiomeId()));
            if (Config.showWorldTimestamp) {
                activity.setStartTimestamp(context.getWorldStartEpochSecond());
            }
        } else {
            activity.setDetails(Config.menuDetails);
            activity.setState(firstNonBlank(context.getScreenTitle(), Config.menuState));
            if (Config.showMenuTimestamp) {
                activity.setStartTimestamp(context.getSessionStartEpochSecond());
            }
        }

        return activity;
    }

    private boolean ensureConnected(String clientId, String steamId) {
        if (client != null && !Objects.equals(activeClientId, clientId)) {
            disconnectClient(PresenceConnectionState.IDLE, "Switching Discord application", false);
        }

        if (client != null) {
            return readyForUpdates;
        }

        if (connectInFlight || tickCounter < nextReconnectTick) {
            return false;
        }

        try {
            Long.parseLong(clientId.trim());
            warnedInvalidClientId = false;
        } catch (NumberFormatException exception) {
            connectionState = PresenceConnectionState.INVALID_CLIENT_ID;
            connectionMessage = "Invalid Discord application ID: " + clientId;
            nextReconnectTick = tickCounter + Math.max(20, Config.reconnectIntervalTicks);
            if (!warnedInvalidClientId) {
                warnedInvalidClientId = true;
                Presencejs.LOGGER.error("The configured Discord application ID is not a valid number: {}", clientId);
            }
            return false;
        }

        if ((Config.autoRegister || !isBlank(steamId)) && !warnedRegistrationUnsupported) {
            warnedRegistrationUnsupported = true;
            Presencejs.LOGGER.warn(
                    "PresenceJS now uses an internal Discord IPC backend; autoRegister and steamId are currently ignored.");
        }

        DiscordIpcClient newClient = new DiscordIpcClient(clientId, Config.debugLogging, Config.verboseLogging);
        newClient.setListener(new ClientListener());
        client = newClient;
        activeClientId = clientId;
        currentDiscordUser = null;
        connectInFlight = true;
        readyForUpdates = false;
        connectionState = PresenceConnectionState.CONNECTING;
        connectionMessage = "Connecting to Discord";
        ioExecutor.execute(() -> connectClient(newClient));
        return false;
    }

    private void disconnectClient(PresenceConnectionState state, String message, boolean postEvent) {
        DiscordIpcClient currentClient = client;
        client = null;
        activeClientId = null;
        connectInFlight = false;
        readyForUpdates = false;
        sendInFlight = false;
        lastSentActivity = null;
        pendingActivity = null;
        lastSendTick = -1L;
        loggedSuccessfulPresenceUpdate = false;
        currentDiscordUser = null;
        connectionState = state;
        connectionMessage = message;
        nextReconnectTick = tickCounter + Math.max(20, Config.reconnectIntervalTicks);

        if (currentClient != null) {
            closeClientAsync(currentClient);
        }

        if (postEvent && PresenceKubeJSCompat.isAvailable()) {
            PresenceKubeJSCompat.postDisconnected(message);
        }
    }

    private void closeForShutdown() {
        try {
            disconnectClient(PresenceConnectionState.IDLE, "Minecraft is shutting down", false);
        } catch (Throwable ignored) {
        }
    }

    private void drainMainThreadActions() {
        Runnable action;
        while ((action = mainThreadActions.poll()) != null) {
            action.run();
        }
    }

    private void scheduleSendIfPossible() {
        DiscordIpcClient currentClient = client;
        PresenceActivity activityToSend = pendingActivity;
        if (currentClient == null || activityToSend == null || connectInFlight || !readyForUpdates || sendInFlight) {
            return;
        }

        sendInFlight = true;
        PresenceActivity queuedActivity = activityToSend.copy();
        ioExecutor.execute(() -> sendPresenceUpdate(currentClient, queuedActivity));
    }

    public void requestRefresh() {
        dirty = true;
    }

    public boolean isConnected() {
        return client != null && connectionState == PresenceConnectionState.READY;
    }

    public PresenceConnectionState getConnectionState() {
        return connectionState;
    }

    public String getConnectionMessage() {
        return connectionMessage;
    }

    public PresenceContext getLastContext() {
        return lastContext;
    }

    public PresenceDiscordUser getCurrentDiscordUser() {
        return currentDiscordUser;
    }

    public PresenceActivity getBaseActivity() {
        return baseActivityOverride == null ? null : baseActivityOverride.copy();
    }

    public void setBaseActivity(PresenceActivity activity) {
        baseActivityOverride = activity == null ? null : activity.copy();
        dirty = true;
    }

    public void clearBaseActivity() {
        baseActivityOverride = null;
        dirty = true;
    }

    public PresenceActivity getLastSentActivity() {
        return lastSentActivity == null ? null : lastSentActivity.copy();
    }

    public void setRuntimeEnabled(boolean enabled) {
        runtimeEnabled = enabled;
        dirty = true;
        if (!enabled) {
            disconnectClient(PresenceConnectionState.DISABLED, "Presence disabled", false);
        }
    }

    public boolean isRuntimeEnabled() {
        return runtimeEnabled;
    }

    public void disconnect() {
        disconnectClient(PresenceConnectionState.IDLE, "Disconnected by script", false);
    }

    private static String appendSection(String left, String right) {
        if (isBlank(left)) {
            return right;
        }
        if (isBlank(right)) {
            return left;
        }
        return left + " • " + right;
    }

    private static String firstNonBlank(String primary, String fallback) {
        return isBlank(primary) ? fallback : primary;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private final class ClientListener implements DiscordIpcClient.Listener {
        @Override
        public void onActivityJoin(DiscordIpcClient callbackClient, String secret) {
            mainThreadActions.add(() -> {
                if (client != callbackClient) {
                    return;
                }
                PresenceKubeJSCompat.postJoin(secret);
            });
        }

        @Override
        public void onActivitySpectate(DiscordIpcClient callbackClient, String secret) {
            mainThreadActions.add(() -> {
                if (client != callbackClient) {
                    return;
                }
                PresenceKubeJSCompat.postSpectate(secret);
            });
        }

        @Override
        public void onActivityJoinRequest(DiscordIpcClient callbackClient, String secret, PresenceDiscordUser user) {
            PresenceDiscordUser requestUser = user;
            mainThreadActions.add(() -> {
                if (client != callbackClient) {
                    return;
                }
                PresenceKubeJSCompat.postJoinRequest(
                        requestUser,
                        secret,
                        () -> respondToJoinRequest(callbackClient, requestUser, DiscordIpcClient.ApprovalMode.ACCEPT),
                        () -> respondToJoinRequest(callbackClient, requestUser, DiscordIpcClient.ApprovalMode.DENY),
                        () -> {
                        });
            });
        }

        @Override
        public void onReady(DiscordIpcClient callbackClient) {
            mainThreadActions.add(() -> {
                if (client != callbackClient) {
                    return;
                }
                currentDiscordUser = callbackClient.getCurrentUser();
                readyForUpdates = true;
                connectionState = PresenceConnectionState.READY;
                connectionMessage = currentDiscordUser == null
                        ? "Connected to Discord"
                        : "Connected as " + currentDiscordUser.getEffectiveName();
                dirty = true;
                PresenceKubeJSCompat.postReady(currentDiscordUser, callbackClient.getDiscordBuild());
                scheduleSendIfPossible();
            });
        }

        @Override
        public void onClose(DiscordIpcClient callbackClient, JsonObject json) {
            mainThreadActions.add(() -> {
                if (client != callbackClient) {
                    return;
                }
                disconnectClient(PresenceConnectionState.DISCONNECTED, "Discord RPC connection closed", true);
            });
        }

        @Override
        public void onDisconnect(DiscordIpcClient callbackClient, Throwable throwable) {
            mainThreadActions.add(() -> {
                if (client != callbackClient) {
                    return;
                }
                String message = throwable == null ? "Discord RPC disconnected" : firstNonBlank(throwable.getMessage(), "Discord RPC disconnected");
                disconnectClient(PresenceConnectionState.DISCONNECTED, message, true);
            });
        }
    }

    private void respondToJoinRequest(
            DiscordIpcClient callbackClient, PresenceDiscordUser user, DiscordIpcClient.ApprovalMode approvalMode) {
        if (callbackClient == null || user == null || isBlank(user.getId())) {
            return;
        }

        ioExecutor.execute(() -> {
            try {
                callbackClient.respondToJoinRequest(user.getId(), approvalMode);
            } catch (Throwable throwable) {
                Presencejs.LOGGER.error("Failed to respond to Discord join request", throwable);
            }
        });
    }

    private void connectClient(DiscordIpcClient newClient) {
        try {
            newClient.connect();
            mainThreadActions.add(() -> {
                if (client != newClient) {
                    closeClientAsync(newClient);
                    return;
                }

                connectInFlight = false;
                readyForUpdates = true;
                dirty = true;
                scheduleSendIfPossible();
            });
        } catch (DiscordIpcClient.NoDiscordClientException exception) {
            if (Config.debugLogging) {
                Presencejs.LOGGER.debug("Discord desktop client was not available", exception);
            }
            handleAsyncConnectionFailure(
                    newClient,
                    PresenceConnectionState.DISCORD_NOT_FOUND,
                    "Discord desktop client was not found");
        } catch (Throwable throwable) {
            Presencejs.LOGGER.error("Failed to connect to Discord RPC", throwable);
            handleAsyncConnectionFailure(
                    newClient,
                    PresenceConnectionState.ERROR,
                    firstNonBlank(throwable.getMessage(), "Failed to connect to Discord RPC"));
        }
    }

    private void handleAsyncConnectionFailure(
            DiscordIpcClient failedClient, PresenceConnectionState state, String message) {
        closeClientAsync(failedClient);
        mainThreadActions.add(() -> {
            if (client != failedClient) {
                return;
            }

            client = null;
            activeClientId = null;
            connectInFlight = false;
            readyForUpdates = false;
            lastSentActivity = null;
            currentDiscordUser = null;
            connectionState = state;
            connectionMessage = message;
            nextReconnectTick = tickCounter + Math.max(20, Config.reconnectIntervalTicks);
        });
    }

    private void sendPresenceUpdate(DiscordIpcClient targetClient, PresenceActivity activity) {
        try {
            targetClient.sendRichPresence(activity.toDiscordJson());
            mainThreadActions.add(() -> handleSendSuccess(targetClient, activity));
        } catch (Throwable throwable) {
            mainThreadActions.add(() -> handleSendFailure(targetClient, throwable));
        }
    }

    private void handleSendSuccess(DiscordIpcClient targetClient, PresenceActivity activity) {
        if (client != targetClient) {
            return;
        }

        sendInFlight = false;
        if (pendingActivity != null && pendingActivity.equals(activity)) {
            pendingActivity = null;
            lastSentActivity = activity.copy();
            lastSendTick = tickCounter;
            dirty = false;
        } else {
            dirty = true;
        }

        connectionState = PresenceConnectionState.READY;
        connectionMessage = currentDiscordUser == null
                ? "Connected to Discord"
                : "Connected as " + currentDiscordUser.getEffectiveName();
        if (!loggedSuccessfulPresenceUpdate) {
            loggedSuccessfulPresenceUpdate = true;
            Presencejs.LOGGER.info(
                    "PresenceJS accepted Discord activity update: details='{}', state='{}'",
                    activity.getDetails(),
                    activity.getState());
        }
        scheduleSendIfPossible();
    }

    private void handleSendFailure(DiscordIpcClient targetClient, Throwable throwable) {
        if (client != targetClient) {
            return;
        }

        sendInFlight = false;
        Presencejs.LOGGER.error("Failed to send Discord Rich Presence update", throwable);
        disconnectClient(PresenceConnectionState.ERROR, "Failed to send Discord Rich Presence", true);
    }

    private void closeClientAsync(DiscordIpcClient closingClient) {
        ioExecutor.execute(() -> {
            try {
                closingClient.close();
            } catch (Throwable throwable) {
                if (Config.debugLogging) {
                    Presencejs.LOGGER.debug("Ignoring Discord RPC close failure", throwable);
                }
            }
        });
    }
}








