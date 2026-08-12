package atomicstryker.minions.registry;

import atomicstryker.minions.MinionsMod;
import atomicstryker.minions.common.item.MastersStaffItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MinionsItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MinionsMod.MOD_ID);

    public static final DeferredItem<MastersStaffItem> MASTER_STAFF = ITEMS.register(
            "masterstaff",
            MastersStaffItem::new
    );

    private MinionsItems() {
    }
}
