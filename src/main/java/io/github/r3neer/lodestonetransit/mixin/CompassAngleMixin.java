package io.github.r3neer.lodestonetransit.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import io.github.r3neer.lodestonetransit.LodestoneTransit;
import io.github.r3neer.lodestonetransit.name.DestinationNaming;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngleState;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A stack emerging from a chest resolves its current target before its next inventory tick. */
@Mixin(CompassAngleState.class)
public class CompassAngleMixin {
    @WrapOperation(method = "calculate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/properties/numeric/CompassAngleState$CompassTarget;get(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/ItemOwner;)Lnet/minecraft/core/GlobalPos;"))
    private GlobalPos transit$target(CompassAngleState.CompassTarget type, ClientLevel level, ItemStack stack, ItemOwner owner, Operation<GlobalPos> original) {
        var d = stack.get(LodestoneTransit.DESTINATION);
        if (d != null && d.anchorId().isPresent()) {
            var a = DestinationNaming.CLIENT_ANCHORS.get(d.anchorId().get());
            return a != null && a.valid() ? a.position() : null;
        }
        return original.call(type, level, stack, owner);
    }
}
