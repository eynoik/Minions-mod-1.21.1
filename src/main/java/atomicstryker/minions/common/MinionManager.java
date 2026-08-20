package atomicstryker.minions.common;

import atomicstryker.minions.MinionsMod;
import atomicstryker.minions.common.entity.MinionEntity;
import atomicstryker.minions.registry.MinionsEntities;
import atomicstryker.minions.registry.MinionsSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = MinionsMod.MOD_ID)
public final class MinionManager {
    private static final Set<MinionEntity> LOADED_MINIONS = ConcurrentHashMap.newKeySet();

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
            level.playSound(null, target, MinionsSounds.MINION_SPAWN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
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
        playOrder(player, MinionsSounds.RANDOM_ORDER.get());
    }

    public static void follow(ServerPlayer player) {
        for (MinionEntity minion : getOwned(player)) {
            minion.clearWork();
            minion.clearMoveTarget();
            minion.setFollowing(true);
        }
        playOrder(player, MinionsSounds.ORDER_FOLLOW_PLAYER.get());
    }

    public static void stopWork(ServerPlayer player) {
        for (MinionEntity minion : getOwned(player)) {
            minion.clearWork();
            minion.clearMoveTarget();
            minion.setFollowing(false);
        }
        playOrder(player, MinionsSounds.RANDOM_ORDER.get());
    }

