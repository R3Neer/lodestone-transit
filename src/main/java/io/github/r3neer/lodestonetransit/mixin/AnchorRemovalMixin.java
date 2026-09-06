package io.github.r3neer.lodestonetransit.mixin;

import io.github.r3neer.lodestonetransit.anchor.AnchorRegistry;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Covers mining, explosions, commands and replacement, but not chunk unloading. */
@Mixin(LevelChunk.class)
public abstract class AnchorRemovalMixin {
    @Shadow public abstract Level getLevel();
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void transit$removed(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        if (getLevel() instanceof ServerLevel level && cir.getReturnValue() != null && io.github.r3neer.lodestonetransit.anchor.AnchorBlocks.isAnchor(cir.getReturnValue()) && !state.is(cir.getReturnValue().getBlock()))
            AnchorRegistry.get(level.getServer()).invalidate(GlobalPos.of(level.dimension(), pos));
    }
}
