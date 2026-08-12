from pathlib import Path


def replace_exact(path: str, old: str, new: str):
    p = Path(path)
    text = p.read_text()
    if old not in text:
        raise SystemExit(f"Pattern not found in {path}:\n{old[:240]}")
    p.write_text(text.replace(old, new, 1))

# --- MinionManager: one queued job per whole tree, plus leaf scanner ---
manager = "src/main/java/atomicstryker/minions/common/MinionManager.java"
replace_exact(manager,
"""import java.util.HashSet;\nimport java.util.List;\nimport java.util.Queue;\nimport java.util.Set;\n""",
"""import java.util.HashSet;\nimport java.util.HashMap;\nimport java.util.List;\nimport java.util.Map;\nimport java.util.Queue;\nimport java.util.Set;\n""")

replace_exact(manager,
"""    private static List<BlockPos> collectTreeLogs(ServerLevel level, BlockPos base, int limit) {\n""",
"""    public static List<BlockPos> collectTreeLogs(ServerLevel level, BlockPos base, int limit) {\n""")

needle = """        return logs;\n    }\n\n    private static void assignTreeWork(ServerPlayer player, List<List<BlockPos>> trees) {\n"""
insert = """        return logs;\n    }\n\n    /**\n     * Finds the leaf canopy belonging to a scanned tree. Leaves are flood-filled\n     * from the target logs up to vanilla's normal six-block leaf-distance range.\n     * A leaf directly touching a foreign log is not claimed, which avoids eating\n     * an adjacent tree trunk/canopy in dense forests as much as practical.\n     */\n    public static List<BlockPos> collectTreeLeaves(ServerLevel level, List<BlockPos> treeLogs) {\n        if (treeLogs.isEmpty()) {\n            return List.of();\n        }\n\n        Set<BlockPos> targetLogs = new HashSet<>(treeLogs);\n        Queue<BlockPos> open = new ArrayDeque<>();\n        Map<BlockPos, Integer> distance = new HashMap<>();\n\n        for (BlockPos log : treeLogs) {\n            for (Direction direction : Direction.values()) {\n                BlockPos candidate = log.relative(direction).immutable();\n                if (level.getBlockState(candidate).is(BlockTags.LEAVES) && !distance.containsKey(candidate)) {\n                    distance.put(candidate, 1);\n                    open.add(candidate);\n                }\n            }\n        }\n\n        List<BlockPos> leaves = new ArrayList<>();\n        while (!open.isEmpty()) {\n            BlockPos leaf = open.remove();\n            int currentDistance = distance.getOrDefault(leaf, 7);\n            if (!level.getBlockState(leaf).is(BlockTags.LEAVES)) {\n                continue;\n            }\n\n            boolean touchesForeignLog = false;\n            for (Direction direction : Direction.values()) {\n                BlockPos neighbour = leaf.relative(direction);\n                if (level.getBlockState(neighbour).is(BlockTags.LOGS) && !targetLogs.contains(neighbour)) {\n                    touchesForeignLog = true;\n                    break;\n                }\n            }\n            if (touchesForeignLog) {\n                continue;\n            }\n\n            leaves.add(leaf);\n            if (currentDistance >= 6) {\n                continue;\n            }\n            for (Direction direction : Direction.values()) {\n                BlockPos next = leaf.relative(direction).immutable();\n                if (!distance.containsKey(next) && level.getBlockState(next).is(BlockTags.LEAVES)) {\n                    distance.put(next, currentDistance + 1);\n                    open.add(next);\n                }\n            }\n        }\n        return leaves;\n    }\n\n    private static void assignTreeWork(ServerPlayer player, List<List<BlockPos>> trees) {\n"""
replace_exact(manager, needle, insert)

replace_exact(manager,
"""        int workerIndex = 0;\n        for (List<BlockPos> tree : trees) {\n            MinionEntity worker = minions.get(workerIndex++ % minions.size());\n            for (BlockPos log : tree) {\n                worker.enqueueTreeWork(log);\n            }\n        }\n""",
"""        int workerIndex = 0;\n        for (List<BlockPos> tree : trees) {\n            if (tree.isEmpty()) {\n                continue;\n            }\n            MinionEntity worker = minions.get(workerIndex++ % minions.size());\n            // Legacy BlockTask_TreeChop was one task for an entire scanned tree.\n            // Queue only the lowest trunk block; the Minion rescans the intact\n            // tree when it starts the job and removes the whole tree at finish.\n            worker.enqueueTreeWork(tree.get(0));\n        }\n""")

