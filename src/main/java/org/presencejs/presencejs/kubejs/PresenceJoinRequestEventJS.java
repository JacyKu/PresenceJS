package org.presencejs.presencejs.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;
import org.presencejs.presencejs.client.PresenceDiscordUser;

public class PresenceJoinRequestEventJS extends EventJS {
    private final PresenceDiscordUser user;
    private final String secret;
    private final Runnable approve;
    private final Runnable deny;
    private final Runnable ignore;

    public PresenceJoinRequestEventJS() {
        this(null, null, null, null, null);
    }

    public PresenceJoinRequestEventJS(
            PresenceDiscordUser user,
            String secret,
            Runnable approve,
            Runnable deny,
            Runnable ignore) {
        this.user = user;
        this.secret = secret;
        this.approve = approve;
        this.deny = deny;
        this.ignore = ignore;
    }

    public PresenceDiscordUser getUser() {
        return user;
    }

    public String getSecret() {
        return secret;
    }

    public void approve() {
        if (approve != null) {
            approve.run();
        }
    }

    public void deny() {
        if (deny != null) {
            deny.run();
        }
    }

    public void ignore() {
        if (ignore != null) {
            ignore.run();
        }
    }
}

