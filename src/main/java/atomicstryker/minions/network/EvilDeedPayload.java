package atomicstryker.minions.network;

import atomicstryker.minions.MinionsMod;
import atomicstryker.minions.common.EvilDeeds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EvilDeedPayload(int deedIndex) implements CustomPacketPayload {
    public static final Type<EvilDeedPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MinionsMod.MOD_ID, "evil_deed")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, EvilDeedPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarInt(payload.deedIndex),
            buf -> new EvilDeedPayload(buf.readVarInt())
    );

    public void handle(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            EvilDeeds.commit(player, deedIndex);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
