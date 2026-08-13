from pathlib import Path

entity = Path('src/main/java/atomicstryker/minions/common/entity/MinionEntity.java')
s = entity.read_text()

old = '''        BlockPos navigationTarget = order.action() == WorkAction.TREE_BREAK
                ? findTreeWorkPosition(level, target)
                : target;
        boolean navigating = navigation.moveTo(
                navigationTarget.getX() + 0.5D, navigationTarget.getY(), navigationTarget.getZ() + 0.5D, 1.15D);
        if (!navigating) {
            stuckTicks++;
            if (stuckTicks > 80) {
                BlockPos safe = order.action() == WorkAction.TREE_BREAK
                        ? findTreeWorkPosition(level, target)
                        : findSafeTeleport(level, target);
                teleportTo(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
                stuckTicks = 0;
            }
        } else {
            stuckTicks = Math.max(0, stuckTicks - 1);
        }
'''

new = '''        BlockPos navigationTarget;
        if (order.action() == WorkAction.TREE_BREAK) {
            navigationTarget = findTreeWorkPosition(level, target);
        } else if (stripMining) {
            // Never ask vanilla navigation to path into the still-solid block.
            // Pick a two-block-tall air position in the already opened tunnel
            // that remains inside the normal three-block work reach instead.
            navigationTarget = findStripMineWorkPosition(level, target);
        } else {
            navigationTarget = target;
        }

        if (stripMining && Math.abs(getY() - navigationTarget.getY()) > 3.0D) {
            // Strip miners must not escape vertically to the surface while the
            // navigator searches for a route. Snap them back to the tunnel-side
            // access position rather than letting the old upward recovery loop run.
            teleportTo(navigationTarget.getX() + 0.5D, navigationTarget.getY(), navigationTarget.getZ() + 0.5D);
            stuckTicks = 0;
            return;
        }

        boolean navigating = navigation.moveTo(
                navigationTarget.getX() + 0.5D, navigationTarget.getY(), navigationTarget.getZ() + 0.5D, 1.15D);
        if (!navigating) {
            stuckTicks++;
            int recoveryTicks = stripMining ? 40 : 80;
            if (stuckTicks > recoveryTicks) {
                BlockPos safe;
                if (order.action() == WorkAction.TREE_BREAK) {
                    safe = findTreeWorkPosition(level, target);
                } else if (stripMining) {
                    safe = findStripMineWorkPosition(level, target);
                } else {
                    safe = findSafeTeleport(level, target);
                }
                teleportTo(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
                stuckTicks = 0;
            }
        } else {
            stuckTicks = Math.max(0, stuckTicks - 1);
        }
'''

if old not in s:
    raise SystemExit('tickWork navigation block not found')
s = s.replace(old, new, 1)

marker = '''    private BlockPos findSafeTeleport(ServerLevel level, BlockPos around) {
        for (int dy = 1; dy <= 4; dy++) {
            BlockPos candidate = around.above(dy);
            if (level.getBlockState(candidate).isAir() && level.getBlockState(candidate.above()).isAir()) {
                return candidate;
            }
        }
        return around.above();
    }
'''

helper = '''    private BlockPos findStripMineWorkPosition(ServerLevel level, BlockPos target) {
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;

        // Search only inside normal Minion work reach. Standard strip segments
        // resolve to the open 1x2 tunnel cell directly behind the wall; ore
        // tasks can also use an already-open side pocket without climbing away.
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (dx * dx + dy * dy + dz * dz > 9) {
                        continue;
                    }
                    BlockPos candidate = target.offset(dx, dy, dz);
                    if (!isStripMineStandingPosition(level, candidate)) {
                        continue;
                    }

                    double targetDistance = candidate.distSqr(target);
                    double workerDistance = distanceToSqr(
                            candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
                    double verticalPenalty = Math.abs(candidate.getY() - target.getY()) * 12.0D;
                    double score = targetDistance * 6.0D + workerDistance + verticalPenalty;
                    if (score < bestScore) {
                        bestScore = score;
                        best = candidate.immutable();
                    }
                }
            }
        }

        // Never fall back to the generic upward recovery scan for strip mining.
        // If a valid tunnel access cell does not exist yet, staying put is safer
        // than walking to the surface and teleporting down again.
        return best == null ? blockPosition().immutable() : best;
    }

    private boolean isStripMineStandingPosition(ServerLevel level, BlockPos candidate) {
        return level.getBlockState(candidate).isAir()
                && level.getBlockState(candidate.above()).isAir()
                && !level.getBlockState(candidate.below()).isAir();
    }

'''

if marker not in s:
    raise SystemExit('findSafeTeleport marker not found')
s = s.replace(marker, helper + marker, 1)
entity.write_text(s)

gradle = Path('gradle.properties')
g = gradle.read_text()
if 'mod_version=2.0.3-1.21.1-beta.11' not in g:
    raise SystemExit('beta.11 version marker missing')
gradle.write_text(g.replace('mod_version=2.0.3-1.21.1-beta.11', 'mod_version=2.0.3-1.21.1-beta.12', 1))

readme = Path('README.md')
r = readme.read_text()
r = r.replace('**`2.0.3-1.21.1-beta.11`**', '**`2.0.3-1.21.1-beta.12`**', 1)
old_bullet = '- independent 1x2 strip mines, including assigning individual Minions'
new_bullet = '- independent 1x2 strip mines with tunnel-safe access pathing/recovery, including assigning individual Minions'
if old_bullet not in r:
    raise SystemExit('README strip mine bullet missing')
readme.write_text(r.replace(old_bullet, new_bullet, 1))
