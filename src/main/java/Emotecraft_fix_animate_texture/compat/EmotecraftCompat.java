package Emotecraft_fix_animate_texture.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

public final class EmotecraftCompat {
    private static final String EMOTECRAFT_MOD_ID = "emotecraft";
    private static final String EMOTE_PLAYER_ENTITY = "io.github.kosmx.emotes.executor.emotePlayer.IEmotePlayerEntity";

    private static boolean resolved;
    private static Class<?> emotePlayerEntityClass;
    private static Method isPlayingEmoteMethod;

    private EmotecraftCompat() {
    }

    public static boolean isPlayerEmoting(Player player) {
        if (!ModList.get().isLoaded(EMOTECRAFT_MOD_ID)) {
            return false;
        }

        resolve();
        if (emotePlayerEntityClass == null || isPlayingEmoteMethod == null) {
            return false;
        }

        if (!emotePlayerEntityClass.isInstance(player)) {
            return false;
        }

        try {
            return (boolean) isPlayingEmoteMethod.invoke(player);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static void resolve() {
        if (resolved) {
            return;
        }

        resolved = true;
        try {
            emotePlayerEntityClass = Class.forName(EMOTE_PLAYER_ENTITY);
            isPlayingEmoteMethod = emotePlayerEntityClass.getMethod("isPlayingEmote");
        } catch (ReflectiveOperationException ignored) {
            emotePlayerEntityClass = null;
            isPlayingEmoteMethod = null;
        }
    }
}
