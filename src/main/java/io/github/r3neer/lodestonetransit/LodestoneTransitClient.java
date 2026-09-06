package io.github.r3neer.lodestonetransit;

import io.github.r3neer.lodestonetransit.anchor.AnchorNetworking;
import io.github.r3neer.lodestonetransit.name.DestinationNaming;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;

public final class LodestoneTransitClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties.ID_MAPPER.put(LodestoneTransit.id("charges"), io.github.r3neer.lodestonetransit.item.ChargeModelProperty.CODEC);
        io.github.r3neer.lodestonetransit.mixin.ModelPropertyRegistryAccessor.transit$registry().put(LodestoneTransit.id("p1kl_available"), io.github.r3neer.lodestonetransit.compat.P1klAvailable.CODEC);
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.registerBuiltinResourcePack(LodestoneTransit.id("p1kl_compat"),
            net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("lodestone_transit").orElseThrow(),
            net.minecraft.network.chat.Component.literal("Lodestone Transit: p1kl’s 3D Items"), net.fabricmc.fabric.api.resource.ResourcePackActivationType.NORMAL);
        ClientPlayNetworking.registerGlobalReceiver(io.github.r3neer.lodestonetransit.item.ReadinessNetworking.Update.TYPE, (payload, context) -> context.client().execute(() -> io.github.r3neer.lodestonetransit.item.ClientReadiness.accept(payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> io.github.r3neer.lodestonetransit.item.ClientReadiness.clear());
        ClientPlayNetworking.registerGlobalReceiver(AnchorNetworking.Update.TYPE, (payload, context) -> context.client().execute(() -> DestinationNaming.CLIENT_ANCHORS.put(payload.anchor().id(), payload.anchor())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> DestinationNaming.CLIENT_ANCHORS.clear());
    }
}
