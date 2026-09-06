package io.github.r3neer.lodestonetransit.teleport;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.phys.Vec3;

/** Callers debit fuel before entering this service. No path refunds it. */
public final class TeleportService {
    public static boolean attempt(ServerPlayer player, TeleportDestination destination, boolean dimensional) {
        boolean success = false;
        try {
            var target = TeleportResolver.resolve(player, destination);
            if (target != null && (dimensional || target.dimension().equals(player.level().dimension())) && player.isAlive() && !player.isSleeping()) {
                var world = player.level().getServer().getLevel(target.dimension());
                if (world != null) {
                    var group = new TeleportGroup(player);
                    var landing = SafeLandingFinder.find(world, group.root(), Vec3.atBottomCenterOf(target.pos()), 5, List.of());
                    if (landing.isPresent()) success = group.travel(world, landing.get());
                }
            }
            return success;
        } finally {
            // Exact 26.2 vanilla source and amount; general damage hooks see an ender-pearl hit.
            player.resetFallDistance(); player.resetCurrentImpulseContext();
            player.hurtServer(player.level(), player.damageSources().enderPearl(), 5.0F);
            feedback(player, success);
        }
    }
    public static void feedback(ServerPlayer player, boolean success) {
        player.level().playSound(null, player.blockPosition(), success ? SoundEvents.PLAYER_TELEPORT : SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, .7f, success ? 1 : .8f);
        player.level().sendParticles(success ? ParticleTypes.PORTAL : ParticleTypes.SMOKE, player.getX(), player.getY() + .7, player.getZ(), success ? 12 : 3, .25, .4, .25, .02);
    }
}
