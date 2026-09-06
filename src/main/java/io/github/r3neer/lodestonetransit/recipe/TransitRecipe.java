package io.github.r3neer.lodestonetransit.recipe;

import com.mojang.serialization.Codec;
import io.github.r3neer.lodestonetransit.LodestoneTransit;
import io.github.r3neer.lodestonetransit.anchor.CompassAnchors;
import io.github.r3neer.lodestonetransit.compat.AlexsMobsCompat;
import io.github.r3neer.lodestonetransit.teleport.TeleportDestination;
import java.util.Optional;
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
        else if (operation.equals("teleporter")) {
            if (!center.is(Items.COMPASS) && !center.is(Items.RECOVERY_COMPASS)) return false;
            if (level instanceof ServerLevel server) CompassAnchors.update(center, server);
        } else if (operation.equals("station")) { if (!center.is(LodestoneTransit.TELEPORTER) && !center.is(LodestoneTransit.DIMENSIONAL_TELEPORTER)) return false; }
        else return false;
        for (int i = 0; i < 9; i++) if (i != 4) {
            var expected = operation.equals("station") ? Items.CHISELED_STONE_BRICKS : operation.equals("teleporter") && i == 1 ? Items.AMETHYST_SHARD : Items.ENDER_EYE;
            if (!input.getItem(i).is(expected)) return false;
        }
        return true;
    }
    @Override public ItemStack assemble(CraftingInput input) {
        if (operation.equals("core")) return new ItemStack(LodestoneTransit.CORE);
        if (operation.equals("upgrade")) { for (var s : input.items()) if (s.is(LodestoneTransit.TELEPORTER)) return transfer(s, LodestoneTransit.DIMENSIONAL_TELEPORTER); return ItemStack.EMPTY; }
        var center = input.getItem(4);
        if (operation.equals("station")) return transfer(center, center.is(LodestoneTransit.DIMENSIONAL_TELEPORTER) ? LodestoneTransit.DIMENSIONAL_STATION.asItem() : LodestoneTransit.STATION.asItem());
        var result = transfer(center, LodestoneTransit.TELEPORTER);
        var d = center.get(LodestoneTransit.DESTINATION);
        if (center.is(Items.RECOVERY_COMPASS)) d = TeleportDestination.death();
        else if (d == null) {
            var tracker = center.get(DataComponents.LODESTONE_TRACKER);
            d = tracker == null ? TeleportDestination.spawn() : new TeleportDestination(TeleportDestination.Kind.LODESTONE_ANCHOR, Optional.empty(), tracker.target());
        }
        result.set(LodestoneTransit.DESTINATION, d); result.set(LodestoneTransit.CHARGES, 0); return result;
    }
    private static ItemStack transfer(ItemStack source, Item target) { return source.transmuteCopy(target, 1); }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var result = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        if (operation.equals("upgrade")) for (int i = 0; i < input.size(); i++) if (AlexsMobsCompat.catalyst(input.getItem(i))) result.set(i, AlexsMobsCompat.remainder(input.getItem(i)));
        return result;
    }
    @Override public RecipeSerializer<TransitRecipe> getSerializer() { return SERIALIZER; }
}
