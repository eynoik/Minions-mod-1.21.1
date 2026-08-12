package atomicstryker.minions.client;

import atomicstryker.minions.MinionsMod;
import atomicstryker.minions.client.gui.MinionsScreen;
import atomicstryker.minions.client.render.MinionRenderer;
import atomicstryker.minions.registry.MinionsEntities;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = MinionsMod.MOD_ID, value = Dist.CLIENT)
public final class MinionsClientEvents {
    private static final KeyMapping MENU_KEY = new KeyMapping(
            "key.minions.menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "key.categories.minions"
    );

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MinionsEntities.MINION.get(), MinionRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(MENU_KEY);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        while (MENU_KEY.consumeClick()) {
            minecraft.setScreen(new MinionsScreen());
        }
    }

    private MinionsClientEvents() {
    }
}
