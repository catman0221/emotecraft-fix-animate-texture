package Emotecraft_fix_animate_texture.mixin;

import Emotecraft_fix_animate_texture.compat.PlayerAnimatorBendCompat;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.kosmx.playerAnim.impl.animation.AnimationApplier", remap = false)
public abstract class PlayerAnimatorAnimationApplierMixin {
    @Inject(
            // ModelPart is class_630 in Fabric's production intermediary namespace for 1.21.1.
            method = "updatePart(Ljava/lang/String;Lnet/minecraft/class_630;)V",
            at = @At("HEAD"),
            remap = false
    )
    private void emotecraftFix$repairBendMutator(String partName, ModelPart modelPart, CallbackInfo callbackInfo) {
        PlayerAnimatorBendCompat.ensureBendMutator(partName, modelPart);
    }
}
