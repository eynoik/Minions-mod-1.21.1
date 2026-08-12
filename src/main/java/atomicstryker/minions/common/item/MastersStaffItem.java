package atomicstryker.minions.common.item;

import atomicstryker.minions.common.MinionManager;
import atomicstryker.minions.common.entity.MinionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class MastersStaffItem extends Item {
    private static final int HOLD_TICKS = 20;

    public MastersStaffItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        if (target instanceof MinionEntity minion && serverPlayer.getUUID().equals(minion.getOwnerUUID())) {
            minion.dropPassengerAndItems();
            serverPlayer.displayClientMessage(Component.translatable("message.minions.drop"), true);
            return InteractionResult.SUCCESS;
        }

        if (MinionManager.pickup(serverPlayer, target)) {
            serverPlayer.displayClientMessage(Component.translatable("message.minions.pickup"), true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (level.isClientSide || !(living instanceof ServerPlayer player)) {
            return;
        }

        int usedTicks = getUseDuration(stack, living) - timeLeft;
        if (usedTicks >= HOLD_TICKS) {
            if (player.isShiftKeyDown()) {
                MinionManager.unsummon(player);
                player.displayClientMessage(Component.translatable("message.minions.unsummoned"), true);
            } else {
                MinionManager.follow(player);
                player.displayClientMessage(Component.translatable("message.minions.follow"), true);
            }
            return;
        }

        HitResult hit = player.pick(30.0D, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            return;
        }

        BlockPos clicked = blockHit.getBlockPos();
        BlockEntity blockEntity = level.getBlockEntity(clicked);
        if (blockEntity instanceof Container && MinionManager.hasMinions(player)) {
            for (MinionEntity minion : MinionManager.getOwned(player)) {
                minion.setReturnContainer(clicked);
            }
            player.displayClientMessage(Component.translatable("message.minions.chest"), true);
            return;
        }

        if (!MinionManager.canUsePower(player)) {
            player.displayClientMessage(Component.translatable("message.minions.no_willpower"), true);
            return;
        }

        if (level.getBlockState(clicked).is(BlockTags.LOGS) && MinionManager.hasMinions(player)) {
            if (MinionManager.chopTree(player, clicked)) {
                player.displayClientMessage(Component.translatable("message.minions.chop"), true);
            }
            return;
        }

        if (player.isShiftKeyDown() && MinionManager.hasMinions(player)) {
            if (MinionManager.mineVein(player, clicked)) {
                player.displayClientMessage(Component.translatable("message.minions.mine"), true);
            }
            return;
        }

        BlockPos target = clicked.relative(blockHit.getDirection());
        boolean spawned = MinionManager.spawnOrMove(player, target);
        MinionManager.exhaustBig(player);
        player.displayClientMessage(Component.translatable(spawned ? "message.minions.summon" : "message.minions.move"), true);
    }
}
