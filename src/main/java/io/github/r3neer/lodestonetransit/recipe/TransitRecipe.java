package io.github.r3neer.lodestonetransit.recipe;

import com.mojang.serialization.Codec;
import io.github.r3neer.lodestonetransit.LodestoneTransit;
import io.github.r3neer.lodestonetransit.anchor.CompassAnchors;
import io.github.r3neer.lodestonetransit.compat.AlexsMobsCompat;
import io.github.r3neer.lodestonetransit.teleport.TeleportDestination;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.world.item.crafting.display.*;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** All component transfers and catalyst remainders are resolved by server crafting. */
public final class TransitRecipe extends CustomRecipe {
    public static final RecipeSerializer<TransitRecipe> SERIALIZER = new RecipeSerializer<>(Codec.STRING.fieldOf("operation").xmap(TransitRecipe::new, r -> r.operation), ByteBufCodecs.STRING_UTF8.map(TransitRecipe::new, r -> r.operation).cast());
    private final String operation;
    public TransitRecipe(String operation) { this.operation = operation; }
    private boolean portable() { return operation.equals("teleporter") || operation.equals("recovery"); }
    private boolean station() { return operation.equals("station") || operation.equals("dimensional_station"); }
    private Item catalyst() { return AlexsMobsCompat.available() ? net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("alexsmobs", "dimensional_carver")) : LodestoneTransit.CORE; }
    private List<Ingredient> ingredients() {
        if (operation.equals("upgrade")) return List.of(Ingredient.of(LodestoneTransit.TELEPORTER), Ingredient.of(catalyst()));
        Item center = switch (operation) {
            case "teleporter" -> Items.COMPASS;
            case "recovery" -> Items.RECOVERY_COMPASS;
            case "station" -> LodestoneTransit.TELEPORTER;
            case "dimensional_station" -> LodestoneTransit.DIMENSIONAL_TELEPORTER;
            case "core" -> Items.NETHER_STAR;
            default -> throw new IllegalArgumentException("Unknown transit recipe: " + operation);
        };
        var result = new ArrayList<Ingredient>();
        for (int i = 0; i < 9; i++) result.add(Ingredient.of(i == 4 ? center : station() ? Items.CHISELED_STONE_BRICKS : portable() && i == 1 ? Items.AMETHYST_SHARD : Items.ENDER_EYE));
        return result;
    }
    @Override public boolean isSpecial() { return false; }
    @Override public boolean showNotification() { return true; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.create(ingredients()); }
    @Override public List<RecipeDisplay> display() {
        Item result = switch (operation) {
            case "core" -> LodestoneTransit.CORE;
            case "upgrade" -> LodestoneTransit.DIMENSIONAL_TELEPORTER;
            case "station" -> LodestoneTransit.STATION.asItem();
            case "dimensional_station" -> LodestoneTransit.DIMENSIONAL_STATION.asItem();
            default -> LodestoneTransit.TELEPORTER;
        };
        var preview = new ItemStack(result);
        if (portable()) preview.set(LodestoneTransit.DESTINATION, operation.equals("recovery") ? TeleportDestination.death() : TeleportDestination.spawn());
        io.github.r3neer.lodestonetransit.item.DeviceAppearance.update(preview);
        var output = new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(preview));
        var table = Ingredient.of(Items.CRAFTING_TABLE).display();
        var inputs = ingredients().stream().map(Ingredient::display).toList();
        return List.of(operation.equals("upgrade") ? new ShapelessCraftingRecipeDisplay(inputs, output, table) : new ShapedCraftingRecipeDisplay(3, 3, inputs, output, table));
    }
    @Override public boolean matches(CraftingInput input, Level level) {
        if (operation.equals("upgrade")) {
            if (input.ingredientCount() != 2) return false;
            boolean device = false, catalyst = false;
            for (int i = 0; i < input.size(); i++) { var s = input.getItem(i); if (s.isEmpty()) continue; if (s.is(LodestoneTransit.TELEPORTER) && !device) device = true; else if (AlexsMobsCompat.catalyst(s) && !catalyst) catalyst = true; else return false; }
            return device && catalyst;
        }
        if (input.width() != 3 || input.height() != 3 || input.ingredientCount() != 9) return false;
        var center = input.getItem(4);
        if (operation.equals("core")) { if (AlexsMobsCompat.available() || !center.is(Items.NETHER_STAR)) return false; }
        else if (portable()) {
            if (!center.is(operation.equals("recovery") ? Items.RECOVERY_COMPASS : Items.COMPASS)) return false;
            if (level instanceof ServerLevel server) CompassAnchors.update(center, server);
        } else if (station()) { if (!center.is(operation.equals("dimensional_station") ? LodestoneTransit.DIMENSIONAL_TELEPORTER : LodestoneTransit.TELEPORTER)) return false; }
        else return false;
        for (int i = 0; i < 9; i++) if (i != 4) {
            var expected = station() ? Items.CHISELED_STONE_BRICKS : portable() && i == 1 ? Items.AMETHYST_SHARD : Items.ENDER_EYE;
            if (!input.getItem(i).is(expected)) return false;
        }
        return true;
    }
    @Override public ItemStack assemble(CraftingInput input) {
        if (operation.equals("core")) return new ItemStack(LodestoneTransit.CORE);
        if (operation.equals("upgrade")) { for (var s : input.items()) if (s.is(LodestoneTransit.TELEPORTER)) return transfer(s, LodestoneTransit.DIMENSIONAL_TELEPORTER); return ItemStack.EMPTY; }
        var center = input.getItem(4);
        if (station()) return transfer(center, center.is(LodestoneTransit.DIMENSIONAL_TELEPORTER) ? LodestoneTransit.DIMENSIONAL_STATION.asItem() : LodestoneTransit.STATION.asItem());
        var result = transfer(center, LodestoneTransit.TELEPORTER);
        var d = center.get(LodestoneTransit.DESTINATION);
        if (center.is(Items.RECOVERY_COMPASS)) d = TeleportDestination.death();
        else if (d == null) {
            var tracker = center.get(DataComponents.LODESTONE_TRACKER);
            d = tracker == null ? TeleportDestination.spawn() : new TeleportDestination(TeleportDestination.Kind.LODESTONE_ANCHOR, Optional.empty(), tracker.target());
        }
        result.set(LodestoneTransit.DESTINATION, d); result.set(LodestoneTransit.CHARGES, 0); io.github.r3neer.lodestonetransit.item.DeviceAppearance.update(result); return result;
    }
    private static ItemStack transfer(ItemStack source, Item target) { var result = source.transmuteCopy(target, 1); io.github.r3neer.lodestonetransit.item.DeviceAppearance.update(result); return result; }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var result = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        if (operation.equals("upgrade")) for (int i = 0; i < input.size(); i++) if (AlexsMobsCompat.catalyst(input.getItem(i))) result.set(i, AlexsMobsCompat.remainder(input.getItem(i)));
        return result;
    }
    @Override public RecipeSerializer<TransitRecipe> getSerializer() { return SERIALIZER; }
}
