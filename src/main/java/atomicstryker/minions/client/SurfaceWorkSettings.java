package atomicstryker.minions.client;

import net.minecraft.core.BlockPos;

public final class SurfaceWorkSettings {
    public enum TargetMode {
        SMART,
        PALETTE_ONLY,
        ALL_STRUCTURAL;

        public TargetMode next() {
            TargetMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public static final class TextureOptions {
        public int coverage = 100;
        public int patchSize = 3;
        public int patchStrength = 75;
        public boolean surfaceOnly = true;
        public boolean fillAir = false;
        public TargetMode targetMode = TargetMode.SMART;

        public void reset() {
            coverage = 100;
            patchSize = 3;
            patchStrength = 75;
            surfaceOnly = true;
            fillAir = false;
            targetMode = TargetMode.SMART;
        }
    }

    public static final class WeatherOptions {
        public int coverage = 100;
        public int patchSize = 3;
        public int patchStrength = 80;
        public boolean surfaceOnly = true;
        public boolean fillAir = false;
        public TargetMode targetMode = TargetMode.SMART;
        public int weatherStrength = 65;
        public int groundBias = 65;
        public int waterBias = 85;
        public int skyBias = 20;
        public int waterRadius = 3;

        public void reset() {
            coverage = 100;
            patchSize = 3;
            patchStrength = 80;
            surfaceOnly = true;
            fillAir = false;
            targetMode = TargetMode.SMART;
            weatherStrength = 65;
            groundBias = 65;
            waterBias = 85;
            skyBias = 20;
            waterRadius = 3;
        }
    }

    private static final TextureOptions TEXTURE = new TextureOptions();
    private static final WeatherOptions WEATHER = new WeatherOptions();
    private static BlockPos materialChest;

    private SurfaceWorkSettings() {
    }

    public static TextureOptions texture() {
        return TEXTURE;
    }

    public static WeatherOptions weather() {
        return WEATHER;
    }

    public static BlockPos materialChest() {
        return materialChest;
    }

    public static void setMaterialChest(BlockPos pos) {
        materialChest = pos == null ? null : pos.immutable();
    }
}
