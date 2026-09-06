package io.github.r3neer.lodestonetransit.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import io.github.r3neer.lodestonetransit.LodestoneTransit;

/** Registry-only optional integration: exactly one damage point, ignoring Unbreaking. */
public final class AlexsMobsCompat {
    private static final Identifier CARVER = Identifier.fromNamespaceAndPath("alexsmobs", "dimensional_carver");
    public static boolean available() { return BuiltInRegistries.ITEM.containsKey(CARVER); }
    public static boolean catalyst(ItemStack stack) { return stack.is(available() ? BuiltInRegistries.ITEM.getValue(CARVER) : LodestoneTransit.CORE) && stack.isDamageableItem() && stack.getDamageValue() < stack.getMaxDamage(); }
    public static ItemStack remainder(ItemStack input) {
        if (!catalyst(input) || input.getDamageValue() + 1 >= input.getMaxDamage()) return ItemStack.EMPTY;
        var output = input.copyWithCount(1); output.setDamageValue(input.getDamageValue() + 1); return output;
    }
}