# --- MinionEntity: cache whole tree, walk up to it, work for tree-sized duration, remove all at once ---
entity = "src/main/java/atomicstryker/minions/common/entity/MinionEntity.java"
replace_exact(entity,
"""    private boolean stripMining;\n    private WorkOrder activeOrder;\n""",
"""    private boolean stripMining;\n    private WorkOrder activeOrder;\n    private List<BlockPos> activeTreeLogs = List.of();\n    private List<BlockPos> activeTreeLeaves = List.of();\n""")

replace_exact(entity,
"""        if (activeOrder != order) {\n            resetActiveWork();\n            activeOrder = order;\n            workTicks = 0;\n        }\n\n        BlockPos target = order.pos();\n""",
"""        if (activeOrder != order) {\n            resetActiveWork();\n            activeOrder = order;\n            workTicks = 0;\n            if (order.action() == WorkAction.TREE_BREAK) {\n                activeTreeLogs = MinionManager.collectTreeLogs(\n                        level, order.pos(), Math.max(64, MinionsConfig.MAX_TREE_BLOCKS.get()));\n                activeTreeLeaves = MinionManager.collectTreeLeaves(level, activeTreeLogs);\n            }\n        }\n\n        BlockPos target = order.pos();\n""")

replace_exact(entity,
"""        double distance = distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);\n        double workReachSq = order.action() == WorkAction.TREE_BREAK ? 256.0D : 9.0D;\n""",
"""        double distance = distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);\n        // Legacy BlockTask_TreeChop inherited the normal 3-block work reach.\n        // The Minion must actually walk up to the trunk instead of chopping from afar.\n        double workReachSq = 9.0D;\n""")

replace_exact(entity,
"""            int requiredTicks = (order.action() == WorkAction.BREAK || order.action() == WorkAction.TREE_BREAK)\n                    ? MinionsConfig.WORK_TICKS_PER_BLOCK.get()\n                    : Math.max(4, MinionsConfig.WORK_TICKS_PER_BLOCK.get() / 3);\n""",
"""            int requiredTicks;\n            if (order.action() == WorkAction.TREE_BREAK) {\n                // Original BlockTask_TreeChop used 1000 ms per wood block and\n                // then removed the complete tree in one finish step.\n                requiredTicks = Math.max(\n                        MinionsConfig.WORK_TICKS_PER_BLOCK.get(),\n                        MinionsConfig.WORK_TICKS_PER_BLOCK.get() * Math.max(1, activeTreeLogs.size())\n                );\n            } else if (order.action() == WorkAction.BREAK) {\n                requiredTicks = MinionsConfig.WORK_TICKS_PER_BLOCK.get();\n            } else {\n                requiredTicks = Math.max(4, MinionsConfig.WORK_TICKS_PER_BLOCK.get() / 3);\n            }\n""")

replace_exact(entity,
"""            if (order.action() == WorkAction.BREAK || order.action() == WorkAction.TREE_BREAK) {\n                harvestBlockIntoInventory(level, target, state);\n            } else {\n""",
"""            if (order.action() == WorkAction.TREE_BREAK) {\n                harvestWholeTreeIntoInventory(level);\n            } else if (order.action() == WorkAction.BREAK) {\n                harvestBlockIntoInventory(level, target, state);\n            } else {\n""")

replace_exact(entity,
"""        adaptWorkingTool(state, order.action());\n        boolean navigating = navigation.moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1.15D);\n        if (!navigating) {\n            stuckTicks++;\n            if (stuckTicks > 80) {\n                BlockPos safe = findSafeTeleport(level, target);\n                teleportTo(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);\n                stuckTicks = 0;\n            }\n""",
"""        adaptWorkingTool(state, order.action());\n        BlockPos navigationTarget = order.action() == WorkAction.TREE_BREAK\n                ? findTreeWorkPosition(level, target)\n                : target;\n        boolean navigating = navigation.moveTo(\n                navigationTarget.getX() + 0.5D, navigationTarget.getY(), navigationTarget.getZ() + 0.5D, 1.15D);\n        if (!navigating) {\n            stuckTicks++;\n            if (stuckTicks > 80) {\n                BlockPos safe = order.action() == WorkAction.TREE_BREAK\n                        ? findTreeWorkPosition(level, target)\n                        : findSafeTeleport(level, target);\n                teleportTo(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);\n                stuckTicks = 0;\n            }\n""")

