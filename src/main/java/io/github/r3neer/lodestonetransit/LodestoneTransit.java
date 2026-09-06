package io.github.r3neer.lodestonetransit;

import com.mojang.serialization.Codec;
import io.github.r3neer.lodestonetransit.anchor.*;
import io.github.r3neer.lodestonetransit.block.*;
import io.github.r3neer.lodestonetransit.compat.AlexsMobsCompat;
import io.github.r3neer.lodestonetransit.item.*;
import io.github.r3neer.lodestonetransit.recipe.TransitRecipe;
import io.github.r3neer.lodestonetransit.teleport.TeleportDestination;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.Optional;

public final class LodestoneTransit implements ModInitializer {
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath("lodestone_transit", path); }
    public static final DataComponentType<TeleportDestination> DESTINATION = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("destination"), DataComponentType.<TeleportDestination>builder().persistent(TeleportDestination.CODEC).build());
    public static final DataComponentType<Integer> CHARGES = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("charges"), DataComponentType.<Integer>builder().persistent(Codec.intRange(0, 16)).build());
    private static Item.Properties itemProperties(String name) { return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name))).stacksTo(1); }
    private static BlockBehaviour.Properties blockProperties(String name) { return BlockBehaviour.Properties.ofFullCopy(Blocks.LODESTONE).pushReaction(net.minecraft.world.level.material.PushReaction.NORMAL).setId(ResourceKey.create(Registries.BLOCK, id(name))); }
    public static final Item TELEPORTER = Registry.register(BuiltInRegistries.ITEM, id("teleporter"), new TeleporterItem(itemProperties("teleporter").component(CHARGES, 0), false));
    public static final Item DIMENSIONAL_TELEPORTER = Registry.register(BuiltInRegistries.ITEM, id("dimensional_teleporter"), new TeleporterItem(itemProperties("dimensional_teleporter").component(CHARGES, 0), true));
    public static final Item CORE = Registry.register(BuiltInRegistries.ITEM, id("dimensional_core"), new Item(itemProperties("dimensional_core").durability(20)));
    public static final Block CALIBRATED_LODESTONE = Registry.register(BuiltInRegistries.BLOCK, id("calibrated_lodestone"), new CalibratedLodestoneBlock(blockProperties("calibrated_lodestone")));
    public static final TeleportStationBlock STATION = Registry.register(BuiltInRegistries.BLOCK, id("teleport_station"), new TeleportStationBlock(blockProperties("teleport_station"), false));
    public static final TeleportStationBlock DIMENSIONAL_STATION = Registry.register(BuiltInRegistries.BLOCK, id("dimensional_teleport_station"), new TeleportStationBlock(blockProperties("dimensional_teleport_station"), true));
    public static final BlockEntityType<TeleportStationBlockEntity> STATION_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("station"), FabricBlockEntityTypeBuilder.create(TeleportStationBlockEntity::new, STATION, DIMENSIONAL_STATION).build());
    @Override public void onInitialize() {
        Registry.register(BuiltInRegistries.ITEM, id("calibrated_lodestone"), new BlockItem(CALIBRATED_LODESTONE, itemProperties("calibrated_lodestone").stacksTo(64).useBlockDescriptionPrefix()));
        Registry.register(BuiltInRegistries.ITEM, id("teleport_station"), new StationItem(STATION, itemProperties("teleport_station").useBlockDescriptionPrefix(), false));
        Registry.register(BuiltInRegistries.ITEM, id("dimensional_teleport_station"), new StationItem(DIMENSIONAL_STATION, itemProperties("dimensional_teleport_station").useBlockDescriptionPrefix(), true));
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("transit"), TransitRecipe.SERIALIZER);
        AnchorNetworking.register();
        ReadinessNetworking.register();
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(EquipReadiness::tick));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> EquipReadiness.clear());
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> EquipReadiness.forget(handler.player));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> { entries.accept(TELEPORTER); entries.accept(DIMENSIONAL_TELEPORTER); if (!AlexsMobsCompat.available()) entries.accept(CORE); });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> { entries.accept(CALIBRATED_LODESTONE); entries.accept(STATION); entries.accept(DIMENSIONAL_STATION); });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!AnchorBlocks.isAnchor(world.getBlockState(hit.getBlockPos())) || player.isSpectator()) return InteractionResult.PASS;
            var stack = player.getItemInHand(hand);
            boolean naming = stack.is(Items.NAME_TAG) && stack.has(DataComponents.CUSTOM_NAME);
            boolean device = stack.getItem() instanceof TeleporterItem;
            if (!naming && !stack.is(Items.COMPASS) && !device) return InteractionResult.PASS;
            if (world instanceof ServerLevel level) {
                var registry = AnchorRegistry.get(level.getServer()); var anchor = registry.adopt(level, hit.getBlockPos());
                if (anchor == null) { io.github.r3neer.lodestonetransit.teleport.TravelMessage.ANCHOR_UNAVAILABLE.fail((ServerPlayer) player); return InteractionResult.SUCCESS; }
                if (naming) { registry.rename(anchor, stack.get(DataComponents.CUSTOM_NAME)); stack.consume(1, player); }
                else if (device) {
                    // Mutate the destination only; fuel, manual name and readiness remain intact.
                    stack.set(DESTINATION, TeleportDestination.anchor(anchor.id()));
                    DeviceAppearance.update(stack);
                    stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(anchor.position()), false));
                    (AnchorBlocks.calibrated(world.getBlockState(hit.getBlockPos()))
                        ? io.github.r3neer.lodestonetransit.teleport.TravelMessage.LINKED_CALIBRATED
                        : io.github.r3neer.lodestonetransit.teleport.TravelMessage.LINKED_UNCALIBRATED).show((ServerPlayer) player);
                }
                else {
                    var linked = stack.copyWithCount(1);
                    linked.set(DESTINATION, TeleportDestination.anchor(anchor.id())); linked.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(anchor.position()), false));
                    if (stack.getCount() == 1) player.setItemInHand(hand, linked);
                    else { stack.consume(1, player); if (!player.getInventory().add(linked)) player.drop(linked, false); }
                }
                world.playSound(null, hit.getBlockPos(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, .6f, 1);
            }
            return InteractionResult.SUCCESS;
        });
        // Catch pearl-in-main-hand before vanilla throws it; the offhand device owns the action.
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (hand == InteractionHand.MAIN_HAND && player.isShiftKeyDown() && player.getMainHandItem().is(Items.ENDER_PEARL) && player.getOffhandItem().getItem() instanceof TeleporterItem teleporter)
                return teleporter.use(world, player, InteractionHand.OFF_HAND);
            return InteractionResult.PASS;
        });
    }
}
