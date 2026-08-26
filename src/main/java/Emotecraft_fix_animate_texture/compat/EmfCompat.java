package Emotecraft_fix_animate_texture.compat;

import Emotecraft_fix_animate_texture.Emotecraft_fix_animate_texture;
import Emotecraft_fix_animate_texture.state.EmoteStateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Function;

public final class EmfCompat {
    private static final String ENTITY_MODEL_FEATURES_MOD_ID = "entity_model_features";
    private static final String EMF_CONTEXT = "traben.entity_model_features.models.animation.EMFAnimationEntityContext";
    private static final String EMF_API = "traben.entity_model_features.EMFAnimationApi";
    private static boolean contextResolved;
    private static Method getCurrentEntityMethod;
    private static boolean apiResolved;
    private static boolean hooksRegistered;
    private static boolean modernApiAvailable;
    private static Method registerPauseConditionMethod;
    private static Method registerVanillaModelConditionMethod;
    private static Method lockEntityToVanillaModelMethod;
    private static Method unlockEntityToVanillaModelMethod;
    private static Method pauseAllCustomAnimationsForEntityMethod;
    private static Method resumeAllCustomAnimationsForEntityMethod;
    private static final java.util.Map<UUID, Boolean> ACTIVE_EMF_SUPPRESSION = new java.util.concurrent.ConcurrentHashMap<>();

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
        if (!modernApiAvailable) {
            Emotecraft_fix_animate_texture.LOGGER.info("Modern EMF condition API unavailable; legacy compatibility fallback will be used");
            return;
        }
        if (hooksRegistered) {
            return;
        }

        try {
            // These callbacks execute from EMF's render path. They must remain pure:
            // only read the state captured by ClientTick and return a boolean.
            Function<Object, Boolean> pauseCondition = EmfCompat::isTrackedEmotingPlayer;
            Function<Object, Boolean> vanillaCondition = EmfCompat::isTrackedEmotingPlayer;

            boolean pauseRegistered = Boolean.TRUE.equals(registerPauseConditionMethod.invoke(null, pauseCondition));
            boolean vanillaRegistered = Boolean.TRUE.equals(registerVanillaModelConditionMethod.invoke(null, vanillaCondition));

            hooksRegistered = pauseRegistered && vanillaRegistered;
            Emotecraft_fix_animate_texture.LOGGER.info("Registered EMF compatibility hooks: pauseCondition={}, vanillaModelCondition={}", pauseRegistered, vanillaRegistered);
        } catch (ReflectiveOperationException exception) {
            Emotecraft_fix_animate_texture.LOGGER.warn("Failed to register EMF compatibility hooks", exception);
        }
    }

    public static void syncPlayerAnimationState(Player player, boolean emoting) {
        if (!isLoaded()) {
            return;
        }

        if (!apiResolved) {
            return;
        }
        // Modern EMF is controlled exclusively by the two pure registered
        // conditions. Never combine registerVanillaModelCondition with lock.
        if (modernApiAvailable) {
            return;
        }
        if (pauseAllCustomAnimationsForEntityMethod == null
                && resumeAllCustomAnimationsForEntityMethod == null
                && lockEntityToVanillaModelMethod == null
                && unlockEntityToVanillaModelMethod == null) {
            return;
        }

        UUID playerId = player.getUUID();
        Boolean previous = ACTIVE_EMF_SUPPRESSION.put(playerId, emoting);
        boolean stateChanged = previous == null || previous.booleanValue() != emoting;
        if (!stateChanged) {
            return;
        }

        try {
            if (emoting) {
                boolean paused = invokeEntityOperation(pauseAllCustomAnimationsForEntityMethod, player);
                boolean locked = invokeEntityOperation(lockEntityToVanillaModelMethod, player);
                Emotecraft_fix_animate_texture.LOGGER.debug(
                        "Activated aggressive EMF suppression for player {} ({}): paused={}, lockedVanilla={}",
                        player.getScoreboardName(), playerId, paused, locked
                );
            } else {
                boolean resumed = invokeEntityOperation(resumeAllCustomAnimationsForEntityMethod, player);
                boolean unlocked = invokeEntityOperation(unlockEntityToVanillaModelMethod, player);
                Emotecraft_fix_animate_texture.LOGGER.debug(
                        "Released aggressive EMF suppression for player {} ({}): resumed={}, unlockedVanilla={}",
                        player.getScoreboardName(), playerId, resumed, unlocked
                );
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

    public static boolean shouldSuppressCurrentAnimatedPlayer(String ignoredSource) {
        Player player = getCurrentAnimatedPlayer();
        return player != null && EmoteStateManager.isEmoteActive(player.getUUID());
    }

    public static boolean isModernApiAvailable() {
        return apiResolved && modernApiAvailable;
    }

    private static boolean isTrackedEmotingPlayer(Object emfEntity) {
        Player player = extractPlayer(emfEntity);
        return player != null && EmoteStateManager.isEmoteActive(player.getUUID());
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
            ClassLoader classLoader = EmfCompat.class.getClassLoader();
            Class<?> apiClass = Class.forName(EMF_API, false, classLoader);

            registerPauseConditionMethod = tryResolve(apiClass, "registerPauseCondition", Function.class);
            registerVanillaModelConditionMethod = tryResolve(apiClass, "registerVanillaModelCondition", Function.class);
            modernApiAvailable = registerPauseConditionMethod != null && registerVanillaModelConditionMethod != null;

            if (!modernApiAvailable) {
                Class<?> emfEntityClass = Class.forName("traben.entity_model_features.utils.EMFEntity", false, classLoader);
                lockEntityToVanillaModelMethod = tryResolve(apiClass, "lockEntityToVanillaModel", emfEntityClass);
                unlockEntityToVanillaModelMethod = tryResolve(apiClass, "unlockEntityToVanillaModel", emfEntityClass);
                pauseAllCustomAnimationsForEntityMethod = tryResolve(apiClass, "pauseAllCustomAnimationsForEntity", emfEntityClass);
                resumeAllCustomAnimationsForEntityMethod = tryResolve(apiClass, "resumeAllCustomAnimationsForEntity", emfEntityClass);
            }

            Emotecraft_fix_animate_texture.LOGGER.info(
                    "Resolved EMF compatibility: modernApi={}, pauseCondition={}, vanillaCondition={}, legacyPause={}, legacyResume={}, legacyLockVanilla={}, legacyUnlockVanilla={}",
                    modernApiAvailable,
                    registerPauseConditionMethod != null,
                    registerVanillaModelConditionMethod != null,
                    pauseAllCustomAnimationsForEntityMethod != null,
                    resumeAllCustomAnimationsForEntityMethod != null,
                    lockEntityToVanillaModelMethod != null,
                    unlockEntityToVanillaModelMethod != null
            );
        } catch (ReflectiveOperationException exception) {
            modernApiAvailable = false;
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
}
