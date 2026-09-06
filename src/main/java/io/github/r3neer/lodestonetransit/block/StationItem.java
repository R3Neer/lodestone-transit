package io.github.r3neer.lodestonetransit.block;

import io.github.r3neer.lodestonetransit.name.DestinationNaming;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;

public final class StationItem extends BlockItem {
    private final boolean dimensional;
    public StationItem(TeleportStationBlock block, Properties properties, boolean dimensional) {
        super(block, properties.component(io.github.r3neer.lodestonetransit.LodestoneTransit.DESTINATION,
            io.github.r3neer.lodestonetransit.teleport.TeleportDestination.spawn()));
        this.dimensional = dimensional;
    }
    @Override public Component getName(ItemStack stack) { return DestinationNaming.name(stack, super.getName(stack)); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> output, TooltipFlag flag) { DestinationNaming.tooltip(stack, dimensional, false, output); }
}
