package atomicstryker.minions.common;

import atomicstryker.minions.common.SurfaceWorkSavedData.PendingReplacement;
import atomicstryker.minions.common.entity.MinionEntity;
import atomicstryker.minions.registry.MinionsSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SurfaceWorkJob {
    private static final int MAX_SPAN = 128;
    private static final long MAX_VOLUME = 131_072L;

    private SurfaceWorkJob() {
    }

    public static Result start(ServerPlayer player, Operation operation,
                               BlockPos firstCorner, BlockPos secondCorner, BlockPos materialChest,
                               int coverage, int patchSize, int patchStrength, boolean surfaceOnly, boolean fillAir,
                               int targetMode, int weatherStrength, int groundBias, int waterBias, int skyBias,
                               int waterRadius) {
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
        boolean thinSelection = sizeX == 1L || sizeY == 1L || sizeZ == 1L;

        ServerLevel level = player.serverLevel();
        List<MaterialChestAccess.Part> materialContainers = MaterialChestAccess.resolve(level, materialChest);
        if (materialContainers.isEmpty()) {
            return Result.INVALID_CHEST;
        }

        List<PaletteEntry> palette = readPalette(materialContainers);
        if (palette.isEmpty()) {
            return Result.EMPTY_PALETTE;
        }

        List<MinionEntity> minions = MinionManager.getOwned(player);
        if (minions.isEmpty()) {
            return Result.NO_MINIONS;
        }

        coverage = clamp(coverage, 5, 100);
        patchSize = clamp(patchSize, 1, 8);
        patchStrength = clamp(patchStrength, 0, 100);
        targetMode = clamp(targetMode, 0, 2);
        weatherStrength = clamp(weatherStrength, 0, 100);
        groundBias = clamp(groundBias, 0, 100);
        waterBias = clamp(waterBias, 0, 100);
        skyBias = clamp(skyBias, 0, 100);
        waterRadius = clamp(waterRadius, 1, 6);

        List<Candidate> candidates = new ArrayList<>();
        Map<Block, Integer> blockCounts = new HashMap<>();
        for (int y = maxY; y >= minY; y--) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    if (fillAir && state.isAir()) {
                        if (!surfaceOnly || isFillAirSurface(level, pos)) {
                            candidates.add(new Candidate(pos, state));
                        }
                        continue;
                    }

                    if (!isEligible(level, pos, state)) {
                        continue;
                    }
                    if (surfaceOnly && !isExposed(level, pos)) {
                        continue;
                    }
                    candidates.add(new Candidate(pos, state));
                    blockCounts.merge(state.getBlock(), 1, Integer::sum);
                }
            }
        }

        if (candidates.isEmpty()) {
            return Result.NO_TARGETS;
        }

        Block dominant = blockCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        Set<Block> paletteBlocks = new HashSet<>();
        Map<Block, Integer> remaining = new LinkedHashMap<>();
        for (PaletteEntry entry : palette) {
            paletteBlocks.add(entry.block);
            remaining.put(entry.block, entry.count);
        }

        long seed = level.getRandom().nextLong()
                ^ player.getUUID().getMostSignificantBits()
                ^ firstCorner.asLong()
                ^ Long.rotateLeft(secondCorner.asLong(), 17);

        List<PendingReplacement> replacements = new ArrayList<>();
        List<QueuedReplacement> queue = new ArrayList<>();
        int minionIndex = 0;

        for (Candidate candidate : candidates) {
            Block currentBlock = candidate.state.getBlock();
            boolean buildingIntoAir = candidate.state.isAir();
            boolean targeted = buildingIntoAir || switch (targetMode) {
                case 1 -> paletteBlocks.contains(currentBlock); // palette only
                case 2 -> true; // all structural/eligible blocks
                // A one-block-thick selection is already an explicit surface chosen by the player.
                // In that case Smart must not punch gaps through dirt/grass/other surface variants.
                default -> thinSelection || currentBlock == dominant || paletteBlocks.contains(currentBlock);
            };
            if (!targeted) {
                continue;
            }

            double coverageRoll = sample(seed ^ 0x4D595DF4D0F33173L,
                    candidate.pos.getX(), candidate.pos.getY(), candidate.pos.getZ());
            if (coverageRoll * 100.0D >= coverage) {
                continue;
            }

            double paletteRoll = patchSample(seed, candidate.pos, patchSize, patchStrength);
            if (operation == Operation.WEATHERING) {
                double environmental = weatherInfluence(level, candidate.pos, minY, maxY,
                        groundBias, waterBias, skyBias, waterRadius);
                double shift = (environmental - 0.5D) * (weatherStrength / 100.0D) * 0.9D;
                paletteRoll = clamp01(paletteRoll + shift);
            }

            int selectedIndex = weightedIndex(palette, paletteRoll);
            selectedIndex = findAvailableMaterial(palette, remaining, selectedIndex, currentBlock);
            if (selectedIndex < 0) {
                continue;
            }

            PaletteEntry selected = palette.get(selectedIndex);
            BlockState desired = selected.block.defaultBlockState();
            if (!buildingIntoAir && selected.block == currentBlock) {
                // The chest ratio can deliberately allocate the existing/base material.
                // That is a no-op and consumes no item, exactly like leaving this patch clean.
                continue;
            }

            int stock = remaining.getOrDefault(selected.block, 0);
            if (stock <= 0) {
                continue;
            }
            remaining.put(selected.block, stock - 1);

            replacements.add(new PendingReplacement(
                    player.getUUID(), candidate.pos, materialChest, candidate.state, desired));
            if (!buildingIntoAir) {
                queue.add(new QueuedReplacement(minions.get(minionIndex++ % minions.size()), candidate.pos));
            }
        }

        if (replacements.isEmpty()) {
            return Result.NO_TARGETS;
        }

        for (MinionEntity minion : minions) {
            minion.clearWork();
            minion.setFollowing(false);
            minion.clearMoveTarget();
        }

        if (!SurfaceWorkSavedData.replaceJob(level, player.getUUID(), replacements)) {
            return Result.MATERIALS_CHANGED;
        }
        for (QueuedReplacement order : queue) {
            order.worker.enqueueWork(order.pos, 0);
        }

        level.playSound(null, player.blockPosition(), MinionsSounds.RANDOM_ORDER.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        MinionManager.exhaustBig(player);
        return Result.STARTED;
    }

    private static List<PaletteEntry> readPalette(List<MaterialChestAccess.Part> parts) {
        Map<Block, MutablePaletteEntry> entries = new LinkedHashMap<>();
        int logicalSlot = 0;
        for (MaterialChestAccess.Part part : parts) {
            var container = part.container();
            for (int slot = 0; slot < container.getContainerSize(); slot++, logicalSlot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
                    continue;
                }
                Block block = blockItem.getBlock();
                if (block.asItem() == Items.AIR) {
                    continue;
                }
                MutablePaletteEntry entry = entries.get(block);
                if (entry == null) {
                    entry = new MutablePaletteEntry(block, logicalSlot);
                    entries.put(block, entry);
                }
                entry.count += stack.getCount();
            }
        }
        List<PaletteEntry> result = new ArrayList<>();
        for (MutablePaletteEntry entry : entries.values()) {
            if (entry.count > 0) {
                result.add(new PaletteEntry(entry.block, entry.count, entry.firstSlot));
            }
        }
        return result;
    }

    private static boolean isEligible(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.isAir()
                && state.getDestroySpeed(level, pos) >= 0.0F
                && !state.hasBlockEntity()
                && state.getBlock().asItem() != Items.AIR
                && !state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean isExposed(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbourPos = pos.relative(direction);
            BlockState neighbour = level.getBlockState(neighbourPos);
            if (neighbour.isAir()
                    || !neighbour.getFluidState().isEmpty()
                    || neighbour.getCollisionShape(level, neighbourPos).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * With Surface Only enabled, Fill Air repairs/builds only cells touching an
     * actual structural surface. Turning Surface Only off intentionally allows
     * the selected cuboid to be filled solid.
     */
    private static boolean isFillAirSurface(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbourPos = pos.relative(direction);
            BlockState neighbour = level.getBlockState(neighbourPos);
            if (isEligible(level, neighbourPos, neighbour)) {
                return true;
            }
        }
        return false;
    }

    private static int weightedIndex(List<PaletteEntry> palette, double unit) {
        long total = 0L;
        for (PaletteEntry entry : palette) {
            total += entry.count;
        }
        if (total <= 0L) {
            return -1;
        }
        double threshold = clamp01(unit) * total;
        long cumulative = 0L;
        for (int i = 0; i < palette.size(); i++) {
            cumulative += palette.get(i).count;
            if (threshold < cumulative) {
                return i;
            }
        }
        return palette.size() - 1;
    }

    private static int findAvailableMaterial(List<PaletteEntry> palette, Map<Block, Integer> remaining,
                                             int preferred, Block currentBlock) {
        if (preferred < 0 || palette.isEmpty()) {
            return -1;
        }
        for (int offset = 0; offset < palette.size(); offset++) {
            int index = (preferred + offset) % palette.size();
            Block block = palette.get(index).block;
            if (block == currentBlock || remaining.getOrDefault(block, 0) > 0) {
                return index;
            }
        }
        return -1;
    }

    private static double patchSample(long seed, BlockPos pos, int patchSize, int patchStrength) {
        int zoneX = Math.floorDiv(pos.getX(), patchSize);
        int zoneY = Math.floorDiv(pos.getY(), patchSize);
        int zoneZ = Math.floorDiv(pos.getZ(), patchSize);
        double zone = sample(seed ^ 0x9E3779B97F4A7C15L, zoneX, zoneY, zoneZ);
        double local = sample(seed ^ 0xC2B2AE3D27D4EB4FL, pos.getX(), pos.getY(), pos.getZ());
        double selector = sample(seed ^ 0x165667B19E3779F9L, pos.getZ(), pos.getX(), pos.getY());
        return selector < patchStrength / 100.0D ? zone : local;
    }

    private static double weatherInfluence(ServerLevel level, BlockPos pos, int minY, int maxY,
                                           int groundBias, int waterBias, int skyBias, int waterRadius) {
        double heightRange = Math.max(1.0D, maxY - minY);
        double ground = 1.0D - ((pos.getY() - minY) / heightRange);
        ground = clamp01(ground);

        double water = 0.0D;
        for (Direction direction : Direction.values()) {
            for (int distance = 1; distance <= waterRadius; distance++) {
                if (level.getFluidState(pos.relative(direction, distance)).is(FluidTags.WATER)) {
                    water = Math.max(water, (waterRadius - distance + 1.0D) / waterRadius);
                    break;
                }
            }
        }

        double sky = level.canSeeSky(pos.above()) ? 1.0D : 0.0D;
        int totalBias = groundBias + waterBias + skyBias;
        if (totalBias <= 0) {
            return 0.5D;
        }
        return clamp01((ground * groundBias + water * waterBias + sky * skyBias) / totalBias);
    }

    private static double sample(long seed, int x, int y, int z) {
        long value = seed;
        value ^= (long) x * 0x632BE59BD9B4E019L;
        value ^= (long) y * 0x9E3779B97F4A7C15L;
        value ^= (long) z * 0x94D049BB133111EBL;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (value >>> 11) * 0x1.0p-53;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(0.999999999999D, value));
    }

    public enum Operation {
        TEXTURE,
        WEATHERING
    }

    public enum Result {
        STARTED,
        TOO_LARGE,
        INVALID_CHEST,
        EMPTY_PALETTE,
        NO_MINIONS,
        NO_TARGETS,
        MATERIALS_CHANGED
    }

    private record Candidate(BlockPos pos, BlockState state) {
        private Candidate {
            pos = pos.immutable();
        }
    }

    private record PaletteEntry(Block block, int count, int firstSlot) {
    }

    private static final class MutablePaletteEntry {
        private final Block block;
        private final int firstSlot;
        private int count;

        private MutablePaletteEntry(Block block, int firstSlot) {
            this.block = block;
            this.firstSlot = firstSlot;
        }
    }

    private record QueuedReplacement(MinionEntity worker, BlockPos pos) {
    }
}
