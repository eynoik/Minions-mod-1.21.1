package atomicstryker.minions.common.entity;

import atomicstryker.minions.common.MinionManager;
import atomicstryker.minions.common.MinionsConfig;
import atomicstryker.minions.registry.MinionsSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

public final class MinionEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> MASTER_NAME = SynchedEntityData.defineId(
            MinionEntity.class,
            EntityDataSerializers.STRING
    );
    private static final EntityDataAccessor<Boolean> WORKING = SynchedEntityData.defineId(
            MinionEntity.class,
            EntityDataSerializers.BOOLEAN
    );

    private final SimpleContainer inventory = new SimpleContainer(24);
    private final Queue<WorkOrder> workQueue = new ArrayDeque<>();

    private UUID ownerUUID;
    private boolean followingMaster;
    private BlockPos moveTarget;
    private BlockPos returnContainer;
    private UUID carryTargetUUID;
    private int stuckTicks;
    private int pickupCooldown;
    private int pickupDisabledTicks;
    private int ownerMissingTicks;
    private int workTicks;
    private boolean inventoryFull;
    private boolean forceReturnGoods;
    private boolean stripMining;
    private WorkOrder activeOrder;
    private List<BlockPos> activeTreeLogs = List.of();
    private List<BlockPos> activeTreeLeaves = List.of();

    public MinionEntity(EntityType<? extends MinionEntity> entityType, Level level) {
        super(entityType, level);
        setPersistenceRequired();
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createDefaultAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.225D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8D, 100));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MASTER_NAME, "undef");
        builder.define(WORKING, false);
    }

    public void setOwner(ServerPlayer player) {
        ownerUUID = player.getUUID();
        setMasterName(player.getGameProfile().getName());
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setMasterName(String name) {
        entityData.set(MASTER_NAME, name == null || name.isBlank() ? "undef" : name);
    }

    public String getMasterName() {
        String name = entityData.get(MASTER_NAME);
        return name.isBlank() ? "undef" : name;
    }

    public boolean isWorking() {
        return entityData.get(WORKING);
    }

    public void setFollowing(boolean following) {
        followingMaster = following;
        if (following) {
            moveTarget = null;
        }
    }

    public boolean isFollowingMaster() {
        return followingMaster;
    }

    public void setMoveTarget(BlockPos target) {
        moveTarget = target == null ? null : target.immutable();
        followingMaster = false;
        stuckTicks = 0;
    }

    public void clearMoveTarget() {
        moveTarget = null;
        stuckTicks = 0;
    }

    public void enqueueWork(BlockPos pos) {
        enqueueWork(pos, 0);
    }

    public void enqueueWork(BlockPos pos, int phase) {
        enqueue(new WorkOrder(pos.immutable(), WorkAction.BREAK, null, phase));
    }

    public void enqueueTreeWork(BlockPos pos) {
        enqueue(new WorkOrder(pos.immutable(), WorkAction.TREE_BREAK, null, 0));
    }

    public void enqueuePlaceCobble(BlockPos pos) {
        enqueuePlaceCobble(pos, 0);
    }

    public void enqueuePlaceCobble(BlockPos pos, int phase) {
        enqueue(new WorkOrder(pos.immutable(), WorkAction.PLACE_COBBLE, null, phase));
    }

    public void enqueuePlaceDirt(BlockPos pos) {
        enqueuePlaceDirt(pos, 0);
    }

    public void enqueuePlaceDirt(BlockPos pos, int phase) {
        enqueue(new WorkOrder(pos.immutable(), WorkAction.PLACE_DIRT, null, phase));
    }

    public void enqueuePlaceStair(BlockPos pos, Direction facing) {
        enqueuePlaceStair(pos, facing, 0);
    }

    public void enqueuePlaceStair(BlockPos pos, Direction facing, int phase) {
        enqueue(new WorkOrder(pos.immutable(), WorkAction.PLACE_STAIR, facing, phase));
    }

    public void enqueuePlaceTorch(BlockPos pos) {
        enqueuePlaceTorch(pos, 0);
    }

    public void enqueuePlaceTorch(BlockPos pos, int phase) {
        enqueue(new WorkOrder(pos.immutable(), WorkAction.PLACE_TORCH, null, phase));
    }

    private void enqueue(WorkOrder order) {
        workQueue.add(order);
        entityData.set(WORKING, true);
    }

    public void clearWork() {
        workQueue.clear();
        stripMining = false;
        forceReturnGoods = false;
        resetActiveWork();
        entityData.set(WORKING, false);
        navigation.stop();
        setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    public int queuedWork() {
        return workQueue.size();
    }

    public int lowestQueuedPhase() {
        return workQueue.stream().mapToInt(WorkOrder::phase).min().orElse(-1);
    }

    public boolean isStripMining() {
        return stripMining;
    }

    public void setStripMining(boolean value) {
        stripMining = value;
    }

    public Container getInventory() {
        return inventory;
    }

    public void setReturnContainer(BlockPos pos) {
        setReturnContainer(pos, false);
    }

    public void setReturnContainer(BlockPos pos, boolean returnImmediately) {
        returnContainer = pos == null ? null : pos.immutable();
        if (returnImmediately && !inventory.isEmpty()) {
            forceReturnGoods = true;
        }
    }

    public void setCarryTarget(LivingEntity target) {
        carryTargetUUID = target == null ? null : target.getUUID();
        clearWork();
        followingMaster = false;
        moveTarget = null;
    }

    public void dropPassengerAndItems() {
        ejectPassengers();
        carryTargetUUID = null;
        if (level() instanceof ServerLevel serverLevel) {
            ServerPlayer owner = getOwner(serverLevel);
            if (owner != null) {
                dropStoredItemsToward(owner);
                return;
            }
        }
        dropStoredItems();
    }

    /** Restores the old DropAll behavior: throw the inventory toward the master. */
    public void dropStoredItemsToward(ServerPlayer player) {
        if (level().isClientSide) {
            return;
        }
        pickupDisabledTicks = 60; // legacy blockItemPickUp(): three seconds
        inventoryFull = false;
        forceReturnGoods = false;
        Vec3 source = position().add(0.0D, getBbHeight() * 0.65D, 0.0D);
        Vec3 toPlayer = player.getEyePosition().subtract(source);
        Vec3 velocity = toPlayer.lengthSqr() > 0.0001D
                ? toPlayer.normalize().scale(0.38D).add(0.0D, 0.12D, 0.0D)
                : new Vec3(0.0D, 0.18D, 0.0D);

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.removeItemNoUpdate(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemEntity item = new ItemEntity(level(), source.x, source.y, source.z, stack);
            item.setDeltaMovement(velocity);
            level().addFreshEntity(item);
        }
    }

    public void dropStoredItems() {
        if (level().isClientSide) {
            return;
        }
        pickupDisabledTicks = 60;
        inventoryFull = false;
        forceReturnGoods = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.removeItemNoUpdate(slot);
            if (!stack.isEmpty()) {
                spawnAtLocation(stack);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        tickOwnerDespawn(serverLevel);
        if (isRemoved()) {
            return;
        }

        if (pickupDisabledTicks > 0) {
            pickupDisabledTicks--;
        } else if (pickupCooldown-- <= 0) {
            pickupCooldown = 10;
            collectNearbyItems(serverLevel);
        }

        if ((inventoryFull || forceReturnGoods) && !inventory.isEmpty()) {
            if (tickInventoryReturn(serverLevel)) {
                return;
            }
        }

        if (!workQueue.isEmpty()) {
            tickWork(serverLevel);
        } else {
            resetActiveWork();
            entityData.set(WORKING, false);
            stripMining = false;
            setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (carryTargetUUID != null) {
                tickCarryTarget(serverLevel);
            } else if (!inventory.isEmpty() && (returnContainer != null || forceReturnGoods)) {
                tickInventoryReturn(serverLevel);
            } else if (followingMaster) {
                tickFollow(serverLevel);
            } else if (moveTarget != null) {
                tickMoveTarget(serverLevel);
            }
        }
    }

    private void tickOwnerDespawn(ServerLevel level) {
        ServerPlayer owner = getOwner(level);
        if (owner != null && owner.isAlive()) {
            ownerMissingTicks = 0;
            return;
        }

        ownerMissingTicks++;
        int delayTicks = MinionsConfig.AUTOMATIC_DESPAWN_DELAY.get() * 20;
        if (ownerMissingTicks >= delayTicks) {
            dropStoredItems();
            discard();
        }
    }

    private void tickWork(ServerLevel level) {
        WorkOrder order = workQueue.peek();
        if (order == null) {
            entityData.set(WORKING, false);
            return;
        }
        if (order.phase() > 0 && !MinionManager.canWorkPhase(ownerUUID, order.phase())) {
            entityData.set(WORKING, false);
            navigation.stop();
            return;
        }

        if (activeOrder != order) {
            resetActiveWork();
            activeOrder = order;
            workTicks = 0;
            if (order.action() == WorkAction.TREE_BREAK) {
                activeTreeLogs = MinionManager.collectTreeLogs(
                        level, order.pos(), Math.max(64, MinionsConfig.MAX_TREE_BLOCKS.get()));
                activeTreeLeaves = MinionManager.collectTreeLeaves(level, activeTreeLogs);
            }
        }

        BlockPos target = order.pos();
        BlockState state = level.getBlockState(target);
        if (order.action() == WorkAction.BREAK || order.action() == WorkAction.TREE_BREAK) {
            if (state.isAir() || state.getDestroySpeed(level, target) < 0.0F) {
                completeCurrentOrder(level);
                return;
            }
        } else {
            BlockState desired = desiredState(order);
            if (desired == null) {
                completeCurrentOrder(level);
                return;
            }
            if (state.equals(desired)) {
                completeCurrentOrder(level);
                return;
            }
            if (!state.isAir() && state.getDestroySpeed(level, target) < 0.0F) {
                completeCurrentOrder(level);
                return;
            }
        }

        double distance = distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
        // Legacy BlockTask_TreeChop inherited the normal 3-block work reach.
        // The Minion must actually walk up to the trunk instead of chopping from afar.
        double workReachSq = 9.0D;
        if (distance <= workReachSq) {
            navigation.stop();
            entityData.set(WORKING, true);
            getLookControl().setLookAt(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
            adaptWorkingTool(state, order.action());

            int requiredTicks;
            if (order.action() == WorkAction.TREE_BREAK) {
                // Original BlockTask_TreeChop used 1000 ms per wood block and
                // then removed the complete tree in one finish step.
                requiredTicks = Math.max(
                        MinionsConfig.WORK_TICKS_PER_BLOCK.get(),
                        MinionsConfig.WORK_TICKS_PER_BLOCK.get() * Math.max(1, activeTreeLogs.size())
                );
            } else if (order.action() == WorkAction.BREAK) {
                requiredTicks = MinionsConfig.WORK_TICKS_PER_BLOCK.get();
            } else {
                requiredTicks = Math.max(4, MinionsConfig.WORK_TICKS_PER_BLOCK.get() / 3);
            }
            workTicks++;

            if (order.action() == WorkAction.BREAK || order.action() == WorkAction.TREE_BREAK) {
                int crack = Math.min(9, (workTicks * 10) / Math.max(1, requiredTicks));
                level.destroyBlockProgress(getId(), target, crack);
                if (workTicks == 1 || workTicks % 6 == 0) {
                    swing(InteractionHand.MAIN_HAND);
                    SoundType soundType = state.getSoundType();
                    level.playSound(null, target, soundType.getHitSound(), SoundSource.BLOCKS,
                            Math.max(0.1F, soundType.getVolume() * 0.35F), soundType.getPitch());
                }
            } else if (workTicks == 1) {
                swing(InteractionHand.MAIN_HAND);
            }

            if (workTicks < requiredTicks) {
                return;
            }

            level.destroyBlockProgress(getId(), target, -1);
            if (order.action() == WorkAction.TREE_BREAK) {
                harvestWholeTreeIntoInventory(level);
            } else if (order.action() == WorkAction.BREAK) {
                harvestBlockIntoInventory(level, target, state);
            } else {
                if (!state.isAir()) {
                    harvestBlockIntoInventory(level, target, state);
                }
                BlockState desired = desiredState(order);
                if (desired != null && desired.canSurvive(level, target)) {
                    level.setBlock(target, desired, 3);
                }
            }

            completeCurrentOrder(level);
            return;
        }

        entityData.set(WORKING, false);
        if (workTicks > 0) {
            level.destroyBlockProgress(getId(), target, -1);
            workTicks = 0;
        }
        adaptWorkingTool(state, order.action());
        BlockPos navigationTarget = order.action() == WorkAction.TREE_BREAK
                ? findTreeWorkPosition(level, target)
                : target;
        boolean navigating = navigation.moveTo(
                navigationTarget.getX() + 0.5D, navigationTarget.getY(), navigationTarget.getZ() + 0.5D, 1.15D);
        if (!navigating) {
            stuckTicks++;
            if (stuckTicks > 80) {
                BlockPos safe = order.action() == WorkAction.TREE_BREAK
                        ? findTreeWorkPosition(level, target)
                        : findSafeTeleport(level, target);
                teleportTo(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
                stuckTicks = 0;
            }
        } else {
            stuckTicks = Math.max(0, stuckTicks - 1);
        }
    }

    private void adaptWorkingTool(BlockState state, WorkAction action) {
        if (action != WorkAction.BREAK && action != WorkAction.TREE_BREAK) {
            return;
        }
        ItemStack wanted;
        if (action == WorkAction.TREE_BREAK || state.is(BlockTags.MINEABLE_WITH_AXE)) {
            wanted = new ItemStack(Items.IRON_AXE);
        } else if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            wanted = new ItemStack(Items.IRON_SHOVEL);
        } else {
            wanted = new ItemStack(Items.IRON_PICKAXE);
        }
        ItemStack held = getMainHandItem();
        if (!ItemStack.isSameItemSameComponents(held, wanted)) {
            setItemInHand(InteractionHand.MAIN_HAND, wanted);
        }
    }

    /**
     * Finishes a legacy-style tree job in one server tick: after the Minion has
     * spent time chopping at the trunk, all scanned logs and their canopy are
     * removed together. Log drops and normal leaf loot (saplings/apples/sticks
     * according to the active vanilla/modded loot table) go into the backpack.
     */
    private void harvestWholeTreeIntoInventory(ServerLevel level) {
        for (BlockPos log : activeTreeLogs) {
            BlockState state = level.getBlockState(log);
            if (state.is(BlockTags.LOGS)) {
                harvestBlockIntoInventory(level, log, state);
            }
        }
        for (BlockPos leaf : activeTreeLeaves) {
            BlockState state = level.getBlockState(leaf);
            if (state.is(BlockTags.LEAVES)) {
                // The held iron axe is neither shears nor Silk Touch, therefore
                // Block.getDrops follows the normal Minecraft leaf loot chances.
                harvestBlockIntoInventory(level, leaf, state);
            }
        }
    }

    /**
     * Legacy Minions do not leave normal mining drops scattered on the floor.
     * The harvested block is converted to drops using the Minion's visible tool
     * and those drops are inserted directly into its 24-slot backpack. Overflow
     * is left in-world and marks the backpack full so return/deposit logic runs.
     * This is deliberately separate from dropStoredItemsToward(), so the manual
     * "give me your items" command keeps its old throw-toward-owner behavior.
     */
    private void harvestBlockIntoInventory(ServerLevel level, BlockPos target, BlockState state) {
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(target) : null;
        List<ItemStack> drops = Block.getDrops(state, level, target, blockEntity, this, getMainHandItem());

        level.destroyBlock(target, false, this);

        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            ItemStack remaining = inventory.addItem(drop.copy());
            if (!remaining.isEmpty()) {
                inventoryFull = true;
                ItemEntity overflow = new ItemEntity(
                        level,
                        target.getX() + 0.5D,
                        target.getY() + 0.5D,
                        target.getZ() + 0.5D,
                        remaining
                );
                level.addFreshEntity(overflow);
            }
        }
    }

    private void completeCurrentOrder(ServerLevel level) {
        if (activeOrder != null) {
            level.destroyBlockProgress(getId(), activeOrder.pos(), -1);
        }
        workQueue.poll();
        activeOrder = null;
        activeTreeLogs = List.of();
        activeTreeLeaves = List.of();
        workTicks = 0;
        stuckTicks = 0;
        entityData.set(WORKING, false);
        if (workQueue.isEmpty()) {
            stripMining = false;
            if (!inventory.isEmpty()) {
                forceReturnGoods = true;
            }
        }
    }

    private void resetActiveWork() {
        if (activeOrder != null && level() instanceof ServerLevel serverLevel) {
            serverLevel.destroyBlockProgress(getId(), activeOrder.pos(), -1);
        }
        activeOrder = null;
        activeTreeLogs = List.of();
        activeTreeLeaves = List.of();
        workTicks = 0;
    }

    private BlockState desiredState(WorkOrder order) {
        return switch (order.action()) {
            case BREAK, TREE_BREAK -> null;
            case PLACE_COBBLE -> Blocks.COBBLESTONE.defaultBlockState();
            case PLACE_DIRT -> Blocks.DIRT.defaultBlockState();
            case PLACE_TORCH -> Blocks.TORCH.defaultBlockState();
            case PLACE_STAIR -> Blocks.STONE_STAIRS.defaultBlockState().setValue(
                    BlockStateProperties.HORIZONTAL_FACING,
                    order.facing() == null || order.facing().getAxis() == Direction.Axis.Y ? Direction.NORTH : order.facing()
            );
        };
    }

    private void tickCarryTarget(ServerLevel level) {
        Entity target = level.getEntity(carryTargetUUID);
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            carryTargetUUID = null;
            navigation.stop();
            return;
        }
        if (distanceToSqr(living) <= 4.0D) {
            navigation.stop();
            living.startRiding(this, true);
            carryTargetUUID = null;
        } else {
            navigation.moveTo(living, 1.2D);
        }
    }

    private void tickFollow(ServerLevel level) {
        ServerPlayer owner = getOwner(level);
        if (owner == null) {
            navigation.stop();
            return;
        }
        double distance = distanceToSqr(owner);
        if (distance > 48.0D * 48.0D) {
            teleportTo(owner.getX(), owner.getY(), owner.getZ());
        } else if (distance > 3.5D * 3.5D) {
            navigation.moveTo(owner, 1.2D);
        } else {
            navigation.stop();
        }
    }

    private void tickMoveTarget(ServerLevel level) {
        if (moveTarget == null) {
            return;
        }
        double distance = distanceToSqr(moveTarget.getX() + 0.5D, moveTarget.getY(), moveTarget.getZ() + 0.5D);
        if (distance <= 4.0D) {
            navigation.stop();
            moveTarget = null;
            return;
        }
        if (!navigation.moveTo(moveTarget.getX() + 0.5D, moveTarget.getY(), moveTarget.getZ() + 0.5D, 1.15D)) {
            stuckTicks++;
            if (stuckTicks > 100) {
                teleportTo(moveTarget.getX() + 0.5D, moveTarget.getY(), moveTarget.getZ() + 0.5D);
                moveTarget = null;
                stuckTicks = 0;
            }
        }
    }

    private boolean tickInventoryReturn(ServerLevel level) {
        if (inventory.isEmpty()) {
            inventoryFull = false;
            forceReturnGoods = false;
            return false;
        }

        if (returnContainer != null) {
            BlockEntity blockEntity = level.getBlockEntity(returnContainer);
            if (!(blockEntity instanceof Container container)) {
                returnContainer = null;
            } else {
                if (distanceToSqr(returnContainer.getX() + 0.5D, returnContainer.getY() + 0.5D, returnContainer.getZ() + 0.5D) > 9.0D) {
                    navigation.moveTo(returnContainer.getX() + 0.5D, returnContainer.getY(), returnContainer.getZ() + 0.5D, 1.15D);
                    return true;
                }

                for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                    ItemStack carried = inventory.getItem(slot);
                    if (carried.isEmpty()) {
                        continue;
                    }
                    ItemStack remaining = carried.copy();
                    for (int targetSlot = 0; targetSlot < container.getContainerSize() && !remaining.isEmpty(); targetSlot++) {
                        ItemStack existing = container.getItem(targetSlot);
                        if (existing.isEmpty()) {
                            container.setItem(targetSlot, remaining.copy());
                            remaining = ItemStack.EMPTY;
                        } else if (ItemStack.isSameItemSameComponents(existing, remaining) && existing.getCount() < existing.getMaxStackSize()) {
                            int moved = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                            existing.grow(moved);
                            remaining.shrink(moved);
                            container.setItem(targetSlot, existing);
                        }
                    }
                    inventory.setItem(slot, remaining);
                }
                container.setChanged();
                if (inventory.isEmpty()) {
                    inventoryFull = false;
                    forceReturnGoods = false;
                    return false;
                }
                return true;
            }
        }

        ServerPlayer owner = getOwner(level);
        if (owner == null) {
            navigation.stop();
            return true;
        }
        if (distanceToSqr(owner) > 9.0D) {
            navigation.moveTo(owner, 1.15D);
            return true;
        }
        navigation.stop();
        dropStoredItemsToward(owner);
        return false;
    }

    private void collectNearbyItems(ServerLevel level) {
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(blockPosition()).inflate(4.5D));
        for (ItemEntity itemEntity : items) {
            if (itemEntity.tickCount <= 200) {
                continue;
            }
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack remaining = inventory.addItem(stack.copy());
            int accepted = stack.getCount() - remaining.getCount();
            if (accepted <= 0) {
                inventoryFull = true;
                break;
            }
            if (remaining.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(remaining);
                inventoryFull = true;
                break;
            }
        }
    }

    private ServerPlayer getOwner(ServerLevel level) {
        if (ownerUUID == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(ownerUUID);
    }

    private BlockPos findTreeWorkPosition(ServerLevel level, BlockPos trunk) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                BlockPos candidate = trunk.offset(dx, 0, dz);
                if (!level.getBlockState(candidate).isAir() || !level.getBlockState(candidate.above()).isAir()) {
                    continue;
                }
                if (level.getBlockState(candidate.below()).isAir()) {
                    continue;
                }
                double d = distanceToSqr(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = candidate.immutable();
                }
            }
        }
        return best == null ? trunk : best;
    }

    private BlockPos findSafeTeleport(ServerLevel level, BlockPos around) {
        for (int dy = 1; dy <= 4; dy++) {
            BlockPos candidate = around.above(dy);
            if (level.getBlockState(candidate).isAir() && level.getBlockState(candidate.above()).isAir()) {
                return candidate;
            }
        }
        return around.above();
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        // The 1.12.2 Minion rejected ordinary damage entirely. For the modern
        // port keep player/mob-caused damage semantics available, but make the
        // worker immune to environmental hazards: lava/fire, drowning,
        // suffocation, falling, cactus, stalactites and other source-less cave
        // damage. Projectile/mob damage still has a source entity.
        if (source.getEntity() == null && source.getDirectEntity() == null) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public Component getName() {
        return hasCustomName() ? getCustomName() : Component.translatable("entity.minions.minion");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("masterUsername", getMasterName());
        if (ownerUUID != null) {
            tag.putUUID("masterUUID", ownerUUID);
        }
        tag.putBoolean("followingMaster", followingMaster);
        tag.putBoolean("inventoryFull", inventoryFull);
        tag.putBoolean("forceReturnGoods", forceReturnGoods);
        tag.putBoolean("stripMining", stripMining);
        tag.putInt("ownerMissingTicks", ownerMissingTicks);
        if (moveTarget != null) {
            tag.putLong("moveTarget", moveTarget.asLong());
        }
        if (returnContainer != null) {
            tag.putLong("returnContainer", returnContainer.asLong());
        }
        if (carryTargetUUID != null) {
            tag.putUUID("carryTargetUUID", carryTargetUUID);
        }

        NonNullList<ItemStack> savedItems = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            savedItems.set(slot, inventory.getItem(slot));
        }
        CompoundTag inventoryTag = new CompoundTag();
        ContainerHelper.saveAllItems(inventoryTag, savedItems, level().registryAccess());
        tag.put("MinionInventory", inventoryTag);

        ListTag workList = new ListTag();
        for (WorkOrder order : workQueue) {
            CompoundTag workTag = new CompoundTag();
            workTag.putLong("Pos", order.pos().asLong());
            workTag.putInt("Action", order.action().ordinal());
            workTag.putInt("Facing", order.facing() == null ? -1 : order.facing().get3DDataValue());
            workTag.putInt("Phase", order.phase());
            workList.add(workTag);
        }
        tag.put("workQueueV2", workList);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("masterUsername")) {
            setMasterName(tag.getString("masterUsername"));
        }
        if (tag.hasUUID("masterUUID")) {
            ownerUUID = tag.getUUID("masterUUID");
        }
        followingMaster = tag.getBoolean("followingMaster");
        inventoryFull = tag.getBoolean("inventoryFull");
        forceReturnGoods = tag.getBoolean("forceReturnGoods");
        stripMining = tag.getBoolean("stripMining");
        ownerMissingTicks = tag.getInt("ownerMissingTicks");
        moveTarget = tag.contains("moveTarget") ? BlockPos.of(tag.getLong("moveTarget")) : null;
        returnContainer = tag.contains("returnContainer") ? BlockPos.of(tag.getLong("returnContainer")) : null;
        carryTargetUUID = tag.hasUUID("carryTargetUUID") ? tag.getUUID("carryTargetUUID") : null;

        if (tag.contains("MinionInventory")) {
            NonNullList<ItemStack> savedItems = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag.getCompound("MinionInventory"), savedItems, level().registryAccess());
            for (int slot = 0; slot < savedItems.size(); slot++) {
                inventory.setItem(slot, savedItems.get(slot));
            }
        }

        workQueue.clear();
        if (tag.contains("workQueueV2", Tag.TAG_LIST)) {
            ListTag workList = tag.getList("workQueueV2", Tag.TAG_COMPOUND);
            for (int i = 0; i < workList.size(); i++) {
                CompoundTag workTag = workList.getCompound(i);
                int actionId = workTag.getInt("Action");
                WorkAction[] actions = WorkAction.values();
                WorkAction action = actionId >= 0 && actionId < actions.length ? actions[actionId] : WorkAction.BREAK;
                int facingId = workTag.getInt("Facing");
                Direction facing = facingId < 0 ? null : Direction.from3DDataValue(facingId);
                workQueue.add(new WorkOrder(BlockPos.of(workTag.getLong("Pos")), action, facing, workTag.contains("Phase") ? workTag.getInt("Phase") : 0));
            }
        } else {
            // beta.1 compatibility
            for (long packed : tag.getLongArray("workQueue")) {
                workQueue.add(new WorkOrder(BlockPos.of(packed), WorkAction.BREAK, null, 0));
            }
        }
        entityData.set(WORKING, !workQueue.isEmpty());
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public enum WorkAction {
        BREAK,
        PLACE_COBBLE,
        PLACE_DIRT,
        PLACE_STAIR,
        PLACE_TORCH,
        TREE_BREAK
    }

    public record WorkOrder(BlockPos pos, WorkAction action, Direction facing, int phase) {
    }
}
