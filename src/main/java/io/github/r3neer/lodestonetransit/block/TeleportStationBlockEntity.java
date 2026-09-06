package io.github.r3neer.lodestonetransit.block;

import io.github.r3neer.lodestonetransit.LodestoneTransit;
import io.github.r3neer.lodestonetransit.teleport.TeleportDestination;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

/** Embedded item preserves arbitrary component metadata; fuel is a real one-slot container. */
public final class TeleportStationBlockEntity extends BlockEntity implements Container, net.minecraft.world.Nameable {
    private ItemStack embedded = ItemStack.EMPTY;
    private ItemStack fuel = ItemStack.EMPTY;
    private boolean pendingLightCheck = true;
    public TeleportStationBlockEntity(BlockPos pos, BlockState state) { super(LodestoneTransit.STATION_ENTITY, pos, state); }
    public TeleportDestination destination() { return embedded.getOrDefault(LodestoneTransit.DESTINATION, TeleportDestination.spawn()); }
    @Override public net.minecraft.network.chat.Component getName() {
        var custom = embedded.get(DataComponents.CUSTOM_NAME);
        if (custom != null) return custom;
        var d = destination();
        if (level instanceof net.minecraft.server.level.ServerLevel server && d != null && d.anchorId().isPresent()) {
            var anchor = io.github.r3neer.lodestonetransit.anchor.AnchorRegistry.get(server.getServer()).get(d.anchorId().get());
            if (anchor != null && anchor.name().isPresent()) return net.minecraft.network.chat.Component.translatable(getBlockState().is(LodestoneTransit.DIMENSIONAL_STATION) ? "transit.name.dimensional_station" : "transit.name.station", anchor.name().get());
        }
        return asItem().getHoverName();
    }
    @Override public net.minecraft.network.chat.Component getCustomName() { return embedded.get(DataComponents.CUSTOM_NAME); }
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    public ItemStack asItem() {
        var result = embedded.isEmpty() ? new ItemStack(getBlockState().getBlock()) : embedded.transmuteCopy(getBlockState().getBlock().asItem(), 1);
        result.remove(LodestoneTransit.CHARGES); result.remove(DataComponents.CONTAINER);
        return result;
    }
    @Override protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        embedded = new ItemStack(getBlockState().getBlock());
        var destination = components.get(LodestoneTransit.DESTINATION);
        if (destination != null) embedded.set(LodestoneTransit.DESTINATION, destination);
        var name = components.get(DataComponents.CUSTOM_NAME);
        if (name != null) embedded.set(DataComponents.CUSTOM_NAME, name);
        int charges = Math.clamp(components.getOrDefault(LodestoneTransit.CHARGES, 0), 0, 16);
        fuel = charges == 0 ? ItemStack.EMPTY : new ItemStack(Items.ENDER_PEARL, charges);
    }
    public void preserveItem(ItemStack placed) { embedded = placed.copyWithCount(1); embedded.remove(LodestoneTransit.CHARGES); setChanged(); }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        pendingLightCheck = true;
        embedded = input.read("embedded", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        fuel = input.read("fuel", ItemStack.CODEC).filter(s -> s.is(Items.ENDER_PEARL)).orElse(ItemStack.EMPTY);
        if (!fuel.isEmpty()) fuel.setCount(Math.min(16, fuel.getCount()));
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!embedded.isEmpty()) output.store("embedded", ItemStack.CODEC, embedded);
        if (!fuel.isEmpty()) output.store("fuel", ItemStack.CODEC, fuel);
    }
    @Override protected void collectImplicitComponents(DataComponentMap.Builder builder) { super.collectImplicitComponents(builder); if (!embedded.isEmpty()) builder.addAll(embedded.getComponents()); }
    @Override public int getContainerSize() { return 1; }
    @Override public int getMaxStackSize() { return 16; }
    @Override public boolean isEmpty() { return fuel.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 0 ? fuel : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int count) { if (slot != 0 || count <= 0) return ItemStack.EMPTY; var result = fuel.split(count); setChanged(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { if (slot != 0) return ItemStack.EMPTY; var result = fuel; fuel = ItemStack.EMPTY; setChanged(); return result; }
    @Override public void setItem(int slot, ItemStack stack) { if (slot != 0 || (!stack.isEmpty() && !canPlaceItem(slot, stack))) return; fuel = stack; if (!fuel.isEmpty()) fuel.setCount(Math.min(16, fuel.getCount())); setChanged(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && stack.is(Items.ENDER_PEARL); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { fuel = ItemStack.EMPTY; setChanged(); }
    /** State is derived from the real slot, including hopper changes and older saved chunks. */
    public void syncPearls() {
        if (level == null || level.isClientSide()) return;
        var state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof TeleportStationBlock)) return;
        int count = Math.clamp(fuel.getCount(), 0, 16);
        if (state.getValue(TeleportStationBlock.PEARLS) != count) {
            level.setBlock(worldPosition, state.setValue(TeleportStationBlock.PEARLS, count), 3);
        }
        // Older chunks can retain pre-lighting cached light with the same pearl state.
        if (pendingLightCheck) {
            pendingLightCheck = false;
            level.getChunkSource().getLightEngine().checkBlock(worldPosition);
        }
    }
    @Override public void setChanged() {
        super.setChanged();
        if (level != null) { syncPearls(); level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock()); }
    }
}
