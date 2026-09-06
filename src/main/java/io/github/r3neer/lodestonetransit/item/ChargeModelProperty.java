package io.github.r3neer.lodestonetransit.item;

import com.mojang.serialization.MapCodec;
import io.github.r3neer.lodestonetransit.LodestoneTransit;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;

/** Reads the authoritative component even for stacks in containers or item entities. */
public record ChargeModelProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<ChargeModelProperty> CODEC = MapCodec.unit(new ChargeModelProperty());
    @Override public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
        return Math.clamp(stack.getOrDefault(LodestoneTransit.CHARGES, 0), 0, 4);
    }
    @Override public MapCodec<ChargeModelProperty> type() { return CODEC; }
}
