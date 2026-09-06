package io.github.r3neer.lodestonetransit.block;

import com.mojang.serialization.MapCodec;
import io.github.r3neer.lodestonetransit.item.EquipReadiness;
import io.github.r3neer.lodestonetransit.teleport.TeleportService;
import io.github.r3neer.lodestonetransit.teleport.TravelMessage;
import io.github.r3neer.lodestonetransit.teleport.FuelFeedback;
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
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty PEARLS = net.minecraft.world.level.block.state.properties.IntegerProperty.create("pearls", 0, 16);
    public static final MapCodec<TeleportStationBlock> CODEC = simpleCodec(p -> new TeleportStationBlock(p, false));
    private final boolean dimensional;
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = stationShape();
    private static net.minecraft.world.phys.shapes.VoxelShape stationShape() {
        var shape = net.minecraft.world.phys.shapes.Shapes.block();
        // Quarter-unit strips approximate the 45-degree instrument footprint.
        double radius = 4.5 * Math.sqrt(2);
        for (double x = 8-radius; x < 8+radius; x += .25) {
            double width = radius - Math.abs(x+.125-8);
            if (width > 0) shape = net.minecraft.world.phys.shapes.Shapes.or(shape, Block.box(x,16,8-width,Math.min(x+.25,8+radius),18,8+width));
        }
        return shape.optimize();
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) { return SHAPE; }
    public TeleportStationBlock(Properties properties, boolean dimensional) {
        super(properties.lightLevel(state -> (state.getValue(PEARLS) / 4) * (dimensional ? 2 : 1)));
        this.dimensional = dimensional;
        registerDefaultState(stateDefinition.any().setValue(PEARLS, 0));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) { builder.add(PEARLS); }
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, io.github.r3neer.lodestonetransit.LodestoneTransit.STATION_ENTITY,
            (world, pos, blockState, station) -> station.syncPearls());
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TeleportStationBlockEntity(pos, state); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof TeleportStationBlockEntity station) station.preserveItem(stack);
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && EquipReadiness.attemptedThisTick(server)) return InteractionResult.SUCCESS;
        // Block interaction starts with the main hand: reserve an offhand pearl before travel.
        if (!stack.is(Items.ENDER_PEARL) && hand == InteractionHand.MAIN_HAND && player.getOffhandItem().is(Items.ENDER_PEARL)) {
            return useItemOn(player.getOffhandItem(), state, level, pos, player, InteractionHand.OFF_HAND, hit);
        }
        if (stack.is(Items.ENDER_PEARL)) {
            if (player instanceof ServerPlayer server && EquipReadiness.claimAttempt(server)) {
                if (!(level.getBlockEntity(pos) instanceof TeleportStationBlockEntity station)) {
                    TravelMessage.STATION_UNAVAILABLE.fail(server);
                } else if (station.getItem(0).getCount() >= 16) {
                    FuelFeedback.station(server, 16, TravelMessage.FUEL_FULL);
                    FuelFeedback.sound(server, pos, true, SoundSource.BLOCKS);
                } else {
                    var fuel = station.getItem(0);
                    if (fuel.isEmpty()) station.setItem(0, new ItemStack(Items.ENDER_PEARL));
                    else { fuel.grow(1); station.setChanged(); }
                    stack.consume(1, player);
                    level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, .5f, 1.1f);
                    FuelFeedback.station(server, station.getItem(0).getCount(), null);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return activate(level, pos, player);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.getOffhandItem().is(Items.ENDER_PEARL)) {
            return useItemOn(player.getOffhandItem(), state, level, pos, player, InteractionHand.OFF_HAND, hit);
        }
        return activate(level, pos, player);
    }
    private InteractionResult activate(Level level, BlockPos pos, Player player) {
        if (player instanceof ServerPlayer server && EquipReadiness.attemptedThisTick(server)) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof TeleportStationBlockEntity station) {
            if (station.isEmpty()) {
                if (EquipReadiness.claimAttempt(server)) {
                    FuelFeedback.station(server, 0, TravelMessage.NO_FUEL);
                    FuelFeedback.sound(server, pos, false, SoundSource.BLOCKS);
                }
                return InteractionResult.SUCCESS;
            }
            if (EquipReadiness.claimAttempt(server)) { station.removeItem(0, 1); TeleportService.attempt(server, station.destination(), dimensional, station.getItem(0).getCount()); }
        } else if (player instanceof ServerPlayer server) TravelMessage.STATION_UNAVAILABLE.fail(server);
        return InteractionResult.SUCCESS;
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) { return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos)); }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) { Containers.updateNeighboursAfterDestroy(state, level, pos); }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof TeleportStationBlockEntity station ? station.asItem() : new ItemStack(this));
    }
}
