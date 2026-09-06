package io.github.r3neer.lodestonetransit;

import io.github.r3neer.lodestonetransit.anchor.*;
import io.github.r3neer.lodestonetransit.block.*;
import io.github.r3neer.lodestonetransit.recipe.TransitRecipe;
import io.github.r3neer.lodestonetransit.teleport.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class TransitGameTests {
    @GameTest(padding = 24) public void calibratedAndOrdinaryLinkingHaveDifferentTravelPermissions(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var ordinary = h.absolutePos(new BlockPos(1, 1, 1));
        var calibrated = ordinary.east(3);
        h.getLevel().setBlockAndUpdate(ordinary, Blocks.LODESTONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(calibrated, LodestoneTransit.CALIBRATED_LODESTONE.defaultBlockState());
        for (var item : List.of(LodestoneTransit.TELEPORTER, LodestoneTransit.DIMENSIONAL_TELEPORTER)) {
            for (var hand : net.minecraft.world.InteractionHand.values()) {
                var stack = new ItemStack(item);
                stack.set(LodestoneTransit.DESTINATION, TeleportDestination.death());
                stack.set(LodestoneTransit.CHARGES, 3);
                stack.set(DataComponents.CUSTOM_NAME, Component.literal("My device"));
                player.setItemInHand(hand, stack);
                for (var pos : List.of(ordinary, calibrated)) {
                    var result = net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.invoker().interact(player, h.getLevel(), hand,
                        new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                    check(result.consumesAction(), "linking consumes interaction instead of triggering travel");
                    check(stack.get(LodestoneTransit.CHARGES) == 3, "linking preserves fuel");
                    check(stack.getHoverName().getString().equals("My device"), "linking preserves manual name");
                    var resolved = TeleportResolver.resolveDetailed(player, stack.get(LodestoneTransit.DESTINATION));
                    check(pos.equals(ordinary) ? resolved.failure() == TravelMessage.UNCALIBRATED : resolved.target().pos().equals(calibrated), "normal links cannot travel; calibrated links can");
                    check(stack.get(DataComponents.ITEM_MODEL).equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item)), "recovery art resets on relink");
                }
            }
        }
        check(TeleportResolver.resolveDetailed(player, null).failure() == TravelMessage.NO_DESTINATION, "missing target has its own reason");
        player.setLastDeathLocation(Optional.empty());
        check(TeleportResolver.resolveDetailed(player, TeleportDestination.death()).failure() == TravelMessage.NO_DEATH, "missing death has its own reason");
        check(TeleportResolver.resolveDetailed(player, TeleportDestination.anchor(UUID.randomUUID())).failure() == TravelMessage.ANCHOR_UNAVAILABLE, "unknown identity has its own reason");
        h.getLevel().setBlockAndUpdate(calibrated, Blocks.AIR.defaultBlockState());
        check(TeleportResolver.resolveDetailed(player, player.getMainHandItem().get(LodestoneTransit.DESTINATION)).failure() == TravelMessage.ANCHOR_UNAVAILABLE, "calibrated destruction invalidates link");
        h.succeed();
    }
    @GameTest(padding = 24) public void creativeDefaultsAndLegacyRecoveryAppearance(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        for (var item : List.of(LodestoneTransit.TELEPORTER, LodestoneTransit.DIMENSIONAL_TELEPORTER)) {
            var device = new ItemStack(item);
            check(device.get(LodestoneTransit.DESTINATION).equals(TeleportDestination.spawn()), "creative device targets spawn");
            check(TeleportResolver.resolve(player, device.get(LodestoneTransit.DESTINATION)).equals(h.getLevel().getServer().getRespawnData().globalPos()), "target matches vanilla world spawn");
            device.remove(LodestoneTransit.DESTINATION);
            CompassAnchors.update(device, h.getLevel());
            check(device.get(LodestoneTransit.DESTINATION).equals(TeleportDestination.spawn()), "older unconfigured device is repaired");
            device.set(DataComponents.LODESTONE_TRACKER, new net.minecraft.world.item.component.LodestoneTracker(Optional.empty(), true));
            CompassAnchors.update(device, h.getLevel());
            check(TeleportResolver.resolveDetailed(player, device.get(LodestoneTransit.DESTINATION)).failure() == TravelMessage.ANCHOR_UNAVAILABLE, "broken tracker is not silently redirected to spawn");
            device.set(LodestoneTransit.DESTINATION, TeleportDestination.death());
            device.set(LodestoneTransit.CHARGES, 3);
            device.set(DataComponents.CUSTOM_NAME, Component.literal("My recovery"));
            device.set(DataComponents.ITEM_MODEL, LodestoneTransit.id("recovery_teleporter"));
            io.github.r3neer.lodestonetransit.item.DeviceAppearance.update(device);
            check(device.get(DataComponents.ITEM_MODEL).equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item)), "existing recovery uses normal model");
            check(device.get(LodestoneTransit.DESTINATION).equals(TeleportDestination.death()) && device.get(LodestoneTransit.CHARGES) == 3 && device.getHoverName().getString().equals("My recovery"), "appearance migration preserves destination, fuel and manual name");
        }
        h.succeed();
    }
    @GameTest(padding = 24) public void recipeBookMetadataAndRecoveryNames(GameTestHelper h) {
        for (var operation : List.of("teleporter", "recovery", "station", "dimensional_station", "upgrade", "core")) {
            var recipe = new TransitRecipe(operation);
            check(!recipe.isSpecial() && !recipe.placementInfo().isImpossibleToPlace() && !recipe.display().isEmpty(), "recipe is discoverable and placeable: " + operation);
        }
        var recovery = new TransitRecipe("recovery").assemble(grid(new ItemStack(Items.RECOVERY_COMPASS), Items.ENDER_EYE, true));
        check(recovery.getHoverName().getString().equals("Recovery Teleporter"), "dedicated recovery name");
        check(recovery.get(DataComponents.ITEM_MODEL).equals(LodestoneTransit.id("teleporter")), "recovery shares the normal device model");
        h.succeed();
    }
    @GameTest(padding = 24) public void loadedRecipesWorkThroughCraftingTable(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var menu = new net.minecraft.world.inventory.CraftingMenu(1, player.getInventory(),
                net.minecraft.world.inventory.ContainerLevelAccess.create(h.getLevel(), h.absolutePos(BlockPos.ZERO)));
        boolean alexsMobs = io.github.r3neer.lodestonetransit.compat.AlexsMobsCompat.available();
        var catalyst = alexsMobs ? net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("alexsmobs", "dimensional_carver")) : LodestoneTransit.CORE;
        var inputs = List.of(
                grid(new ItemStack(Items.COMPASS), Items.ENDER_EYE, true),
                grid(new ItemStack(Items.RECOVERY_COMPASS), Items.ENDER_EYE, true),
                grid(new ItemStack(Items.NETHER_STAR), Items.ENDER_EYE, false),
                grid(new ItemStack(LodestoneTransit.TELEPORTER), Items.CHISELED_STONE_BRICKS, false),
                grid(new ItemStack(LodestoneTransit.DIMENSIONAL_TELEPORTER), Items.CHISELED_STONE_BRICKS, false),
                CraftingInput.of(2, 1, List.of(new ItemStack(LodestoneTransit.TELEPORTER), new ItemStack(catalyst))),
                grid(new ItemStack(Items.IRON_INGOT), Items.CHISELED_STONE_BRICKS, true));
        var expected = List.of(LodestoneTransit.TELEPORTER, LodestoneTransit.TELEPORTER, LodestoneTransit.CORE,
                LodestoneTransit.STATION.asItem(), LodestoneTransit.DIMENSIONAL_STATION.asItem(), LodestoneTransit.DIMENSIONAL_TELEPORTER, LodestoneTransit.CALIBRATED_LODESTONE.asItem());
        for (int n = 0; n < inputs.size(); n++) {
            if (n == 2 && alexsMobs) continue; // The fallback core recipe is deliberately disabled.
            var input = inputs.get(n);
            for (var slot : menu.getInputGridSlots()) slot.set(ItemStack.EMPTY);
            for (int y = 0; y < input.height(); y++) for (int x = 0; x < input.width(); x++)
                menu.getInputGridSlots().get(y * 3 + x).set(input.getItem(x, y).copy());
            check(menu.getResultSlot().getItem().is(expected.get(n)), "loaded crafting-table recipe " + n);
            var taken = menu.quickMoveStack(player, 0);
            check(taken.is(expected.get(n)), "crafting-table output can be taken " + n);
            check(menu.getResultSlot().getItem().isEmpty(), "ingredients consumed " + n);
        }
        h.succeed();
    }
    private static void check(boolean condition, String message) { if (!condition) throw new net.minecraft.gametest.framework.GameTestAssertException(Component.literal(message),0); }
    private static CraftingInput grid(ItemStack center, Item outer, boolean amethyst) {
        var list = new ArrayList<ItemStack>(); for (int i = 0; i < 9; i++) list.add(i == 4 ? center : new ItemStack(amethyst && i == 1 ? Items.AMETHYST_SHARD : outer));
        return CraftingInput.of(3, 3, list);
    }
    @GameTest(padding = 24) public void recipesPreserveComponents(GameTestHelper h) {
        var id = UUID.randomUUID(); var compass = new ItemStack(Items.COMPASS);
        compass.set(LodestoneTransit.DESTINATION, TeleportDestination.anchor(id)); compass.set(DataComponents.CUSTOM_NAME, Component.literal("Exit"));
        var recipe = new TransitRecipe("teleporter"); var input = grid(compass, Items.ENDER_EYE, true);
        check(recipe.matches(input, h.getLevel()), "linked compass recipe");
        var device = recipe.assemble(input); check(device.get(LodestoneTransit.DESTINATION).anchorId().orElseThrow().equals(id), "anchor transfer");
        check(device.get(DataComponents.CUSTOM_NAME).getString().equals("Exit"), "manual name transfer");
        var recovery = recipe.assemble(grid(new ItemStack(Items.RECOVERY_COMPASS), Items.ENDER_EYE, true));
        check(recovery.get(LodestoneTransit.DESTINATION).kind() == TeleportDestination.Kind.LAST_DEATH, "recovery destination");
        var spawn = recipe.assemble(grid(new ItemStack(Items.COMPASS), Items.ENDER_EYE, true));
        check(spawn.get(LodestoneTransit.DESTINATION).kind() == TeleportDestination.Kind.WORLD_SPAWN, "spawn destination");
        device.set(LodestoneTransit.CHARGES, 4);
        var stationRecipe = new TransitRecipe("station"); var stationInput = grid(device, Items.CHISELED_STONE_BRICKS, false);
        check(stationRecipe.matches(stationInput, h.getLevel()), "station recipe"); var station = stationRecipe.assemble(stationInput);
        check(station.get(LodestoneTransit.CHARGES) == 4 && station.get(LodestoneTransit.DESTINATION).equals(device.get(LodestoneTransit.DESTINATION)), "station data and fuel");
        var upgrade = new TransitRecipe("upgrade"); var core = new ItemStack(io.github.r3neer.lodestonetransit.compat.AlexsMobsCompat.available() ? net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("alexsmobs", "dimensional_carver")) : LodestoneTransit.CORE);
        var upgradeInput = CraftingInput.of(2, 1, List.of(device, core)); check(upgrade.matches(upgradeInput, h.getLevel()), "upgrade match");
        var dimensional = upgrade.assemble(upgradeInput);
        check(dimensional.is(LodestoneTransit.DIMENSIONAL_TELEPORTER) && dimensional.get(LodestoneTransit.CHARGES) == 4, "upgrade preserves charges");
        check(upgrade.getRemainingItems(upgradeInput).get(1).getDamageValue() == 1, "exactly one catalyst damage");
        core.setDamageValue(core.getMaxDamage()-1); check(upgrade.getRemainingItems(upgradeInput).get(1).isEmpty(), "last catalyst use breaks");
        h.succeed();
    }
    @GameTest(padding = 24) public void stationsInheritSpawnIncludingCreativeAndLegacy(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(1,1,1));
        for (boolean dimensional : new boolean[]{false,true}) {
            var block = dimensional ? LodestoneTransit.DIMENSIONAL_STATION : LodestoneTransit.STATION;
            var portable = new ItemStack(dimensional ? LodestoneTransit.DIMENSIONAL_TELEPORTER : LodestoneTransit.TELEPORTER);
            // Spawn is an item prototype component, not an explicit stack patch.
            var inputs = new ArrayList<ItemStack>();
            for (int i=0;i<9;i++) inputs.add(i==4 ? portable : new ItemStack(Items.CHISELED_STONE_BRICKS));
            var recipe = new TransitRecipe(dimensional ? "dimensional_station" : "station");
            var input = CraftingInput.of(3,3,inputs);
            check(recipe.matches(input,level), "spawn station recipe matches");
            for (var item : List.of(recipe.assemble(input), new ItemStack(block))) {
                check(TeleportDestination.spawn().equals(item.get(LodestoneTransit.DESTINATION)), "crafted and creative stations target spawn");
                level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(pos,block.defaultBlockState());
                block.setPlacedBy(level,pos,level.getBlockState(pos),player,item);
                var station = (TeleportStationBlockEntity)level.getBlockEntity(pos);
                check(TeleportResolver.resolve(player,station.destination()).equals(level.getServer().getRespawnData().globalPos()), "placed station resolves current world spawn");
                var restored = (TeleportStationBlockEntity)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,level.getBlockState(pos),station.saveWithFullMetadata(level.registryAccess()),level.registryAccess());
                check(restored.destination().equals(TeleportDestination.spawn()) && restored.asItem().get(LodestoneTransit.DESTINATION).equals(TeleportDestination.spawn()), "save/load and drop preserve spawn");
                item.remove(LodestoneTransit.DESTINATION);
                station.preserveItem(item);
                check(station.destination().equals(TeleportDestination.spawn()), "legacy station with missing destination targets spawn");
                var broken = TeleportDestination.anchor(UUID.randomUUID());
                item.set(LodestoneTransit.DESTINATION,broken);
                station.preserveItem(item);
                check(station.destination().equals(broken) && TeleportResolver.resolve(player,station.destination())==null, "broken link never falls back to spawn");
            }
        }
        h.succeed();
    }
    @GameTest(padding = 24) public void stationsLoadOffhandPearlsBeforeTravel(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(1,1,1));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.UP,pos,false);
        for (var block : new TeleportStationBlock[]{LodestoneTransit.STATION,LodestoneTransit.DIMENSIONAL_STATION}) {
            level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos,block.defaultBlockState());
            var station = (TeleportStationBlockEntity)level.getBlockEntity(pos);
            for (int initial : new int[]{0,3,15,16}) for (int main : new int[]{0,1,2}) {
                io.github.r3neer.lodestonetransit.item.EquipReadiness.forget(player);
                station.setItem(0,initial==0 ? ItemStack.EMPTY : new ItemStack(Items.ENDER_PEARL,initial));
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,main==0 ? ItemStack.EMPTY : new ItemStack(main==1 ? Items.STICK : Items.ENDER_PEARL,2));
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.ENDER_PEARL,2));
                var origin = player.position(); var health = player.getHealth();
                if (main==0) level.getBlockState(pos).useWithoutItem(level,player,hit);
                else level.getBlockState(pos).useItemOn(player.getMainHandItem(),level,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
                // A second hand callback in the same tick must not add or consume another pearl.
                level.getBlockState(pos).useItemOn(player.getOffhandItem(),level,player,net.minecraft.world.InteractionHand.OFF_HAND,hit);
                int inserted = initial==16 ? 0 : 1;
                check(station.getItem(0).getCount()==initial+inserted, "either hand inserts exactly one pearl, including empty/full boundaries");
                check(player.getOffhandItem().getCount()==2-(main==2 ? 0 : inserted), "offhand debited only when it supplies the pearl");
                if (main!=0) check(player.getMainHandItem().getCount()==2-(main==2 ? inserted : 0), "main-hand pearl takes priority; other items unchanged");
                check(player.position().equals(origin) && player.getHealth()==health, "loading never starts travel or pearl damage");
            }
        }
        io.github.r3neer.lodestonetransit.item.EquipReadiness.forget(player);
        h.succeed();
    }
    @GameTest(padding = 24) public void anchorIdentityAndMovement(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(1, 1, 1)); var level = h.getLevel();
        level.setBlockAndUpdate(pos, Blocks.LODESTONE.defaultBlockState()); var registry = AnchorRegistry.get(level.getServer());
        var first = registry.adopt(level, pos); check(first.id().equals(registry.adopt(level, pos).id()), "idempotent adoption");
        registry.rename(first, Component.literal("Home")); check(registry.get(first.id()).name().orElseThrow().getString().equals("Home"), "anchor rename");
        registry.beginMove(level, List.of(pos), Direction.EAST); level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); level.setBlockAndUpdate(pos.east(), Blocks.LODESTONE.defaultBlockState()); registry.endMove();
        check(registry.get(first.id()).valid() && registry.get(first.id()).position().pos().equals(pos.east()), "movement preserves identity");
        level.setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState()); check(!registry.get(first.id()).valid(), "destruction invalidates");
        level.setBlockAndUpdate(pos.east(), Blocks.LODESTONE.defaultBlockState()); var replacement = registry.adopt(level, pos.east());
        check(!replacement.id().equals(first.id()) && !registry.get(first.id()).valid(), "replacement cannot resurrect old link");
        h.succeed();
    }
    @GameTest(padding = 24) public void stationInventoryComparatorAndPersistence(GameTestHelper h) {
        var pos = new BlockPos(1, 1, 1); h.setBlock(pos, LodestoneTransit.STATION);
        var station = (TeleportStationBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        check(AbstractContainerMenu.getRedstoneSignalFromContainer(station) == 0, "empty comparator");
        check(!station.canPlaceItem(0, new ItemStack(Items.DIAMOND)), "reject other fuel");
        for (int count = 1; count <= 16; count++) {
            station.setItem(0, new ItemStack(Items.ENDER_PEARL, count));
            check(h.getLevel().getBlockState(h.absolutePos(pos)).getValue(io.github.r3neer.lodestonetransit.block.TeleportStationBlock.PEARLS) == count, "exact visible pearl sockets " + count);
            check(AbstractContainerMenu.getRedstoneSignalFromContainer(station) == 1 + (int)Math.floor(14.0 * count / 16), "vanilla comparator " + count);
        }
        check(station.removeItem(0, 1).getCount() == 1 && station.getItem(0).getCount() == 15, "single debit");
        check(h.getLevel().getBlockState(h.absolutePos(pos)).getValue(io.github.r3neer.lodestonetransit.block.TeleportStationBlock.PEARLS) == 15, "socket disappears on extraction");
        var device = new ItemStack(LodestoneTransit.STATION); device.set(LodestoneTransit.DESTINATION, TeleportDestination.anchor(UUID.randomUUID())); device.set(DataComponents.CUSTOM_NAME, Component.literal("Exit"));
        station.preserveItem(device);
        var restored = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(h.absolutePos(pos), LodestoneTransit.STATION.defaultBlockState(), station.saveWithFullMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        check(restored instanceof TeleportStationBlockEntity copy && copy.getItem(0).getCount() == 15 && copy.destination().equals(station.destination()), "BE save/load preserves fuel and target");
        check(!station.asItem().has(LodestoneTransit.CHARGES) && station.asItem().get(DataComponents.CUSTOM_NAME).getString().equals("Exit"), "drop preserves name but excludes fuel");
        station.removeItemNoUpdate(0);
        check(h.getLevel().getBlockState(h.absolutePos(pos)).getValue(io.github.r3neer.lodestonetransit.block.TeleportStationBlock.PEARLS) == 0, "bulk extraction clears sockets");
        station.setItem(0, new ItemStack(Items.ENDER_PEARL, 6));
        station.getItem(0).shrink(1);
        station.syncPearls();
        check(h.getLevel().getBlockState(h.absolutePos(pos)).getValue(io.github.r3neer.lodestonetransit.block.TeleportStationBlock.PEARLS) == 5, "in-place slot mutation reconciled");
        h.succeed();
    }
    // Multiple asynchronous lighting updates need more than the default 20-tick budget.
    @GameTest(structure = "lodestone_transit_test:lighting", padding = 24, maxTicks = 200) public void stationLightTracksCompletePearlGroups(GameTestHelper h) {
        int[] normalLight = {0,0,0,0,1,1,1,1,2,2,2,2,3,3,3,3,4};
        int[] dimensionalLight = {0,0,0,0,2,2,2,2,4,4,4,4,6,6,6,6,8};
        var blocks = new TeleportStationBlock[]{LodestoneTransit.STATION, LodestoneTransit.DIMENSIONAL_STATION};
        var expected = new int[][]{normalLight, dimensionalLight};
        var positions = new BlockPos[]{h.absolutePos(new BlockPos(2,3,2)), h.absolutePos(new BlockPos(2,3,14))};
        var level = h.getLevel();
        for (int i=0; i<blocks.length; i++) {
            for (int count=0; count<=16; count++) {
                h.assertValueEqual(blocks[i].defaultBlockState().setValue(TeleportStationBlock.PEARLS,count).getLightEmission(), expected[i][count], "station emission for " + count + " pearls");
            }
            for (var p : BlockPos.betweenClosed(positions[i].offset(-1,-1,-1), positions[i].offset(1,1,1))) level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(positions[i], blocks[i].defaultBlockState());
        }
        var sequence = h.startSequence();
        // Exercise the real inventory -> block state -> lighting engine path in both directions.
        for (int count : new int[]{0,3,4,7,8,11,12,15,16,15,11,7,3,0}) {
            sequence.thenExecute(() -> {
                for (var pos : positions) {
                    var station = (TeleportStationBlockEntity)level.getBlockEntity(pos);
                    int previous = station.getItem(0).getCount();
                    if (count < previous) station.removeItem(0, previous-count);
                    else station.setItem(0, count == 0 ? ItemStack.EMPTY : new ItemStack(Items.ENDER_PEARL,count));
                }
            }).thenWaitUntil(() -> {
                for (int i=0; i<positions.length; i++) {
                    int light = expected[i][count];
                    h.assertValueEqual(level.getBlockState(positions[i]).getLightEmission(), light, "updated source emission");
                    h.assertValueEqual(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, positions[i]), light, "propagated source light");
                    h.assertValueEqual(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, positions[i].east()), Math.max(0,light-1), "light in neighboring air");
                }
            });
        }
        sequence.thenSucceed();
    }
    @GameTest(structure = "lodestone_transit_test:lighting", padding = 24, maxTicks = 200) public void loadedStationsRefreshPreviouslyDarkLightCache(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(2,3,2));
        var sequence = h.startSequence();
        for (var block : new TeleportStationBlock[]{LodestoneTransit.STATION, LodestoneTransit.DIMENSIONAL_STATION}) {
            int expected = block == LodestoneTransit.STATION ? 4 : 8;
            sequence.thenExecute(() -> {
                for (var p : BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1))) level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(pos, block.defaultBlockState());
            }).thenWaitUntil(() -> h.assertValueEqual(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,pos),0,"old unlit cache"))
                .thenExecute(() -> {
                    var state = block.defaultBlockState().setValue(TeleportStationBlock.PEARLS,16);
                    var saved = new TeleportStationBlockEntity(pos,state);
                    saved.setItem(0,new ItemStack(Items.ENDER_PEARL,16));
                    var restored = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,state,saved.saveWithFullMetadata(level.registryAccess()),level.registryAccess());
                    // Simulate loading the saved palette with unchanged pearls=16 and old zero light.
                    // Bypass normal placement notifications only inside this regression test.
                    var chunk = level.getChunkAt(pos);
                    chunk.getSection(chunk.getSectionIndex(pos.getY())).setBlockState(pos.getX() & 15,pos.getY() & 15,pos.getZ() & 15,state);
                    level.removeBlockEntity(pos);
                    level.setBlockEntity(restored);
                    h.assertValueEqual(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,pos),0,"cached light before first tick");
                }).thenWaitUntil(() -> {
                    h.assertValueEqual(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,pos),expected,"loaded station refreshes source");
                    h.assertValueEqual(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,pos.east()),expected-1,"loaded station refreshes neighbors");
                });
        }
        sequence.thenSucceed();
    }
    @GameTest(padding = 24) public void landingUsesActualGeometry(GameTestHelper h) {
        var pig = h.spawn(EntityTypes.PIG, 1, 2, 1); var target = pig.position().add(8, 0, 0); var world = h.getLevel();
        var floor = BlockPos.containing(target).below();
        for (var p : BlockPos.betweenClosed(floor.offset(-5, 0, -5), floor.offset(5, 0, 5))) world.setBlockAndUpdate(p, Blocks.STONE.defaultBlockState());
        check(SafeLandingFinder.find(world, pig, target, 5, List.of()).isPresent(), "open platform works");
        for (var p : BlockPos.betweenClosed(floor.offset(-6, -5, -6), floor.offset(6, 9, 6))) world.setBlockAndUpdate(p, Blocks.STONE.defaultBlockState());
        check(SafeLandingFinder.find(world, pig, target, 5, List.of()).isEmpty(), "solid destination fails within fixed radius");
        h.succeed();
    }
}
