package Emotecraft_fix_animate_texture.compat;

import net.minecraft.client.player.AbstractClientPlayer;

public final class ActiveEmoteDetector {
    private ActiveEmoteDetector() {
    }

    public static boolean isPlayerEmoteActive(AbstractClientPlayer player) {
        return player != null && EmotecraftCompat.isPlayerEmoting(player);
    }
}
