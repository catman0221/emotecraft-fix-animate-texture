package Emotecraft_fix_animate_texture.mixin;

import Emotecraft_fix_animate_texture.Emotecraft_fix_animate_texture;
import io.github.kosmx.bendylib.ModelPartAccessor;
import io.github.kosmx.bendylib.impl.BendableCuboid;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player Animator 2.0.1 assumes every rendered cuboid still owns its "bend"
 * mutator. EMF player models can replace that cuboid after initialization, so
 * the assumption is not always true. The AnimationApplier mixin repairs that
 * state first; this mixin is only the final anti-crash fallback.
 */
@Pseudo
@Mixin(targets = "dev.kosmx.playerAnim.impl.animation.BendHelper", remap = false)
public abstract class PlayerAnimatorBendHelperMixin {
    private static final float BEND_EPSILON = 0.0001F;
    private static final Set<String> WARNED_MODEL_PART_TYPES = ConcurrentHashMap.newKeySet();

    @Inject(
            // ModelPart is class_630 in the production intermediary namespace used by Fabric 1.21.1.
            method = "bend(Lnet/minecraft/class_630;FF)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void emotecraftFix$applyBendSafely(
            ModelPart modelPart,
            float axis,
            float rotation,
            CallbackInfo callbackInfo
    ) {
        if (Math.abs(rotation) < BEND_EPSILON) {
            return;
        }

        boolean bendMutatorUnavailable;
        try {
            bendMutatorUnavailable = ModelPartAccessor.optionalGetCuboid(modelPart, 0)
                    .map(mutableCuboid -> !mutableCuboid.hasMutator("bend")
                            || !(mutableCuboid.getAndActivateMutator("bend") instanceof BendableCuboid))
                    .orElse(false);
        } catch (RuntimeException exception) {
            bendMutatorUnavailable = true;
        }
        if (!bendMutatorUnavailable) {
            return;
        }

        String modelPartType = modelPart.getClass().getName();
        if (WARNED_MODEL_PART_TYPES.add(modelPartType)) {
            Emotecraft_fix_animate_texture.LOGGER.warn(
                    "Skipped a Player Animator bend because model part {} still has no BendyLib 'bend' mutator "
                            + "after repair; this prevents the EMF/FA Player render crash",
                    modelPartType
            );
        }
        callbackInfo.cancel();
    }
}
