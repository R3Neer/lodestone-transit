package io.github.r3neer.lodestonetransit.teleport;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.phys.Vec3;

/** Callers debit fuel before entering this service. No path refunds it. */
public final class TeleportService {
    public static boolean attempt(ServerPlayer player, TeleportDestination destination, boolean dimensional) {
        return attempt(player, destination, dimensional, -1);
    }
    public static boolean attempt(ServerPlayer player, TeleportDestination destination, boolean dimensional, int stationPearls) {
        boolean success = false;
        TravelMessage message = null;
        try {
            if (!player.isAlive() || player.isRemoved() || player.isSpectator()) { message = TravelMessage.INVALID_STATE; return false; }
            if (player.isSleeping()) { message = TravelMessage.SLEEPING; return false; }
            var resolution = TeleportResolver.resolveDetailed(player, destination);
            if (resolution.failure() != null) { message = resolution.failure(); return false; }
            var target = resolution.target();
            if (!dimensional && !target.dimension().equals(player.level().dimension())) { message = TravelMessage.WRONG_DIMENSION; return false; }
            var world = player.level().getServer().getLevel(target.dimension());
            var group = new TeleportGroup(player);
            if (!group.canTravel(world)) { message = TravelMessage.GROUP_RESTRICTED; return false; }
            var landing = SafeLandingFinder.find(world, group.root(), Vec3.atBottomCenterOf(target.pos()), 5, List.of());
            if (landing.isEmpty()) { message = player.isPassenger() ? TravelMessage.NO_MOUNT_SPACE : TravelMessage.NO_SPACE; return false; }
            success = group.travel(world, landing.get());
            if (!success) message = TravelMessage.TRANSFER_FAILED;
            else if (group.leftBehind()) message = TravelMessage.LEASH_LEFT_BEHIND;
            return success;
        } finally {
            // Exact 26.2 vanilla source and amount; general damage hooks see an ender-pearl hit.
            player.resetFallDistance(); player.resetCurrentImpulseContext();
            player.hurtServer(player.level(), player.damageSources().enderPearl(), 5.0F);
            feedback(player, success);
            if (stationPearls >= 0) FuelFeedback.station(player, stationPearls, message);
            else if (message != null) message.show(player);
        }
    }
    public static void feedback(ServerPlayer player, boolean success) {
        player.level().playSound(null, player.blockPosition(), success ? SoundEvents.PLAYER_TELEPORT : SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, .7f, success ? 1 : .8f);
        player.level().sendParticles(success ? ParticleTypes.PORTAL : ParticleTypes.SMOKE, player.getX(), player.getY() + .7, player.getZ(), success ? 12 : 3, .25, .4, .25, .02);
    }
}
