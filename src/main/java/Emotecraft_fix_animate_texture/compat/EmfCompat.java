package Emotecraft_fix_animate_texture.compat;

import Emotecraft_fix_animate_texture.Emotecraft_fix_animate_texture;
import Emotecraft_fix_animate_texture.state.EmoteStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * EMF compatibility for the 1.19.2 backport.
 *
 * EMF 1.19.2 predates the pause/vanilla-model API used by newer versions,
 * so suppression is performed by EMFAnimationLegacyMixin at the point where
 * EMF is about to calculate and apply a custom animation.
 */
public final class EmfCompat {
    private static final String ENTITY_MODEL_FEATURES_MOD_ID = "entity_model_features";
    private static final String EMF_CONTEXT = "traben.entity_model_features.models.animation.EMFAnimationEntityContext";
    private static final long DEBUG_LOG_COOLDOWN_MS = 5_000L;

    private static boolean contextResolved;
    private static Method getCurrentEntityMethod;
    private static Method getEntityUuidMethod;
    private static boolean contextInvocationFailureLogged;
    private static boolean entityUuidFailureLogged;
    private static boolean hooksRegistered;
    private static final Map<String, Long> LAST_DEBUG_LOGS = new ConcurrentHashMap<>();

    private EmfCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(ENTITY_MODEL_FEATURES_MOD_ID);
    }

    public static void registerHooks() {
        if (!isLoaded() || hooksRegistered) {
            return;
        }

        resolveContext();
        hooksRegistered = true;
        if (getCurrentEntityMethod != null) {
            Emotecraft_fix_animate_texture.LOGGER.info(
                    "EMF 1.19.2 compatibility enabled; legacy animation suppression mixin is active"
            );
        } else {
            Emotecraft_fix_animate_texture.LOGGER.warn(
                    "EMF is loaded but its animation context could not be resolved; compatibility hook will stay dormant"
            );
        }
    }

    /**
     * Kept so EmoteStateManager can share the same shape as newer branches.
     * Legacy EMF has no per-entity pause/lock API, so the mixin performs the
     * suppression directly during rendering instead.
     */
    public static void syncPlayerAnimationState(Player player, boolean emoting) {
        // Intentionally no-op on EMF 1.19.2.
    }

    public static void forgetPlayerState(UUID playerId) {
        // No persistent legacy EMF state to release.
    }

    public static void clearTrackedStates() {
        LAST_DEBUG_LOGS.clear();
    }

    public static Player getCurrentAnimatedPlayer() {
        Object emfEntity = getCurrentEmfEntity();
        if (emfEntity instanceof Player player) {
            return player;
        }

        UUID entityId = getEmfEntityUuid(emfEntity);
        ClientLevel level = Minecraft.getInstance().level;
        if (entityId == null || level == null) {
            return null;
        }

        for (AbstractClientPlayer player : level.players()) {
            if (entityId.equals(player.getUUID())) {
                return player;
            }
        }
        return null;
    }

    public static boolean shouldSuppressCurrentAnimatedPlayer(String source) {
        Player player = getCurrentAnimatedPlayer();
        if (player == null) {
            return false;
        }

        boolean active = EmoteStateManager.isEmoteActive(player.getUUID());
        if (active) {
            logDebug(
                    "legacy_cancel_" + source + "_" + player.getUUID(),
                    "Skipping legacy EMF animation because Emotecraft emote is active for {} ({}) via {}",
                    player.getScoreboardName(), player.getUUID(), source
            );
        }
        return active;
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
        } catch (ReflectiveOperationException | SecurityException exception) {
            if (!contextInvocationFailureLogged) {
                contextInvocationFailureLogged = true;
                Emotecraft_fix_animate_texture.LOGGER.warn(
                        "Failed to read the entity currently animated by legacy EMF",
                        exception
                );
            }
            return null;
        }
    }

    private static UUID getEmfEntityUuid(Object emfEntity) {
        if (emfEntity == null) {
            return null;
        }

        try {
            if (getEntityUuidMethod == null
                    || !getEntityUuidMethod.getDeclaringClass().isAssignableFrom(emfEntity.getClass())) {
                getEntityUuidMethod = emfEntity.getClass().getMethod("etf$getUuid");
            }
            Object result = getEntityUuidMethod.invoke(emfEntity);
            return result instanceof UUID uuid ? uuid : null;
        } catch (ReflectiveOperationException | SecurityException exception) {
            if (!entityUuidFailureLogged) {
                entityUuidFailureLogged = true;
                Emotecraft_fix_animate_texture.LOGGER.warn(
                        "Failed to resolve the UUID of the entity currently animated by legacy EMF",
                        exception
                );
            }
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
            getCurrentEntityMethod.trySetAccessible();
        } catch (ReflectiveOperationException ignored) {
            getCurrentEntityMethod = null;
        }
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
