package atomicstryker.minions.common;

import atomicstryker.minions.MinionsMod;
import atomicstryker.minions.common.entity.MinionEntity;
import atomicstryker.minions.registry.MinionsItems;
import atomicstryker.minions.registry.MinionsSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = MinionsMod.MOD_ID)
public final class StaffMagicEvents {
    private StaffMagicEvents() {
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.getMainHandItem().is(MinionsItems.MASTER_STAFF.get())) {
            return;
        }

        event.setCanceled(true);
        if (!MinionManager.canUsePower(player)) {
            player.level().playSound(null, player.blockPosition(), MinionsSounds.FART.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos strikePos = event.getPos().relative(event.getFace());
        level.playSound(null, player.blockPosition(), MinionsSounds.BOLT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
        if (lightning != null) {
            lightning.setVisualOnly(true);
            lightning.moveTo(strikePos.getX() + 0.5D, strikePos.getY(), strikePos.getZ() + 0.5D);
            level.addFreshEntity(lightning);
        }

        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(strikePos).inflate(1.75D),
                target -> target != player && !(target instanceof MinionEntity)
        )) {
            target.hurt(player.damageSources().playerAttack(player), 4.0F);
            target.igniteForSeconds(2.0F);
        }
        MinionManager.exhaustSmall(player);
    }
}
