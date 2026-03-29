package org.presencejs.presencejs;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Presencejs.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Master toggle for PresenceJS.")
            .define("enabled", true);
    private static final ForgeConfigSpec.ConfigValue<String> CLIENT_ID = BUILDER
            .comment("Discord application ID used for Rich Presence. Leave blank to supply it from KubeJS instead.")
            .define("clientId", "");
    private static final ForgeConfigSpec.ConfigValue<String> STEAM_ID = BUILDER
            .comment("Optional Steam ID for Discord auto registration.")
            .define("steamId", "");
    private static final ForgeConfigSpec.BooleanValue AUTO_REGISTER = BUILDER
            .comment("Whether the Discord application should be auto-registered with the local Discord client.")
            .define("autoRegister", false);
    private static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("Enable additional PresenceJS debug logging.")
            .define("debugLogging", false);
    private static final ForgeConfigSpec.BooleanValue VERBOSE_LOGGING = BUILDER
            .comment("Enable verbose logging from the Discord IPC library.")
            .define("verboseLogging", false);
    private static final ForgeConfigSpec.ConfigValue<String> DEFAULT_ACTIVITY_TYPE = BUILDER
            .comment("Default Discord activity type. Valid values include PLAYING, STREAMING, LISTENING, WATCHING, CUSTOM, and COMPETING.")
            .define("defaultActivityType", "PLAYING");
    private static final ForgeConfigSpec.IntValue UPDATE_INTERVAL_TICKS = BUILDER
            .comment("How often PresenceJS should push updates to Discord when nothing else changes.")
            .defineInRange("updateIntervalTicks", 20, 1, 20 * 60);
    private static final ForgeConfigSpec.IntValue RECONNECT_INTERVAL_TICKS = BUILDER
            .comment("How many ticks to wait before retrying a failed Discord connection.")
            .defineInRange("reconnectIntervalTicks", 100, 20, 20 * 300);
    private static final ForgeConfigSpec.ConfigValue<String> DEFAULT_LARGE_IMAGE_KEY = BUILDER
            .comment("Default large image asset key.")
            .define("defaultLargeImageKey", "");
    private static final ForgeConfigSpec.ConfigValue<String> DEFAULT_LARGE_IMAGE_TEXT = BUILDER
            .comment("Default large image hover text.")
            .define("defaultLargeImageText", "PresenceJS");
    private static final ForgeConfigSpec.ConfigValue<String> DEFAULT_SMALL_IMAGE_KEY = BUILDER
            .comment("Default small image asset key.")
            .define("defaultSmallImageKey", "");
    private static final ForgeConfigSpec.ConfigValue<String> DEFAULT_SMALL_IMAGE_TEXT = BUILDER
            .comment("Default small image hover text.")
            .define("defaultSmallImageText", "");
    private static final ForgeConfigSpec.ConfigValue<String> MENU_DETAILS = BUILDER
            .comment("Automatic details text while the player is not in a world.")
            .define("menuDetails", "In the menus");
    private static final ForgeConfigSpec.ConfigValue<String> MENU_STATE = BUILDER
            .comment("Automatic state text while the player is not in a world.")
            .define("menuState", "Idle");
    private static final ForgeConfigSpec.ConfigValue<String> SINGLEPLAYER_DETAILS = BUILDER
            .comment("Automatic details text while playing singleplayer.")
            .define("singleplayerDetails", "Playing Singleplayer");
    private static final ForgeConfigSpec.ConfigValue<String> MULTIPLAYER_DETAILS = BUILDER
            .comment("Automatic details text while playing multiplayer.")
            .define("multiplayerDetails", "Playing Multiplayer");
    private static final ForgeConfigSpec.BooleanValue SHOW_WORLD_TIMESTAMP = BUILDER
            .comment("If enabled, automatic in-world presence uses the time when the world/server session started as the start timestamp.")
            .define("showWorldTimestamp", true);
    private static final ForgeConfigSpec.BooleanValue SHOW_MENU_TIMESTAMP = BUILDER
            .comment("If enabled, automatic menu presence uses the Minecraft session start as the start timestamp.")
            .define("showMenuTimestamp", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enabled;
    public static String clientId;
    public static String steamId;
    public static boolean autoRegister;
    public static boolean debugLogging;
    public static boolean verboseLogging;
    public static String defaultActivityType;
    public static int updateIntervalTicks;
    public static int reconnectIntervalTicks;
    public static String defaultLargeImageKey;
    public static String defaultLargeImageText;
    public static String defaultSmallImageKey;
    public static String defaultSmallImageText;
    public static String menuDetails;
    public static String menuState;
    public static String singleplayerDetails;
    public static String multiplayerDetails;
    public static boolean showWorldTimestamp;
    public static boolean showMenuTimestamp;

    private Config() {
    }

    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }

        enabled = ENABLED.get();
        clientId = CLIENT_ID.get();
        steamId = STEAM_ID.get();
        autoRegister = AUTO_REGISTER.get();
        debugLogging = DEBUG_LOGGING.get();
        verboseLogging = VERBOSE_LOGGING.get();
        defaultActivityType = DEFAULT_ACTIVITY_TYPE.get();
        updateIntervalTicks = UPDATE_INTERVAL_TICKS.get();
        reconnectIntervalTicks = RECONNECT_INTERVAL_TICKS.get();
        defaultLargeImageKey = DEFAULT_LARGE_IMAGE_KEY.get();
        defaultLargeImageText = DEFAULT_LARGE_IMAGE_TEXT.get();
        defaultSmallImageKey = DEFAULT_SMALL_IMAGE_KEY.get();
        defaultSmallImageText = DEFAULT_SMALL_IMAGE_TEXT.get();
        menuDetails = MENU_DETAILS.get();
        menuState = MENU_STATE.get();
        singleplayerDetails = SINGLEPLAYER_DETAILS.get();
        multiplayerDetails = MULTIPLAYER_DETAILS.get();
        showWorldTimestamp = SHOW_WORLD_TIMESTAMP.get();
        showMenuTimestamp = SHOW_MENU_TIMESTAMP.get();
    }
}
