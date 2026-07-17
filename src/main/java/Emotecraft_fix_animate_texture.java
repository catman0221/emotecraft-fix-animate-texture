package Emotecraft_fix_animate_texture;

import Emotecraft_fix_animate_texture.state.EmoteStateManager;
import Emotecraft_fix_animate_texture.compat.EmfCompat;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

public final class Emotecraft_fix_animate_texture implements ClientModInitializer {
    public static final String MODID = "emotecraft_fix_animate_texture";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing {}", MODID);
        boolean emotecraftLoaded = FabricLoader.getInstance().isModLoaded("emotecraft");
        boolean emfLoaded = FabricLoader.getInstance().isModLoaded("entity_model_features");

        LOGGER.info("Detected Emotecraft loaded: {}", emotecraftLoaded);
        LOGGER.info("Detected EMF loaded: {}", emfLoaded);

        EmfCompat.registerHooks();
        ClientTickEvents.END_CLIENT_TICK.register(EmoteStateManager::refresh);
    }
}
