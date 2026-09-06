package io.github.r3neer.lodestonetransit.item;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.*;

/** Independent of attack strength, item cooldown rules and damage callbacks. */
public final class EquipReadiness {
    private record Held(ItemStack stack, int slot, long since, boolean notified) {}
    private static final Map<UUID, EnumMap<InteractionHand, Held>> HELD = new HashMap<>();
    private static final Map<UUID, Long> ATTEMPTS = new HashMap<>();
    public static void clear() { HELD.clear(); ATTEMPTS.clear(); }
    public static void reset(ServerPlayer player) { HELD.remove(player.getUUID()); }
    public static void reselect(ServerPlayer player) { var hands = HELD.get(player.getUUID()); if (hands != null) hands.remove(InteractionHand.MAIN_HAND); }
    public static void forget(ServerPlayer player) { reset(player); ATTEMPTS.remove(player.getUUID()); }
    public static void tick(ServerPlayer player) {
        long now = player.level().getServer().getTickCount();
        var hands = HELD.computeIfAbsent(player.getUUID(), id -> new EnumMap<>(InteractionHand.class));
        for (var hand : InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof TeleporterItem)) { hands.remove(hand); continue; }
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : -1;
            var held = hands.get(hand);
            if (held == null || held.stack != stack || held.slot != slot) {
                hands.put(hand, new Held(stack, slot, now, false));
                player.getCooldowns().addCooldown(stack, 20);
            } else if (!held.notified && now - held.since >= 20) {
                hands.put(hand, new Held(stack, slot, held.since, true));
                player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, .3f, 1.4f);
            }
        }
        if (hands.isEmpty()) reset(player);
    }
    public static boolean ready(ServerPlayer player, InteractionHand hand) {
        tick(player); var hands = HELD.get(player.getUUID()); var held = hands == null ? null : hands.get(hand);
        return held != null && player.level().getServer().getTickCount() - held.since >= 20;
    }
    public static boolean claimAttempt(ServerPlayer player) {
        long tick = player.level().getServer().getTickCount();
        return !Objects.equals(ATTEMPTS.put(player.getUUID(), tick), tick);
    }
}
