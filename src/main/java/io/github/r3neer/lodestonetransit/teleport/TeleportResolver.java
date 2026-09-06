package io.github.r3neer.lodestonetransit.teleport;

import io.github.r3neer.lodestonetransit.anchor.AnchorRegistry;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;

public final class TeleportResolver {
    public static GlobalPos resolve(ServerPlayer player, TeleportDestination destination) {
        if (destination == null) return null;
        return switch (destination.kind()) {
            case WORLD_SPAWN -> player.level().getServer().getRespawnData().globalPos();
            case LAST_DEATH -> player.getLastDeathLocation().orElse(null);
            case LODESTONE_ANCHOR -> {
                if (destination.anchorId().isEmpty()) yield null;
                var registry = AnchorRegistry.get(player.level().getServer());
                var a = registry.get(destination.anchorId().get());
                if (a == null || !a.valid()) yield null;
                var world = player.level().getServer().getLevel(a.position().dimension());
                if (world == null) yield null;
                var state = world.getBlockState(a.position().pos());
                if (!state.is(Blocks.LODESTONE)) {
                    if (!(world.getBlockEntity(a.position().pos()) instanceof PistonMovingBlockEntity moving && moving.getMovedState().is(Blocks.LODESTONE))) registry.invalidate(a.position());
                    yield null;
                }
                yield a.position();
            }
        };
    }
}
