package Emotecraft_fix_animate_texture.compat;

import Emotecraft_fix_animate_texture.Emotecraft_fix_animate_texture;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EmotecraftCompat {
    private static final String MOD_ID = "emotecraft";
    private static final String MODERN_INTERFACE =
            "io.github.kosmx.emotes.main.mixinFunctions.IPlayerEntity";
    private static final String LEGACY_INTERFACE =
            "io.github.kosmx.emotes.executor.emotePlayer.IEmotePlayerEntity";

    private static final Map<Class<?>, Method> DIRECT_METHODS = new ConcurrentHashMap<>();
    private static boolean resolved;
    private static Candidate modernCandidate;
    private static Candidate legacyCandidate;
    private static boolean missingMethodLogged;

    private EmotecraftCompat() {
    }

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static boolean isPlayerEmoting(Player player) {
        if (player == null || !isLoaded()) {
            return false;
        }

        resolve();

        Method method = methodFor(player);
        if (method == null) {
            if (!missingMethodLogged) {
                missingMethodLogged = true;
                Emotecraft_fix_animate_texture.LOGGER.warn(
                        "Could not resolve Emotecraft isPlayingEmote(); emote detection is unavailable"
                );
            }
            return false;
        }

        try {
            return Boolean.TRUE.equals(method.invoke(player));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Emotecraft_fix_animate_texture.LOGGER.debug(
                    "Failed to query Emotecraft state for {}", player.getUUID(), exception
            );
            return false;
        }
    }

    private static Method methodFor(Player player) {
        if (modernCandidate != null && modernCandidate.owner().isInstance(player)) {
            return modernCandidate.method();
        }
        if (legacyCandidate != null && legacyCandidate.owner().isInstance(player)) {
            return legacyCandidate.method();
        }

        Class<?> playerClass = player.getClass();
        Method cached = DIRECT_METHODS.get(playerClass);
        if (cached != null) {
            return cached;
        }

        Method direct = tryResolve(playerClass, "isPlayingEmote");
        if (direct != null) {
            DIRECT_METHODS.put(playerClass, direct);
        }
        return direct;
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;

        modernCandidate = resolveCandidate(MODERN_INTERFACE);
        legacyCandidate = resolveCandidate(LEGACY_INTERFACE);

        if (modernCandidate != null) {
            Emotecraft_fix_animate_texture.LOGGER.info(
                    "Using modern Emotecraft interface {}", MODERN_INTERFACE
            );
        } else if (legacyCandidate != null) {
            Emotecraft_fix_animate_texture.LOGGER.info(
                    "Using legacy Emotecraft interface {}", LEGACY_INTERFACE
            );
        }
    }

    private static Candidate resolveCandidate(String className) {
        try {
            Class<?> owner = Class.forName(className);
            Method method = tryResolve(owner, "isPlayingEmote");
            return method == null ? null : new Candidate(owner, method);
        } catch (ClassNotFoundException | LinkageError ignored) {
            return null;
        }
    }

    private static Method tryResolve(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return null;
        }
    }

    private record Candidate(Class<?> owner, Method method) {
    }
}
