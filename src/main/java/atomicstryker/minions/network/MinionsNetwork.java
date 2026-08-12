package atomicstryker.minions.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MinionsNetwork {
    private MinionsNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                MinionCommandPayload.TYPE,
                MinionCommandPayload.STREAM_CODEC,
                MinionCommandPayload::handle
        );
    }
}
