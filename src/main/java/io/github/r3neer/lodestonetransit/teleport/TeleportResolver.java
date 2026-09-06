package io.github.r3neer.lodestonetransit.teleport;

import io.github.r3neer.lodestonetransit.anchor.*;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;

public final class TeleportResolver {
    public record Resolution(GlobalPos target, TravelMessage failure) {
        static Resolution failed(TravelMessage failure) { return new Resolution(null, failure); }
    }
    public static GlobalPos resolve(ServerPlayer player, TeleportDestination destination) { return resolveDetailed(player, destination).target(); }
    public static Resolution resolveDetailed(ServerPlayer player, TeleportDestination destination) {
        if (destination == null) return Resolution.failed(TravelMessage.NO_DESTINATION);
        GlobalPos target;
        switch (destination.kind()) {
            case WORLD_SPAWN -> target = player.level().getServer().getRespawnData().globalPos();
            case LAST_DEATH -> {
                target = player.getLastDeathLocation().orElse(null);
                if (target == null) return Resolution.failed(TravelMessage.NO_DEATH);
            }
            case LODESTONE_ANCHOR -> {
                if (destination.anchorId().isEmpty()) return Resolution.failed(TravelMessage.ANCHOR_UNAVAILABLE);
                var registry = AnchorRegistry.get(player.level().getServer());
                var a = registry.get(destination.anchorId().get());
                if (a == null || !a.valid()) return Resolution.failed(TravelMessage.ANCHOR_UNAVAILABLE);
                var world = player.level().getServer().getLevel(a.position().dimension());
                if (world == null) return Resolution.failed(TravelMessage.DIMENSION_UNAVAILABLE);
                var state = world.getBlockState(a.position().pos());
                if (!AnchorBlocks.isAnchor(state)) {
                    if (world.getBlockEntity(a.position().pos()) instanceof PistonMovingBlockEntity moving && AnchorBlocks.isAnchor(moving.getMovedState())) return Resolution.failed(TravelMessage.ANCHOR_MOVING);
                    registry.invalidate(a.position());
                    return Resolution.failed(TravelMessage.ANCHOR_UNAVAILABLE);
                }
                if (!AnchorBlocks.calibrated(state)) return Resolution.failed(TravelMessage.UNCALIBRATED);
                target = a.position();
            }
            default -> throw new IllegalStateException("Unknown destination kind");
        }
        return player.level().getServer().getLevel(target.dimension()) == null ? Resolution.failed(TravelMessage.DIMENSION_UNAVAILABLE) : new Resolution(target, null);
    }
}
