package org.presencejs.presencejs.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class PresenceActivity {
    private static final String DEFAULT_ACTIVITY_TYPE = "PLAYING";
    private static final String DEFAULT_PARTY_PRIVACY = "PUBLIC";

    private boolean enabled = true;
    private boolean replaceDefaults;
    private String clientId;
    private String steamId;
    private String name;
    private String activityType = DEFAULT_ACTIVITY_TYPE;
    private String state;
    private String details;
    private Long startTimestamp;
    private Long endTimestamp;
    private final Image largeImage = new Image();
    private final Image smallImage = new Image();
    private final List<Button> buttons = new ArrayList<>();
    private String partyId;
    private Integer partySize;
    private Integer partyMax;
    private String partyPrivacy = DEFAULT_PARTY_PRIVACY;
    private String matchSecret;
    private String joinSecret;
    private String spectateSecret;
    private Boolean instance;

    public PresenceActivity() {
    }

    public PresenceActivity(PresenceActivity other) {
        enabled = other.enabled;
        replaceDefaults = other.replaceDefaults;
        clientId = other.clientId;
        steamId = other.steamId;
        name = other.name;
        activityType = other.activityType;
        state = other.state;
        details = other.details;
        startTimestamp = other.startTimestamp;
        endTimestamp = other.endTimestamp;
        largeImage.copyFrom(other.largeImage);
        smallImage.copyFrom(other.smallImage);
        buttons.clear();
        other.buttons.stream().map(Button::new).forEach(buttons::add);
        partyId = other.partyId;
        partySize = other.partySize;
        partyMax = other.partyMax;
        partyPrivacy = other.partyPrivacy;
        matchSecret = other.matchSecret;
        joinSecret = other.joinSecret;
        spectateSecret = other.spectateSecret;
        instance = other.instance;
    }

    public PresenceActivity copy() {
        return new PresenceActivity(this);
    }

    public void clear() {
        enabled = true;
        name = null;
        state = null;
        details = null;
        startTimestamp = null;
        endTimestamp = null;
        clearLargeImage();
        clearSmallImage();
        clearButtons();
        clearParty();
        clearSecrets();
        instance = null;
    }

    public void merge(PresenceActivity other) {
        enabled = other.enabled;
        replaceDefaults = other.replaceDefaults;
        clientId = firstNonBlank(other.clientId, clientId);
        steamId = firstNonBlank(other.steamId, steamId);
        name = firstNonBlank(other.name, name);
        activityType = firstNonBlank(other.activityType, activityType);
        state = firstNonBlank(other.state, state);
        details = firstNonBlank(other.details, details);
        startTimestamp = other.startTimestamp != null ? other.startTimestamp : startTimestamp;
        endTimestamp = other.endTimestamp != null ? other.endTimestamp : endTimestamp;
        if (other.largeImage.hasData()) {
            largeImage.copyFrom(other.largeImage);
        }
        if (other.smallImage.hasData()) {
            smallImage.copyFrom(other.smallImage);
        }
        if (!other.buttons.isEmpty()) {
            clearButtons();
            other.buttons.stream().map(Button::new).forEach(buttons::add);
        }
        partyId = firstNonBlank(other.partyId, partyId);
        partySize = other.partySize != null ? other.partySize : partySize;
        partyMax = other.partyMax != null ? other.partyMax : partyMax;
        partyPrivacy = firstNonBlank(other.partyPrivacy, partyPrivacy);
        matchSecret = firstNonBlank(other.matchSecret, matchSecret);
        joinSecret = firstNonBlank(other.joinSecret, joinSecret);
        spectateSecret = firstNonBlank(other.spectateSecret, spectateSecret);
        instance = other.instance != null ? other.instance : instance;
    }

    private static String firstNonBlank(String primary, String fallback) {
        return isBlank(primary) ? fallback : primary;
    }

    public JsonObject toDiscordJson() {
        JsonObject activityJson = new JsonObject();
        JsonObject timestampsJson = new JsonObject();
        JsonObject assetsJson = new JsonObject();
        JsonObject partyJson = new JsonObject();
        JsonObject secretsJson = new JsonObject();

        activityJson.addProperty("type", Integer.valueOf(parseActivityTypeCode(activityType)));

        String sanitizedName = sanitize(name, 128);
        if (sanitizedName != null) {
            activityJson.addProperty("name", sanitizedName);
        }

        String sanitizedState = sanitize(state, 128);
        if (sanitizedState != null) {
            activityJson.addProperty("state", sanitizedState);
        }

        String sanitizedDetails = sanitize(details, 128);
        if (sanitizedDetails != null) {
            activityJson.addProperty("details", sanitizedDetails);
        }

        if (startTimestamp != null && startTimestamp >= 0L) {
            timestampsJson.addProperty("start", startTimestamp);
        }

        if (endTimestamp != null && endTimestamp >= 0L) {
            timestampsJson.addProperty("end", endTimestamp);
        }

        if (largeImage.hasData()) {
            String largeKey = sanitize(largeImage.getKey(), 128);
            if (largeKey != null) {
                assetsJson.addProperty("large_image", largeKey);
                String largeText = sanitize(largeImage.getText(), 128);
                if (largeText != null) {
                    assetsJson.addProperty("large_text", largeText);
                }
            }
        }

        if (smallImage.hasData()) {
            String smallKey = sanitize(smallImage.getKey(), 128);
            if (smallKey != null) {
                assetsJson.addProperty("small_image", smallKey);
                String smallText = sanitize(smallImage.getText(), 128);
                if (smallText != null) {
                    assetsJson.addProperty("small_text", smallText);
                }
            }
        }

        if (!buttons.isEmpty()) {
            JsonArray buttonsArray = new JsonArray();
            for (Button button : buttons.stream().limit(2).toList()) {
                String label = sanitize(button.getLabel(), 32);
                String url = sanitize(button.getUrl(), 512);
                if (label == null || url == null) {
                    continue;
                }

                JsonObject buttonJson = new JsonObject();
                buttonJson.addProperty("label", label);
                buttonJson.addProperty("url", url);
                buttonsArray.add(buttonJson);
            }

            if (buttonsArray.size() > 0) {
                activityJson.add("buttons", buttonsArray);
            }
        }

        if (!isBlank(partyId) && partySize != null && partyMax != null) {
            String sanitizedPartyId = sanitize(partyId, 128);
            if (sanitizedPartyId != null) {
                partyJson.addProperty("id", sanitizedPartyId);
                JsonArray partySizeJson = new JsonArray();
                partySizeJson.add(new JsonPrimitive(Integer.valueOf(Math.max(0, partySize))));
                partySizeJson.add(new JsonPrimitive(Integer.valueOf(Math.max(Math.max(0, partySize), Math.max(0, partyMax)))));
                partyJson.add("size", partySizeJson);
                partyJson.addProperty("privacy", Integer.valueOf(parsePartyPrivacyCode(partyPrivacy)));
            }
        }

        if (!isBlank(matchSecret)) {
            secretsJson.addProperty("match", sanitize(matchSecret, 128));
        }
        if (!isBlank(joinSecret)) {
            secretsJson.addProperty("join", sanitize(joinSecret, 128));
        }
        if (!isBlank(spectateSecret)) {
            secretsJson.addProperty("spectate", sanitize(spectateSecret, 128));
        }

        if (timestampsJson.size() > 0) {
            activityJson.add("timestamps", timestampsJson);
        }
        if (assetsJson.size() > 0) {
            activityJson.add("assets", assetsJson);
        }
        if (partyJson.size() > 0) {
            activityJson.add("party", partyJson);
        }
        if (secretsJson.size() > 0) {
            activityJson.add("secrets", secretsJson);
        }

        if (instance != null) {
            activityJson.addProperty("instance", instance.booleanValue());
        }
        return activityJson;
    }

    private static int parseActivityTypeCode(String value) {
        if (isBlank(value)) {
            return 0;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PLAYING" -> 0;
            case "STREAMING" -> 1;
            case "LISTENING" -> 2;
            case "WATCHING" -> 3;
            case "CUSTOM" -> 4;
            case "COMPETING" -> 5;
            default -> 0;
        };
    }

    private static int parsePartyPrivacyCode(String value) {
        if (isBlank(value)) {
            return 1;
        }

        return value.trim().equalsIgnoreCase("private") ? 0 : 1;
    }

    private static String sanitize(String value, int maxLength) {
        if (isBlank(value)) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isReplaceDefaults() {
        return replaceDefaults;
    }

    public void setReplaceDefaults(boolean replaceDefaults) {
        this.replaceDefaults = replaceDefaults;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String resolveClientId(String fallback) {
        return isBlank(clientId) ? fallback : clientId;
    }

    public String getSteamId() {
        return steamId;
    }

    public void setSteamId(String steamId) {
        this.steamId = steamId;
    }

    public String resolveSteamId(String fallback) {
        return isBlank(steamId) ? fallback : steamId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void clearName() {
        name = null;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void clearState() {
        state = null;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public void clearDetails() {
        details = null;
    }

    public Long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(Long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public void startNow() {
        startTimestamp = System.currentTimeMillis() / 1000L;
    }

    public Long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(Long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public void endInSeconds(long secondsFromNow) {
        endTimestamp = (System.currentTimeMillis() / 1000L) + Math.max(0L, secondsFromNow);
    }

    public Image getLargeImage() {
        return largeImage;
    }

    public void setLargeImage(String key) {
        largeImage.setKey(key);
        largeImage.setText(null);
    }

    public void setLargeImage(String key, String text) {
        largeImage.setKey(key);
        largeImage.setText(text);
    }

    public void clearLargeImage() {
        largeImage.clear();
    }

    public Image getSmallImage() {
        return smallImage;
    }

    public void setSmallImage(String key) {
        smallImage.setKey(key);
        smallImage.setText(null);
    }

    public void setSmallImage(String key, String text) {
        smallImage.setKey(key);
        smallImage.setText(text);
    }

    public void clearSmallImage() {
        smallImage.clear();
    }

    public List<Button> getButtons() {
        return buttons;
    }

    public void addButton(String label, String url) {
        if (buttons.size() < 2) {
            buttons.add(new Button(label, url));
        }
    }

    public void addButton(Button button) {
        if (button != null && buttons.size() < 2) {
            buttons.add(new Button(button));
        }
    }

    public void clearButtons() {
        buttons.clear();
    }

    public String getPartyId() {
        return partyId;
    }

    public Integer getPartySize() {
        return partySize;
    }

    public Integer getPartyMax() {
        return partyMax;
    }

    public String getPartyPrivacy() {
        return partyPrivacy;
    }

    public void setParty(String id, int size, int max, String privacy) {
        partyId = id;
        partySize = size;
        partyMax = max;
        partyPrivacy = privacy;
    }

    public void clearParty() {
        partyId = null;
        partySize = null;
        partyMax = null;
        partyPrivacy = DEFAULT_PARTY_PRIVACY;
    }

    public String getMatchSecret() {
        return matchSecret;
    }

    public void setMatchSecret(String matchSecret) {
        this.matchSecret = matchSecret;
    }

    public String getJoinSecret() {
        return joinSecret;
    }

    public void setJoinSecret(String joinSecret) {
        this.joinSecret = joinSecret;
    }

    public String getSpectateSecret() {
        return spectateSecret;
    }

    public void setSpectateSecret(String spectateSecret) {
        this.spectateSecret = spectateSecret;
    }

    public void clearSecrets() {
        matchSecret = null;
        joinSecret = null;
        spectateSecret = null;
    }

    public Boolean getInstance() {
        return instance;
    }

    public void setInstance(Boolean instance) {
        this.instance = instance;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PresenceActivity that)) {
            return false;
        }
        return enabled == that.enabled
                && replaceDefaults == that.replaceDefaults
                && Objects.equals(clientId, that.clientId)
                && Objects.equals(steamId, that.steamId)
                && Objects.equals(name, that.name)
                && Objects.equals(activityType, that.activityType)
                && Objects.equals(state, that.state)
                && Objects.equals(details, that.details)
                && Objects.equals(startTimestamp, that.startTimestamp)
                && Objects.equals(endTimestamp, that.endTimestamp)
                && Objects.equals(largeImage, that.largeImage)
                && Objects.equals(smallImage, that.smallImage)
                && Objects.equals(buttons, that.buttons)
                && Objects.equals(partyId, that.partyId)
                && Objects.equals(partySize, that.partySize)
                && Objects.equals(partyMax, that.partyMax)
                && Objects.equals(partyPrivacy, that.partyPrivacy)
                && Objects.equals(matchSecret, that.matchSecret)
                && Objects.equals(joinSecret, that.joinSecret)
                && Objects.equals(spectateSecret, that.spectateSecret)
                && Objects.equals(instance, that.instance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                enabled,
                replaceDefaults,
                clientId,
                steamId,
                name,
                activityType,
                state,
                details,
                startTimestamp,
                endTimestamp,
                largeImage,
                smallImage,
                buttons,
                partyId,
                partySize,
                partyMax,
                partyPrivacy,
                matchSecret,
                joinSecret,
                spectateSecret,
                instance);
    }

    public static final class Image {
        private String key;
        private String text;

        public Image() {
        }

        public Image(Image other) {
            copyFrom(other);
        }

        public void copyFrom(Image other) {
            key = other.key;
            text = other.text;
        }

        public void clear() {
            key = null;
            text = null;
        }

        public boolean hasData() {
            return !isBlank(key);
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Image image)) {
                return false;
            }
            return Objects.equals(key, image.key) && Objects.equals(text, image.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, text);
        }
    }

    public static final class Button {
        private String label;
        private String url;

        public Button() {
        }

        public Button(String label, String url) {
            this.label = label;
            this.url = url;
        }

        public Button(Button other) {
            label = other.label;
            url = other.url;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Button button)) {
                return false;
            }
            return Objects.equals(label, button.label) && Objects.equals(url, button.url);
        }

        @Override
        public int hashCode() {
            return Objects.hash(label, url);
        }
    }
}





