package io.github.r3neer.lodestonetransit.mixin;

import io.github.r3neer.lodestonetransit.anchor.CompassAnchors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Refresh before vanilla can invalidate historical coordinates. */
@Mixin(CompassItem.class)
public class CompassMixin {
    @Inject(method = "inventoryTick", at = @At("HEAD"))
    private void transit$update(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot, CallbackInfo ci) { CompassAnchors.update(stack, level); }
}
