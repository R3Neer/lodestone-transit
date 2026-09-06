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
                LodestoneTransit.STATION.asItem(), LodestoneTransit.DIMENSIONAL_STATION.asItem(), LodestoneTransit.DIMENSIONAL_TELEPORTER, Items.LODESTONE);
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
            check(AbstractContainerMenu.getRedstoneSignalFromContainer(station) == 1 + (int)Math.floor(14.0 * count / 16), "vanilla comparator " + count);
        }
        check(station.removeItem(0, 1).getCount() == 1 && station.getItem(0).getCount() == 15, "single debit");
        var device = new ItemStack(LodestoneTransit.STATION); device.set(LodestoneTransit.DESTINATION, TeleportDestination.anchor(UUID.randomUUID())); device.set(DataComponents.CUSTOM_NAME, Component.literal("Exit"));
        station.preserveItem(device);
        var restored = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(h.absolutePos(pos), LodestoneTransit.STATION.defaultBlockState(), station.saveWithFullMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        check(restored instanceof TeleportStationBlockEntity copy && copy.getItem(0).getCount() == 15 && copy.destination().equals(station.destination()), "BE save/load preserves fuel and target");
        check(!station.asItem().has(LodestoneTransit.CHARGES) && station.asItem().get(DataComponents.CUSTOM_NAME).getString().equals("Exit"), "drop preserves name but excludes fuel");
        h.succeed();
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
