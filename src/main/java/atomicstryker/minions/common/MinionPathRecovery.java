package atomicstryker.minions.common;

import atomicstryker.minions.MinionsMod;
import atomicstryker.minions.common.entity.MinionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Last-resort anti-deadlock recovery for ordinary Minion work queues.
 * If a worker fails to get meaningfully closer to its current target for five
 * seconds, teleport it directly to that target. Surface Fill Air has its own
 * builder recovery because those build orders do not live in MinionEntity's queue.
 */
@EventBusSubscriber(modid = MinionsMod.MOD_ID)
public final class MinionPathRecovery {
    private static final int TELEPORT_AFTER_TICKS = 100;
    private static final Field WORK_QUEUE_FIELD = findWorkQueueField();
    private static final Map<UUID, RecoveryProgress> PROGRESS = new HashMap<>();

    private MinionPathRecovery() {
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        if (WORK_QUEUE_FIELD == null) {
            return;
        }

        Set<UUID> active = new HashSet<>();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            for (MinionEntity minion : MinionManager.getOwned(player)) {
                UUID id = minion.getUUID();

                // Strip mining already owns a stricter tunnel/access recovery
                // system. Do not bypass its phased tunnel logic here.
                if (minion.isStripMining() || minion.queuedWork() <= 0) {
                    PROGRESS.remove(id);
                    continue;
                }

                MinionEntity.WorkOrder order = currentOrder(minion);
                if (order == null) {
                    PROGRESS.remove(id);
                    continue;
                }
                active.add(id);

                BlockPos target = order.pos();
                double distance = minion.distanceToSqr(
                        target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
                double workReachSq = order.action() == MinionEntity.WorkAction.ORE_BREAK ? 20.25D : 9.0D;
                if (distance <= workReachSq) {
                    PROGRESS.put(id, new RecoveryProgress(target, 0, distance));
                    continue;
                }

                RecoveryProgress previous = PROGRESS.get(id);
                if (previous == null || !previous.target.equals(target)) {
                    PROGRESS.put(id, new RecoveryProgress(target, 0, distance));
                    continue;
                }

                boolean madeProgress = distance + 0.25D < previous.bestDistanceSq;
                int stuckTicks = madeProgress ? 0 : previous.stuckTicks + 1;
                double bestDistance = Math.min(previous.bestDistanceSq, distance);

                if (stuckTicks >= TELEPORT_AFTER_TICKS) {
                    minion.getNavigation().stop();
                    // Intentionally unrestricted: this may be inside a wall.
                    // Minions are environmentally invulnerable and can then
                    // immediately break/place their target instead of deadlocking.
                    minion.teleportTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
                    minion.setDeltaMovement(0.0D, 0.0D, 0.0D);
                    PROGRESS.put(id, new RecoveryProgress(target, 0, 0.0D));
                } else {
                    PROGRESS.put(id, new RecoveryProgress(target, stuckTicks, bestDistance));
                }
            }
        }

        PROGRESS.keySet().removeIf(id -> !active.contains(id));
    }

    private static MinionEntity.WorkOrder currentOrder(MinionEntity minion) {
        try {
            Object value = WORK_QUEUE_FIELD.get(minion);
            if (value instanceof ArrayDeque<?> queue && queue.peekFirst() instanceof MinionEntity.WorkOrder order) {
                return order;
            }
        } catch (IllegalAccessException ignored) {
        }
        return null;
    }

    private static Field findWorkQueueField() {
        try {
            Field field = MinionEntity.class.getDeclaredField("workQueue");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private record RecoveryProgress(BlockPos target, int stuckTicks, double bestDistanceSq) {
        private RecoveryProgress {
            target = target.immutable();
        }
    }
}
