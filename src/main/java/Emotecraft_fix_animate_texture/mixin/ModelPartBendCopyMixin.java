package Emotecraft_fix_animate_texture.mixin;

import Emotecraft_fix_animate_texture.compat.PlayerAnimatorBendCompat;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prepare replaced skin cuboids before BendyLib copies their active state. */
@Mixin(ModelPart.class)
public abstract class ModelPartBendCopyMixin {
    @Inject(method = "copyFrom", at = @At("HEAD"))
    private void emotecraftFix$prepareBendCopy(ModelPart source, CallbackInfo ci) {
        PlayerAnimatorBendCompat.prepareBendCopy(source, (ModelPart) (Object) this);
    }
}
