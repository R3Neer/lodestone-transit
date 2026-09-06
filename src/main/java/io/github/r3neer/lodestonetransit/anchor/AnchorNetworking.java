package io.github.r3neer.lodestonetransit.anchor;

import io.github.r3neer.lodestonetransit.LodestoneTransit;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;

/** Server-to-client only. One record per packet also bounds login packet size. */
public final class AnchorNetworking {
    public record Update(LodestoneAnchor anchor) implements CustomPacketPayload {
        public static final Type<Update> TYPE = new Type<>(LodestoneTransit.id("anchor_update"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Update> CODEC = ByteBufCodecs.fromCodecWithRegistries(LodestoneAnchor.CODEC).map(Update::new, Update::anchor);
        public Type<Update> type() { return TYPE; }
    }
    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(Update.TYPE, Update.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> AnchorRegistry.get(server).all().forEach(a -> sender.sendPacket(new Update(a))));
    }
    public static void broadcast(MinecraftServer server, LodestoneAnchor anchor) {
        for (var player : server.getPlayerList().getPlayers()) if (ServerPlayNetworking.canSend(player, Update.TYPE)) ServerPlayNetworking.send(player, new Update(anchor));
    }
}
