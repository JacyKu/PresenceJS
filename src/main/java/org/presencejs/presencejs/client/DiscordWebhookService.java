package org.presencejs.presencejs.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.presencejs.presencejs.Config;

public final class DiscordWebhookService {
    private static final String DISCORD_API_BASE = "https://discord.com/api/v10";
    private static final Set<String> ALLOWED_DISCORD_WEBHOOK_HOSTS = Set.of(
            "discord.com",
            "ptb.discord.com",
            "canary.discord.com",
            "discordapp.com",
            "ptb.discordapp.com",
            "canary.discordapp.com");
    private static final DiscordWebhookService INSTANCE = new DiscordWebhookService();
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20L))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final ExecutorService ioExecutor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "PresenceJS-Webhook-" + THREAD_COUNTER.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });
    private final Queue<Runnable> mainThreadActions = new ConcurrentLinkedQueue<>();
    private final Map<Long, CompletableFuture<DiscordWebhookResponse>> pendingRequests = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong();
    private volatile boolean bootstrapped;

    private DiscordWebhookService() {
    }

    public static DiscordWebhookService get() {
        return INSTANCE;
    }

    public void bootstrap() {
        if (bootstrapped) {
            return;
        }

        bootstrapped = true;
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "PresenceJS-WebhookShutdown"));
    }

    public void tick() {
        if (!bootstrapped) {
            bootstrap();
        }

        Runnable action;
        while ((action = mainThreadActions.poll()) != null) {
            action.run();
        }
    }

    public DiscordWebhookRequest request(String method, String pathOrUrl) {
        return DiscordWebhookRequest.of(method, pathOrUrl);
    }

    public DiscordWebhookRequest createWebhook(String channelId) {
        return request("POST", "/channels/" + encodePathSegment(channelId) + "/webhooks").operation("createWebhook");
    }

    public DiscordWebhookRequest getChannelWebhooks(String channelId) {
        return request("GET", "/channels/" + encodePathSegment(channelId) + "/webhooks").operation("getChannelWebhooks");
    }

    public DiscordWebhookRequest getGuildWebhooks(String guildId) {
        return request("GET", "/guilds/" + encodePathSegment(guildId) + "/webhooks").operation("getGuildWebhooks");
    }

    public DiscordWebhookRequest getWebhook(String webhookId) {
        return request("GET", "/webhooks/" + encodePathSegment(webhookId)).operation("getWebhook");
    }

    public DiscordWebhookRequest getWebhookWithToken(String webhookId, String webhookToken) {
        return request("GET", tokenPath(webhookId, webhookToken)).operation("getWebhookWithToken");
    }

    public DiscordWebhookRequest getWebhookWithToken(String webhookUrl) {
        WebhookUrlParts webhookUrlParts = parseWebhookUrl(webhookUrl);
        return getWebhookWithToken(webhookUrlParts.id(), webhookUrlParts.token());
    }

    public DiscordWebhookRequest modifyWebhook(String webhookId) {
        return request("PATCH", "/webhooks/" + encodePathSegment(webhookId)).operation("modifyWebhook");
    }

    public DiscordWebhookRequest modifyWebhookWithToken(String webhookId, String webhookToken) {
        return request("PATCH", tokenPath(webhookId, webhookToken)).operation("modifyWebhookWithToken");
    }

    public DiscordWebhookRequest modifyWebhookWithToken(String webhookUrl) {
        WebhookUrlParts webhookUrlParts = parseWebhookUrl(webhookUrl);
        return modifyWebhookWithToken(webhookUrlParts.id(), webhookUrlParts.token());
    }

    public DiscordWebhookRequest deleteWebhook(String webhookId) {
        return request("DELETE", "/webhooks/" + encodePathSegment(webhookId)).operation("deleteWebhook");
    }

    public DiscordWebhookRequest deleteWebhookWithToken(String webhookId, String webhookToken) {
        return request("DELETE", tokenPath(webhookId, webhookToken)).operation("deleteWebhookWithToken");
    }

    public DiscordWebhookRequest deleteWebhookWithToken(String webhookUrl) {
        WebhookUrlParts webhookUrlParts = parseWebhookUrl(webhookUrl);
        return deleteWebhookWithToken(webhookUrlParts.id(), webhookUrlParts.token());
    }

    public DiscordWebhookRequest executeWebhook(String webhookId, String webhookToken) {
        return request("POST", tokenPath(webhookId, webhookToken)).operation("executeWebhook");
    }

    public DiscordWebhookRequest executeWebhook(String webhookUrl) {
        WebhookUrlParts webhookUrlParts = parseWebhookUrl(webhookUrl);
        return executeWebhook(webhookUrlParts.id(), webhookUrlParts.token());
    }

    public DiscordWebhookRequest executeSlackCompatibleWebhook(String webhookId, String webhookToken) {
        return request("POST", tokenPath(webhookId, webhookToken) + "/slack")
                .operation("executeSlackCompatibleWebhook");
    }

    public DiscordWebhookRequest executeSlackCompatibleWebhook(String webhookUrl) {
        WebhookUrlParts webhookUrlParts = parseWebhookUrl(webhookUrl);
        return executeSlackCompatibleWebhook(webhookUrlParts.id(), webhookUrlParts.token());
    }

    public DiscordWebhookRequest executeGithubCompatibleWebhook(String webhookId, String webhookToken) {
        return request("POST", tokenPath(webhookId, webhookToken) + "/github")
                .operation("executeGithubCompatibleWebhook");
    }

    public DiscordWebhookRequest executeGithubCompatibleWebhook(String webhookUrl) {
        WebhookUrlParts webhookUrlParts = parseWebhookUrl(webhookUrl);
        return executeGithubCompatibleWebhook(webhookUrlParts.id(), webhookUrlParts.token());
    }

    public DiscordWebhookRequest getWebhookMessage(String webhookId, String webhookToken, String messageId) {
        return request("GET", messagePath(webhookId, webhookToken, messageId)).operation("getWebhookMessage");
    }

    public DiscordWebhookRequest getWebhookMessage(String webhookUrl, String messageId) {
        WebhookUrlParts webhookUrlParts = parseWebhookUrl(webhookUrl);
        return getWebhookMessage(webhookUrlParts.id(), webhookUrlParts.token(), messageId);
    }

    public DiscordWebhookRequest editWebhookMessage(String webhookId, String webhookToken, String messageId) {
        return request("PATCH", messagePath(webhookId, webhookToken, messageId)).operation("editWebhookMessage");
    }

    public DiscordWebhookRequest editWebhookMessage(String webhookUrl, String messageId) {
        WebhookUrlParts webhookUrlParts = parseWebhookUrl(webhookUrl);
        return editWebhookMessage(webhookUrlParts.id(), webhookUrlParts.token(), messageId);
    }

    public DiscordWebhookRequest deleteWebhookMessage(String webhookId, String webhookToken, String messageId) {
        return request("DELETE", messagePath(webhookId, webhookToken, messageId)).operation("deleteWebhookMessage");
    }

    public DiscordWebhookRequest deleteWebhookMessage(String webhookUrl, String messageId) {
        WebhookUrlParts webhookUrlParts = parseWebhookUrl(webhookUrl);
        return deleteWebhookMessage(webhookUrlParts.id(), webhookUrlParts.token(), messageId);
    }

    public DiscordWebhookFilePart file(String path) {
        return DiscordWebhookFilePart.fromPath(path);
    }

    public DiscordWebhookFilePart file(String path, String filename) {
        return DiscordWebhookFilePart.fromPath(path, filename);
    }

    public DiscordWebhookFilePart file(String path, String filename, String contentType) {
        return DiscordWebhookFilePart.fromPath(path, filename, contentType);
    }

    public DiscordWebhookFilePart textFile(String filename, String content) {
        return DiscordWebhookFilePart.fromText(filename, content);
    }

    public DiscordWebhookFilePart textFile(String filename, String content, String contentType) {
        return DiscordWebhookFilePart.fromText(filename, content, contentType);
    }

    public boolean isEnabled() {
        return Config.webhookEnabled;
    }

    public DiscordWebhookResponse execute(DiscordWebhookRequest request) {
        DiscordWebhookRequest preparedRequest = requireRequest(request);
        requireEnabled(preparedRequest);

        try {
            HttpResponse<String> response = httpClient.send(buildHttpRequest(preparedRequest), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return DiscordWebhookResponse.from(response);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DiscordWebhookException("Webhook request was interrupted", exception, preparedRequest);
        } catch (IOException exception) {
            throw new DiscordWebhookException("Webhook request failed: " + firstNonBlank(exception.getMessage(), "I/O error"), exception, preparedRequest);
        }
    }

    public long submit(DiscordWebhookRequest request) {
        DiscordWebhookRequest submittedRequest = requireRequest(request).copy();
        requireEnabled(submittedRequest);
        long requestId = requestCounter.incrementAndGet();
        CompletableFuture<DiscordWebhookResponse> future = CompletableFuture.supplyAsync(() -> execute(submittedRequest), ioExecutor);
        pendingRequests.put(requestId, future);
        future.whenComplete((response, throwable) -> mainThreadActions.add(() -> handleCompletion(requestId, submittedRequest, response, throwable)));
        return requestId;
    }

    public boolean cancel(long requestId) {
        CompletableFuture<DiscordWebhookResponse> future = pendingRequests.remove(requestId);
        return future != null && future.cancel(true);
    }

    public int getPendingCount() {
        return pendingRequests.size();
    }

    private void handleCompletion(
            long requestId,
            DiscordWebhookRequest request,
            DiscordWebhookResponse response,
            Throwable throwable) {
        if (pendingRequests.remove(requestId) == null) {
            return;
        }

        if (throwable == null) {
            if (PresenceKubeJSCompat.isAvailable()) {
                PresenceKubeJSCompat.postWebhookResponse(requestId, request, response);
            }
            return;
        }

        Throwable failure = unwrapAsyncThrowable(throwable);
        if (failure instanceof CancellationException) {
            return;
        }

        String failureMessage = failure == null
                ? "Webhook request failed"
                : firstNonBlank(failure.getMessage(), "Webhook request failed");
        String failureType = failure == null ? Throwable.class.getName() : failure.getClass().getName();

        if (PresenceKubeJSCompat.isAvailable()) {
            PresenceKubeJSCompat.postWebhookError(
                    requestId,
                    request,
                    failureMessage,
                    failureType);
        }
    }

    private void shutdown() {
        pendingRequests.values().forEach(future -> future.cancel(true));
        pendingRequests.clear();
        ioExecutor.shutdownNow();
    }

    private HttpRequest buildHttpRequest(DiscordWebhookRequest request) {
        URI uri = resolveUri(request);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30L))
                .header("User-Agent", "PresenceJS/1.0")
                .header("Accept", "application/json");

        if (!isBlank(request.getAuthorization())) {
            builder.header("Authorization", request.getAuthorization());
        }
        if (!isBlank(request.getAuditLogReason())) {
            builder.header("X-Audit-Log-Reason", urlEncode(request.getAuditLogReason()));
        }
        request.getHeaders().forEach(builder::header);

        HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.noBody();
        if (!request.getFiles().isEmpty()) {
            String boundary = "PresenceJSBoundary" + UUID.randomUUID().toString().replace("-", "");
            builder.header("Content-Type", "multipart/form-data; boundary=" + boundary);
            publisher = HttpRequest.BodyPublishers.ofByteArray(buildMultipartPayload(request, boundary));
        } else if (request.getJsonBody() != null) {
            builder.header("Content-Type", "application/json; charset=UTF-8");
            publisher = HttpRequest.BodyPublishers.ofString(request.getJsonBody().toString(), StandardCharsets.UTF_8);
        }

        return builder.method(request.getMethod(), publisher).build();
    }

    private URI resolveUri(DiscordWebhookRequest request) {
        String path = request.getPath();
        URI baseUri;
        if (path.regionMatches(true, 0, "http://", 0, 7) || path.regionMatches(true, 0, "https://", 0, 8)) {
            baseUri = validateAbsoluteWebhookUri(path);
        } else {
            String validatedPath = validateRelativeWebhookPath(path);
            baseUri = URI.create(DISCORD_API_BASE + validatedPath);
        }

        String query = buildQueryString(request.getQueryParameters());
        StringBuilder uriBuilder = new StringBuilder(baseUri.toString());
        if (!query.isEmpty()) {
            uriBuilder.append(uriBuilder.indexOf("?") >= 0 ? '&' : '?').append(query);
        }
        return URI.create(uriBuilder.toString());
    }

    private byte[] buildMultipartPayload(DiscordWebhookRequest request, String boundary) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (request.getJsonBody() != null) {
                writeMultipartPart(output, boundary, "payload_json", null, "application/json; charset=UTF-8", request.getJsonBody().toString().getBytes(StandardCharsets.UTF_8));
            }

            int index = 0;
            for (DiscordWebhookFilePart filePart : request.getFiles()) {
                writeMultipartPart(
                        output,
                        boundary,
                        "files[" + index + "]",
                        filePart.getFilename(),
                        filePart.getContentType(),
                        filePart.getData());
                index++;
            }

            output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return output.toByteArray();
        } catch (IOException exception) {
            throw new DiscordWebhookException(
                    "Failed to build multipart webhook request",
                    exception,
                    request);
        }
    }

    private void writeMultipartPart(
            ByteArrayOutputStream output,
            String boundary,
            String fieldName,
            String filename,
            String contentType,
            byte[] data)
            throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));

        StringBuilder disposition = new StringBuilder("Content-Disposition: form-data; name=\"")
                .append(escapeHeaderValue(fieldName))
                .append("\"");
        if (!isBlank(filename)) {
            disposition.append("; filename=\"").append(escapeHeaderValue(filename)).append("\"");
        }

        output.write((disposition + "\r\n").getBytes(StandardCharsets.UTF_8));
        if (!isBlank(contentType)) {
            output.write(("Content-Type: " + contentType + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
        if (data != null && data.length > 0) {
            output.write(data);
        }
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static DiscordWebhookRequest requireRequest(DiscordWebhookRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        return request;
    }

    private static void requireEnabled(DiscordWebhookRequest request) {
        if (!Config.webhookEnabled) {
            throw new DiscordWebhookException(
                    "PresenceJS webhook functionality is disabled in the client config. Enable webhookEnabled before sending webhook requests.",
                    request);
        }
    }

    private static String messagePath(String webhookId, String webhookToken, String messageId) {
        return tokenPath(webhookId, webhookToken) + "/messages/" + encodePathSegment(messageId);
    }

    private static WebhookUrlParts parseWebhookUrl(String webhookUrl) {
        URI uri = validateAbsoluteWebhookUri(webhookUrl);
        String path = uri.getPath();
        String[] segments = path.split("/");
        for (int index = 0; index < segments.length; index++) {
            if (!"webhooks".equals(segments[index])) {
                continue;
            }

            if (index + 2 >= segments.length || isBlank(segments[index + 1]) || isBlank(segments[index + 2])) {
                break;
            }

            return new WebhookUrlParts(
                    URLDecoder.decode(segments[index + 1], StandardCharsets.UTF_8),
                    URLDecoder.decode(segments[index + 2], StandardCharsets.UTF_8));
        }

        throw new IllegalArgumentException("Discord webhook URL must contain /webhooks/{id}/{token}: " + webhookUrl);
    }

    private static URI validateAbsoluteWebhookUri(String webhookUrl) {
        if (isBlank(webhookUrl)) {
            throw new IllegalArgumentException("Discord webhook URL must not be blank");
        }

        URI uri;
        try {
            uri = URI.create(webhookUrl.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid Discord webhook URL: " + webhookUrl, exception);
        }

        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Discord webhook URLs must use HTTPS");
        }
        if (!isBlank(uri.getUserInfo())) {
            throw new IllegalArgumentException("Discord webhook URLs must not include user info");
        }

        String host = uri.getHost();
        if (isBlank(host) || !ALLOWED_DISCORD_WEBHOOK_HOSTS.contains(host.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Only official Discord webhook hosts are allowed");
        }
        if (uri.getPort() != -1 && uri.getPort() != 443) {
            throw new IllegalArgumentException("Discord webhook URLs must use the default HTTPS port");
        }
        if (!isBlank(uri.getFragment())) {
            throw new IllegalArgumentException("Discord webhook URLs must not include fragments");
        }
        if (!isBlank(uri.getRawQuery())) {
            throw new IllegalArgumentException("Discord webhook URLs must not inline query parameters; use request.query(...) instead");
        }

        String path = uri.getPath();
        if (!Objects.equals(uri.normalize().getPath(), path)) {
            throw new IllegalArgumentException("Discord webhook URLs must not contain path traversal segments");
        }
        if (!isAbsoluteWebhookPath(path)) {
            throw new IllegalArgumentException("Only Discord webhook URLs are allowed");
        }
        return uri;
    }

    private static String validateRelativeWebhookPath(String path) {
        if (isBlank(path)) {
            throw new IllegalArgumentException("Discord webhook path must not be blank");
        }

        String trimmed = path.trim();
        if (!trimmed.startsWith("/")) {
            trimmed = "/" + trimmed;
        }
        if (trimmed.indexOf('?') >= 0 || trimmed.indexOf('#') >= 0) {
            throw new IllegalArgumentException("Discord webhook paths must not inline query parameters or fragments");
        }
        if (!Objects.equals(URI.create(trimmed).normalize().getPath(), trimmed)) {
            throw new IllegalArgumentException("Discord webhook paths must not contain path traversal segments");
        }

        if (!isAllowedRelativeWebhookPath(trimmed)) {
            throw new IllegalArgumentException("Only Discord webhook API paths are allowed");
        }
        return trimmed;
    }

    private static boolean isAllowedRelativeWebhookPath(String path) {
        return isWebhookResourcePath(path)
                || isChannelWebhookCollectionPath(path)
                || isGuildWebhookCollectionPath(path);
    }

    private static boolean isAbsoluteWebhookPath(String path) {
        return isWebhookResourcePath(stripApiPrefix(path));
    }

    private static String stripApiPrefix(String path) {
        if (path == null) {
            return null;
        }
        if (path.startsWith("/api/webhooks/")) {
            return path.substring(4);
        }

        if (path.startsWith("/api/v")) {
            int separatorIndex = path.indexOf('/', 6);
            if (separatorIndex > 0 && path.regionMatches(separatorIndex, "/webhooks/", 0, 10)) {
                return path.substring(separatorIndex);
            }
        }
        return path;
    }

    private static boolean isWebhookResourcePath(String path) {
        if (isBlank(path) || !path.startsWith("/webhooks/")) {
            return false;
        }

        String[] segments = path.split("/");
        if (segments.length < 3 || !hasTokenContent(segments[2])) {
            return false;
        }

        if (segments.length == 3) {
            return true;
        }

        if (!hasTokenContent(segments[3])) {
            return false;
        }

        if (segments.length == 4) {
            return true;
        }

        if (segments.length == 5) {
            return "slack".equals(segments[4]) || "github".equals(segments[4]);
        }

        return segments.length == 6 && "messages".equals(segments[4]) && hasTokenContent(segments[5]);
    }

    private static boolean isChannelWebhookCollectionPath(String path) {
        return isScopedWebhookCollectionPath(path, "/channels/");
    }

    private static boolean isGuildWebhookCollectionPath(String path) {
        return isScopedWebhookCollectionPath(path, "/guilds/");
    }

    private static boolean isScopedWebhookCollectionPath(String path, String prefix) {
        if (isBlank(path) || !path.startsWith(prefix)) {
            return false;
        }

        String[] segments = path.split("/");
        return segments.length == 4 && hasTokenContent(segments[2]) && "webhooks".equals(segments[3]);
    }

    private static boolean hasTokenContent(String value) {
        return value != null && !value.isBlank();
    }

    private static String tokenPath(String webhookId, String webhookToken) {
        return "/webhooks/" + encodePathSegment(webhookId) + "/" + encodePathSegment(webhookToken);
    }

    private static String encodePathSegment(String value) {
        if (isBlank(value)) {
            throw new IllegalArgumentException("Discord webhook path segment must not be blank");
        }
        return urlEncode(value.trim());
    }

    private static String buildQueryString(Map<String, String> queryParameters) {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
            if (query.length() > 0) {
                query.append('&');
            }

            query.append(urlEncode(entry.getKey()));
            if (entry.getValue() != null) {
                query.append('=').append(urlEncode(entry.getValue()));
            }
        }
        return query.toString();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String escapeHeaderValue(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "'");
    }

    private static String firstNonBlank(String primary, String fallback) {
        return isBlank(primary) ? fallback : primary;
    }

    private static Throwable unwrapAsyncThrowable(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null
                && (current instanceof java.util.concurrent.CompletionException
                || current instanceof java.util.concurrent.ExecutionException)) {
            current = current.getCause();
        }
        return current;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DiscordWebhookService that)) {
            return false;
        }
        return Objects.equals(httpClient, that.httpClient)
                && Objects.equals(ioExecutor, that.ioExecutor)
                && Objects.equals(mainThreadActions, that.mainThreadActions)
                && Objects.equals(pendingRequests, that.pendingRequests)
                && Objects.equals(requestCounter, that.requestCounter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(httpClient, ioExecutor, mainThreadActions, pendingRequests, requestCounter);
    }

    private record WebhookUrlParts(String id, String token) {
    }
}