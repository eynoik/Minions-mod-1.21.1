package atomicstryker.minions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the material source selected by the player. Normal containers expose
 * one part; a vanilla/trapped double chest exposes both halves in a stable
 * LEFT -> RIGHT order so palette slot order does not depend on which half was clicked.
 */
public final class MaterialChestAccess {
    private MaterialChestAccess() {
    }

    public static List<Part> resolve(ServerLevel level, BlockPos origin) {
        BlockEntity blockEntity = level.getBlockEntity(origin);
        if (!(blockEntity instanceof Container first)) {
            return List.of();
        }

        BlockState state = level.getBlockState(origin);
        if (!(state.getBlock() instanceof ChestBlock)
                || !state.hasProperty(ChestBlock.TYPE)
                || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return List.of(new Part(origin, first));
        }

        ChestType ownType = state.getValue(ChestBlock.TYPE);
        for (Direction direction : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            BlockPos otherPos = origin.relative(direction);
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.getBlock() != state.getBlock()
                    || !otherState.hasProperty(ChestBlock.TYPE)
                    || otherState.getValue(ChestBlock.TYPE) == ChestType.SINGLE
                    || otherState.getValue(ChestBlock.TYPE) == ownType
                    || !otherState.hasProperty(ChestBlock.FACING)
                    || !state.hasProperty(ChestBlock.FACING)
                    || otherState.getValue(ChestBlock.FACING) != state.getValue(ChestBlock.FACING)) {
                continue;
            }

            BlockEntity otherEntity = level.getBlockEntity(otherPos);
            if (!(otherEntity instanceof Container second)) {
                continue;
            }

            Part own = new Part(origin, first);
            Part other = new Part(otherPos, second);
            return ownType == ChestType.LEFT ? List.of(own, other) : List.of(other, own);
        }

        // If the other half is missing/unloaded, keep the selected half usable
        // instead of treating the whole material source as invalid.
        return List.of(new Part(origin, first));
    }

    public static boolean consumeBlock(ServerLevel level, BlockPos origin, net.minecraft.world.level.block.Block block) {
        for (Part part : resolve(level, origin)) {
            Container container = part.container();
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)
                        || blockItem.getBlock() != block) {
                    continue;
                }
                stack.shrink(1);
                container.setItem(slot, stack);
                container.setChanged();
                return true;
            }
        }
        return false;
    }

    public static ItemStack insert(ServerLevel level, BlockPos origin, ItemStack stack) {
        ItemStack remaining = stack.copy();
        List<Part> parts = new ArrayList<>(resolve(level, origin));
        for (Part part : parts) {
            Container container = part.container();
            for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
                ItemStack existing = container.getItem(slot);
                if (existing.isEmpty()) {
                    int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                    ItemStack inserted = remaining.copy();
                    inserted.setCount(moved);
                    container.setItem(slot, inserted);
                    remaining.shrink(moved);
                } else if (ItemStack.isSameItemSameComponents(existing, remaining)
                        && existing.getCount() < existing.getMaxStackSize()) {
                    int moved = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                    existing.grow(moved);
                    remaining.shrink(moved);
                    container.setItem(slot, existing);
                }
            }
            container.setChanged();
        }
        return remaining;
    }

    public record Part(BlockPos pos, Container container) {
        public Part {
            pos = pos.immutable();
        }
    }
}
