package atomicstryker.minions.common.item;

import net.minecraft.world.item.Item;

/**
 * 1.21.1 port bootstrap of the original ItemMastersStaff.
 * Command behavior will be restored after the networking/client input layer is ported.
 */
public final class MastersStaffItem extends Item {
    public MastersStaffItem() {
        super(new Item.Properties().stacksTo(1));
    }
}
