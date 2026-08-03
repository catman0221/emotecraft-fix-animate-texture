package Emotecraft_fix_animate_texture.compat;

import Emotecraft_fix_animate_texture.Emotecraft_fix_animate_texture;
import Emotecraft_fix_animate_texture.state.EmoteStateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class EmfCompat {
    private static final String ENTITY_MODEL_FEATURES_MOD_ID = "entity_model_features";
    private static final String EMF_CONTEXT = "traben.entity_model_features.models.animation.EMFAnimationEntityContext";
    private static final String EMF_API = "traben.entity_model_features.EMFAnimationApi";
    private static final long DEBUG_LOG_COOLDOWN_MS = 5_000L;

    private static boolean contextResolved;
    private static Method getCurrentEntityMethod;
    private static boolean apiResolved;
    private static boolean hooksRegistered;
    private static Method registerPauseConditionMethod;
    private static Method registerVanillaModelConditionMethod;
    private static Method getApiVersionMethod;
    private static Method lockEntityToVanillaModelMethod;
    private static Method unlockEntityToVanillaModelMethod;
    private static Method pauseAllCustomAnimationsForEntityMethod;
    private static Method resumeAllCustomAnimationsForEntityMethod;
    private static final Map<String, Long> LAST_DEBUG_LOGS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> ACTIVE_EMF_SUPPRESSION = new ConcurrentHashMap<>();

    private EmfCompat() {
    }

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(ENTITY_MODEL_FEATURES_MOD_ID);
    }

    public static void registerHooks() {
        if (!isLoaded()) {
            return;
        }

        resolveApi();
        if (hooksRegistered || registerPauseConditionMethod == null) {
            return;
        }

        try {
            Function<Object, Boolean> pauseCondition = emfEntity -> shouldSuppressRenderedPlayer(emfEntity, "EMF API pause condition");
            Function<Object, Boolean> vanillaCondition = emfEntity -> shouldSuppressRenderedPlayer(emfEntity, "EMF API vanilla model condition");

            boolean pauseRegistered = Boolean.TRUE.equals(registerPauseConditionMethod.invoke(null, pauseCondition));
            boolean vanillaRegistered = registerVanillaModelConditionMethod != null
                    && Boolean.TRUE.equals(registerVanillaModelConditionMethod.invoke(null, vanillaCondition));

            hooksRegistered = pauseRegistered || vanillaRegistered;
            Emotecraft_fix_animate_texture.LOGGER.info("Registered EMF compatibility hooks: pauseCondition={}, vanillaModelCondition={}", pauseRegistered, vanillaRegistered);
        } catch (ReflectiveOperationException exception) {
            Emotecraft_fix_animate_texture.LOGGER.warn("Failed to register EMF compatibility hooks", exception);
        }
    }

    public static void syncPlayerAnimationState(Player player, boolean emoting) {
        if (!isLoaded()) {
            return;
        }

        resolveApi();
        if (pauseAllCustomAnimationsForEntityMethod == null
                && resumeAllCustomAnimationsForEntityMethod == null
                && lockEntityToVanillaModelMethod == null
                && unlockEntityToVanillaModelMethod == null) {
            return;
        }

        UUID playerId = player.getUUID();
        Boolean previous = ACTIVE_EMF_SUPPRESSION.put(playerId, emoting);
        boolean stateChanged = previous == null || previous.booleanValue() != emoting;
        if (!stateChanged && !emoting) {
            return;
        }

        try {
            if (emoting) {
                boolean paused = invokeEntityOperation(pauseAllCustomAnimationsForEntityMethod, player);
                boolean locked = invokeEntityOperation(lockEntityToVanillaModelMethod, player);
                if (stateChanged) {
                    Emotecraft_fix_animate_texture.LOGGER.debug(
                            "Activated aggressive EMF suppression for player {} ({}): paused={}, lockedVanilla={}",
                            player.getScoreboardName(), playerId, paused, locked
                    );
                }
            } else {
                boolean resumed = invokeEntityOperation(resumeAllCustomAnimationsForEntityMethod, player);
                boolean unlocked = invokeEntityOperation(unlockEntityToVanillaModelMethod, player);
                if (stateChanged) {
                    Emotecraft_fix_animate_texture.LOGGER.debug(
                            "Released aggressive EMF suppression for player {} ({}): resumed={}, unlockedVanilla={}",
                            player.getScoreboardName(), playerId, resumed, unlocked
                    );
                }
            }
        } catch (ReflectiveOperationException exception) {
            Emotecraft_fix_animate_texture.LOGGER.warn("Failed to sync EMF suppression state for player {}", playerId, exception);
        }
    }

    public static void forgetPlayerState(UUID playerId) {
        ACTIVE_EMF_SUPPRESSION.remove(playerId);
    }

    public static void clearTrackedStates() {
        ACTIVE_EMF_SUPPRESSION.clear();
    }

    public static Player getCurrentAnimatedPlayer() {
        Object emfEntity = getCurrentEmfEntity();
        return emfEntity instanceof Player player ? player : null;
    }

    public static boolean shouldSuppressCurrentAnimatedPlayer(String source) {
        Player player = getCurrentAnimatedPlayer();
        if (player == null) {
            return false;
        }

        logDebug("mixin_enter_" + source + "_" + player.getUUID(),
                "Entering EMF mixin for player {} ({}) via {}", player.getScoreboardName(), player.getUUID(), source);

        boolean active = EmotecraftCompat.isPlayerEmoting(player);
        EmoteStateManager.recordPlayerState(player, active);
        if (active) {
            logDebug("cancel_" + source + "_" + player.getUUID(),
                    "Skipping EMF player animation because emote is active for {} ({}) via {}",
                    player.getScoreboardName(), player.getUUID(), source);
        } else {
            logDebug("no_cancel_" + source + "_" + player.getUUID(),
                    "EMF hook did not cancel for {} ({}) via {} because no active emote was detected",
                    player.getScoreboardName(), player.getUUID(), source);
        }
        return active;
    }

    private static boolean shouldSuppressRenderedPlayer(Object emfEntity, String source) {
        Player player = extractPlayer(emfEntity);
        if (player == null) {
            return false;
        }

        boolean active = EmotecraftCompat.isPlayerEmoting(player);
        EmoteStateManager.recordPlayerState(player, active);
        if (active) {
            logDebug("api_cancel_" + source + "_" + player.getUUID(),
                    "Skipping EMF player animation because emote is active for {} ({}) via {}",
                    player.getScoreboardName(), player.getUUID(), source);
        }
        return active;
    }

    private static Player extractPlayer(Object emfEntity) {
        if (emfEntity instanceof Player player) {
            return player;
        }

        if (emfEntity instanceof Entity) {
            return null;
        }

        return null;
    }

    private static Object getCurrentEmfEntity() {
        if (!isLoaded()) {
            return null;
        }

        resolveContext();
        if (getCurrentEntityMethod == null) {
            return null;
        }

        try {
            return getCurrentEntityMethod.invoke(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void resolveContext() {
        if (contextResolved) {
            return;
        }

        contextResolved = true;
        try {
            Class<?> contextClass = Class.forName(EMF_CONTEXT);
            getCurrentEntityMethod = contextClass.getMethod("getEMFEntity");
        } catch (ReflectiveOperationException ignored) {
            getCurrentEntityMethod = null;
        }
    }

    private static void resolveApi() {
        if (apiResolved) {
            return;
        }

        apiResolved = true;
        try {
            Class<?> apiClass = Class.forName(EMF_API);
            Class<?> emfEntityClass = Class.forName("traben.entity_model_features.utils.EMFEntity");

            getApiVersionMethod = tryResolve(apiClass, "getApiVersion");
            registerPauseConditionMethod = tryResolve(apiClass, "registerPauseCondition", Function.class);
            registerVanillaModelConditionMethod = tryResolve(apiClass, "registerVanillaModelCondition", Function.class);
            lockEntityToVanillaModelMethod = tryResolve(apiClass, "lockEntityToVanillaModel", emfEntityClass);
            unlockEntityToVanillaModelMethod = tryResolve(apiClass, "unlockEntityToVanillaModel", emfEntityClass);
            pauseAllCustomAnimationsForEntityMethod = tryResolve(apiClass, "pauseAllCustomAnimationsForEntity", emfEntityClass);
            resumeAllCustomAnimationsForEntityMethod = tryResolve(apiClass, "resumeAllCustomAnimationsForEntity", emfEntityClass);

            Object apiVersion = getApiVersionMethod == null ? null : getApiVersionMethod.invoke(null);
            Emotecraft_fix_animate_texture.LOGGER.info(
                    "Resolved aggressive EMF compatibility: apiVersion={}, pauseCondition={}, vanillaCondition={}, pause={}, resume={}, lockVanilla={}, unlockVanilla={}",
                    apiVersion,
                    registerPauseConditionMethod != null,
                    registerVanillaModelConditionMethod != null,
                    pauseAllCustomAnimationsForEntityMethod != null,
                    resumeAllCustomAnimationsForEntityMethod != null,
                    lockEntityToVanillaModelMethod != null,
                    unlockEntityToVanillaModelMethod != null
            );
        } catch (ReflectiveOperationException exception) {
            Emotecraft_fix_animate_texture.LOGGER.warn("Failed to load the EMF animation API; mixin fallback will be used", exception);
        }
    }

    private static Method tryResolve(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return null;
        }
    }

    private static boolean invokeEntityOperation(Method method, Player player) throws ReflectiveOperationException {
        return method != null && Boolean.TRUE.equals(method.invoke(null, player));
    }

    private static void logDebug(String key, String message, Object... args) {
        long now = System.currentTimeMillis();
        Long previous = LAST_DEBUG_LOGS.get(key);
        if (previous != null && now - previous < DEBUG_LOG_COOLDOWN_MS) {
            return;
        }

        LAST_DEBUG_LOGS.put(key, now);
        Emotecraft_fix_animate_texture.LOGGER.debug(message, args);
    }
}
