package atomicstryker.minions.network;

import atomicstryker.minions.MinionsMod;
import atomicstryker.minions.common.MinionManager;
import atomicstryker.minions.common.SurfaceWorkJob;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SurfaceWorkPayload(
        SurfaceWorkJob.Operation operation,
        BlockPos firstCorner,
        BlockPos secondCorner,
        BlockPos materialChest,
        int coverage,
        int patchSize,
        int patchStrength,
        boolean surfaceOnly,
        int targetMode,
        int weatherStrength,
        int groundBias,
        int waterBias,
        int skyBias,
        int waterRadius
) implements CustomPacketPayload {
    public static final Type<SurfaceWorkPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MinionsMod.MOD_ID, "surface_work")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SurfaceWorkPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.operation.ordinal());
                buf.writeBlockPos(payload.firstCorner);
                buf.writeBlockPos(payload.secondCorner);
                buf.writeBlockPos(payload.materialChest);
                buf.writeVarInt(payload.coverage);
                buf.writeVarInt(payload.patchSize);
                buf.writeVarInt(payload.patchStrength);
                buf.writeBoolean(payload.surfaceOnly);
                buf.writeVarInt(payload.targetMode);
                buf.writeVarInt(payload.weatherStrength);
                buf.writeVarInt(payload.groundBias);
                buf.writeVarInt(payload.waterBias);
                buf.writeVarInt(payload.skyBias);
                buf.writeVarInt(payload.waterRadius);
            },
            buf -> {
                int operationId = buf.readVarInt();
                SurfaceWorkJob.Operation[] operations = SurfaceWorkJob.Operation.values();
                SurfaceWorkJob.Operation operation = operationId >= 0 && operationId < operations.length
                        ? operations[operationId]
                        : SurfaceWorkJob.Operation.TEXTURE;
                return new SurfaceWorkPayload(
                        operation,
                        buf.readBlockPos(),
                        buf.readBlockPos(),
                        buf.readBlockPos(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readBoolean(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readVarInt()
                );
            }
    );

    public void handle(IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!MinionManager.canUsePower(player)) {
            player.displayClientMessage(Component.translatable("message.minions.no_willpower"), true);
            return;
        }

        SurfaceWorkJob.Result result = SurfaceWorkJob.start(
                player, operation, firstCorner, secondCorner, materialChest,
                coverage, patchSize, patchStrength, surfaceOnly, targetMode,
                weatherStrength, groundBias, waterBias, skyBias, waterRadius
        );

        String message = switch (result) {
            case STARTED -> operation == SurfaceWorkJob.Operation.WEATHERING
                    ? "message.minions.weather_started"
                    : "message.minions.texture_started";
            case TOO_LARGE -> "message.minions.area_too_large";
            case INVALID_CHEST -> "message.minions.surface_invalid_chest";
            case EMPTY_PALETTE -> "message.minions.surface_empty_palette";
            case NO_MINIONS -> "message.minions.no_minions";
            case NO_TARGETS -> "message.minions.surface_no_targets";
            case MATERIALS_CHANGED -> "message.minions.surface_materials_changed";
        };
        player.displayClientMessage(Component.translatable(message), true);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
