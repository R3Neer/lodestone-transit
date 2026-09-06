package io.github.r3neer.lodestonetransit;

import io.github.r3neer.lodestonetransit.anchor.AnchorRegistry;
import io.github.r3neer.lodestonetransit.name.DestinationNaming;
import io.github.r3neer.lodestonetransit.teleport.TeleportDestination;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class TransitClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (var singleplayer = context.worldBuilder().create()) {
            var anchorId = singleplayer.getServer().computeOnServer(server -> {
                var level=server.overworld();
                for(var pos:BlockPos.betweenClosed(new BlockPos(-6,79,-6),new BlockPos(6,84,8))) level.setBlockAndUpdate(pos,pos.getY()==79 ? Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());
                var pos=new BlockPos(0,80,0); level.setBlockAndUpdate(pos,LodestoneTransit.CALIBRATED_LODESTONE.defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(-2,80,0),LodestoneTransit.STATION.defaultBlockState()); level.setBlockAndUpdate(new BlockPos(2,80,0),LodestoneTransit.DIMENSIONAL_STATION.defaultBlockState());
                var registry=AnchorRegistry.get(server); var anchor=registry.adopt(level,pos); registry.rename(anchor,Component.literal("Home"));
                var player=server.getPlayerList().getPlayers().getFirst();
                Item[] items={LodestoneTransit.TELEPORTER,LodestoneTransit.DIMENSIONAL_TELEPORTER,LodestoneTransit.CORE,LodestoneTransit.STATION.asItem(),LodestoneTransit.DIMENSIONAL_STATION.asItem(),Items.COMPASS};
                for(int i=0;i<items.length;i++) { var stack=new ItemStack(items[i]); if(i!=2) stack.set(LodestoneTransit.DESTINATION,TeleportDestination.anchor(anchor.id())); if(i<2) stack.set(LodestoneTransit.CHARGES,3); player.getInventory().setItem(i,stack); }
                player.teleport(new TeleportTransition(level,new Vec3(.5,80,5.5),Vec3.ZERO,180,24,TeleportTransition.DO_NOTHING));
                return anchor.id();
            });
            context.waitFor(client -> client.player != null && client.player.getMainHandItem().getHoverName().getString().contains("Home"));
            singleplayer.getServer().runOnServer(server -> { var registry=AnchorRegistry.get(server); registry.rename(registry.get(anchorId),Component.literal("North Tower")); });
            context.waitFor(client -> client.player.getMainHandItem().getHoverName().getString().contains("North Tower"));
            context.waitTicks(80);
            context.takeScreenshot("lodestone-transit-world");
            context.runOnClient(client -> {
                if (!DestinationNaming.destination(client.player.getMainHandItem()).getString().equals("North Tower")) throw new AssertionError("Anchor rename was not synchronized");
            });
            context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
            context.waitTicks(10);
            context.takeScreenshot("lodestone-transit-inventory");
            context.setScreen(() -> null);
            var reload = new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.CompletableFuture<Void>>();
            var selected = new java.util.concurrent.atomic.AtomicReference<List<String>>();
            var packIds = new java.util.concurrent.atomic.AtomicReference<List<String>>();
            context.runOnClient(client -> {
                var repository = client.getResourcePackRepository(); repository.reload();
                selected.set(List.copyOf(repository.getSelectedIds()));
                packIds.set(repository.getAvailableIds().stream().filter(id -> id.contains("p1kl")).toList());
            });
            if (packIds.get().stream().anyMatch(id -> id.startsWith("file/"))) {
                context.runOnClient(client -> {
                    var ids = new ArrayList<>(selected.get());
                    packIds.get().stream().filter(id -> id.startsWith("file/")).forEach(ids::add);
                    packIds.get().stream().filter(id -> !id.startsWith("file/")).forEach(ids::add);
                    client.getResourcePackRepository().setSelected(ids);
                    reload.set(client.reloadResourcePacks());
                });
                context.waitFor(client -> reload.get() != null && reload.get().isDone() && !reload.get().isCompletedExceptionally());
                context.waitFor(client -> client.gui.overlay() == null);
                context.waitTicks(30);
                context.runOnClient(client -> {
                    if (!new io.github.r3neer.lodestonetransit.compat.P1klAvailable().get(client.player.getMainHandItem(), client.level, client.player, 0, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)) throw new AssertionError("3D pack not detected");
                });
                context.takeScreenshot("lodestone-transit-p1kl-hand");
                context.runOnClient(client -> client.player.getInventory().setSelectedSlot(1));
                context.waitFor(client -> client.player.getMainHandItem().is(LodestoneTransit.DIMENSIONAL_TELEPORTER));
                context.waitTicks(30);
                context.takeScreenshot("lodestone-transit-p1kl-dimensional-hand");
                context.runOnClient(client -> {
                    var ids = new ArrayList<>(selected.get());
                    packIds.get().stream().filter(id -> !id.startsWith("file/")).forEach(ids::add);
                    client.getResourcePackRepository().setSelected(ids);
                    reload.set(client.reloadResourcePacks());
                });
                context.waitFor(client -> reload.get().isDone() && !reload.get().isCompletedExceptionally());
                context.waitFor(client -> client.gui.overlay() == null);
                context.waitTicks(20);
                context.runOnClient(client -> {
                    if (new io.github.r3neer.lodestonetransit.compat.P1klAvailable().get(client.player.getMainHandItem(), client.level, client.player, 0, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)) throw new AssertionError("3D pack still detected after removal");
                });
                context.takeScreenshot("lodestone-transit-p1kl-removed-fallback");
                context.runOnClient(client -> { client.getResourcePackRepository().setSelected(selected.get()); reload.set(client.reloadResourcePacks()); });
                context.waitFor(client -> reload.get().isDone());
            }
        }
    }
}
