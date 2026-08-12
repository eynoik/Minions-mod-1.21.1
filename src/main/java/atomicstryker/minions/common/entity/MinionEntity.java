package atomicstryker.minions.common.entity;

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
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

    public MinionEntity(EntityType<? extends MinionEntity> entityType, Level level) {
        super(entityType, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createDefaultAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
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
        enqueue(new WorkOrder(pos.immutable(), WorkAction.BREAK, null));
    }

    public void enqueueTreeWork(BlockPos pos) {
        enqueue(new WorkOrder(pos.immutable(), WorkAction.TREE_BREAK, null));
    }

    public void enqueuePlaceCobble(BlockPos pos) {
        enqueue(new WorkOrder(pos.immutable(), WorkAction.PLACE_COBBLE, null));
    }

    public void enqueuePlaceDirt(BlockPos pos) {
        enqueue(new WorkOrder(pos.immutable(), WorkAction.PLACE_DIRT, null));
    }

    public void enqueuePlaceStair(BlockPos pos, Direction facing) {
        enqueue(new WorkOrder(pos.immutable(), WorkAction.PLACE_STAIR, facing));
    }

    public void enqueuePlaceTorch(BlockPos pos) {
        enqueue(new WorkOrder(pos.immutable(), WorkAction.PLACE_TORCH, null));
    }

    private void enqueue(WorkOrder order) {
        workQueue.add(order);
        entityData.set(WORKING, true);
    }

    public void clearWork() {
        workQueue.clear();
        entityData.set(WORKING, false);
        navigation.stop();
    }

    public int queuedWork() {
        return workQueue.size();
    }

    public Container getInventory() {
        return inventory;
    }

    public void setReturnContainer(BlockPos pos) {
        returnContainer = pos == null ? null : pos.immutable();
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

        if (pickupDisabledTicks > 0) {
            pickupDisabledTicks--;
        } else if (pickupCooldown-- <= 0) {
            pickupCooldown = 10;
            collectNearbyItems(serverLevel);
        }

        if (!workQueue.isEmpty()) {
            tickWork(serverLevel);
        } else {
            entityData.set(WORKING, false);
            if (carryTargetUUID != null) {
                tickCarryTarget(serverLevel);
            } else if (returnContainer != null && !inventory.isEmpty()) {
                tickReturnToContainer(serverLevel);
            } else if (followingMaster) {
                tickFollow(serverLevel);
            } else if (moveTarget != null) {
                tickMoveTarget(serverLevel);
            }
        }
    }

    private void tickWork(ServerLevel level) {
        WorkOrder order = workQueue.peek();
        if (order == null) {
            entityData.set(WORKING, false);
            return;
        }

        BlockPos target = order.pos();
        BlockState state = level.getBlockState(target);
        if (order.action() == WorkAction.BREAK || order.action() == WorkAction.TREE_BREAK) {
            if (state.isAir() || state.getDestroySpeed(level, target) < 0.0F) {
                workQueue.poll();
                return;
            }
        } else {
            BlockState desired = desiredState(order);
            if (desired == null) {
                workQueue.poll();
                return;
            }
            if (state.equals(desired)) {
                workQueue.poll();
                return;
            }
            if (!state.isAir() && state.getDestroySpeed(level, target) < 0.0F) {
                workQueue.poll();
                return;
            }
        }

        double distance = distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
        double workReachSq = order.action() == WorkAction.TREE_BREAK ? 256.0D : 9.0D;
        if (distance <= workReachSq) {
            navigation.stop();
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);

            if (order.action() == WorkAction.BREAK || order.action() == WorkAction.TREE_BREAK) {
                level.destroyBlock(target, true, this);
            } else {
                if (!state.isAir()) {
                    level.destroyBlock(target, true, this);
                }
                BlockState desired = desiredState(order);
                if (desired != null && desired.canSurvive(level, target)) {
                    level.setBlock(target, desired, 3);
                }
            }

            workQueue.poll();
            stuckTicks = 0;
            return;
        }

        boolean navigating = navigation.moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1.15D);
        if (!navigating) {
            stuckTicks++;
            if (stuckTicks > 80) {
                BlockPos safe = findSafeTeleport(level, target);
                teleportTo(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
                stuckTicks = 0;
            }
        } else {
            stuckTicks = Math.max(0, stuckTicks - 1);
        }
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

    private void tickReturnToContainer(ServerLevel level) {
        BlockEntity blockEntity = level.getBlockEntity(returnContainer);
        if (!(blockEntity instanceof Container container)) {
            returnContainer = null;
            return;
        }

        if (distanceToSqr(returnContainer.getX() + 0.5D, returnContainer.getY() + 0.5D, returnContainer.getZ() + 0.5D) > 9.0D) {
            navigation.moveTo(returnContainer.getX() + 0.5D, returnContainer.getY(), returnContainer.getZ() + 0.5D, 1.15D);
            return;
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
            returnContainer = null;
        }
    }

    private void collectNearbyItems(ServerLevel level) {
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(blockPosition()).inflate(4.5D));
        for (ItemEntity itemEntity : items) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack remaining = inventory.addItem(stack.copy());
            int accepted = stack.getCount() - remaining.getCount();
            if (accepted <= 0) {
                continue;
            }
            if (remaining.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(remaining);
            }
        }
    }

    private ServerPlayer getOwner(ServerLevel level) {
        if (ownerUUID == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(ownerUUID);
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
    protected SoundEvent getAmbientSound() {
        return MinionsSounds.MINION_SQUEAK.get();
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
                workQueue.add(new WorkOrder(BlockPos.of(workTag.getLong("Pos")), action, facing));
            }
        } else {
            // beta.1 compatibility
            for (long packed : tag.getLongArray("workQueue")) {
                workQueue.add(new WorkOrder(BlockPos.of(packed), WorkAction.BREAK, null));
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

    public record WorkOrder(BlockPos pos, WorkAction action, Direction facing) {
    }
}
