package org.presencejs.presencejs.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;
import org.presencejs.presencejs.client.PresenceActivity;
import org.presencejs.presencejs.client.PresenceContext;

public class PresenceBuildEventJS extends EventJS {
    private final PresenceActivity presence;
    private final PresenceContext context;

    public PresenceBuildEventJS() {
        this(new PresenceActivity(), null);
    }

    public PresenceBuildEventJS(PresenceActivity presence, PresenceContext context) {
        this.presence = presence;
        this.context = context;
    }

    public PresenceActivity getPresence() {
        return presence;
    }

    public PresenceContext getContext() {
        return context;
    }

    public void disable() {
        presence.setEnabled(false);
    }
}

