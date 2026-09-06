package io.github.r3neer.lodestonetransit.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import io.github.r3neer.lodestonetransit.anchor.AnchorRegistry;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Uses the resolved list including modifications made by compatible piston mods. */
@Mixin(PistonBaseBlock.class)
public class PistonMovementMixin {
    /** Vanilla 26.2 marks lodestones BLOCK; lift only that reaction after border/height checks. */
    @WrapOperation(method = "isPushable", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getPistonPushReaction()Lnet/minecraft/world/level/material/PushReaction;"))
    private static net.minecraft.world.level.material.PushReaction transit$movableAnchor(net.minecraft.world.level.block.state.BlockState state, Operation<net.minecraft.world.level.material.PushReaction> original) {
        return state.is(net.minecraft.world.level.block.Blocks.LODESTONE) ? net.minecraft.world.level.material.PushReaction.NORMAL : original.call(state);
    }
    @WrapOperation(method = "moveBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;getToPush()Ljava/util/List;"))
    private List<BlockPos> transit$move(PistonStructureResolver resolver, Operation<List<BlockPos>> original, Level world, BlockPos piston, Direction direction, boolean extending) {
        var positions = original.call(resolver);
        if (world instanceof ServerLevel level) AnchorRegistry.get(level.getServer()).beginMove(level, positions, extending ? direction : direction.getOpposite());
        return positions;
    }
    @Inject(method = "moveBlocks", at = @At("RETURN"))
    private void transit$finish(Level world, BlockPos pos, Direction direction, boolean extending, CallbackInfoReturnable<Boolean> cir) {
        if (world instanceof ServerLevel level) AnchorRegistry.get(level.getServer()).endMove();
    }
}
