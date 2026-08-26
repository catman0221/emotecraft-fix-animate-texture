package Emotecraft_fix_animate_texture.state;

import Emotecraft_fix_animate_texture.Emotecraft_fix_animate_texture;
import Emotecraft_fix_animate_texture.compat.ActiveEmoteDetector;
import Emotecraft_fix_animate_texture.compat.EmfCompat;
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
    private EmoteStateManager() {
    }

    public static void refresh(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null) {
            clear();
            return;
        }

        Set<UUID> seenPlayers = new HashSet<>();
        for (AbstractClientPlayer player : level.players()) {
            UUID uuid = player.getUUID();
            seenPlayers.add(uuid);

            boolean isActive = ActiveEmoteDetector.isPlayerEmoteActive(player);
            recordPlayerState(player, isActive);
        }

        ACTIVE_EMOTES.keySet().removeIf(uuid -> !seenPlayers.contains(uuid));
    }

    public static boolean isEmoteActive(UUID playerId) {
        return ACTIVE_EMOTES.getOrDefault(playerId, false);
    }

    public static void recordPlayerState(Player player, boolean isActive) {
        UUID uuid = player.getUUID();
        Boolean previous = ACTIVE_EMOTES.put(uuid, isActive);
        EmfCompat.syncPlayerAnimationState(player, isActive);
        if (previous == null || previous.booleanValue() != isActive) {
            if (isActive) {
                Emotecraft_fix_animate_texture.LOGGER.debug("Detected active emote for player {} ({})", player.getScoreboardName(), uuid);
            } else {
                Emotecraft_fix_animate_texture.LOGGER.debug("Detected emote end for player {} ({})", player.getScoreboardName(), uuid);
            }
        }
    }

    public static void remove(UUID playerId) {
        ACTIVE_EMOTES.remove(playerId);
        EmfCompat.forgetPlayerState(playerId);
    }

    public static void clear() {
        ACTIVE_EMOTES.clear();
        EmfCompat.clearTrackedStates();
    }
}
