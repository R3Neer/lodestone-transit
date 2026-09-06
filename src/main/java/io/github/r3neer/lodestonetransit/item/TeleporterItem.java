package io.github.r3neer.lodestonetransit.item;

import io.github.r3neer.lodestonetransit.LodestoneTransit;
import io.github.r3neer.lodestonetransit.anchor.CompassAnchors;
import io.github.r3neer.lodestonetransit.name.DestinationNaming;
import io.github.r3neer.lodestonetransit.teleport.TeleportService;
import io.github.r3neer.lodestonetransit.teleport.TravelMessage;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public final class TeleporterItem extends Item {
    public final boolean dimensional;
    public TeleporterItem(Properties properties, boolean dimensional) { super(properties.component(LodestoneTransit.DESTINATION, io.github.r3neer.lodestonetransit.teleport.TeleportDestination.spawn())); this.dimensional = dimensional; }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        if (EquipReadiness.attemptedThisTick(serverPlayer)) return InteractionResult.SUCCESS;
        var stack = player.getItemInHand(hand);
        var other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (player.isShiftKeyDown() && other.is(Items.ENDER_PEARL)) {
            int fuel = stack.getOrDefault(LodestoneTransit.CHARGES, 0);
            if (fuel < 4 && EquipReadiness.claimAttempt(serverPlayer)) {
                stack.set(LodestoneTransit.CHARGES, fuel + 1); other.consume(1, player);
                level.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.PLAYERS, .5f, 1.2f);
            }
            if (fuel >= 4) TravelMessage.FUEL_FULL.fail(serverPlayer);
            return InteractionResult.SUCCESS;
        }
        if (!EquipReadiness.ready(serverPlayer, hand)) { TravelMessage.WARMING_UP.show(serverPlayer); return InteractionResult.SUCCESS; }
        int fuel = stack.getOrDefault(LodestoneTransit.CHARGES, 0);
        if (fuel <= 0) { TravelMessage.NO_FUEL.fail(serverPlayer); return InteractionResult.SUCCESS; }
        if (!EquipReadiness.claimAttempt(serverPlayer)) return InteractionResult.SUCCESS;
        stack.set(LodestoneTransit.CHARGES, fuel - 1);
        CompassAnchors.update(stack, serverPlayer.level());
        TeleportService.attempt(serverPlayer, stack.get(LodestoneTransit.DESTINATION), dimensional);
        return InteractionResult.SUCCESS;
    }
    @Override public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) { CompassAnchors.update(stack, level); DeviceAppearance.update(stack); }
    @Override public Component getName(ItemStack stack) { return DestinationNaming.name(stack, super.getName(stack)); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> output, TooltipFlag flag) { DestinationNaming.tooltip(stack, dimensional, true, output); }
}
