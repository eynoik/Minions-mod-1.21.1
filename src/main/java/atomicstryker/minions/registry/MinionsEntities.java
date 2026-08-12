package atomicstryker.minions.registry;

import atomicstryker.minions.MinionsMod;
import atomicstryker.minions.common.entity.MinionEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MinionsEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MinionsMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<MinionEntity>> MINION = ENTITY_TYPES.register(
            "minion",
            () -> EntityType.Builder.of(MinionEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.2F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .fireImmune()
                    .build("minion")
    );

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(MINION.get(), MinionEntity.createDefaultAttributes().build());
    }

    private MinionsEntities() {
    }
}
