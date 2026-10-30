package Emotecraft_fix_animate_texture.compat;

import Emotecraft_fix_animate_texture.Emotecraft_fix_animate_texture;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

public final class EmotecraftCompat {
    private static final String EMOTECRAFT_MOD_ID = "emotecraft";
    private static final String MIXIN_PLAYER_ENTITY = "io.github.kosmx.emotes.main.mixinFunctions.IPlayerEntity";
    private static final String EXECUTOR_PLAYER_ENTITY = "io.github.kosmx.emotes.executor.emotePlayer.IEmotePlayerEntity";

    private static boolean resolved;
    private static Class<?> emotePlayerEntityClass;
    private static Method isPlayingEmoteMethod;

    private EmotecraftCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(EMOTECRAFT_MOD_ID);
    }

    public static boolean isPlayerEmoting(Player player) {
        if (!isLoaded()) {
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
            emotePlayerEntityClass = Class.forName(MIXIN_PLAYER_ENTITY);
            isPlayingEmoteMethod = emotePlayerEntityClass.getMethod("isPlayingEmote");
            Emotecraft_fix_animate_texture.LOGGER.debug("Resolved Emotecraft emote interface {}", MIXIN_PLAYER_ENTITY);
        } catch (ReflectiveOperationException primaryFailure) {
            try {
                emotePlayerEntityClass = Class.forName(EXECUTOR_PLAYER_ENTITY);
                isPlayingEmoteMethod = emotePlayerEntityClass.getMethod("isPlayingEmote");
                Emotecraft_fix_animate_texture.LOGGER.debug("Resolved Emotecraft emote interface {}", EXECUTOR_PLAYER_ENTITY);
            } catch (ReflectiveOperationException ignored) {
                emotePlayerEntityClass = null;
                isPlayingEmoteMethod = null;
                Emotecraft_fix_animate_texture.LOGGER.warn("Failed to resolve Emotecraft emote interface; active emote detection is unavailable");
            }
            return;
        }
    }
}
