package io.github.r3neer.lodestonetransit.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import io.github.r3neer.lodestonetransit.item.ClientReadiness;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Change display only: combat calculations and server attack strength stay vanilla. */
@Mixin(Hud.class)
public class ReadinessHudMixin {
    @WrapOperation(method = {"extractCrosshair", "extractItemHotbar"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F"))
    private float transit$progress(LocalPlayer player, float partialTick, Operation<Float> original) {
        float progress = ClientReadiness.progress();
        return progress < 0 ? original.call(player, partialTick) : progress;
    }
}
