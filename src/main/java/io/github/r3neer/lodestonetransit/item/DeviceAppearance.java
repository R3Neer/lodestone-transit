package io.github.r3neer.lodestonetransit.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class DeviceAppearance {
    public static void update(ItemStack stack) {
        var model = BuiltInRegistries.ITEM.getKey(stack.getItem());
        stack.set(DataComponents.ITEM_MODEL, model);
    }
}
