package Emotecraft_fix_animate_texture;

import Emotecraft_fix_animate_texture.state.EmoteStateManager;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Emotecraft_fix_animate_texture.MODID)
public final class Emotecraft_fix_animate_texture {
    public static final String MODID = "emotecraft_fix_animate_texture";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Emotecraft_fix_animate_texture() {
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
                EmoteStateManager.remove(player.getUUID());
            }
        }
    }
}
