package org.presencejs.presencejs.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;

public class PresenceJSKubeJSPlugin extends KubeJSPlugin {
    @Override
    public void registerEvents() {
        PresenceJSEvents.GROUP.register();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        if (event.getType() == ScriptType.CLIENT) {
            event.add("PresenceJS", PresenceJSBindings.INSTANCE);
        }
    }

    @Override
    public void registerClasses(ScriptType scriptType, ClassFilter classFilter) {
        if (scriptType == ScriptType.CLIENT) {
            classFilter.allow("org.presencejs.presencejs.client");
            classFilter.allow("org.presencejs.presencejs.kubejs");
            classFilter.allow("com.google.gson");
        }
    }
}

