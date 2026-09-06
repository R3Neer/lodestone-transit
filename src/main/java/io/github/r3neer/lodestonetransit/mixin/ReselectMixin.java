package io.github.r3neer.lodestonetransit.mixin;

import io.github.r3neer.lodestonetransit.item.EquipReadiness;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tail runs after vanilla's main-thread packet handoff, independent of combat mods. */
@Mixin(ServerGamePacketListenerImpl.class)
public class ReselectMixin {
    @Shadow public ServerPlayer player;
    @Inject(method = "handleSetCarriedItem", at = @At("TAIL"))
    private void transit$reselect(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) { EquipReadiness.reselect(player); }
    @Inject(method = "handleContainerClick", at = @At("TAIL"))
    private void transit$inventoryClick(net.minecraft.network.protocol.game.ServerboundContainerClickPacket packet, CallbackInfo ci) { EquipReadiness.tick(player); }
}
