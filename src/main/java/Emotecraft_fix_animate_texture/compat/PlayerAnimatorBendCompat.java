package Emotecraft_fix_animate_texture.compat;

import Emotecraft_fix_animate_texture.Emotecraft_fix_animate_texture;
import io.github.kosmx.bendylib.ModelPartAccessor;
import io.github.kosmx.bendylib.impl.BendableCuboid;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerAnimatorBendCompat {
    private static final Set<String> REPAIR_LOGGED_PARTS = ConcurrentHashMap.newKeySet();
    private static final Set<String> FAILED_LOGGED_PARTS = ConcurrentHashMap.newKeySet();

    private PlayerAnimatorBendCompat() {
    }

    public static void ensureBendMutator(String partName, ModelPart modelPart) {
        Direction direction = bendDirection(partName);
        if (direction == null || modelPart == null) {
            return;
        }

        try {
            ModelPartAccessor.optionalGetCuboid(modelPart, 0).ifPresent(mutableCuboid -> {
                try {
                    if (mutableCuboid.hasMutator("bend")) {
                        return;
                    }

                    boolean registered = mutableCuboid.registerMutator(
                            "bend",
                            data -> new BendableCuboid.Builder().setDirection(direction).build(data)
                    );
                    String normalizedPartName = normalize(partName);
                    if (registered && mutableCuboid.hasMutator("bend")) {
                        if (REPAIR_LOGGED_PARTS.add(normalizedPartName)) {
                            Emotecraft_fix_animate_texture.LOGGER.info(
                                    "Re-registered missing Player Animator bend mutator for {} with direction {}",
                                    partName, direction
                            );
                        }
                    } else {
                        logRepairFailure(partName);
                    }
                } catch (RuntimeException exception) {
                    // A foreign cuboid implementation must never make the
                    // repair attempt crash render. BendHelper is the fallback.
                    logRepairFailure(partName);
                }
            });
        } catch (RuntimeException exception) {
            // Accessing a replaced ModelPart can itself fail. Leave the part
            // untouched and let the null-safe BendHelper skip only this bend.
            logRepairFailure(partName);
        }
    }

    private static Direction bendDirection(String partName) {
        return switch (normalize(partName)) {
            case "torso", "body" -> Direction.DOWN;
            case "leftarm", "rightarm", "leftleg", "rightleg" -> Direction.UP;
            default -> null;
        };
    }

    private static String normalize(String partName) {
        return partName == null ? "" : partName.replace("_", "").toLowerCase(Locale.ROOT);
    }

    private static void logRepairFailure(String partName) {
        if (FAILED_LOGGED_PARTS.add(normalize(partName))) {
            Emotecraft_fix_animate_texture.LOGGER.warn(
                    "Could not safely re-register Player Animator bend mutator for {}; null-safe fallback will be used",
                    partName
            );
        }
    }
}
