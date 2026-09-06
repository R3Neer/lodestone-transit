package io.github.r3neer.lodestonetransit.teleport;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Each gameplay outcome has one localized action-bar message. */
public enum TravelMessage {
    WARMING_UP, NO_FUEL, NO_DESTINATION, ANCHOR_UNAVAILABLE, UNCALIBRATED, ANCHOR_MOVING,
    NO_DEATH, DIMENSION_UNAVAILABLE, WRONG_DIMENSION, INVALID_STATE, SLEEPING,
    NO_SPACE, NO_MOUNT_SPACE, GROUP_RESTRICTED, TRANSFER_FAILED, LEASH_LEFT_BEHIND,
    STATION_UNAVAILABLE, FUEL_FULL, LINKED_UNCALIBRATED, LINKED_CALIBRATED;

    public Component text() { return Component.translatable("transit.message." + name().toLowerCase(java.util.Locale.ROOT)); }
    public void show(ServerPlayer player) { player.sendOverlayMessage(text()); }
    public void fail(ServerPlayer player) { show(player); TeleportService.feedback(player, false); }
}
