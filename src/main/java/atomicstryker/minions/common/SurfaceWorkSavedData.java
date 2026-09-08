package atomicstryker.minions.common;

import atomicstryker.minions.MinionsMod;
import atomicstryker.minions.common.entity.MinionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = MinionsMod.MOD_ID)
public final class SurfaceWorkSavedData extends SavedData {
    private static final String DATA_NAME = "minions_surface_work";
    private static final double BUILDER_REACH_SQ = 64.0D; // 8 block build reach for tall walls
    private static final int BUILDER_TELEPORT_AFTER_TICKS = 100; // five seconds at 20 TPS
    private static final Factory<SurfaceWorkSavedData> FACTORY = new Factory<>(
            SurfaceWorkSavedData::new,
            SurfaceWorkSavedData::load
    );

    /**
     * Every pending entry owns exactly one already-reserved replacement block.
     * The item is removed from the selected palette chest before Minions start
     * working. Entries whose original state is air are Fill Air build orders;
     * they are never placed automatically and wait for a Minion to reach them.
     */
    private final List<PendingReplacement> pending = new ArrayList<>();
    private final Map<UUID, BuilderProgress> builderProgress = new HashMap<>();

    public SurfaceWorkSavedData() {
    }

    public static SurfaceWorkSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    /**
     * Replaces the owner's previous surface job and reserves all materials for
     * the new one. The caller must clear the previous Minion work queues first.
     */
    public static boolean replaceJob(ServerLevel level, UUID owner, List<PendingReplacement> replacements) {
        SurfaceWorkSavedData data = get(level);
        data.finishOwner(level, owner);

        List<PendingReplacement> reserved = new ArrayList<>();
        for (PendingReplacement order : replacements) {
            if (!MaterialChestAccess.consumeBlock(level, order.materialChest, order.replacement.getBlock())) {
                // Atomic rollback of the partial reservation.
                for (PendingReplacement alreadyReserved : reserved) {
                    returnReservedMaterial(level, alreadyReserved);
                }
                data.setDirty();
                return false;
            }
            reserved.add(order);
        }

        data.pending.addAll(replacements);
        data.setDirty();
        return true;
    }

