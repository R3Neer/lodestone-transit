package io.github.r3neer.lodestonetransit.teleport;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.*;

/** Captures leash edges before vanilla cross-dimensional teleport removes them. */
public final class TeleportGroup {
    private record Edge(Entity child, UUID holder) {}
    private final Entity root;
    private final List<Edge> edges = new ArrayList<>();
    private boolean leftBehind;
    public boolean leftBehind() { return leftBehind; }
    public TeleportGroup(Entity player) {
        root = player.getRootVehicle();
        var queue = new ArrayDeque<Entity>(SafeLandingFinder.passengers(root));
        var seen = new HashSet<UUID>();
        for (var e : queue) seen.add(e.getUUID());
        while (!queue.isEmpty()) {
            var holder = queue.remove();
            for (var leash : Leashable.leashableLeashedTo(holder)) {
                var child = (Entity)leash;
                edges.add(new Edge(child, holder.getUUID()));
                if (seen.add(child.getUUID())) queue.add(child);
            }
        }
    }
    public Entity root() { return root; }
    public boolean canTravel(ServerLevel destination) {
        var essential = SafeLandingFinder.passengers(root);
        for (var entity : essential) if (!entity.isAlive() || !entity.canTeleport(entity.level(), destination)) return false;
        // Check constructors before vanilla moves passengers, to avoid a partial group on null factories.
        if (root.level() != destination) for (var e : essential) if (!(e instanceof net.minecraft.server.level.ServerPlayer) && e.getType().create(destination, EntitySpawnReason.DIMENSION_TRAVEL) == null) return false;
        return true;
    }
    public boolean travel(ServerLevel destination, Vec3 pos) {
        if (!canTravel(destination)) return false;
        var moved = new HashMap<UUID, Entity>();
        var main = teleport(root, destination, pos);
        if (main == null) return false;
        var occupied = new ArrayList<AABB>();
        for (var e : SafeLandingFinder.passengers(main)) { moved.put(e.getUUID(), e); occupied.add(e.getBoundingBox()); }
        for (var edge : edges) {
            var holder = moved.get(edge.holder);
            if (holder == null) continue; // The dependent subtree remains connected in the origin.
            if (moved.containsKey(edge.child.getUUID())) continue;
            var child = edge.child;
            var landing = child.canTeleport(child.level(), destination) ? SafeLandingFinder.find(destination, child, main.position(), 3, occupied) : Optional.<Vec3>empty();
            if (landing.isEmpty()) { leftBehind = true; ((Leashable)child).dropLeash(); continue; }
            var arrived = teleport(child, destination, landing.get());
            if (arrived instanceof Leashable leash) {
                leash.setLeashedTo(holder, true); moved.put(arrived.getUUID(), arrived); occupied.add(arrived.getBoundingBox());
            } else { leftBehind = true; if (!child.isRemoved()) ((Leashable)child).dropLeash(); }
        }
        // Rebuild also back-edges/cycles and links involving an essential passenger.
        for (var edge : edges) {
            var child = moved.get(edge.child.getUUID()); var holder = moved.get(edge.holder);
            if (child instanceof Leashable leash) {
                if (holder != null) leash.setLeashedTo(holder, true);
                else leash.dropLeash();
            }
        }
        return true;
    }
    private static Entity teleport(Entity entity, ServerLevel level, Vec3 pos) {
        return entity.teleport(new TeleportTransition(level, pos, Vec3.ZERO, entity.getYRot(), entity.getXRot(), e -> {
            e.resetFallDistance();
            e.placePortalTicket(net.minecraft.core.BlockPos.containing(e.position()));
            if (e instanceof LivingEntity living) living.resetCurrentImpulseContext();
        }));
    }
}
