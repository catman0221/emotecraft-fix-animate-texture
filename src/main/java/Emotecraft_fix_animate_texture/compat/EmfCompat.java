package Emotecraft_fix_animate_texture.compat;

import Emotecraft_fix_animate_texture.Emotecraft_fix_animate_texture;
import Emotecraft_fix_animate_texture.state.EmoteStateManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class EmfCompat {
    private static final String MOD_ID = "entity_model_features";
    private static final String API_CLASS = "traben.entity_model_features.EMFAnimationApi";
    private static final String ENTITY_CLASS = "traben.entity_model_features.utils.EMFEntity";
    private static final String CONTEXT_CLASS =
            "traben.entity_model_features.models.animation.EMFAnimationEntityContext";

    private static final Map<UUID, Player> SUPPRESSED_PLAYERS = new ConcurrentHashMap<>();

    private static boolean initialized;
    private static EmfSuppressionStrategy strategy = EmfSuppressionStrategy.NONE;
    private static Method getApiVersionMethod;
    private static Method getCurrentEntityApiMethod;
    private static Method getCurrentEntityContextMethod;
    private static Method registerPauseConditionMethod;
    private static Method pauseAllMethod;
    private static Method resumeAllMethod;

    private EmfCompat() {
    }

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        if (!isLoaded()) {
            strategy = EmfSuppressionStrategy.NONE;
            Emotecraft_fix_animate_texture.LOGGER.info("EMF is absent; compatibility hooks are inactive");
            return;
        }

        resolveCapabilities();
        logApiVersion();

        if (registerNativeCondition()) {
            strategy = EmfSuppressionStrategy.NATIVE_PAUSE_CONDITION;
        } else if (pauseAllMethod != null && resumeAllMethod != null) {
            strategy = EmfSuppressionStrategy.PAUSE_BY_ENTITY;
        } else if (getCurrentEntityApiMethod != null || getCurrentEntityContextMethod != null) {
            strategy = EmfSuppressionStrategy.MIXIN_FALLBACK;
        } else {
            strategy = EmfSuppressionStrategy.NONE;
        }

        Emotecraft_fix_animate_texture.LOGGER.info(
                "Selected EMF suppression strategy: {}", strategy
        );
    }

    public static EmfSuppressionStrategy strategy() {
        return strategy;
    }

    public static boolean usesMixinFallback() {
        return strategy == EmfSuppressionStrategy.MIXIN_FALLBACK;
    }

    public static void syncPlayerAnimationState(Player player, boolean emoting) {
        if (player == null || strategy != EmfSuppressionStrategy.PAUSE_BY_ENTITY) {
            return;
        }

        if (emoting) {
            suppress(player);
        } else {
            restore(player.getUUID());
        }
    }

    public static void restore(UUID playerId) {
        Player player = SUPPRESSED_PLAYERS.remove(playerId);
        if (player == null || resumeAllMethod == null) {
            return;
        }

        invokeEntityOperation(resumeAllMethod, player, "resume", false);
    }

    public static void restoreAll() {
        List<UUID> playerIds = new ArrayList<>(SUPPRESSED_PLAYERS.keySet());
        for (UUID playerId : playerIds) {
            restore(playerId);
        }
        SUPPRESSED_PLAYERS.clear();
    }

    public static Player getCurrentAnimatedPlayer() {
        Object current = invokeNoArgs(getCurrentEntityApiMethod);
        if (!(current instanceof Player)) {
            current = invokeNoArgs(getCurrentEntityContextMethod);
        }
        return current instanceof Player player ? player : null;
    }

    public static boolean shouldSuppressCurrentAnimatedPlayer(String source) {
        if (!usesMixinFallback()) {
            return false;
        }

        Player player = getCurrentAnimatedPlayer();
        if (player == null) {
            return false;
        }

        boolean emoting = EmotecraftCompat.isPlayerEmoting(player);
        EmoteStateManager.recordPlayerState(player, emoting);
        if (emoting) {
            EmoteStateManager.onEmfAnimationSkipped(player.getUUID(), source);
        }
        return emoting;
    }

    private static void suppress(Player player) {
        UUID playerId = player.getUUID();
        if (SUPPRESSED_PLAYERS.containsKey(playerId)) {
            return;
        }

        if (invokeEntityOperation(pauseAllMethod, player, "pause", true)) {
            SUPPRESSED_PLAYERS.put(playerId, player);
        }
    }

    private static boolean registerNativeCondition() {
        if (registerPauseConditionMethod == null) {
            return false;
        }

        Function<Object, Boolean> condition = emfEntity -> {
            if (!(emfEntity instanceof Player player)) {
                return false;
            }

            boolean emoting = EmotecraftCompat.isPlayerEmoting(player);
            EmoteStateManager.recordPlayerState(player, emoting);
            return emoting;
        };

        try {
            return Boolean.TRUE.equals(registerPauseConditionMethod.invoke(null, condition));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Emotecraft_fix_animate_texture.LOGGER.warn(
                    "Failed to register the native EMF pause condition; trying a fallback",
                    exception
            );
            return false;
        }
    }

    private static void resolveCapabilities() {
        Class<?> apiClass = tryLoad(API_CLASS);
        Class<?> emfEntityClass = tryLoad(ENTITY_CLASS);
        Class<?> contextClass = tryLoad(CONTEXT_CLASS);

        if (apiClass != null) {
            getApiVersionMethod = tryResolve(apiClass, "getApiVersion");
            getCurrentEntityApiMethod = tryResolve(apiClass, "getCurrentEntity");
            registerPauseConditionMethod =
                    tryResolve(apiClass, "registerPauseCondition", Function.class);

            if (emfEntityClass != null) {
                pauseAllMethod = tryResolve(
                        apiClass, "pauseAllCustomAnimationsForEntity", emfEntityClass
                );
                resumeAllMethod = tryResolve(
                        apiClass, "resumeAllCustomAnimationsForEntity", emfEntityClass
                );
            }
        }

        if (contextClass != null) {
            getCurrentEntityContextMethod = tryResolve(contextClass, "getEMFEntity");
        }

        Emotecraft_fix_animate_texture.LOGGER.info(
                "Resolved EMF capabilities: condition={}, pauseByEntity={}, currentEntity={}",
                registerPauseConditionMethod != null,
                pauseAllMethod != null && resumeAllMethod != null,
                getCurrentEntityApiMethod != null || getCurrentEntityContextMethod != null
        );
    }

    private static void logApiVersion() {
        Object apiVersion = invokeNoArgs(getApiVersionMethod);
        if (apiVersion instanceof Number number) {
            Emotecraft_fix_animate_texture.LOGGER.info(
                    "Detected EMF animation API version: {}", number.intValue()
            );
        } else {
            Emotecraft_fix_animate_texture.LOGGER.info(
                    "EMF animation API does not expose a version number"
            );
        }
    }

    private static boolean invokeEntityOperation(
            Method method,
            Player player,
            String operation,
            boolean warnOnFailure
    ) {
        if (method == null) {
            return false;
        }

        try {
            boolean successful = Boolean.TRUE.equals(method.invoke(null, player));
            if (!successful && warnOnFailure) {
                Emotecraft_fix_animate_texture.LOGGER.warn(
                        "EMF rejected the {} operation for player {}", operation, player.getUUID()
                );
            }
            return successful;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Emotecraft_fix_animate_texture.LOGGER.warn(
                    "Failed to {} EMF animations for player {}", operation, player.getUUID(), exception
            );
            return false;
        }
    }

    private static Object invokeNoArgs(Method method) {
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Class<?> tryLoad(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException | LinkageError | SecurityException ignored) {
            return null;
        }
    }

    private static Method tryResolve(
            Class<?> owner,
            String name,
            Class<?>... parameterTypes
    ) {
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return null;
        }
    }
}
