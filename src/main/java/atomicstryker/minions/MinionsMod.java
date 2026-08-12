package atomicstryker.minions;

import atomicstryker.minions.registry.MinionsEntities;
import atomicstryker.minions.registry.MinionsItems;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(MinionsMod.MOD_ID)
public final class MinionsMod {
    public static final String MOD_ID = "minions";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MinionsMod(IEventBus modEventBus) {
        MinionsItems.ITEMS.register(modEventBus);
        MinionsEntities.ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(MinionsEntities::registerAttributes);

        LOGGER.info("Minions NeoForge 1.21.1 port bootstrap loaded");
    }
}
