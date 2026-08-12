package atomicstryker.minions.common;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MinionsConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue MINIONS_PER_PLAYER;
    public static final ModConfigSpec.IntValue EVIL_DEED_XP_COST;
    public static final ModConfigSpec.DoubleValue FOLLOW_RANGE;
    public static final ModConfigSpec.IntValue AUTOMATIC_DESPAWN_DELAY;
    public static final ModConfigSpec.IntValue MAX_TREE_BLOCKS;
    public static final ModConfigSpec.IntValue MAX_VEIN_BLOCKS;
    public static final ModConfigSpec.IntValue STRIP_MINE_LENGTH;
    public static final ModConfigSpec.IntValue CUSTOM_DIG_SIZE;
    public static final ModConfigSpec.IntValue CUSTOM_DIG_HEIGHT;
    public static final ModConfigSpec.IntValue WORK_TICKS_PER_BLOCK;
    public static final ModConfigSpec.DoubleValue BIG_EXHAUSTION;
    public static final ModConfigSpec.DoubleValue SMALL_EXHAUSTION;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("general");
        MINIONS_PER_PLAYER = builder.comment("Maximum amount of summoned minions per player.")
                .defineInRange("minionsPerPlayer", 4, 1, 16);
        EVIL_DEED_XP_COST = builder.comment("Experience levels paid per evil deed. Set to -1 to disable survival acquisition of the staff.")
                .defineInRange("evilDeedXPCost", 2, -1, 100);
        FOLLOW_RANGE = builder.comment("Base follow range of minions.")
                .defineInRange("followRange", 30.0D, 8.0D, 128.0D);
        AUTOMATIC_DESPAWN_DELAY = builder.comment("Seconds before a Minion whose owner is offline/dead despawns and drops its inventory. Legacy default: 300.")
                .defineInRange("automaticDespawnDelay", 300, 0, 3600);
        BIG_EXHAUSTION = builder.comment("Food exhaustion for expensive staff orders.")
                .defineInRange("foodCostBig", 20.0D, 0.0D, 100.0D);
        SMALL_EXHAUSTION = builder.comment("Food exhaustion for small staff actions.")
                .defineInRange("foodCostSmall", 1.5D, 0.0D, 20.0D);
        builder.pop();

        builder.push("jobs");
        MAX_TREE_BLOCKS = builder.defineInRange("maxTreeBlocks", 192, 16, 2048);
        MAX_VEIN_BLOCKS = builder.defineInRange("maxVeinBlocks", 128, 8, 1024);
        STRIP_MINE_LENGTH = builder.comment("Legacy strip mines were 80 blocks long.")
                .defineInRange("stripMineLength", 80, 8, 256);
        CUSTOM_DIG_SIZE = builder.comment("Default custom dig width. The in-game legacy menu can select 3..71.")
                .defineInRange("customDigSize", 3, 3, 71);
        CUSTOM_DIG_HEIGHT = builder.comment("Default custom dig height. The in-game legacy menu can select 3..25.")
                .defineInRange("customDigHeight", 3, 3, 25);
        WORK_TICKS_PER_BLOCK = builder.comment("Base work time per mined block. Legacy BlockTask default was one second = 20 ticks.")
                .defineInRange("workTicksPerBlock", 20, 1, 200);
        builder.pop();

        SPEC = builder.build();
    }

    private MinionsConfig() {
    }
}