    /**
     * Restores the old owner-hit work boost as a staff action: all currently
     * loaded Minions owned by the player work at 2x speed for 30 seconds.
     */
    public static boolean boostWork(ServerPlayer player) {
        List<MinionEntity> minions = getOwned(player);
        if (minions.isEmpty()) {
            return false;
        }
        for (MinionEntity minion : minions) {
            minion.applyWorkBoost(20 * 30);
        }
        player.serverLevel().playSound(
                null, player.blockPosition(), MinionsSounds.BOLT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    public static void assignReturnContainer(ServerPlayer player, BlockPos target, boolean preserveCurrentJob) {
        if (!(player.serverLevel().getBlockEntity(target) instanceof net.minecraft.world.Container)) {
            return;
        }
        for (MinionEntity minion : getOwned(player)) {
            if (!preserveCurrentJob) {
                minion.clearWork();
                minion.clearMoveTarget();
                minion.setFollowing(false);
            }
            minion.setReturnContainer(target, !preserveCurrentJob);
        }
        playOrder(player, MinionsSounds.RANDOM_ORDER.get());
    }

    public static boolean canWorkPhase(UUID owner, int phase) {
        if (owner == null || phase <= 0) {
            return true;
        }
        LOADED_MINIONS.removeIf(entity -> entity.isRemoved() || !entity.isAlive());
        for (MinionEntity minion : LOADED_MINIONS) {
            if (!owner.equals(minion.getOwnerUUID())) {
                continue;
            }
            int lowest = minion.lowestQueuedPhase();
            if (lowest >= 0 && lowest < phase) {
                return false;
            }
        }
        return true;
    }

    public static boolean pickup(ServerPlayer player, LivingEntity target) {
        boolean accepted = getOwned(player).stream()
                .filter(minion -> !minion.isVehicle())
                .min(Comparator.comparingDouble(minion -> minion.distanceToSqr(target)))
                .map(minion -> {
                    minion.setCarryTarget(target);
                    return true;
                })
                .orElse(false);
        if (accepted) {
            playOrder(player, MinionsSounds.GRAB_ANIMAL_ORDER.get());
        }
        return accepted;
    }

    public static void unsummon(ServerPlayer player) {
        for (MinionEntity minion : new ArrayList<>(getOwned(player))) {
            minion.dropStoredItems();
            minion.releaseChunkLoading();
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

        List<List<BlockPos>> trees = scanNearbyTrees(level, start);
        assignTreeWork(player, trees);
        if (!trees.isEmpty()) {
            playOrder(player, MinionsSounds.ORDER_TREE_CUTTING.get());
            exhaustBig(player);
            return true;
        }
        return false;
    }

    /**
     * Legacy TreeScanner parity: one clicked tree starts a local harvest job which
     * searches outward for up to sixteen trees, out to sixty-four blocks.
     */
    private static List<List<BlockPos>> scanNearbyTrees(ServerLevel level, BlockPos start) {
        final int searchRadius = 64;
        final int maxTrees = 16;
        final int maxBlocksPerTree = Math.max(64, MinionsConfig.MAX_TREE_BLOCKS.get());
        List<List<BlockPos>> trees = new ArrayList<>();
        Set<BlockPos> claimedLogs = new HashSet<>();

        BlockPos clickedBase = start.immutable();
        while (clickedBase.getY() > level.getMinBuildHeight() && level.getBlockState(clickedBase.below()).is(BlockTags.LOGS)) {
            clickedBase = clickedBase.below();
        }
        tryAddTree(level, clickedBase, maxBlocksPerTree, trees, claimedLogs, maxTrees);

        int centerX = start.getX();
        int centerZ = start.getZ();
        for (int radius = 1; radius <= searchRadius && trees.size() < maxTrees; radius++) {
            for (int dx = -radius; dx <= radius && trees.size() < maxTrees; dx++) {
                tryAddTreeColumn(level, centerX + dx, centerZ - radius, maxBlocksPerTree, trees, claimedLogs, maxTrees);
                tryAddTreeColumn(level, centerX + dx, centerZ + radius, maxBlocksPerTree, trees, claimedLogs, maxTrees);
            }
            for (int dz = -radius + 1; dz <= radius - 1 && trees.size() < maxTrees; dz++) {
                tryAddTreeColumn(level, centerX - radius, centerZ + dz, maxBlocksPerTree, trees, claimedLogs, maxTrees);
                tryAddTreeColumn(level, centerX + radius, centerZ + dz, maxBlocksPerTree, trees, claimedLogs, maxTrees);
            }
        }
        return trees;
    }

    private static void tryAddTreeColumn(ServerLevel level, int x, int z, int maxBlocksPerTree,
                                         List<List<BlockPos>> trees, Set<BlockPos> claimedLogs, int maxTrees) {
        if (trees.size() >= maxTrees) {
            return;
        }
        int topY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int bottomY = Math.max(level.getMinBuildHeight(), topY - 48);
        for (int y = topY; y >= bottomY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).is(BlockTags.LOGS)) {
                continue;
            }
            while (pos.getY() > level.getMinBuildHeight() && level.getBlockState(pos.below()).is(BlockTags.LOGS)) {
                pos = pos.below();
            }
            tryAddTree(level, pos, maxBlocksPerTree, trees, claimedLogs, maxTrees);
            return;
        }
    }

    private static void tryAddTree(ServerLevel level, BlockPos base, int maxBlocksPerTree,
                                   List<List<BlockPos>> trees, Set<BlockPos> claimedLogs, int maxTrees) {
        if (trees.size() >= maxTrees || claimedLogs.contains(base) || !level.getBlockState(base).is(BlockTags.LOGS)) {
            return;
        }
        List<BlockPos> tree = collectTreeLogs(level, base, maxBlocksPerTree);
        if (tree.size() <= 3) {
            return;
        }
        claimedLogs.addAll(tree);
        tree.sort(Comparator.comparingInt(BlockPos::getY));
        trees.add(tree);
    }

    public static List<BlockPos> collectTreeLogs(ServerLevel level, BlockPos base, int limit) {
        Queue<BlockPos> open = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> logs = new ArrayList<>();
        open.add(base.immutable());

        while (!open.isEmpty() && logs.size() < limit) {
            BlockPos pos = open.remove();
            if (!seen.add(pos) || !level.getBlockState(pos).is(BlockTags.LOGS)) {
                continue;
            }
            logs.add(pos);

            // Match the legacy scanner: same layer plus one layer upward in a 3x3 footprint.
            // This reaches diagonal branches and wide/modded trunks the beta.2 six-way flood missed.
            for (int dy = 0; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos next = pos.offset(dx, dy, dz);
                        if (!seen.contains(next)) {
                            open.add(next.immutable());
                        }
                    }
                }
            }
        }
        return logs;
    }

    /**
     * Finds the leaf canopy belonging to a scanned tree. Leaves are flood-filled
     * from the target logs up to vanilla's normal six-block leaf-distance range.
     * A leaf directly touching a foreign log is not claimed, which avoids eating
     * an adjacent tree trunk/canopy in dense forests as much as practical.
     */
    public static List<BlockPos> collectTreeLeaves(ServerLevel level, List<BlockPos> treeLogs) {
        if (treeLogs.isEmpty()) {
            return List.of();
        }

        Set<BlockPos> targetLogs = new HashSet<>(treeLogs);
        Queue<BlockPos> open = new ArrayDeque<>();
        Map<BlockPos, Integer> distance = new HashMap<>();

        for (BlockPos log : treeLogs) {
            for (Direction direction : Direction.values()) {
                BlockPos candidate = log.relative(direction).immutable();
                if (level.getBlockState(candidate).is(BlockTags.LEAVES) && !distance.containsKey(candidate)) {
                    distance.put(candidate, 1);
                    open.add(candidate);
                }
            }
        }

        List<BlockPos> leaves = new ArrayList<>();
        while (!open.isEmpty()) {
            BlockPos leaf = open.remove();
            int currentDistance = distance.getOrDefault(leaf, 7);
            if (!level.getBlockState(leaf).is(BlockTags.LEAVES)) {
                continue;
            }

            boolean touchesForeignLog = false;
            for (Direction direction : Direction.values()) {
                BlockPos neighbour = leaf.relative(direction);
                if (level.getBlockState(neighbour).is(BlockTags.LOGS) && !targetLogs.contains(neighbour)) {
                    touchesForeignLog = true;
                    break;
                }
            }
            if (touchesForeignLog) {
                continue;
            }

            leaves.add(leaf);
            if (currentDistance >= 6) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = leaf.relative(direction).immutable();
                if (!distance.containsKey(next) && level.getBlockState(next).is(BlockTags.LEAVES)) {
                    distance.put(next, currentDistance + 1);
                    open.add(next);
                }
            }
        }
        return leaves;
    }

    private static void assignTreeWork(ServerPlayer player, List<List<BlockPos>> trees) {
        List<MinionEntity> minions = getOwned(player);
        if (minions.isEmpty()) {
            return;
        }
        for (MinionEntity minion : minions) {
            minion.clearWork();
            minion.setFollowing(false);
            minion.clearMoveTarget();
        }

        int workerIndex = 0;
        for (List<BlockPos> tree : trees) {
            if (tree.isEmpty()) {
                continue;
            }
            MinionEntity worker = minions.get(workerIndex++ % minions.size());
            // Legacy BlockTask_TreeChop was one task for an entire scanned tree.
            // Queue only the lowest trunk block; the Minion rescans the intact
            // tree when it starts the job and removes the whole tree at finish.
            worker.enqueueTreeWork(tree.get(0));
        }
    }

    public static boolean mineVein(ServerPlayer player, BlockPos start) {
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(start);
        if (state.isAir() || state.getDestroySpeed(level, start) < 0.0F || state.is(BlockTags.LOGS)) {
            return false;
        }
        List<BlockPos> work = floodMatching(level, start, MinionsConfig.MAX_VEIN_BLOCKS.get(), false);
        assignBreakWork(player, work);
        if (!work.isEmpty()) {
            playOrder(player, MinionsSounds.RANDOM_ORDER.get());
            exhaustBig(player);
            return true;
        }
        return false;
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
            if (!matches || state.getDestroySpeed(level, pos) < 0.0F) {
                continue;
            }
            result.add(pos);
            for (Direction direction : Direction.values()) {
                open.add(pos.relative(direction).immutable());
            }
        }
        return result;
    }

    public static void digCustom(ServerPlayer player, BlockPos start, int requestedSize, int requestedHeight) {
        int size = clampOdd(requestedSize, 3, 71);
        int height = Math.max(3, Math.min(25, requestedHeight));
        Direction forward = directionFromPlayerToTarget(player, start);
        int half = (size - 1) / 2;

        int minX;
        int minZ;
        if (forward.getAxis() == Direction.Axis.X) {
            minX = forward == Direction.EAST ? start.getX() : start.getX() - (size - 1);
            minZ = start.getZ() - half;
        } else {
            minX = start.getX() - half;
            minZ = forward == Direction.SOUTH ? start.getZ() : start.getZ() - (size - 1);
        }
        int maxX = minX + size - 1;
        int maxZ = minZ + size - 1;
        int minY = start.getY();
        int maxY = start.getY() + height - 1;

        List<QueuedWork> work = new ArrayList<>();
        // Match the old job: clear the three entrance blocks first, then index top-down.
        work.add(QueuedWork.breakBlock(start, 0));
        work.add(QueuedWork.breakBlock(start.above(), 0));
        work.add(QueuedWork.breakBlock(start.above(2), 0));
        for (int y = maxY; y >= minY; y--) {
            int phase = (maxY - y) + 1;
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!player.serverLevel().getBlockState(pos).isAir()) {
                        work.add(QueuedWork.breakBlock(pos, phase));
                    }
                }
            }
        }

        assignOrders(player, work);
        if (!work.isEmpty()) {
            playOrder(player, MinionsSounds.RANDOM_ORDER.get());
            exhaustBig(player);
        }
    }

    public static void digStairwell(ServerPlayer player, BlockPos start) {
        ServerLevel level = player.serverLevel();
        List<QueuedWork> work = new ArrayList<>();
        int startY = start.getY();
        int currentSegment = 0;
        int minY = level.getMinBuildHeight() + 8;

        for (int depth = 0; startY - depth > minY; depth++) {
            if (depth % 3 == 0) {
                currentSegment++;
                if (currentSegment == 5) {
                    currentSegment = 1;
                }
            }

            int y = startY - depth;
            for (int xDiff = 0; xDiff <= 4; xDiff++) {
                for (int zDiff = 0; zDiff <= 4; zDiff++) {
                    BlockPos pos = start.offset(xDiff, -depth, zDiff);
                    if (isStairwellCorner(depth, currentSegment, xDiff, zDiff)) {
                        work.add(QueuedWork.placeCobble(pos, depth + 1));
                    } else {
                        Direction stairFacing = stairFacing(depth, currentSegment, xDiff, zDiff);
                        if (stairFacing != null) {
                            work.add(QueuedWork.placeStair(pos, stairFacing, depth + 1));
                        } else if (!level.getBlockState(pos).isAir()) {
                            work.add(QueuedWork.breakBlock(pos, depth + 1));
                        }
                    }
                }
            }
        }

        assignOrders(player, work);
        if (!work.isEmpty()) {
            playOrder(player, MinionsSounds.ORDER_MINESHAFT.get());
            exhaustBig(player);
        }
    }

    public static void stripMine(ServerPlayer player, BlockPos start) {
        List<MinionEntity> minions = getOwned(player);
        MinionEntity worker = minions.stream().filter(minion -> !minion.isStripMining()).findFirst().orElse(null);
        if (worker == null) {
            return;
        }

        ServerLevel level = player.serverLevel();
        Direction forward = directionFromPlayerToTarget(player, start);
        Direction side = forward.getClockWise();
        int length = MinionsConfig.STRIP_MINE_LENGTH.get();
        Set<BlockPos> queued = new HashSet<>();

        worker.clearWork();
        worker.setFollowing(false);
        worker.clearMoveTarget();
        worker.setStripMining(true);

        for (int step = 0; step < length; step++) {
            int phase = -1; // independent single-Minion strip queue; do not join shared layer barriers
            BlockPos base = start.relative(forward, step);
            queueBreak(worker, queued, base, phase);
            queueBreak(worker, queued, base.above(), phase);

            if (level.getBlockState(base.below()).isAir()) {
                worker.enqueuePlaceDirt(base.below(), phase);
            }

            scanValuableVein(level, worker, queued, base.above(2), phase);
            scanValuableVein(level, worker, queued, base.relative(side), phase);
            scanValuableVein(level, worker, queued, base.relative(side.getOpposite()), phase);
            scanValuableVein(level, worker, queued, base.above().relative(side), phase);
            scanValuableVein(level, worker, queued, base.above().relative(side.getOpposite()), phase);
            scanValuableVein(level, worker, queued, base.below(), phase);

            if (step > 0 && step % 7 == 0) {
                worker.enqueuePlaceTorch(base.relative(forward.getOpposite(), 2), phase);
            }
        }

        playOrder(player, MinionsSounds.RANDOM_ORDER.get());
        exhaustBig(player);
    }

    private static void scanValuableVein(ServerLevel level, MinionEntity worker, Set<BlockPos> queued, BlockPos start, int phase) {
        if (!isValuable(level, start)) {
            return;
        }
        for (BlockPos pos : floodMatching(level, start, Math.min(64, MinionsConfig.MAX_VEIN_BLOCKS.get()), false)) {
            queueOreBreak(worker, queued, pos, phase);
        }
    }

    private static void queueBreak(MinionEntity worker, Set<BlockPos> queued, BlockPos pos, int phase) {
        if (queued.add(pos.immutable())) {
            worker.enqueueWork(pos, phase);
        }
    }

    private static void queueOreBreak(MinionEntity worker, Set<BlockPos> queued, BlockPos pos, int phase) {
        if (queued.add(pos.immutable())) {
            worker.enqueueOreWork(pos, phase);
        }
    }
    private static boolean isValuable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir()
                && state.getDestroySpeed(level, pos) >= 0.0F
                && state.is(Tags.Blocks.ORES);
    }

    private static boolean isStairwellCorner(int depth, int segment, int xDiff, int zDiff) {
        if (depth % 3 != 0) {
            return false;
        }
        return switch (segment) {
            case 1 -> xDiff == 0 && zDiff == 0;
            case 2 -> xDiff == 4 && zDiff == 0;
            case 3 -> xDiff == 4 && zDiff == 4;
            case 4 -> xDiff == 0 && zDiff == 4;
            default -> false;
        };
    }

    private static Direction stairFacing(int depth, int segment, int xDiff, int zDiff) {
        int mod = Math.floorMod(depth, 4);
        boolean stair = switch (segment) {
            case 1 -> xDiff - 1 == mod && zDiff == 0;
            case 2 -> xDiff == 4 && ((zDiff == 1 && mod == 3) || (zDiff == 2 && mod == 0) || (zDiff == 3 && mod == 1));
            case 3 -> zDiff == 4 && ((xDiff == 3 && mod == 2) || (xDiff == 2 && mod == 3) || (xDiff == 1 && mod == 0));
            case 4 -> xDiff == 0 && areModsCounterPosed(zDiff, depth);
            default -> false;
        };
        if (!stair) {
            return null;
        }
        return switch (segment) {
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            case 3 -> Direction.EAST;
            default -> Direction.SOUTH;
        };
    }

    private static boolean areModsCounterPosed(int i, int j) {
        return switch (Math.floorMod(i, 4)) {
            case 1 -> Math.floorMod(j, 4) == 3;
            case 2 -> Math.floorMod(j, 4) == 2;
            case 3 -> Math.floorMod(j, 4) == 1;
            default -> false;
        };
    }

    private static void assignBreakWork(ServerPlayer player, List<BlockPos> positions) {
        List<QueuedWork> orders = positions.stream().map(QueuedWork::breakBlock).toList();
        assignOrders(player, orders);
    }

    private static void assignOrders(ServerPlayer player, List<QueuedWork> orders) {
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
        for (QueuedWork order : orders) {
            MinionEntity worker = minions.get(index++ % minions.size());
            switch (order.action) {
                case BREAK -> worker.enqueueWork(order.pos, order.phase);
                case COBBLE -> worker.enqueuePlaceCobble(order.pos, order.phase);
                case DIRT -> worker.enqueuePlaceDirt(order.pos, order.phase);
                case STAIR -> worker.enqueuePlaceStair(order.pos, order.facing, order.phase);
                case TORCH -> worker.enqueuePlaceTorch(order.pos, order.phase);
            }
        }
    }

    public static Direction directionFromPlayerToTarget(ServerPlayer player, BlockPos start) {
        int bossX = player.blockPosition().getX();
        int bossZ = player.blockPosition().getZ();
        int dx = start.getX() - bossX;
        int dz = start.getZ() - bossZ;
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static void playOrder(ServerPlayer player, SoundEvent sound) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static int clampOdd(int value, int min, int max) {
        int result = Math.max(min, Math.min(max, value));
        if ((result & 1) == 0) {
            result = Math.min(max, result + 1);
        }
        return result;
    }

    private enum OrderAction {
        BREAK,
        COBBLE,
        DIRT,
        STAIR,
        TORCH
    }

    private record QueuedWork(BlockPos pos, OrderAction action, Direction facing, int phase) {
        private static QueuedWork breakBlock(BlockPos pos) {
            return breakBlock(pos, 0);
        }

        private static QueuedWork breakBlock(BlockPos pos, int phase) {
            return new QueuedWork(pos.immutable(), OrderAction.BREAK, null, phase);
        }

        private static QueuedWork placeCobble(BlockPos pos, int phase) {
            return new QueuedWork(pos.immutable(), OrderAction.COBBLE, null, phase);
        }

        private static QueuedWork placeStair(BlockPos pos, Direction facing, int phase) {
            return new QueuedWork(pos.immutable(), OrderAction.STAIR, facing, phase);
        }
    }
}
