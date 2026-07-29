package Emotecraft_fix_animate_texture.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class OptionalCompatMixinPlugin implements IMixinConfigPlugin {
    private static final String EMF_TARGET =
            "traben.entity_model_features.models.animation.EMFAnimationEntityContext";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !mixinClassName.endsWith("EMFAnimationEntityContextMixin")
                || FabricLoader.getInstance().isModLoaded("entity_model_features")
                && isClassAvailable(EMF_TARGET);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo
    ) {
    }

    @Override
    public void postApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo
    ) {
    }

    private static boolean isClassAvailable(String className) {
        try {
            Class.forName(
                    className,
                    false,
                    OptionalCompatMixinPlugin.class.getClassLoader()
            );
            return true;
        } catch (ClassNotFoundException | LinkageError | SecurityException ignored) {
            return false;
        }
    }
}
