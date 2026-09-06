package io.github.r3neer.lodestonetransit.anchor;

import io.github.r3neer.lodestonetransit.LodestoneTransit;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Compass anchors include ordinary lodestones; travel requires calibration. */
public final class AnchorBlocks {
    public static boolean isAnchor(BlockState state) { return state.is(Blocks.LODESTONE) || calibrated(state); }
    public static boolean calibrated(BlockState state) { return state.is(LodestoneTransit.CALIBRATED_LODESTONE); }
}
