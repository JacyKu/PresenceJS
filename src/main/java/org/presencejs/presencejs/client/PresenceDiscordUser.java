package org.presencejs.presencejs.client;

import com.google.gson.JsonObject;
import java.util.Objects;

public final class PresenceDiscordUser {
    private final String name;
    private final String nickname;
    private final String effectiveName;
    private final String discriminator;
    private final String id;
    private final String avatarUrl;
    private final boolean bot;

    public PresenceDiscordUser(
            String name,
            String nickname,
            String effectiveName,
            String discriminator,
            String id,
            String avatarUrl,
            boolean bot) {
        this.name = name;
        this.nickname = nickname;
        this.effectiveName = effectiveName;
        this.discriminator = discriminator;
        this.id = id;
        this.avatarUrl = avatarUrl;
        this.bot = bot;
    }

    public static PresenceDiscordUser fromJson(JsonObject user) {
        if (user == null) {
            return null;
        }

        String id = getString(user, "id");
        String name = getString(user, "username");
        String nickname = getString(user, "global_name");
        String discriminator = firstNonBlank(getString(user, "discriminator"), "0");
        String avatarId = getString(user, "avatar");

        return new PresenceDiscordUser(
                name,
                nickname,
                firstNonBlank(nickname, name),
                discriminator,
                id,
                resolveAvatarUrl(id, avatarId, discriminator),
                user.has("bot") && !user.get("bot").isJsonNull() && user.get("bot").getAsBoolean());
    }

    private static String resolveAvatarUrl(String id, String avatarId, String discriminator) {
        if (id != null && avatarId != null && !avatarId.isBlank()) {
            return "https://cdn.discordapp.com/avatars/"
                    + id
                    + "/"
                    + avatarId
                    + (avatarId.startsWith("a_") ? ".gif" : ".png");
        }

        int defaultAvatarIndex;
        try {
            defaultAvatarIndex = "0".equals(discriminator)
                    ? (int) ((Long.parseLong(id) >> 22) % 6)
                    : Math.floorMod(Integer.parseInt(discriminator), 5);
        } catch (Throwable ignored) {
            defaultAvatarIndex = 0;
        }
        return "https://discord.com/assets/" + defaultAvatarIndex + ".png";
    }

    private static String getString(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        return object.get(key).getAsString();
    }

    private static String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    public String getName() {
        return name;
    }

    public String getNickname() {
        return nickname;
    }

    public String getEffectiveName() {
        return effectiveName;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    public String getId() {
        return id;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public boolean isBot() {
        return bot;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PresenceDiscordUser that)) {
            return false;
        }
        return bot == that.bot
                && Objects.equals(name, that.name)
                && Objects.equals(nickname, that.nickname)
                && Objects.equals(effectiveName, that.effectiveName)
                && Objects.equals(discriminator, that.discriminator)
                && Objects.equals(id, that.id)
                && Objects.equals(avatarUrl, that.avatarUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, nickname, effectiveName, discriminator, id, avatarUrl, bot);
    }
}



