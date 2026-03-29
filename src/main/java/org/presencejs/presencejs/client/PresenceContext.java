package org.presencejs.presencejs.client;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class PresenceContext {
    private static final Field CLIENT_ADVANCEMENTS_PROGRESS_FIELD = findClientAdvancementsProgressField();

    private final String username;
    private final boolean inWorld;
    private final boolean singleplayer;
    private final boolean multiplayer;
    private final boolean paused;
    private final String screenName;
    private final String screenTitle;
    private final String worldName;
    private final String serverName;
    private final String serverAddress;
    private final String dimensionId;
    private final String biomeId;
    private final String gameMode;
    private final String difficulty;
    private final double health;
    private final double maxHealth;
    private final int foodLevel;
    private final float saturation;
    private final int experienceLevel;
    private final int totalExperience;
    private final int advancementCount;
    private final int completedAdvancementCount;
    private final double x;
    private final double y;
    private final double z;
    private final String selectedItemId;
    private final String selectedItemName;
    private final long sessionStartEpochSecond;
    private final long worldStartEpochSecond;
    private final long nowEpochSecond;
    private final String changeToken;

    public PresenceContext(
            String username,
            boolean inWorld,
            boolean singleplayer,
            boolean multiplayer,
            boolean paused,
            String screenName,
            String screenTitle,
            String worldName,
            String serverName,
            String serverAddress,
            String dimensionId,
            String biomeId,
            String gameMode,
            String difficulty,
            double health,
            double maxHealth,
            int foodLevel,
            float saturation,
            int experienceLevel,
            int totalExperience,
            int advancementCount,
            int completedAdvancementCount,
            double x,
            double y,
            double z,
            String selectedItemId,
            String selectedItemName,
            long sessionStartEpochSecond,
            long worldStartEpochSecond,
            long nowEpochSecond,
            String changeToken) {
        this.username = username;
        this.inWorld = inWorld;
        this.singleplayer = singleplayer;
        this.multiplayer = multiplayer;
        this.paused = paused;
        this.screenName = screenName;
        this.screenTitle = screenTitle;
        this.worldName = worldName;
        this.serverName = serverName;
        this.serverAddress = serverAddress;
        this.dimensionId = dimensionId;
        this.biomeId = biomeId;
        this.gameMode = gameMode;
        this.difficulty = difficulty;
        this.health = health;
        this.maxHealth = maxHealth;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.experienceLevel = experienceLevel;
        this.totalExperience = totalExperience;
        this.advancementCount = advancementCount;
        this.completedAdvancementCount = completedAdvancementCount;
        this.x = x;
        this.y = y;
        this.z = z;
        this.selectedItemId = selectedItemId;
        this.selectedItemName = selectedItemName;
        this.sessionStartEpochSecond = sessionStartEpochSecond;
        this.worldStartEpochSecond = worldStartEpochSecond;
        this.nowEpochSecond = nowEpochSecond;
        this.changeToken = changeToken;
    }

    public static PresenceContext capture(long sessionStartEpochSecond, long worldStartEpochSecond) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        ServerData serverData = minecraft.getCurrentServer();

        boolean inWorld = player != null && level != null;
        boolean singleplayer = inWorld && minecraft.hasSingleplayerServer();
        boolean multiplayer = inWorld && !singleplayer;
        boolean paused = minecraft.isPaused();

        String screenName = minecraft.screen == null ? null : minecraft.screen.getClass().getSimpleName();
        String screenTitle = minecraft.screen == null || minecraft.screen.getTitle() == null
                ? null
                : minecraft.screen.getTitle().getString();
        String worldName = singleplayer && minecraft.getSingleplayerServer() != null
                ? minecraft.getSingleplayerServer().getWorldData().getLevelName()
                : null;
        String serverName = serverData == null ? null : serverData.name;
        String serverAddress = serverData == null ? null : serverData.ip;
        String dimensionId = level == null ? null : level.dimension().location().toString();
        String biomeId = level == null || player == null
                ? null
                : level.getBiome(player.blockPosition())
                        .unwrapKey()
                        .map(key -> key.location().toString())
                        .orElse(null);
        String gameMode = minecraft.gameMode == null || minecraft.gameMode.getPlayerMode() == null
                ? null
                : minecraft.gameMode.getPlayerMode().getName();
        String difficulty = level == null ? null : level.getDifficulty().getDisplayName().getString();

        double health = player == null ? 0D : player.getHealth();
        double maxHealth = player == null ? 0D : player.getMaxHealth();
        int foodLevel = player == null ? 0 : player.getFoodData().getFoodLevel();
        float saturation = player == null ? 0F : player.getFoodData().getSaturationLevel();
        int experienceLevel = player == null ? 0 : player.experienceLevel;
        int totalExperience = player == null ? 0 : player.totalExperience;
        AdvancementStats advancementStats = captureAdvancementStats(minecraft.getConnection());
        double x = player == null ? 0D : player.getX();
        double y = player == null ? 0D : player.getY();
        double z = player == null ? 0D : player.getZ();

        ItemStack selectedStack = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        ResourceLocation selectedItemKey = selectedStack.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(selectedStack.getItem());
        String selectedItemId = selectedItemKey == null ? null : selectedItemKey.toString();
        String selectedItemName = selectedStack.isEmpty() ? null : selectedStack.getHoverName().getString();
        long nowEpochSecond = System.currentTimeMillis() / 1000L;
        String username = minecraft.getUser() == null ? "Player" : minecraft.getUser().getName();

        String changeToken = String.join(
                "|",
                safeLocationToken(inWorld, singleplayer, multiplayer),
                safe(screenName),
                safe(screenTitle),
                safe(worldName),
                safe(serverName),
                safe(serverAddress),
                safe(dimensionId),
                safe(biomeId),
                safe(gameMode),
                safe(selectedItemId));

        return new PresenceContext(
                username,
                inWorld,
                singleplayer,
                multiplayer,
                paused,
                screenName,
                screenTitle,
                worldName,
                serverName,
                serverAddress,
                dimensionId,
                biomeId,
                gameMode,
                difficulty,
                health,
                maxHealth,
                foodLevel,
                saturation,
                experienceLevel,
                totalExperience,
                advancementStats.totalCount(),
                advancementStats.completedCount(),
                x,
                y,
                z,
                selectedItemId,
                selectedItemName,
                sessionStartEpochSecond,
                worldStartEpochSecond,
                nowEpochSecond,
                changeToken);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String safeLocationToken(boolean inWorld, boolean singleplayer, boolean multiplayer) {
        if (!inWorld) {
            return "menu";
        }
        if (singleplayer) {
            return "singleplayer";
        }
        if (multiplayer) {
            return "multiplayer";
        }
        return "world";
    }

    private static AdvancementStats captureAdvancementStats(ClientPacketListener connection) {
        if (connection == null) {
            return AdvancementStats.EMPTY;
        }

        ClientAdvancements clientAdvancements = connection.getAdvancements();
        if (clientAdvancements == null) {
            return AdvancementStats.EMPTY;
        }

        int totalCount = 0;
        Collection<?> advancements = clientAdvancements.getAdvancements().getAllAdvancements();
        if (advancements != null) {
            totalCount = advancements.size();
        }

        int completedCount = 0;
        Field progressField = CLIENT_ADVANCEMENTS_PROGRESS_FIELD;
        if (progressField != null) {
            try {
                Object progressValue = progressField.get(clientAdvancements);
                if (progressValue instanceof Map<?, ?> progressMap) {
                    for (Object value : progressMap.values()) {
                        if (value instanceof AdvancementProgress progress && progress.isDone()) {
                            completedCount++;
                        }
                    }
                }
            } catch (IllegalAccessException ignored) {
            }
        }

        return new AdvancementStats(totalCount, completedCount);
    }

    private static Field findClientAdvancementsProgressField() {
        try {
            Field field = ClientAdvancements.class.getDeclaredField("progress");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public String getUsername() {
        return username;
    }

    public boolean isInWorld() {
        return inWorld;
    }

    public boolean isSingleplayer() {
        return singleplayer;
    }

    public boolean isMultiplayer() {
        return multiplayer;
    }

    public boolean isPaused() {
        return paused;
    }

    public String getScreenName() {
        return screenName;
    }

    public String getScreenTitle() {
        return screenTitle;
    }

    public String getWorldName() {
        return worldName;
    }

    public String getServerName() {
        return serverName;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getDimensionId() {
        return dimensionId;
    }

    public String getBiomeId() {
        return biomeId;
    }

    public String getGameMode() {
        return gameMode;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public double getHealth() {
        return health;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public int getFoodLevel() {
        return foodLevel;
    }

    public float getSaturation() {
        return saturation;
    }

    public int getExperienceLevel() {
        return experienceLevel;
    }

    public int getTotalExperience() {
        return totalExperience;
    }

    public int getAdvancementCount() {
        return advancementCount;
    }

    public int getCompletedAdvancementCount() {
        return completedAdvancementCount;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public String getSelectedItemId() {
        return selectedItemId;
    }

    public String getSelectedItemName() {
        return selectedItemName;
    }

    public long getSessionStartEpochSecond() {
        return sessionStartEpochSecond;
    }

    public long getWorldStartEpochSecond() {
        return worldStartEpochSecond;
    }

    public long getNowEpochSecond() {
        return nowEpochSecond;
    }

    public long getSessionDurationSeconds() {
        return Math.max(0L, nowEpochSecond - sessionStartEpochSecond);
    }

    public long getWorldDurationSeconds() {
        return Math.max(0L, nowEpochSecond - worldStartEpochSecond);
    }

    public String getChangeToken() {
        return changeToken;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PresenceContext that)) {
            return false;
        }
        return inWorld == that.inWorld
                && singleplayer == that.singleplayer
                && multiplayer == that.multiplayer
                && paused == that.paused
                && Double.compare(that.health, health) == 0
                && Double.compare(that.maxHealth, maxHealth) == 0
                && foodLevel == that.foodLevel
                && Float.compare(that.saturation, saturation) == 0
                && experienceLevel == that.experienceLevel
                && totalExperience == that.totalExperience
                && advancementCount == that.advancementCount
                && completedAdvancementCount == that.completedAdvancementCount
                && Double.compare(that.x, x) == 0
                && Double.compare(that.y, y) == 0
                && Double.compare(that.z, z) == 0
                && sessionStartEpochSecond == that.sessionStartEpochSecond
                && worldStartEpochSecond == that.worldStartEpochSecond
                && nowEpochSecond == that.nowEpochSecond
                && Objects.equals(username, that.username)
                && Objects.equals(screenName, that.screenName)
                && Objects.equals(screenTitle, that.screenTitle)
                && Objects.equals(worldName, that.worldName)
                && Objects.equals(serverName, that.serverName)
                && Objects.equals(serverAddress, that.serverAddress)
                && Objects.equals(dimensionId, that.dimensionId)
                && Objects.equals(biomeId, that.biomeId)
                && Objects.equals(gameMode, that.gameMode)
                && Objects.equals(difficulty, that.difficulty)
                && Objects.equals(selectedItemId, that.selectedItemId)
                && Objects.equals(selectedItemName, that.selectedItemName)
                && Objects.equals(changeToken, that.changeToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                username,
                inWorld,
                singleplayer,
                multiplayer,
                paused,
                screenName,
                screenTitle,
                worldName,
                serverName,
                serverAddress,
                dimensionId,
                biomeId,
                gameMode,
                difficulty,
                health,
                maxHealth,
                foodLevel,
                saturation,
                experienceLevel,
                totalExperience,
                advancementCount,
                completedAdvancementCount,
                x,
                y,
                z,
                selectedItemId,
                selectedItemName,
                sessionStartEpochSecond,
                worldStartEpochSecond,
                nowEpochSecond,
                changeToken);
    }

    private record AdvancementStats(int totalCount, int completedCount) {
        private static final AdvancementStats EMPTY = new AdvancementStats(0, 0);
    }
}



