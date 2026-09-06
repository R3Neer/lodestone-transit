package io.github.r3neer.lodestonetransit.item;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** Both HUD indicators consume this same authoritative per-hand snapshot. */
public final class ClientReadiness {
    private static ReadinessNetworking.Update state = new ReadinessNetworking.Update(-1, -1, -1);
    public static void accept(ReadinessNetworking.Update update) { state = update; }
    public static void clear() { state = new ReadinessNetworking.Update(-1, -1, -1); }
    public static float remaining(ItemStack stack) {
        var player = Minecraft.getInstance().player;
        if (player == null || !(stack.getItem() instanceof TeleporterItem)) return 0;
        if (stack == player.getMainHandItem()) return state.slot() == player.getInventory().getSelectedSlot() && state.main() >= 0 ? state.main() / 20f : 1;
        if (stack == player.getOffhandItem()) return state.off() >= 0 ? state.off() / 20f : 1;
        return 0;
    }
    public static float progress() {
        var player = Minecraft.getInstance().player;
        if (player == null) return -1;
        if (player.getMainHandItem().getItem() instanceof TeleporterItem) return 1 - remaining(player.getMainHandItem());
        if (player.getOffhandItem().getItem() instanceof TeleporterItem) return 1 - remaining(player.getOffhandItem());
        return -1;
    }
}
