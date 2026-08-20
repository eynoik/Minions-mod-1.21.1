package atomicstryker.minions;

import atomicstryker.minions.common.MinionsConfig;
import atomicstryker.minions.network.MinionsNetwork;
import atomicstryker.minions.registry.MinionsEntities;
import atomicstryker.minions.registry.MinionsItems;
import atomicstryker.minions.registry.MinionsSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

@Mod(MinionsMod.MOD_ID)
public final class MinionsMod {
    public static final String MOD_ID = "minions";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final TicketController MINION_CHUNK_TICKETS = new TicketController(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "minion_chunks")
    );

    public MinionsMod(IEventBus modEventBus, ModContainer modContainer) {
        MinionsItems.ITEMS.register(modEventBus);
        MinionsEntities.ENTITY_TYPES.register(modEventBus);
        MinionsSounds.SOUND_EVENTS.register(modEventBus);
        modEventBus.addListener(MinionsEntities::registerAttributes);
        modEventBus.addListener(MinionsNetwork::registerPayloads);
        modEventBus.addListener(MinionsMod::registerTicketControllers);
        modEventBus.addListener(this::addCreativeTabItems);
        modContainer.registerConfig(ModConfig.Type.COMMON, MinionsConfig.SPEC);

        LOGGER.info("Minions NeoForge 1.21.1 port loaded");
    }

    private static void registerTicketControllers(RegisterTicketControllersEvent event) {
        event.register(MINION_CHUNK_TICKETS);
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(MinionsItems.MASTER_STAFF);
        }
    }
}
