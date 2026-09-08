package atomicstryker.minions.common;

import atomicstryker.minions.MinionsMod;
import atomicstryker.minions.common.entity.MinionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = MinionsMod.MOD_ID)
public final class SurfaceWorkSavedData extends SavedData {
    private static final String DATA_NAME = "minions_surface_work";
    private static final Factory<SurfaceWorkSavedData> FACTORY = new Factory<>(
            SurfaceWorkSavedData::new,
            SurfaceWorkSavedData::load
    );

    private final List<PendingReplacement> pending = new ArrayList<>();

    public SurfaceWorkSavedData() {
    }

    public static SurfaceWorkSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static void replaceJob(ServerLevel level, UUID owner, List<PendingReplacement> replacements) {
        SurfaceWorkSavedData data = get(level);
        data.pending.removeIf(order -> owner.equals(order.owner));
        data.pending.addAll(replacements);
        data.setDirty();
    }

    public static void cancel(ServerPlayer player) {
        SurfaceWorkSavedData data = get(player.serverLevel());
        if (data.pending.removeIf(order -> player.getUUID().equals(order.owner))) {
            data.setDirty();
        }
    }

    public static boolean hasJob(ServerPlayer player) {
        UUID owner = player.getUUID();
        return get(player.serverLevel()).pending.stream().anyMatch(order -> owner.equals(order.owner));
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            SurfaceWorkSavedData data = get(level);
            if (!data.pending.isEmpty()) {
                data.tick(level);
            }
        }
    }

    private void tick(ServerLevel level) {
        boolean changed = false;
        Set<UUID> owners = new HashSet<>();

        Iterator<PendingReplacement> iterator = pending.iterator();
        while (iterator.hasNext()) {
            PendingReplacement order = iterator.next();
            owners.add(order.owner);

            BlockState current = level.getBlockState(order.pos);
            if (current.is(order.replacement.getBlock())) {
                iterator.remove();
                changed = true;
                continue;
            }

            if (!current.isAir()) {
                continue;
            }

            BlockState desired = copyCompatibleProperties(order.original, order.replacement);
            boolean canPlace = desired.canSurvive(level, order.pos);
            if (canPlace && consumeMaterial(level, order.materialChest, desired.getBlock())) {
                level.setBlock(order.pos, desired, 3);
                level.playSound(null, order.pos, desired.getSoundType().getPlaceSound(),
                        net.minecraft.sounds.SoundSource.BLOCKS,
                        Math.max(0.15F, desired.getSoundType().getVolume() * 0.7F),
                        desired.getSoundType().getPitch());
                swingNearestWorker(level, order.owner, order.pos);
            } else {
                // Never leave a hole merely because the palette chest changed while the job was running.
                level.setBlock(order.pos, order.original, 3);
            }
            iterator.remove();
            changed = true;
        }

        // If the player cancelled/replaced the Minion order, discard untouched pending replacements.
        // Air cells have already been handled above, so this cannot strand a freshly mined hole.
        for (UUID owner : owners) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
            if (player == null) {
                continue;
            }
            boolean anyQueuedWork = MinionManager.getOwned(player).stream().anyMatch(minion -> minion.queuedWork() > 0);
            if (!anyQueuedWork && pending.removeIf(order -> owner.equals(order.owner))) {
                changed = true;
            }
        }

        if (changed) {
            setDirty();
        }
    }

    private static boolean consumeMaterial(ServerLevel level, BlockPos chestPos, Block block) {
        BlockEntity blockEntity = level.getBlockEntity(chestPos);
        if (!(blockEntity instanceof Container container)) {
            return false;
        }
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem) || blockItem.getBlock() != block) {
                continue;
            }
            stack.shrink(1);
            container.setItem(slot, stack);
            container.setChanged();
            return true;
        }
        return false;
    }

    private static void swingNearestWorker(ServerLevel level, UUID owner, BlockPos pos) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player == null) {
            return;
        }
        MinionEntity nearest = null;
        double nearestDistance = 64.0D;
        for (MinionEntity minion : MinionManager.getOwned(player)) {
            double distance = minion.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = minion;
            }
        }
        if (nearest != null) {
            nearest.swing(InteractionHand.MAIN_HAND);
        }
    }

    private static BlockState copyCompatibleProperties(BlockState original, BlockState replacement) {
        BlockState result = replacement;
        for (Property<?> property : original.getProperties()) {
            if (result.hasProperty(property)) {
                result = copyProperty(original, result, property);
            }
        }
        return result;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    public static SurfaceWorkSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SurfaceWorkSavedData data = new SurfaceWorkSavedData();
        if (!tag.contains("Pending", Tag.TAG_LIST)) {
            return data;
        }
        ListTag list = tag.getList("Pending", Tag.TAG_COMPOUND);
        var blocks = registries.lookupOrThrow(Registries.BLOCK);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("Owner") || !entry.contains("Original", Tag.TAG_COMPOUND)
                    || !entry.contains("Replacement", Tag.TAG_COMPOUND)) {
                continue;
            }
            data.pending.add(new PendingReplacement(
                    entry.getUUID("Owner"),
                    BlockPos.of(entry.getLong("Pos")),
                    BlockPos.of(entry.getLong("Chest")),
                    NbtUtils.readBlockState(blocks, entry.getCompound("Original")),
                    NbtUtils.readBlockState(blocks, entry.getCompound("Replacement"))
            ));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (PendingReplacement order : pending) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Owner", order.owner);
            entry.putLong("Pos", order.pos.asLong());
            entry.putLong("Chest", order.materialChest.asLong());
            entry.put("Original", NbtUtils.writeBlockState(order.original));
            entry.put("Replacement", NbtUtils.writeBlockState(order.replacement));
            list.add(entry);
        }
        tag.put("Pending", list);
        return tag;
    }

    public record PendingReplacement(UUID owner, BlockPos pos, BlockPos materialChest,
                                     BlockState original, BlockState replacement) {
        public PendingReplacement {
            pos = pos.immutable();
            materialChest = materialChest.immutable();
        }
    }
}
