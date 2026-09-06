package io.github.r3neer.lodestonetransit.item;

import io.github.r3neer.lodestonetransit.LodestoneTransit;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class ReadinessNetworking {
    public record Update(int slot, int main, int off) implements CustomPacketPayload {
        public static final Type<Update> TYPE = new Type<>(LodestoneTransit.id("readiness"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Update> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Update::slot, ByteBufCodecs.VAR_INT, Update::main, ByteBufCodecs.VAR_INT, Update::off, Update::new);
        public Type<Update> type() { return TYPE; }
    }
    public static void register() { PayloadTypeRegistry.clientboundPlay().register(Update.TYPE, Update.CODEC); }
    public static void send(ServerPlayer player, Update update) {
        if (ServerPlayNetworking.canSend(player, Update.TYPE)) ServerPlayNetworking.send(player, update);
    }
}
