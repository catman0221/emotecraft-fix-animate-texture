package Emotecraft_fix_animate_texture.compat;

import net.minecraft.world.entity.Entity;

import java.lang.reflect.Method;
import java.util.UUID;

public final class EmfCompat {
    private static final String EMF_CONTEXT = "traben.entity_model_features.models.animation.EMFAnimationEntityContext";

    private static boolean resolved;
    private static Method getCurrentEntityMethod;

    private EmfCompat() {
    }

    public static UUID getCurrentAnimatedPlayerUuid() {
        resolve();
        if (getCurrentEntityMethod == null) {
            return null;
        }

        try {
            Object emfEntity = getCurrentEntityMethod.invoke(null);
            if (emfEntity instanceof Entity entity) {
                return entity.getUUID();
            }

            if (emfEntity == null) {
                return null;
            }

            Method uuidMethod = emfEntity.getClass().getMethod("etf$getUuid");
            Object uuid = uuidMethod.invoke(emfEntity);
            return uuid instanceof UUID value ? value : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void resolve() {
        if (resolved) {
            return;
        }

        resolved = true;
        try {
            Class<?> contextClass = Class.forName(EMF_CONTEXT);
            getCurrentEntityMethod = contextClass.getMethod("getEMFEntity");
        } catch (ReflectiveOperationException ignored) {
            getCurrentEntityMethod = null;
        }
    }
}
