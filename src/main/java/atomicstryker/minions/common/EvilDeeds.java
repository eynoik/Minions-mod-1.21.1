package atomicstryker.minions.common;

import atomicstryker.minions.MinionsMod;
import atomicstryker.minions.registry.MinionsItems;
import atomicstryker.minions.registry.MinionsSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = MinionsMod.MOD_ID)
public final class EvilDeeds {
    private static final String COMMITS_KEY = "minionsEvilCommits";
    private static final int COMMITS_FOR_STAFF = 4;

    public static final List<Deed> DEEDS = List.of(
            new Deed("Spawn Satanist Cult", "spawnsatanistcult", 3),
            new Deed("Hold dark Mess", "gong", 1),
            new Deed("Aid Corporate Supremacy", "corporatesupremacy", 4),
            new Deed("Manipulate Baseball League", "baseballbatjingle", 3),
            new Deed("Destroy Cardboard boxes", "fff", 8),
            new Deed("Play someone off", "keyboardcat", 3),
            new Deed("Unspeakable Act of Lust", "romance", 7),
            new Deed("Join the Republicans", "republicans", 4),
            new Deed("Desecrate a Grave", "graveyard", 8),
            new Deed("Be Professional", "boomheadshot", 2),
            new Deed("Cheat Death", "denied", 1),
            new Deed("Mock Peace Nobel Prize", "boo", 1),
            new Deed("Talk Demontongue", "cornholio", 3),
            new Deed("Taunt the Liberals", "crysomemore", 3),
            new Deed("Spread Sun Tzu Teachings", "evillaugh", 3),
            new Deed("Jam Radio Frequencies", "radios", 3),
            new Deed("Pass Gas in someones Face", "pfft", 2),
            new Deed("Insurance Fraud", "nooo", 3),
            new Deed("Supreme Hedonism", "ohyeah", 8),
            new Deed("Baby Seal Clubbing", "babysealclubbing", 6),
            new Deed("Nelson someone", "haha", 1),
            new Deed("Support Fox News", "march", 5),
            new Deed("Make Dinner", "chicken", 2),
            new Deed("Disturb Burial Ceremony", "laughter2", 3),
            new Deed("Become Pop Smash Star", "superstarme", 5),
            new Deed("Corrupt Pure Nun Thoughts", "teabagnuns", 5),
            new Deed("Mess up the plan", "leeroy", 5)
    );

    private static final List<PendingReward> PENDING_REWARDS = new ArrayList<>();

    private EvilDeeds() {
    }

    public static boolean hasMasterStaff(Player player) {
        return player != null && player.getInventory().contains(new ItemStack(MinionsItems.MASTER_STAFF.get()));
    }

    public static void commit(ServerPlayer player, int deedIndex) {
        // Once the player owns a Master's Staff there is no reason to keep farming
        // evil-deed progress. Keep this enforced server-side as well as hidden in the UI.
        if (hasMasterStaff(player)) {
            return;
        }

        int cost = MinionsConfig.EVIL_DEED_XP_COST.get();
        if (cost < 0) {
            player.displayClientMessage(Component.translatable("message.minions.deeds_disabled"), false);
            return;
        }
        if (deedIndex < 0 || deedIndex >= DEEDS.size()) {
            return;
        }
        if (!player.isCreative() && player.experienceLevel < cost) {
            player.displayClientMessage(Component.translatable("message.minions.need_xp", cost), false);
            return;
        }

        Deed deed = DEEDS.get(deedIndex);
        if (!player.isCreative() && cost > 0) {
            player.giveExperienceLevels(-cost);
        }

        SoundEvent deedSound = MinionsSounds.byName(deed.sound());
        if (deedSound != null) {
            player.level().playSound(null, player.blockPosition(), deedSound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, deed.soundLengthSeconds() * 30, 0));

        int commits = player.getPersistentData().getInt(COMMITS_KEY) + 1;
        boolean rewardStaff = commits >= COMMITS_FOR_STAFF;
        if (rewardStaff) {
            commits = 0;
        }
        player.getPersistentData().putInt(COMMITS_KEY, commits);
        PENDING_REWARDS.add(new PendingReward(player.getUUID(), Math.max(1, deed.soundLengthSeconds() * 20), rewardStaff, commits));
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        if (PENDING_REWARDS.isEmpty()) {
            return;
        }

        for (int i = PENDING_REWARDS.size() - 1; i >= 0; i--) {
            PendingReward pending = PENDING_REWARDS.get(i);
            pending.ticks--;
            if (pending.ticks > 0) {
                continue;
            }

            ServerPlayer player = event.getServer().getPlayerList().getPlayer(pending.playerId);
            if (player != null) {
                if (pending.rewardStaff) {
                    ItemStack staff = new ItemStack(MinionsItems.MASTER_STAFF.get());
                    if (!player.getInventory().add(staff)) {
                        player.drop(staff, false);
                    }
                    player.level().playSound(null, player.blockPosition(), MinionsSounds.GODS_REWARDED.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
                    player.displayClientMessage(Component.translatable("message.minions.staff_reward"), false);
                } else {
                    player.level().playSound(null, player.blockPosition(), MinionsSounds.GODS_PLEASED.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.translatable("message.minions.deed_progress", pending.commitsAfter, COMMITS_FOR_STAFF), false);
                }
            }
            PENDING_REWARDS.remove(i);
        }
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        int commits = event.getOriginal().getPersistentData().getInt(COMMITS_KEY);
        event.getEntity().getPersistentData().putInt(COMMITS_KEY, commits);
    }

    public record Deed(String buttonText, String sound, int soundLengthSeconds) {
    }

    private static final class PendingReward {
        private final UUID playerId;
        private int ticks;
        private final boolean rewardStaff;
        private final int commitsAfter;

        private PendingReward(UUID playerId, int ticks, boolean rewardStaff, int commitsAfter) {
            this.playerId = playerId;
            this.ticks = ticks;
            this.rewardStaff = rewardStaff;
            this.commitsAfter = commitsAfter;
        }
    }
}
