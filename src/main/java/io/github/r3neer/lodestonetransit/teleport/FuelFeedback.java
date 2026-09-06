package io.github.r3neer.lodestonetransit.teleport;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;

/** Container feedback is distinct from a failed, fueled teleport attempt. */
public final class FuelFeedback {
    public static void sound(ServerPlayer player, BlockPos position, boolean full, SoundSource source) {
        player.level().playSound(null, position, full ? SoundEvents.BUNDLE_INSERT_FAIL : SoundEvents.DECORATED_POT_INSERT_FAIL, source, .65f, 1f);
    }
    public static Component stationText(int count, TravelMessage outcome) {
        var counter = Component.translatable("transit.station.pearls", count);
        if (outcome == TravelMessage.NO_FUEL || outcome == TravelMessage.FUEL_FULL)
            return counter.append(" · ").append(Component.translatable(outcome == TravelMessage.NO_FUEL ? "transit.container.empty" : "transit.container.full"));
        return outcome == null ? counter : outcome.text().copy().append(" · ").append(counter);
    }
    public static void station(ServerPlayer player, int count, TravelMessage outcome) {
        player.sendOverlayMessage(stationText(count, outcome));
    }
}
