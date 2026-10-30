package Emotecraft_fix_animate_texture.compat;

import io.github.kosmx.bendylib.ICuboidBuilder;
import io.github.kosmx.bendylib.impl.ICuboid;
import io.github.kosmx.bendylib.mixin.CuboidMutator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BendCopyTest {
    private static ICuboid state(AtomicInteger value) {
        return (ICuboid) Proxy.newProxyInstance(ICuboid.class.getClassLoader(), new Class<?>[]{ICuboid.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("copyState")) {
                        value.set(((Number) args[0].getClass().getMethod("hashCode").invoke(args[0])).intValue());
                    }
                    if (method.getName().equals("hashCode")) return value.get();
                    return null;
                });
    }

    @Test
    void replacedOuterLayerCopiesActiveBendWithoutSharingGeometry() {
        var source = new CuboidMutator();
        var destination = new CuboidMutator();
        var bend = new AtomicInteger(73);
        var copied = new AtomicInteger();
        AtomicInteger instances = new AtomicInteger();
        ICuboidBuilder<ICuboid> builder = data -> state(instances.getAndIncrement() == 0 ? bend : copied);
        source.registerMutator("bend", builder);
        var original = source.getAndActivateMutator("bend");

        // Reproduces beta.4: copyStateFrom silently leaves the layer undeformed.
        destination.copyStateFrom(source);
        assertNull(destination.getActiveMutator());

        PlayerAnimatorBendCompat.prepareCuboidCopy(source, destination);
        destination.copyStateFrom(source);
        assertEquals(73, copied.get());
        assertNotSame(original, destination.getActiveMutator().getB());
        assertSame(builder, destination.getCuboidBuilder("bend"));

        // Emote cancellation must clear the copied bend on the next model copy.
        source.getAndActivateMutator(null);
        destination.copyStateFrom(source);
        assertNull(destination.getActiveMutator());
    }

    @Test
    void preservesExistingFactoriesAndIgnoresSourcesWithoutBending() {
        var source = new CuboidMutator();
        var destination = new CuboidMutator();
        PlayerAnimatorBendCompat.prepareCuboidCopy(source, destination);
        assertFalse(destination.hasMutator("bend"));
        ICuboidBuilder<ICuboid> existing = data -> null;
        ICuboidBuilder<ICuboid> other = data -> null;
        destination.registerMutator("bend", existing);
        source.registerMutator("bend", other);
        PlayerAnimatorBendCompat.prepareCuboidCopy(source, destination);
        PlayerAnimatorBendCompat.prepareCuboidCopy(source, destination);
        assertSame(existing, destination.getCuboidBuilder("bend"));
    }
}
