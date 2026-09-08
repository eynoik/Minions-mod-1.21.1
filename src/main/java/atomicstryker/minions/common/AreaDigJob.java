package atomicstryker.minions.common;

import atomicstryker.minions.common.entity.MinionEntity;
import atomicstryker.minions.registry.MinionsSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class AreaDigJob {
    private static final int MAX_SPAN = 128;
    private static final long MAX_VOLUME = 131_072L;

    private AreaDigJob() {
    }

    public static Result start(ServerPlayer player, BlockPos firstCorner, BlockPos secondCorner) {
        int minX = Math.min(firstCorner.getX(), secondCorner.getX());
        int minY = Math.min(firstCorner.getY(), secondCorner.getY());
        int minZ = Math.min(firstCorner.getZ(), secondCorner.getZ());
        int maxX = Math.max(firstCorner.getX(), secondCorner.getX());
        int maxY = Math.max(firstCorner.getY(), secondCorner.getY());
        int maxZ = Math.max(firstCorner.getZ(), secondCorner.getZ());

        long sizeX = (long) maxX - minX + 1L;
        long sizeY = (long) maxY - minY + 1L;
        long sizeZ = (long) maxZ - minZ + 1L;
        long volume = sizeX * sizeY * sizeZ;
        if (sizeX > MAX_SPAN || sizeY > MAX_SPAN || sizeZ > MAX_SPAN || volume > MAX_VOLUME) {
            return Result.TOO_LARGE;
        }

        List<MinionEntity> minions = MinionManager.getOwned(player);
        if (minions.isEmpty()) {
            return Result.NO_MINIONS;
        }

        ServerLevel level = player.serverLevel();
        List<AreaBlock> work = new ArrayList<>();
        for (int y = maxY; y >= minY; y--) {
            int phase = (maxY - y) + 1;
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) {
                        continue;
                    }
                    work.add(new AreaBlock(pos, phase));
                }
            }
        }

        if (work.isEmpty()) {
            return Result.EMPTY;
        }

        for (MinionEntity minion : minions) {
            minion.clearWork();
            minion.setFollowing(false);
            minion.clearMoveTarget();
        }

        int workerIndex = 0;
        for (AreaBlock order : work) {
            MinionEntity worker = minions.get(workerIndex++ % minions.size());
            worker.enqueueWork(order.pos, order.phase);
        }

        level.playSound(null, player.blockPosition(), MinionsSounds.RANDOM_ORDER.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        MinionManager.exhaustBig(player);
        return Result.STARTED;
    }

    public enum Result {
        STARTED,
        TOO_LARGE,
        EMPTY,
        NO_MINIONS
    }

    private record AreaBlock(BlockPos pos, int phase) {
        private AreaBlock {
            pos = pos.immutable();
        }
    }
}
