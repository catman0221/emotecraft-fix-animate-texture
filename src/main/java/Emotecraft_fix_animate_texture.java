package Emotecraft_fix_animate_texture;

import Emotecraft_fix_animate_texture.state.EmoteStateManager;
import Emotecraft_fix_animate_texture.compat.EmfCompat;
import net.minecraftforge.fml.ModList;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Emotecraft_fix_animate_texture.MODID)
public final class Emotecraft_fix_animate_texture {
    public static final String MODID = "emotecraft_fix_animate_texture";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Emotecraft_fix_animate_texture() {
        LOGGER.info("Initializing {}", MODID);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(Emotecraft_fix_animate_texture::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        boolean emotecraftLoaded = ModList.get().isLoaded("emotecraft");
        boolean emfLoaded = ModList.get().isLoaded("entity_model_features");

        LOGGER.info("Detected Emotecraft loaded: {}", emotecraftLoaded);
        LOGGER.info("Detected EMF loaded: {}", emfLoaded);

        event.enqueueWork(EmfCompat::registerHooks);
    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }

            EmoteStateManager.refresh(Minecraft.getInstance());
        }

        @SubscribeEvent
        public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
            if (!event.getLevel().isClientSide()) {
                return;
            }

            if (event.getEntity() instanceof Player player) {
                EmfCompat.syncPlayerAnimationState(player, false);
                EmoteStateManager.remove(player.getUUID());
            }
        }
    }
}
