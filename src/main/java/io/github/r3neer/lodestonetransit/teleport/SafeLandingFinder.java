package io.github.r3neer.lodestonetransit.teleport;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Bounded geometry search. Actual passenger boxes retain their offsets from the vehicle. */
public final class SafeLandingFinder {
    public static double score(double support, double clearance, double distance, boolean hazard) {
        return 40 * support + 30 * clearance + 30 * (1 - distance / 5) - (hazard ? 100 : 0);
    }
    public static List<Entity> passengers(Entity root) {
        var result = new ArrayList<Entity>(); var todo = new ArrayDeque<Entity>(); var seen = new HashSet<UUID>();
        todo.add(root);
        while (!todo.isEmpty()) { var e = todo.remove(); if (!seen.add(e.getUUID())) continue; result.add(e); todo.addAll(e.getPassengers()); }
        return result;
    }
    public static Optional<Vec3> find(ServerLevel world, Entity root, Vec3 target, double radius, List<AABB> occupied) {
        var group = passengers(root);
        double best = -Double.MAX_VALUE; Vec3 bestPos = null;
        // Half-block steps support slabs and small entities without a player-size assumption.
        int steps = (int)Math.floor(radius * 2);
        var loaded = new HashSet<Long>();
        for (int y = -steps; y <= steps; y++) for (int x = -steps; x <= steps; x++) for (int z = -steps; z <= steps; z++) {
            var offset = new Vec3(x * .5, y * .5, z * .5);
            double distance = offset.length(); if (distance > radius) continue;
            var candidate = target.add(offset); var delta = candidate.subtract(root.position());
            boolean valid = true;
            for (var entity : group) {
                var box = entity.getBoundingBox().move(delta).deflate(1.0e-5);
                if (box.minY < world.getMinY() || box.maxY > world.getMaxY() + 1 || !world.getWorldBorder().isWithinBounds(box)) { valid = false; break; }
                for (int cx = ((int)Math.floor(box.minX - 1)) >> 4; cx <= ((int)Math.floor(box.maxX + 1)) >> 4; cx++)
                    for (int cz = ((int)Math.floor(box.minZ - 1)) >> 4; cz <= ((int)Math.floor(box.maxZ + 1)) >> 4; cz++) {
                        long key = net.minecraft.world.level.ChunkPos.pack(cx, cz);
                        if (loaded.add(key)) world.getChunk(cx, cz);
                    }
                if (!world.noBlockCollision(entity, box) || occupied.stream().anyMatch(box::intersects)) { valid = false; break; }
                if (!world.getEntities(entity, box, other -> !group.contains(other) && other.isAlive() && !other.isSpectator()).isEmpty()) { valid = false; break; }
            }
            if (!valid) continue;
            var box = root.getBoundingBox().move(delta);
            double support = support(world, root, box);
            boolean hazard = support == 0 || hazardous(world, box);
            double clearance = 0;
            for (double margin : new double[]{.125, .25, .5}) if (world.noBlockCollision(root, box.inflate(margin, 0, margin).expandTowards(0, margin, 0))) clearance += 1.0 / 3;
            double value = score(support, clearance, distance, hazard);
            // Hazardous candidates remain geometrically valid, but lose strongly to safe ones.
            if (value > best) { best = value; bestPos = candidate; }
        }
        return Optional.ofNullable(bestPos);
    }
    private static double support(ServerLevel world, Entity entity, AABB box) {
        int supported = 0;
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
            double px = box.minX + (box.maxX - box.minX) * (x + .5) / 5;
            double pz = box.minZ + (box.maxZ - box.minZ) * (z + .5) / 5;
            if (!world.noBlockCollision(entity, new AABB(px - .005, box.minY - .06, pz - .005, px + .005, box.minY, pz + .005))) supported++;
        }
        return supported / 25.0;
    }
    private static boolean hazardous(ServerLevel world, AABB box) {
        for (var pos : BlockPos.betweenClosed(BlockPos.containing(box.minX - .05, box.minY - .1, box.minZ - .05), BlockPos.containing(box.maxX + .05, box.maxY, box.maxZ + .05))) {
            var s = world.getBlockState(pos);
            if (s.getFluidState().is(FluidTags.LAVA) || s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE) || s.is(Blocks.CACTUS) || s.is(Blocks.MAGMA_BLOCK) || s.is(Blocks.WITHER_ROSE) || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.POWDER_SNOW)
                || ((s.is(Blocks.CAMPFIRE) || s.is(Blocks.SOUL_CAMPFIRE)) && s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT))) return true;
        }
        return false;
    }
}
