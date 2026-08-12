package atomicstryker.minions.common;

import atomicstryker.minions.MinionsMod;
import atomicstryker.minions.registry.MinionsItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = MinionsMod.MOD_ID)
public final class EvilDeeds {
    private static final String COMMITS_KEY = "minionsEvilCommits";
    private static final int COMMITS_FOR_STAFF = 4;

    private EvilDeeds() {
    }

    public static void commit(ServerPlayer player) {
        int cost = MinionsConfig.EVIL_DEED_XP_COST.get();
        if (cost < 0) {
            player.displayClientMessage(Component.translatable("message.minions.deeds_disabled"), false);
            return;
        }
        if (!player.isCreative() && player.experienceLevel < cost) {
            player.displayClientMessage(Component.translatable("message.minions.need_xp", cost), false);
            return;
        }

        if (!player.isCreative() && cost > 0) {
            player.giveExperienceLevels(-cost);
        }

        int commits = player.getPersistentData().getInt(COMMITS_KEY) + 1;
        if (commits >= COMMITS_FOR_STAFF) {
            commits = 0;
            ItemStack staff = new ItemStack(MinionsItems.MASTER_STAFF.get());
            if (!player.getInventory().add(staff)) {
                player.drop(staff, false);
            }
            player.level().playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.8F);
            player.displayClientMessage(Component.translatable("message.minions.staff_reward"), false);
        } else {
            player.level().playSound(null, player.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 0.75F + commits * 0.08F);
            player.displayClientMessage(Component.translatable("message.minions.deed_progress", commits, COMMITS_FOR_STAFF), false);
        }
        player.getPersistentData().putInt(COMMITS_KEY, commits);
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        int commits = event.getOriginal().getPersistentData().getInt(COMMITS_KEY);
        event.getEntity().getPersistentData().putInt(COMMITS_KEY, commits);
    }
}
