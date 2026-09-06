package io.github.r3neer.lodestonetransit.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import io.github.r3neer.lodestonetransit.item.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Render a readiness shade without suppressing use packets needed for failure feedback. */
@Mixin(GuiGraphicsExtractor.class)
public class ReadinessItemOverlayMixin {
    @WrapOperation(method = "itemCooldown", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemCooldowns;getCooldownPercent(Lnet/minecraft/world/item/ItemStack;F)F"))
    private float transit$remaining(ItemCooldowns cooldowns, ItemStack stack, float partialTick, Operation<Float> original) {
        return stack.getItem() instanceof TeleporterItem ? ClientReadiness.remaining(stack) : original.call(cooldowns, stack, partialTick);
    }
}