    /**
     * Cancelling is safe even if a Minion broke a replacement target on this
     * same tick. Fill Air orders are simply refunded if they were not built yet.
     */
    public static void cancel(ServerPlayer player) {
        SurfaceWorkSavedData data = get(player.serverLevel());
        if (data.finishOwner(player.serverLevel(), player.getUUID())) {
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

            if (order.original.isAir()) {
                // Fill Air is a real build task. If somebody else occupied the
                // cell before a Minion got there, do not overwrite it.
                if (!current.isAir()) {
                    returnReservedMaterial(level, order);
                    iterator.remove();
                    changed = true;
                }
                continue;
            }

            // A normal Minion BREAK order creates the air cell. Only then do we
            // spend the already-reserved material by placing the desired state.
            if (!current.isAir()) {
                continue;
            }

            placeReservedReplacement(level, order);
            iterator.remove();
            changed = true;
        }

        Set<UUID> activeBuilders = new HashSet<>();
        for (UUID owner : owners) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
            if (player == null) {
                continue;
            }

            List<MinionEntity> minions = MinionManager.getOwned(player);
            boolean anyQueuedWork = minions.stream().anyMatch(minion -> minion.queuedWork() > 0);
            if (anyQueuedWork) {
                continue;
            }

            // If an ordinary replacement order lost its worker or was skipped,
            // settle it now. Fill Air orders remain pending for the build phase.
            Iterator<PendingReplacement> stale = pending.iterator();
            while (stale.hasNext()) {
                PendingReplacement order = stale.next();
                if (!owner.equals(order.owner) || order.original.isAir()) {
                    continue;
                }
                if (level.getBlockState(order.pos).isAir()) {
                    placeReservedReplacement(level, order);
                } else {
                    returnReservedMaterial(level, order);
                }
                stale.remove();
                changed = true;
            }

            List<PendingReplacement> buildOrders = pending.stream()
                    .filter(order -> owner.equals(order.owner) && order.original.isAir())
                    .toList();
            if (buildOrders.isEmpty()) {
                continue;
            }

            List<MinionEntity> builders = minions.stream()
                    .filter(minion -> minion.queuedWork() == 0 && minion.getInventory().isEmpty())
                    .toList();
            int assignments = Math.min(buildOrders.size(), builders.size());
            for (int i = 0; i < assignments; i++) {
                PendingReplacement order = buildOrders.get(i);
                MinionEntity minion = builders.get(i);
                UUID minionId = minion.getUUID();
                activeBuilders.add(minionId);

                BuilderProgress progress = builderProgress.get(minionId);
                if (progress == null || !progress.owner.equals(owner) || !progress.pos.equals(order.pos)) {
                    progress = new BuilderProgress(owner, order.pos, 0, 0, Double.MAX_VALUE);
                }

                BlockState current = level.getBlockState(order.pos);
                if (!current.isAir()) {
                    returnReservedMaterial(level, order);
                    pending.remove(order);
                    builderProgress.remove(minionId);
                    changed = true;
                    continue;
                }

                minion.setFollowing(false);
                minion.clearMoveTarget();
                minion.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(order.replacement.getBlock()));
                double distance = minion.distanceToSqr(
                        order.pos.getX() + 0.5D, order.pos.getY() + 0.5D, order.pos.getZ() + 0.5D);

                if (distance > BUILDER_REACH_SQ) {
                    boolean madeProgress = distance + 0.25D < progress.bestDistanceSq;
                    double bestDistance = Math.min(progress.bestDistanceSq, distance);
                    boolean navigating = minion.getNavigation().moveTo(
                            order.pos.getX() + 0.5D, order.pos.getY(), order.pos.getZ() + 0.5D, 1.15D);
                    int stuckTicks = madeProgress ? 0 : progress.stuckTicks + 1;
                    if (!navigating) {
                        stuckTicks = Math.max(stuckTicks, progress.stuckTicks + 1);
                    }

                    if (stuckTicks >= BUILDER_TELEPORT_AFTER_TICKS) {
                        BlockPos teleport = findEmergencyBuilderTeleport(level, order.pos);
                        minion.getNavigation().stop();
                        minion.teleportTo(teleport.getX() + 0.5D, teleport.getY(), teleport.getZ() + 0.5D);
                        minion.setDeltaMovement(0.0D, 0.0D, 0.0D);
                        builderProgress.put(minionId,
                                new BuilderProgress(owner, order.pos, 0, 0, Double.MAX_VALUE));
                    } else {
                        builderProgress.put(minionId,
                                new BuilderProgress(owner, order.pos, 0, stuckTicks, bestDistance));
                    }
                    continue;
                }

                minion.getNavigation().stop();
                minion.getLookControl().setLookAt(
                        order.pos.getX() + 0.5D, order.pos.getY() + 0.5D, order.pos.getZ() + 0.5D);
                int ticks = progress.workTicks + 1;
                if (ticks == 1 || ticks % 4 == 0) {
                    minion.swing(InteractionHand.MAIN_HAND);
                }

                int requiredTicks = Math.max(4, MinionsConfig.WORK_TICKS_PER_BLOCK.get() / 3);
                if (ticks < requiredTicks) {
                    builderProgress.put(minionId,
                            new BuilderProgress(owner, order.pos, ticks, 0, distance));
                    continue;
                }

                BlockState desired = copyCompatibleProperties(order.original, order.replacement);
                if (desired.canSurvive(level, order.pos)) {
                    placeReservedReplacement(level, order);
                } else {
                    returnReservedMaterial(level, order);
                }
                pending.remove(order);
                builderProgress.remove(minionId);
                minion.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                changed = true;
            }
        }

        builderProgress.keySet().removeIf(minionId -> !activeBuilders.contains(minionId));

        if (changed) {
            setDirty();
        }
    }

    private static BlockPos findEmergencyBuilderTeleport(ServerLevel level, BlockPos target) {
        // Prefer an empty two-block-tall cell next to the work position, but do
        // not require a floor. This lets builders reach tall/floating walls.
        for (Direction direction : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            BlockPos candidate = target.relative(direction);
            if (level.getBlockState(candidate).isAir() && level.getBlockState(candidate.above()).isAir()) {
                return candidate;
            }
        }
        BlockPos above = target.above();
        if (level.getBlockState(above).isAir()) {
            return above;
        }

        // Last-resort recovery is deliberately unrestricted. Minions are immune
        // to environmental suffocation, and reaching the job is preferable to a
        // permanent deadlock even if the teleport cell is inside solid blocks.
        return target;
    }

    /**
     * Finishes and removes every pending order for one owner. For replacements,
     * air means the Minion already performed the destructive half and therefore
     * receives the reserved replacement. Fill Air entries never appear on cancel;
     * their still-reserved block is returned instead.
     */
    private boolean finishOwner(ServerLevel level, UUID owner) {
        boolean changed = false;
        Iterator<PendingReplacement> iterator = pending.iterator();
        while (iterator.hasNext()) {
            PendingReplacement order = iterator.next();
            if (!owner.equals(order.owner)) {
                continue;
            }

            if (order.original.isAir()) {
                returnReservedMaterial(level, order);
            } else if (level.getBlockState(order.pos).isAir()) {
                placeReservedReplacement(level, order);
            } else {
                returnReservedMaterial(level, order);
            }
            iterator.remove();
            changed = true;
        }
        builderProgress.entrySet().removeIf(entry -> owner.equals(entry.getValue().owner));
        return changed;
    }

    private static void returnReservedMaterial(ServerLevel level, PendingReplacement order) {
        ItemStack returned = new ItemStack(order.replacement.getBlock());
        if (returned.isEmpty()) {
            return;
        }

        returned = MaterialChestAccess.insert(level, order.materialChest, returned);
        if (!returned.isEmpty()) {
            ItemEntity overflow = new ItemEntity(
                    level,
                    order.materialChest.getX() + 0.5D,
                    order.materialChest.getY() + 1.0D,
                    order.materialChest.getZ() + 0.5D,
                    returned
            );
            level.addFreshEntity(overflow);
        }
    }

    private static void placeReservedReplacement(ServerLevel level, PendingReplacement order) {
        BlockState desired = copyCompatibleProperties(order.original, order.replacement);
        level.setBlock(order.pos, desired, 3);
        level.playSound(null, order.pos, desired.getSoundType().getPlaceSound(),
                net.minecraft.sounds.SoundSource.BLOCKS,
                Math.max(0.15F, desired.getSoundType().getVolume() * 0.7F),
                desired.getSoundType().getPitch());
        swingNearestWorker(level, order.owner, order.pos);
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

    private record BuilderProgress(UUID owner, BlockPos pos, int workTicks, int stuckTicks, double bestDistanceSq) {
        private BuilderProgress {
            pos = pos.immutable();
        }
    }
}
