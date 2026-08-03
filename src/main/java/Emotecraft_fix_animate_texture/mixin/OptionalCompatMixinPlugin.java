package Emotecraft_fix_animate_texture.mixin;

import Emotecraft_fix_animate_texture.Emotecraft_fix_animate_texture;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class OptionalCompatMixinPlugin implements IMixinConfigPlugin {
    private static final String EMF_TARGET = "traben.entity_model_features.models.animation.EMFAnimationEntityContext";
    private static Boolean emfTargetAvailable;

    @Override
    public void onLoad(String mixinPackage) {
        Emotecraft_fix_animate_texture.LOGGER.debug("Loading optional compat mixin plugin for {}", mixinPackage);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("EMFAnimationEntityContextMixin")) {
            boolean available = isClassAvailable(EMF_TARGET);
            if (available) {
                Emotecraft_fix_animate_texture.LOGGER.debug("Applying {} to {}", mixinClassName, targetClassName);
            } else {
                Emotecraft_fix_animate_texture.LOGGER.warn("Skipping {} because target class {} was not found", mixinClassName, EMF_TARGET);
            }
            return available;
        }

        return true;
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

    private static boolean isClassAvailable(String className) {
        if (emfTargetAvailable != null) {
            return emfTargetAvailable;
        }

        try {
            Class.forName(className, false, OptionalCompatMixinPlugin.class.getClassLoader());
            emfTargetAvailable = true;
        } catch (ClassNotFoundException ignored) {
            emfTargetAvailable = false;
        }

        return emfTargetAvailable;
    }
}
