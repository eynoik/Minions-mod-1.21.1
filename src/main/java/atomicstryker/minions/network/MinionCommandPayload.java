package atomicstryker.minions.network;

import atomicstryker.minions.MinionsMod;
import atomicstryker.minions.common.EvilDeeds;
import atomicstryker.minions.common.MinionManager;
import atomicstryker.minions.common.entity.MinionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MinionCommandPayload(Command command, BlockPos target) implements CustomPacketPayload {
    public static final Type<MinionCommandPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MinionsMod.MOD_ID, "command")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, MinionCommandPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.command.ordinal());
                buf.writeBlockPos(payload.target);
            },
            buf -> {
                int id = buf.readVarInt();
                Command[] values = Command.values();
                Command command = id >= 0 && id < values.length ? values[id] : Command.FOLLOW;
                return new MinionCommandPayload(command, buf.readBlockPos());
            }
    );

    public void handle(IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        switch (command) {
            case COMMIT_EVIL -> EvilDeeds.commit(player);
            case FOLLOW -> MinionManager.follow(player);
            case UNSUMMON -> MinionManager.unsummon(player);
            case MOVE -> MinionManager.moveTo(player, target);
            case DIG_STAIRWELL -> {
                if (MinionManager.canUsePower(player)) MinionManager.digStairwell(player, target);
            }
            case STRIP_MINE -> {
                if (MinionManager.canUsePower(player)) MinionManager.stripMine(player, target);
            }
            case CUSTOM_DIG -> {
                if (MinionManager.canUsePower(player)) MinionManager.digCustom(player, target);
            }
            case CHOP_TREE -> {
                if (MinionManager.canUsePower(player)) MinionManager.chopTree(player, target);
            }
            case MINE_VEIN -> {
                if (MinionManager.canUsePower(player)) MinionManager.mineVein(player, target);
            }
            case DROP_ITEMS -> {
                for (MinionEntity minion : MinionManager.getOwned(player)) {
                    minion.dropStoredItems();
                }
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Command {
        COMMIT_EVIL,
        FOLLOW,
        UNSUMMON,
        MOVE,
        DIG_STAIRWELL,
        STRIP_MINE,
        CUSTOM_DIG,
        CHOP_TREE,
        MINE_VEIN,
        DROP_ITEMS
    }
}
