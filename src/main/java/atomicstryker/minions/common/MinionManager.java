package atomicstryker.minions.common;

import atomicstryker.minions.MinionsMod;
import atomicstryker.minions.common.entity.MinionEntity;
import atomicstryker.minions.registry.MinionsEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = MinionsMod.MOD_ID)
public final class MinionManager {
    private static final Set<MinionEntity> LOADED_MINIONS = ConcurrentHashMap.newKeySet();
    private static final Direction[] CARDINALS = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private MinionManager() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof MinionEntity minion) {
            LOADED_MINIONS.add(minion);
        }
    }

    public static List<MinionEntity> getOwned(ServerPlayer player) {
        UUID owner = player.getUUID();
        LOADED_MINIONS.removeIf(entity -> entity.isRemoved() || !entity.isAlive());
        return LOADED_MINIONS.stream()
                .filter(entity -> owner.equals(entity.getOwnerUUID()))
                .sorted(Comparator.comparingInt(MinionEntity::getId))
                .toList();
    }

    public static boolean hasAllMinions(ServerPlayer player) {
        return getOwned(player).size() >= MinionsConfig.MINIONS_PER_PLAYER.get();
    }

    public static boolean hasMinions(ServerPlayer player) {
        return !getOwned(player).isEmpty();
    }

    public static boolean spawnOrMove(ServerPlayer player, BlockPos target) {
        ServerLevel level = player.serverLevel();
        List<MinionEntity> owned = getOwned(player);
        if (owned.size() < MinionsConfig.MINIONS_PER_PLAYER.get()) {
            MinionEntity minion = MinionsEntities.MINION.get().create(level);
            if (minion == null) {
                return false;
            }
            minion.setOwner(player);
            minion.moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, player.getYRot(), 0.0F);
            minion.setMoveTarget(target);
            level.addFreshEntity(minion);
            LOADED_MINIONS.add(minion);
            level.levelEvent(2004, target, 0);
            return true;
        }

        moveTo(player, target);
        return false;
    }

    public static void moveTo(ServerPlayer player, BlockPos target) {
        for (MinionEntity minion : getOwned(player)) {
            minion.clearWork();
            minion.setFollowing(false);
            minion.setMoveTarget(target);
        }
    }

    public static void follow(ServerPlayer player) {
        for (MinionEntity minion : getOwned(player)) {
            minion.clearWork();
            minion.clearMoveTarget();
            minion.setFollowing(true);
        }
    }

    public static boolean pickup(ServerPlayer player, LivingEntity target) {
        return getOwned(player).stream()
                .filter(minion -> !minion.isVehicle())
                .min(Comparator.comparingDouble(minion -> minion.distanceToSqr(target)))
                .map(minion -> {
                    minion.setCarryTarget(target);
                    return true;
                })
                .orElse(false);
    }

    public static void unsummon(ServerPlayer player) {
        for (MinionEntity minion : new ArrayList<>(getOwned(player))) {
            minion.dropStoredItems();
            minion.discard();
            LOADED_MINIONS.remove(minion);
        }
    }

    public static boolean canUsePower(ServerPlayer player) {
        return player.isCreative() || player.getFoodData().getFoodLevel() > 3;
    }

    public static void exhaustBig(ServerPlayer player) {
        if (!player.isCreative()) {
            player.causeFoodExhaustion(MinionsConfig.BIG_EXHAUSTION.get().floatValue());
        }
    }

    public static void exhaustSmall(ServerPlayer player) {
        if (!player.isCreative()) {
            player.causeFoodExhaustion(MinionsConfig.SMALL_EXHAUSTION.get().floatValue());
        }
    }

    public static boolean chopTree(ServerPlayer player, BlockPos start) {
        ServerLevel level = player.serverLevel();
        if (!level.getBlockState(start).is(BlockTags.LOGS)) {
            return false;
        }
        List<BlockPos> work = floodMatching(level, start, MinionsConfig.MAX_TREE_BLOCKS.get(), true);
        assignWork(player, work);
        exhaustBig(player);
        return !work.isEmpty();
    }

    public static boolean mineVein(ServerPlayer player, BlockPos start) {
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(start);
        if (state.isAir() || state.getDestroySpeed(level, start) < 0.0F || state.is(BlockTags.LOGS)) {
            return false;
        }
        List<BlockPos> work = floodMatching(level, start, MinionsConfig.MAX_VEIN_BLOCKS.get(), false);
        assignWork(player, work);
        exhaustBig(player);
        return !work.isEmpty();
    }

    private static List<BlockPos> floodMatching(ServerLevel level, BlockPos start, int limit, boolean logs) {
        BlockState initial = level.getBlockState(start);
        Queue<BlockPos> open = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> result = new ArrayList<>();
        open.add(start.immutable());

        while (!open.isEmpty() && result.size() < limit) {
            BlockPos pos = open.remove();
            if (!seen.add(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            boolean matches = logs ? state.is(BlockTags.LOGS) : state.is(initial.getBlock());
            if (!matches) {
                continue;
            }
            if (state.getDestroySpeed(level, pos) < 0.0F) {
                continue;
            }
            result.add(pos);
            for (Direction direction : Direction.values()) {
                open.add(pos.relative(direction).immutable());
            }
        }
        return result;
    }

    public static void digCustom(ServerPlayer player, BlockPos center) {
        int size = MinionsConfig.CUSTOM_DIG_SIZE.get();
        int height = MinionsConfig.CUSTOM_DIG_HEIGHT.get();
        int half = size / 2;
        List<BlockPos> work = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = -half; x <= half; x++) {
                for (int z = -half; z <= half; z++) {
                    work.add(center.offset(x, y, z));
                }
            }
        }
        assignWork(player, work);
        exhaustBig(player);
    }

    public static void digStairwell(ServerPlayer player, BlockPos start) {
        Direction forward = player.getDirection();
        Direction side = forward.getClockWise();
        List<BlockPos> work = new ArrayList<>();
        for (int step = 0; step < 32; step++) {
            int down = step / 4;
            BlockPos base = start.relative(forward, step).below(down);
            for (int width = -1; width <= 2; width++) {
                for (int y = 0; y < 3; y++) {
                    work.add(base.relative(side, width).above(y));
                }
            }
        }
        assignWork(player, work);
        exhaustBig(player);
    }

    public static void stripMine(ServerPlayer player, BlockPos start) {
        Direction forward = player.getDirection();
        Direction side = forward.getClockWise();
        List<BlockPos> work = new ArrayList<>();
        int length = MinionsConfig.STRIP_MINE_LENGTH.get();
        for (int step = 0; step < length; step++) {
            BlockPos base = start.relative(forward, step);
            for (int width = -1; width <= 1; width++) {
                for (int y = 0; y < 2; y++) {
                    work.add(base.relative(side, width).above(y));
                }
            }
        }
        assignWork(player, work);
        exhaustBig(player);
    }

    private static void assignWork(ServerPlayer player, List<BlockPos> positions) {
        List<MinionEntity> minions = getOwned(player);
        if (minions.isEmpty()) {
            return;
        }
        for (MinionEntity minion : minions) {
            minion.clearWork();
            minion.setFollowing(false);
            minion.clearMoveTarget();
        }

        int index = 0;
        for (BlockPos pos : positions) {
            MinionEntity worker = minions.get(index++ % minions.size());
            worker.enqueueWork(pos);
        }
    }

    public static Direction nearestCardinal(float yaw) {
        return CARDINALS[Math.floorMod(Math.round(yaw / 90.0F), CARDINALS.length)];
    }
}
