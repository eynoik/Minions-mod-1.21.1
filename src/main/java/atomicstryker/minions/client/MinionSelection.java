package atomicstryker.minions.client;

import atomicstryker.minions.common.SurfaceWorkJob;
import atomicstryker.minions.network.AreaDigPayload;
import atomicstryker.minions.network.MinionCommandPayload;
import atomicstryker.minions.network.SurfaceWorkPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class MinionSelection {
    public enum Mode {
        NONE,
        MINESHAFT,
        STRIP_MINE,
        CUSTOM,
        DIG_AREA,
        TEXTURE_AREA,
        WEATHER_AREA,
        MATERIAL_CHEST,
        MOVE,
        CHOP_TREE,
        MINE_VEIN,
        ASSIGN_CHEST
    }

    private static Mode mode = Mode.NONE;
    private static BlockPos target;
    private static BlockPos firstCorner;
    private static int customSizeXZ = 3;
    private static int customSizeY = 3;

    private MinionSelection() {
    }

    public static void start(Mode newMode) {
        mode = newMode;
        target = null;
        firstCorner = null;
    }

    public static void startCustom(int xzSize, int ySize) {
        customSizeXZ = clampOdd(xzSize, 3, 71);
        customSizeY = Math.max(3, Math.min(25, ySize));
        start(Mode.CUSTOM);
    }

    public static void clear() {
        mode = Mode.NONE;
        target = null;
        firstCorner = null;
    }

    public static boolean isActive() {
        return mode != Mode.NONE;
    }

    public static Mode mode() {
        return mode;
    }

    public static int customSizeXZ() {
        return customSizeXZ;
    }

    public static int customSizeY() {
        return customSizeY;
    }

    public static BlockPos target() {
        return target;
    }

    public static BlockPos firstCorner() {
        return firstCorner;
    }

    public static void updateFromCrosshair() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.hitResult instanceof BlockHitResult blockHit) {
            target = mode == Mode.MOVE
                    ? blockHit.getBlockPos().relative(blockHit.getDirection()).immutable()
                    : blockHit.getBlockPos().immutable();
        }
    }

    public static boolean confirm() {
        if (target == null || mode == Mode.NONE) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (mode == Mode.MATERIAL_CHEST) {
            if (minecraft.level != null && minecraft.level.getBlockEntity(target) instanceof Container) {
                SurfaceWorkSettings.setMaterialChest(target);
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(Component.translatable(
                            "message.minions.surface_chest_set", target.getX(), target.getY(), target.getZ()), true);
                }
                clear();
            } else if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable("message.minions.surface_invalid_chest"), true);
            }
            return true;
        }

        if (mode == Mode.DIG_AREA) {
            if (firstCorner == null) {
                setFirstCorner(minecraft);
                return true;
            }

            PacketDistributor.sendToServer(new AreaDigPayload(firstCorner, target));
            clear();
            return true;
        }

        if (mode == Mode.TEXTURE_AREA || mode == Mode.WEATHER_AREA) {
            BlockPos chest = SurfaceWorkSettings.materialChest();
            if (chest == null) {
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(Component.translatable("message.minions.surface_need_chest"), true);
                }
                clear();
                return true;
            }
            if (firstCorner == null) {
                setFirstCorner(minecraft);
                return true;
            }

            if (mode == Mode.TEXTURE_AREA) {
                SurfaceWorkSettings.TextureOptions options = SurfaceWorkSettings.texture();
                PacketDistributor.sendToServer(new SurfaceWorkPayload(
                        SurfaceWorkJob.Operation.TEXTURE,
                        firstCorner, target, chest,
                        options.coverage, options.patchSize, options.patchStrength,
                        options.surfaceOnly, options.fillAir, options.targetMode.ordinal(),
                        0, 0, 0, 0, 1
                ));
            } else {
                SurfaceWorkSettings.WeatherOptions options = SurfaceWorkSettings.weather();
                PacketDistributor.sendToServer(new SurfaceWorkPayload(
                        SurfaceWorkJob.Operation.WEATHERING,
                        firstCorner, target, chest,
                        options.coverage, options.patchSize, options.patchStrength,
                        options.surfaceOnly, options.fillAir, options.targetMode.ordinal(),
                        options.weatherStrength, options.groundBias, options.waterBias,
                        options.skyBias, options.waterRadius
                ));
            }
            clear();
            return true;
        }

        int sneaking = minecraft.player != null && minecraft.player.isShiftKeyDown() ? 1 : 0;
        MinionCommandPayload payload = switch (mode) {
            case MINESHAFT -> new MinionCommandPayload(MinionCommandPayload.Command.DIG_STAIRWELL, target);
            case STRIP_MINE -> new MinionCommandPayload(MinionCommandPayload.Command.STRIP_MINE, target);
            case CUSTOM -> new MinionCommandPayload(MinionCommandPayload.Command.CUSTOM_DIG, target, customSizeXZ, customSizeY);
            case MOVE -> new MinionCommandPayload(MinionCommandPayload.Command.MOVE, target);
            case CHOP_TREE -> new MinionCommandPayload(MinionCommandPayload.Command.CHOP_TREE, target);
            case MINE_VEIN -> new MinionCommandPayload(MinionCommandPayload.Command.MINE_VEIN, target);
            case ASSIGN_CHEST -> new MinionCommandPayload(MinionCommandPayload.Command.ASSIGN_CHEST, target, sneaking, 0);
            case DIG_AREA, TEXTURE_AREA, WEATHER_AREA, MATERIAL_CHEST, NONE -> null;
        };
        if (payload == null) {
            return false;
        }
        PacketDistributor.sendToServer(payload);
        clear();
        return true;
    }

    private static void setFirstCorner(Minecraft minecraft) {
        firstCorner = target.immutable();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable("message.minions.area_first"), true);
        }
    }

    public static AABB mainBox() {
        Minecraft minecraft = Minecraft.getInstance();
        if (target == null || minecraft.player == null) {
            return null;
        }

        Direction direction = directionFromPlayer(target);
        return switch (mode) {
            case MINESHAFT -> new AABB(
                    target.getX(), target.getY(), target.getZ(),
                    target.getX() + 5.0D, target.getY() + 1.0D, target.getZ() + 5.0D
            );
            case STRIP_MINE -> boxBetween(target, target.relative(direction, 2).above());
            case CUSTOM -> customBox(target, direction, customSizeXZ, customSizeY);
            case DIG_AREA, TEXTURE_AREA, WEATHER_AREA -> firstCorner == null
                    ? new AABB(target)
                    : boxBetween(firstCorner, target);
            case MOVE, CHOP_TREE, MINE_VEIN, ASSIGN_CHEST, MATERIAL_CHEST -> new AABB(target);
            case NONE -> null;
        };
    }

    public static List<AABB> helperBoxes() {
        List<AABB> boxes = new ArrayList<>();
        if (target == null) {
            return boxes;
        }
        if (mode == Mode.MINESHAFT) {
            for (int i = 1; i <= 3; i++) {
                BlockPos p = target.offset(i, -i, 0);
                boxes.add(new AABB(p));
            }
        } else if ((mode == Mode.DIG_AREA || mode == Mode.TEXTURE_AREA || mode == Mode.WEATHER_AREA)
                && firstCorner != null) {
            boxes.add(new AABB(firstCorner).inflate(0.02D));
        }
        return boxes;
    }

    public static Direction directionFromPlayer(BlockPos start) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return Direction.NORTH;
        }
        int bossX = minecraft.player.blockPosition().getX();
        int bossZ = minecraft.player.blockPosition().getZ();
        int dx = start.getX() - bossX;
        int dz = start.getZ() - bossZ;
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static AABB customBox(BlockPos start, Direction direction, int size, int height) {
        int half = (size - 1) / 2;
        int minX;
        int maxX;
        int minZ;
        int maxZ;

        if (direction.getAxis() == Direction.Axis.X) {
            minX = direction == Direction.EAST ? start.getX() : start.getX() - (size - 1);
            maxX = minX + size - 1;
            minZ = start.getZ() - half;
            maxZ = start.getZ() + half;
        } else {
            minZ = direction == Direction.SOUTH ? start.getZ() : start.getZ() - (size - 1);
            maxZ = minZ + size - 1;
            minX = start.getX() - half;
            maxX = start.getX() + half;
        }

        return new AABB(minX, start.getY(), minZ, maxX + 1.0D, start.getY() + height, maxZ + 1.0D);
    }

    private static AABB boxBetween(BlockPos a, BlockPos b) {
        int minX = Math.min(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxX = Math.max(a.getX(), b.getX()) + 1;
        int maxY = Math.max(a.getY(), b.getY()) + 1;
        int maxZ = Math.max(a.getZ(), b.getZ()) + 1;
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static int clampOdd(int value, int min, int max) {
        int result = Math.max(min, Math.min(max, value));
        if ((result & 1) == 0) {
            result = Math.min(max, result + 1);
        }
        return result;
    }
}
