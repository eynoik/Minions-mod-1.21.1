package atomicstryker.minions.network;

import atomicstryker.minions.MinionsMod;
import atomicstryker.minions.common.AreaDigJob;
import atomicstryker.minions.common.MinionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AreaDigPayload(BlockPos firstCorner, BlockPos secondCorner) implements CustomPacketPayload {
    public static final Type<AreaDigPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MinionsMod.MOD_ID, "area_dig")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, AreaDigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.firstCorner);
                buf.writeBlockPos(payload.secondCorner);
            },
            buf -> new AreaDigPayload(buf.readBlockPos(), buf.readBlockPos())
    );

    public void handle(IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (!MinionManager.canUsePower(player)) {
            player.displayClientMessage(Component.translatable("message.minions.no_willpower"), true);
            return;
        }

        AreaDigJob.Result result = AreaDigJob.start(player, firstCorner, secondCorner);
        String message = switch (result) {
            case STARTED -> "message.minions.area_started";
            case TOO_LARGE -> "message.minions.area_too_large";
            case EMPTY -> "message.minions.area_empty";
            case NO_MINIONS -> "message.minions.no_minions";
        };
        player.displayClientMessage(Component.translatable(message), true);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
