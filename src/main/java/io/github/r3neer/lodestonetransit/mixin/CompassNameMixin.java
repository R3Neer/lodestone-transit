package io.github.r3neer.lodestonetransit.mixin;

import io.github.r3neer.lodestonetransit.name.DestinationNaming;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CompassItem.class)
public class CompassNameMixin {
    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void transit$name(ItemStack stack, CallbackInfoReturnable<Component> cir) { cir.setReturnValue(DestinationNaming.name(stack, cir.getReturnValue())); }
}
