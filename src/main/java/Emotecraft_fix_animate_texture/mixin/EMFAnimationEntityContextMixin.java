package Emotecraft_fix_animate_texture.mixin;

import Emotecraft_fix_animate_texture.compat.EmfCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = "traben.entity_model_features.models.animation.EMFAnimationEntityContext",
        remap = false
)
public abstract class EMFAnimationEntityContextMixin {
    @Inject(
            method = "isEntityAnimPaused",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private static void emotecraftFix$pauseLegacy(CallbackInfoReturnable<Boolean> cir) {
        pauseIfEmoting(cir, "EMFAnimationEntityContext.isEntityAnimPaused");
    }

    @Inject(
            method = "isEntityAnimPausedWrapped",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private static void emotecraftFix$pauseWrapped(CallbackInfoReturnable<Boolean> cir) {
        pauseIfEmoting(cir, "EMFAnimationEntityContext.isEntityAnimPausedWrapped");
    }

    private static void pauseIfEmoting(
            CallbackInfoReturnable<Boolean> cir,
            String source
    ) {
        if (EmfCompat.shouldSuppressCurrentAnimatedPlayer(source)) {
            cir.setReturnValue(true);
        }
    }
}
