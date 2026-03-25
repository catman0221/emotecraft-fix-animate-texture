package Emotecraft_fix_animate_texture.mixin;

import Emotecraft_fix_animate_texture.compat.EmfCompat;
import Emotecraft_fix_animate_texture.state.EmoteStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Pseudo
@Mixin(targets = "traben.entity_model_features.models.animation.EMFAnimationEntityContext", remap = false)
public abstract class EMFAnimationEntityContextMixin {
    @Inject(method = "isEntityAnimPaused", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void emotecraft_fix_animate_texture$pauseLegacy(CallbackInfoReturnable<Boolean> cir) {
        pauseIfPlayerIsEmoting(cir);
    }

    @Inject(method = "isEntityAnimPausedWrapped", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void emotecraft_fix_animate_texture$pauseWrapped(CallbackInfoReturnable<Boolean> cir) {
        pauseIfPlayerIsEmoting(cir);
    }

    private static void pauseIfPlayerIsEmoting(CallbackInfoReturnable<Boolean> cir) {
        UUID playerId = EmfCompat.getCurrentAnimatedPlayerUuid();
        if (playerId == null || !EmoteStateManager.isEmoteActive(playerId)) {
            return;
        }

        EmoteStateManager.onEmfAnimationSkipped(playerId);
        cir.setReturnValue(true);
    }
}