needle2 = """    /**\n     * Legacy Minions do not leave normal mining drops scattered on the floor.\n"""
insert2 = """    /**\n     * Finishes a legacy-style tree job in one server tick: after the Minion has\n     * spent time chopping at the trunk, all scanned logs and their canopy are\n     * removed together. Log drops and normal leaf loot (saplings/apples/sticks\n     * according to the active vanilla/modded loot table) go into the backpack.\n     */\n    private void harvestWholeTreeIntoInventory(ServerLevel level) {\n        for (BlockPos log : activeTreeLogs) {\n            BlockState state = level.getBlockState(log);\n            if (state.is(BlockTags.LOGS)) {\n                harvestBlockIntoInventory(level, log, state);\n            }\n        }\n        for (BlockPos leaf : activeTreeLeaves) {\n            BlockState state = level.getBlockState(leaf);\n            if (state.is(BlockTags.LEAVES)) {\n                // The held iron axe is neither shears nor Silk Touch, therefore\n                // Block.getDrops follows the normal Minecraft leaf loot chances.\n                harvestBlockIntoInventory(level, leaf, state);\n            }\n        }\n    }\n\n    /**\n     * Legacy Minions do not leave normal mining drops scattered on the floor.\n"""
replace_exact(entity, needle2, insert2)

replace_exact(entity,
"""        workQueue.poll();\n        activeOrder = null;\n        workTicks = 0;\n""",
"""        workQueue.poll();\n        activeOrder = null;\n        activeTreeLogs = List.of();\n        activeTreeLeaves = List.of();\n        workTicks = 0;\n""")

replace_exact(entity,
"""        activeOrder = null;\n        workTicks = 0;\n    }\n\n    private BlockState desiredState(WorkOrder order) {\n""",
"""        activeOrder = null;\n        activeTreeLogs = List.of();\n        activeTreeLeaves = List.of();\n        workTicks = 0;\n    }\n\n    private BlockState desiredState(WorkOrder order) {\n""")

needle3 = """    private BlockPos findSafeTeleport(ServerLevel level, BlockPos around) {\n"""
insert3 = """    private BlockPos findTreeWorkPosition(ServerLevel level, BlockPos trunk) {\n        BlockPos best = null;\n        double bestDistance = Double.MAX_VALUE;\n        for (int dx = -2; dx <= 2; dx++) {\n            for (int dz = -2; dz <= 2; dz++) {\n                if (dx == 0 && dz == 0) {\n                    continue;\n                }\n                BlockPos candidate = trunk.offset(dx, 0, dz);\n                if (!level.getBlockState(candidate).isAir() || !level.getBlockState(candidate.above()).isAir()) {\n                    continue;\n                }\n                if (level.getBlockState(candidate.below()).isAir()) {\n                    continue;\n                }\n                double d = distanceToSqr(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);\n                if (d < bestDistance) {\n                    bestDistance = d;\n                    best = candidate.immutable();\n                }\n            }\n        }\n        return best == null ? trunk : best;\n    }\n\n    private BlockPos findSafeTeleport(ServerLevel level, BlockPos around) {\n"""
replace_exact(entity, needle3, insert3)

# --- Version/docs ---
replace_exact("gradle.properties", "mod_version=2.0.3-1.21.1-beta.7", "mod_version=2.0.3-1.21.1-beta.8")
replace_exact("README.md", "**`2.0.3-1.21.1-beta.7`**", "**`2.0.3-1.21.1-beta.8`**")
replace_exact("README.md",
              "- nearby forest harvesting: one order can find up to 16 trees in a scan extending to 64 blocks",
              "- nearby forest harvesting: one order can find up to 16 trees in a scan extending to 64 blocks; each Minion walks to a trunk, chops for the legacy duration, then removes the whole tree and canopy at once with normal leaf loot")

print("beta.8 whole-tree parity patch applied")
