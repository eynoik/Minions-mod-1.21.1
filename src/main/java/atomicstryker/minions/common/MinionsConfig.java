package atomicstryker.minions.common;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MinionsConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue MINIONS_PER_PLAYER;
    public static final ModConfigSpec.DoubleValue FOLLOW_RANGE;
    public static final ModConfigSpec.IntValue MAX_TREE_BLOCKS;
    public static final ModConfigSpec.IntValue MAX_VEIN_BLOCKS;
    public static final ModConfigSpec.IntValue STRIP_MINE_LENGTH;
    public static final ModConfigSpec.IntValue CUSTOM_DIG_SIZE;
    public static final ModConfigSpec.IntValue CUSTOM_DIG_HEIGHT;
    public static final ModConfigSpec.DoubleValue BIG_EXHAUSTION;
    public static final ModConfigSpec.DoubleValue SMALL_EXHAUSTION;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("general");
        MINIONS_PER_PLAYER = builder.comment("Maximum amount of summoned minions per player.")
                .defineInRange("minionsPerPlayer", 4, 1, 16);
        FOLLOW_RANGE = builder.comment("Base follow range of minions.")
                .defineInRange("followRange", 30.0D, 8.0D, 128.0D);
        BIG_EXHAUSTION = builder.comment("Food exhaustion for expensive staff orders.")
                .defineInRange("foodCostBig", 20.0D, 0.0D, 100.0D);
        SMALL_EXHAUSTION = builder.comment("Food exhaustion for small staff actions.")
                .defineInRange("foodCostSmall", 1.5D, 0.0D, 20.0D);
        builder.pop();

        builder.push("jobs");
        MAX_TREE_BLOCKS = builder.defineInRange("maxTreeBlocks", 192, 16, 2048);
        MAX_VEIN_BLOCKS = builder.defineInRange("maxVeinBlocks", 128, 8, 1024);
        STRIP_MINE_LENGTH = builder.defineInRange("stripMineLength", 48, 8, 256);
        CUSTOM_DIG_SIZE = builder.defineInRange("customDigSize", 5, 1, 15);
        CUSTOM_DIG_HEIGHT = builder.defineInRange("customDigHeight", 4, 1, 16);
        builder.pop();

        SPEC = builder.build();
    }

    private MinionsConfig() {
    }
}
