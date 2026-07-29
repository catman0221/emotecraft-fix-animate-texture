package Emotecraft_fix_animate_texture.state;

import Emotecraft_fix_animate_texture.Emotecraft_fix_animate_texture;
import Emotecraft_fix_animate_texture.compat.EmfCompat;
import Emotecraft_fix_animate_texture.compat.EmotecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EmoteStateManager {
    private static final Map<UUID, Boolean> ACTIVE_EMOTES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_SKIP_LOGS = new ConcurrentHashMap<>();
    private static final long SKIP_LOG_COOLDOWN_MS = 5_000L;

    private static ClientLevel trackedLevel;

    private EmoteStateManager() {
    }

    public static void refresh(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level != trackedLevel) {
            clearAndRestore();
            trackedLevel = level;
        }

        if (level == null) {
            return;
        }

        Set<UUID> seenPlayers = new HashSet<>();
        for (AbstractClientPlayer player : level.players()) {
            UUID playerId = player.getUUID();
            seenPlayers.add(playerId);

            boolean emoting = EmotecraftCompat.isPlayerEmoting(player);
            recordPlayerState(player, emoting);
            if (!emoting) {
                LAST_SKIP_LOGS.remove(playerId);
            }
        }

        Set<UUID> missingPlayers = new HashSet<>(ACTIVE_EMOTES.keySet());
        missingPlayers.removeAll(seenPlayers);
        for (UUID playerId : missingPlayers) {
            removeAndRestore(playerId);
        }
    }

    public static void recordPlayerState(Player player, boolean emoting) {
        UUID playerId = player.getUUID();
        Boolean previous = ACTIVE_EMOTES.put(playerId, emoting);
        EmfCompat.syncPlayerAnimationState(player, emoting);

        if (previous == null || previous.booleanValue() != emoting) {
            Emotecraft_fix_animate_texture.LOGGER.debug(
                    "{} emote for player {} ({})",
                    emoting ? "Detected active" : "Detected end of",
                    player.getScoreboardName(),
                    playerId
            );
        }
    }

    public static void onEmfAnimationSkipped(UUID playerId, String source) {
        if (!ACTIVE_EMOTES.getOrDefault(playerId, false)) {
            return;
        }

        long now = System.currentTimeMillis();
        Long previous = LAST_SKIP_LOGS.get(playerId);
        if (previous != null && now - previous < SKIP_LOG_COOLDOWN_MS) {
            return;
        }

        LAST_SKIP_LOGS.put(playerId, now);
        Emotecraft_fix_animate_texture.LOGGER.debug(
                "Skipping EMF player animation for {} via {}", playerId, source
        );
    }

    public static void removeAndRestore(UUID playerId) {
        ACTIVE_EMOTES.remove(playerId);
        LAST_SKIP_LOGS.remove(playerId);
        EmfCompat.restore(playerId);
    }

    public static void clearAndRestore() {
        EmfCompat.restoreAll();
        ACTIVE_EMOTES.clear();
        LAST_SKIP_LOGS.clear();
        trackedLevel = null;
    }
}
