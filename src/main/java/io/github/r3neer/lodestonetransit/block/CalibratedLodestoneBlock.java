package io.github.r3neer.lodestonetransit.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

/** The decorative bud is included in selection; the sturdy stone body carries collision. */
public final class CalibratedLodestoneBlock extends Block {
    private static final VoxelShape OUTLINE = Shapes.or(Shapes.block(), Block.box(5, 16, 5, 11, 20, 11));
    public CalibratedLodestoneBlock(Properties properties) { super(properties); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return OUTLINE; }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.block(); }
}
