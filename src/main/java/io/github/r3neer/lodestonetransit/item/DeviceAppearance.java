package io.github.r3neer.lodestonetransit.item;

import io.github.r3neer.lodestonetransit.LodestoneTransit;
import io.github.r3neer.lodestonetransit.teleport.TeleportDestination;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class DeviceAppearance {
    public static void update(ItemStack stack) {
        var model = BuiltInRegistries.ITEM.getKey(stack.getItem());
        var destination = stack.get(LodestoneTransit.DESTINATION);
        if (stack.getItem() instanceof TeleporterItem device && destination != null && destination.kind() == TeleportDestination.Kind.LAST_DEATH)
            model = LodestoneTransit.id(device.dimensional ? "dimensional_recovery_teleporter" : "recovery_teleporter");
        stack.set(DataComponents.ITEM_MODEL, model);
    }
}
