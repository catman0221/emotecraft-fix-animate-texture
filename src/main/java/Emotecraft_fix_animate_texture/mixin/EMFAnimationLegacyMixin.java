package Emotecraft_fix_animate_texture.mixin;

import Emotecraft_fix_animate_texture.Emotecraft_fix_animate_texture;
import Emotecraft_fix_animate_texture.compat.EmfCompat;
import Emotecraft_fix_animate_texture.state.EmoteStateManager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * EMF 1.19.2 has no isEntityAnimPaused hook. Its custom animations are applied
 * directly from EMFAnimation.calculateAndSet(), so that is the correct legacy
 * interception point.
 */
@Pseudo
@Mixin(targets = "traben.entity_model_features.models.animation.EMFAnimation", remap = false)
public abstract class EMFAnimationLegacyMixin {
    private static boolean emotecraft_fix_animate_texture$invocationConfirmed;

    @Inject(method = "calculateAndSet", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void emotecraft_fix_animate_texture$skipWhileEmoting(CallbackInfo ci) {
        if (!emotecraft_fix_animate_texture$invocationConfirmed) {
            emotecraft_fix_animate_texture$invocationConfirmed = true;
            Emotecraft_fix_animate_texture.LOGGER.info(
                    "Confirmed legacy EMF animation mixin invocation"
            );
        }

        if (!EmfCompat.shouldSuppressCurrentAnimatedPlayer("EMFAnimation.calculateAndSet")) {
            return;
        }

        Player player = EmfCompat.getCurrentAnimatedPlayer();
        if (player != null) {
            EmoteStateManager.onEmfAnimationSkipped(player.getUUID());
        }
        ci.cancel();
    }
}
