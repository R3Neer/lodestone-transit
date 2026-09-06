package io.github.r3neer.lodestonetransit.compat;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;

/** Resource presence, not a loader mod ID: removing the external pack restores flat models. */
public record P1klAvailable() implements ConditionalItemModelProperty {
    public static final MapCodec<P1klAvailable> CODEC = MapCodec.unit(new P1klAvailable());
    public MapCodec<P1klAvailable> type() { return CODEC; }
    public boolean get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed, ItemDisplayContext context) {
        var resources = Minecraft.getInstance().getResourceManager();
        return resources.getResource(Identifier.withDefaultNamespace("textures/item/compass_in_hand.png")).isPresent()
                && resources.getResource(Identifier.withDefaultNamespace("models/item/recovery_compass_00_in_hand.json")).isPresent();
    }
}
