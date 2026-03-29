package org.presencejs.presencejs.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;
import org.presencejs.presencejs.client.PresenceDiscordUser;

public class PresenceConnectionEventJS extends EventJS {
    private final String type;
    private final String message;
    private final PresenceDiscordUser user;
    private final String discordBuild;
    private final String secret;

    public PresenceConnectionEventJS() {
        this(null, null, null, null, null);
    }

    public PresenceConnectionEventJS(
            String type,
            String message,
            PresenceDiscordUser user,
            String discordBuild,
            String secret) {
        this.type = type;
        this.message = message;
        this.user = user;
        this.discordBuild = discordBuild;
        this.secret = secret;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public PresenceDiscordUser getUser() {
        return user;
    }

    public String getDiscordBuild() {
        return discordBuild;
    }

    public String getSecret() {
        return secret;
    }
}

