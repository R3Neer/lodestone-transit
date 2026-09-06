package io.github.r3neer.lodestonetransit.block;

import com.mojang.serialization.MapCodec;
import io.github.r3neer.lodestonetransit.item.EquipReadiness;
import io.github.r3neer.lodestonetransit.teleport.TeleportService;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

public final class TeleportStationBlock extends BaseEntityBlock {
    public static final MapCodec<TeleportStationBlock> CODEC = simpleCodec(p -> new TeleportStationBlock(p, false));
    private final boolean dimensional;
    public TeleportStationBlock(Properties properties, boolean dimensional) { super(properties); this.dimensional = dimensional; }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TeleportStationBlockEntity(pos, state); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof TeleportStationBlockEntity station) station.preserveItem(stack);
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Items.ENDER_PEARL)) {
            if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof TeleportStationBlockEntity station && station.getItem(0).getCount() < 16 && EquipReadiness.claimAttempt(server)) {
                var fuel = station.getItem(0); if (fuel.isEmpty()) station.setItem(0, new ItemStack(Items.ENDER_PEARL)); else { fuel.grow(1); station.setChanged(); }
                stack.consume(1, player); level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, .5f, 1.1f);
            }
            return InteractionResult.SUCCESS;
        }
        return activate(level, pos, player);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return activate(level, pos, player); }
    private InteractionResult activate(Level level, BlockPos pos, Player player) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof TeleportStationBlockEntity station) {
            if (station.isEmpty()) { TeleportService.feedback(server, false); return InteractionResult.SUCCESS; }
            if (EquipReadiness.claimAttempt(server)) { station.removeItem(0, 1); TeleportService.attempt(server, station.destination(), dimensional); }
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) { return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos)); }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) { Containers.updateNeighboursAfterDestroy(state, level, pos); }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof TeleportStationBlockEntity station ? station.asItem() : new ItemStack(this));
    }
}
