package io.github.r3neer.lodestonetransit.anchor;

import java.util.Optional;
import io.github.r3neer.lodestonetransit.LodestoneTransit;
import io.github.r3neer.lodestonetransit.teleport.TeleportDestination;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;

public final class CompassAnchors {
    public static void update(ItemStack stack, ServerLevel level) {
        var destination = stack.get(LodestoneTransit.DESTINATION);
        var tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        if (destination == null && tracker != null) {
            destination = new TeleportDestination(TeleportDestination.Kind.LODESTONE_ANCHOR, Optional.empty(), tracker.target());
        }
        if (destination == null || destination.kind() != TeleportDestination.Kind.LODESTONE_ANCHOR) return;
        var registry = AnchorRegistry.get(level.getServer());
        if (destination.anchorId().isEmpty() && destination.legacyTarget().isPresent()) {
            var target = destination.legacyTarget().get();
            var world = level.getServer().getLevel(target.dimension());
            if (world == null) return;
            // Local adoption only; no global inventory scans or permanent chunk tickets.
            var anchor = registry.adopt(world, target.pos());
            // A failed adoption becomes a permanently broken identity, never a deferred
            // coordinate link which a newly placed lodestone could accidentally revive.
            destination = TeleportDestination.anchor(anchor != null ? anchor.id() : java.util.UUID.randomUUID());
        }
        stack.set(LodestoneTransit.DESTINATION, destination);
        if (destination.anchorId().isPresent()) {
            var a = registry.get(destination.anchorId().get());
            stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(a != null && a.valid() ? Optional.of(a.position()) : Optional.empty(), false));
        }
    }
}
