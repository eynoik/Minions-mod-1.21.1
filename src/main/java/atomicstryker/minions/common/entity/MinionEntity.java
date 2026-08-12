package atomicstryker.minions.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.level.Level;

/**
 * First functional 1.21.1 skeleton of the original EntityMinion.
 *
 * The old class mixed ownership, inventory, jobs, custom A* pathing, chunk loading,
 * interaction and rendering state. Those systems are being restored incrementally
 * instead of dragging obsolete Forge 1.12.2 APIs into the active source tree.
 */
public final class MinionEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> MASTER_NAME = SynchedEntityData.defineId(
            MinionEntity.class,
            EntityDataSerializers.STRING
    );

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
        goalSelector.addGoal(3, new RandomStrollGoal(this, 1.2D));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MASTER_NAME, "undef");
    }

    public void setMasterName(String name) {
        entityData.set(MASTER_NAME, name == null || name.isBlank() ? "undef" : name);
    }

    public String getMasterName() {
        String name = entityData.get(MASTER_NAME);
        return name.isBlank() ? "undef" : name;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("masterUsername", getMasterName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("masterUsername")) {
            setMasterName(tag.getString("masterUsername"));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}
