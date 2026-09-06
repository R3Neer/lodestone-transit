package io.github.r3neer.lodestonetransit;

import io.github.r3neer.lodestonetransit.anchor.AnchorNetworking;
import io.github.r3neer.lodestonetransit.name.DestinationNaming;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;

public final class LodestoneTransitClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(AnchorNetworking.Update.TYPE, (payload, context) -> context.client().execute(() -> DestinationNaming.CLIENT_ANCHORS.put(payload.anchor().id(), payload.anchor())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> DestinationNaming.CLIENT_ANCHORS.clear());
    }
}
