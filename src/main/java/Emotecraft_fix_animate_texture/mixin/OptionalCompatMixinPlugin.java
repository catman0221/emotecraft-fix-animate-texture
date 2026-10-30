package Emotecraft_fix_animate_texture.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class OptionalCompatMixinPlugin implements IMixinConfigPlugin {
    private static final String EMF_MOD_ID = "entity_model_features";
    private static final String PLAYER_ANIMATOR_MOD_ID = "playeranimator";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("EMFAnimationEntityContextMixin")) {
            return FabricLoader.getInstance().isModLoaded(EMF_MOD_ID);
        }

        if (mixinClassName.endsWith("ModelPartBendCopyMixin")) {
            return FabricLoader.getInstance().isModLoaded(PLAYER_ANIMATOR_MOD_ID)
                    && hasBendyLib();
        }

        if (mixinClassName.endsWith("PlayerAnimatorBendHelperMixin")
                || mixinClassName.endsWith("PlayerAnimatorAnimationApplierMixin")) {
            return FabricLoader.getInstance().isModLoaded(PLAYER_ANIMATOR_MOD_ID) && hasBendyLib();
        }

        return true;
    }

    private static boolean hasBendyLib() {
        // Check library availability without loading its classes during mixin selection.
        return OptionalCompatMixinPlugin.class.getClassLoader()
                .getResource("io/github/kosmx/bendylib/ModelPartAccessor.class") != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

}
