package atomicstryker.minions.client;

import atomicstryker.minions.MinionsMod;
import atomicstryker.minions.client.render.MinionRenderer;
import atomicstryker.minions.registry.MinionsEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = MinionsMod.MOD_ID, value = Dist.CLIENT)
public final class MinionsClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MinionsEntities.MINION.get(), MinionRenderer::new);
    }

    private MinionsClientEvents() {
    }
}
