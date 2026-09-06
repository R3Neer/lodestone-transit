package io.github.r3neer.lodestonetransit.name;

import io.github.r3neer.lodestonetransit.LodestoneTransit;
import io.github.r3neer.lodestonetransit.anchor.LodestoneAnchor;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Client cache is display-only; destination validation never reads it. */
public final class DestinationNaming {
    public static final Map<UUID, LodestoneAnchor> CLIENT_ANCHORS = new HashMap<>();
    public static Component destination(ItemStack stack) {
        var d = stack.get(LodestoneTransit.DESTINATION);
        if (d == null) return Component.translatable("transit.destination.unavailable");
        return switch (d.kind()) {
            case WORLD_SPAWN -> Component.translatable("transit.destination.world_spawn");
            case LAST_DEATH -> Component.translatable("transit.destination.last_death");
            case LODESTONE_ANCHOR -> {
                var a = d.anchorId().map(CLIENT_ANCHORS::get).orElse(null);
                yield a == null ? Component.translatable("block.minecraft.lodestone") : a.name().orElse(Component.translatable("block.minecraft.lodestone"));
            }
        };
    }
    public static Component name(ItemStack stack, Component fallback) {
        if (!stack.has(LodestoneTransit.DESTINATION)) return fallback;
        String kind = stack.is(Items.COMPASS) ? "compass" : stack.is(LodestoneTransit.STATION.asItem()) ? "station" : stack.is(LodestoneTransit.DIMENSIONAL_STATION.asItem()) ? "dimensional_station" : stack.is(LodestoneTransit.DIMENSIONAL_TELEPORTER) ? "dimensional_teleporter" : "teleporter";
        return Component.translatable("transit.name." + kind, destination(stack));
    }
    public static void tooltip(ItemStack stack, boolean dimensional, boolean portable, Consumer<Component> out) {
        if (dimensional) out.accept(Component.translatable("transit.dimensional").withStyle(ChatFormatting.LIGHT_PURPLE));
        out.accept(Component.translatable("transit.destination", destination(stack)).withStyle(ChatFormatting.GRAY));
        if (portable) out.accept(Component.translatable("transit.charges", stack.getOrDefault(LodestoneTransit.CHARGES, 0), 4).withStyle(ChatFormatting.GRAY));
        var d = stack.get(LodestoneTransit.DESTINATION);
        if (d != null && d.anchorId().isPresent()) {
            var a = CLIENT_ANCHORS.get(d.anchorId().get());
            if (a == null || !a.valid()) out.accept(Component.translatable("transit.unavailable").withStyle(ChatFormatting.RED));
        }
    }
}
