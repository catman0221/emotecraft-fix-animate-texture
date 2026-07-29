package Emotecraft_fix_animate_texture;

import Emotecraft_fix_animate_texture.compat.EmfCompat;
import Emotecraft_fix_animate_texture.state.EmoteStateManager;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

public final class Emotecraft_fix_animate_texture implements ClientModInitializer {
    public static final String MODID = "emotecraft_fix_animate_texture";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitializeClient() {
        boolean emotecraftLoaded = FabricLoader.getInstance().isModLoaded("emotecraft");
        boolean emfLoaded = FabricLoader.getInstance().isModLoaded("entity_model_features");

        LOGGER.info("Initializing {} for Minecraft 1.21.1", MODID);
        LOGGER.info("Detected Emotecraft loaded: {}", emotecraftLoaded);
        LOGGER.info("Detected EMF loaded: {}", emfLoaded);

        EmfCompat.initialize();
        ClientTickEvents.END_CLIENT_TICK.register(EmoteStateManager::refresh);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, minecraft) -> EmoteStateManager.clearAndRestore());
    }
}
