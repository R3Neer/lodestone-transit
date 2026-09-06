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
    private static String overlay(net.minecraft.client.Minecraft client) {
        try {
            var field = net.minecraft.client.gui.Hud.class.getDeclaredField("overlayMessageString");
            field.setAccessible(true);
            var text = (Component) field.get(client.gui.hud);
            return text == null ? "" : text.getString();
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
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
            // Every portable charge state must be available before inventory ticking.
            context.runOnClient(client -> {
                var property = new io.github.r3neer.lodestonetransit.item.ChargeModelProperty();
                for (int count=0; count<=4; count++) {
                    var stack = new ItemStack(LodestoneTransit.TELEPORTER);
                    stack.set(LodestoneTransit.CHARGES,count);
                    if (property.get(stack,client.level,null,0)!=count) throw new AssertionError("Charge property " + count);
                    for (var item : new Item[]{LodestoneTransit.TELEPORTER,LodestoneTransit.DIMENSIONAL_TELEPORTER}) {
                        var device = new ItemStack(item);
                        if (count>0) device.set(LodestoneTransit.CHARGES,count);
                        var lines = new ArrayList<Component>();
                        DestinationNaming.tooltip(device,item==LodestoneTransit.DIMENSIONAL_TELEPORTER,true,lines::add);
                        String expected = Component.translatable("transit.charges",count,4).getString();
                        if (lines.stream().noneMatch(line -> line.getString().equals(expected))) throw new AssertionError("Missing tooltip count: " + expected);
                    }
                }
            });
            singleplayer.getServer().runOnServer(server -> {
                var player=server.getPlayerList().getPlayers().getFirst();
                for (int count=0;count<=4;count++) {
                    var base=new ItemStack(LodestoneTransit.TELEPORTER); base.set(LodestoneTransit.CHARGES,count);
                    var dim=new ItemStack(LodestoneTransit.DIMENSIONAL_TELEPORTER); dim.set(LodestoneTransit.CHARGES,count);
                    player.getInventory().setItem(9+count,base); player.getInventory().setItem(18+count,dim);
                }
            });
            for (int count=0;count<=16;count++) {
                final int pearls=count;
                singleplayer.getServer().runOnServer(server -> {
                    var level=server.overworld();
                    for (int x:new int[]{-2,2}) {
                        var station=(io.github.r3neer.lodestonetransit.block.TeleportStationBlockEntity)level.getBlockEntity(new BlockPos(x,80,0));
                        station.setItem(0,pearls==0 ? ItemStack.EMPTY : new ItemStack(Items.ENDER_PEARL,pearls));
                    }
                });
                context.waitFor(client -> client.level.getBlockState(new BlockPos(-2,80,0)).getValue(io.github.r3neer.lodestonetransit.block.TeleportStationBlock.PEARLS)==pearls
                    && client.level.getBlockState(new BlockPos(2,80,0)).getValue(io.github.r3neer.lodestonetransit.block.TeleportStationBlock.PEARLS)==pearls);
                context.waitFor(client -> client.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,new BlockPos(-3,80,0))==Math.max(0,pearls/4-1)
                    && client.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,new BlockPos(3,80,0))==Math.max(0,(pearls/4)*2-1));
            }
            context.takeScreenshot("lodestone-transit-world");
            context.runOnClient(client -> {
                if (!DestinationNaming.destination(client.player.getMainHandItem()).getString().equals("North Tower")) throw new AssertionError("Anchor rename was not synchronized");
            });
            context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
            context.waitTicks(10);
            context.takeScreenshot("lodestone-transit-inventory");
            context.setScreen(() -> null);
            context.runOnClient(client -> client.player.getInventory().setSelectedSlot(2));
            context.waitFor(client -> client.player.getMainHandItem().is(LodestoneTransit.CORE));
            context.waitTicks(60);
            context.takeScreenshot("dimensional-core-hand");
            context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
            context.waitFor(client -> client.player.getMainHandItem().is(LodestoneTransit.TELEPORTER));
            singleplayer.getServer().runOnServer(server -> {
                var player=server.getPlayerList().getPlayers().getFirst();
                player.teleport(new TeleportTransition(server.overworld(),new Vec3(.5,82,3.6),Vec3.ZERO,180,47,TeleportTransition.DO_NOTHING));
                player.setNoGravity(true);
            });
            context.waitTicks(15);
            context.takeScreenshot("station-relief-and-full-fragments");
            singleplayer.getServer().runOnServer(server -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set midnight"));
            context.waitTicks(30);
            context.takeScreenshot("station-light-night");
            singleplayer.getServer().runOnServer(server -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set noon"));
            singleplayer.getServer().runOnServer(server -> {
                var player=server.getPlayerList().getPlayers().getFirst(); player.setNoGravity(false);
                player.teleport(new TeleportTransition(server.overworld(),new Vec3(.5,80,5.5),Vec3.ZERO,180,24,TeleportTransition.DO_NOTHING));
            });
            var heard = new java.util.concurrent.ConcurrentLinkedQueue<String>();
            net.minecraft.client.sounds.SoundEventListener listener = (sound, event, range) -> heard.add(sound.getIdentifier().toString());
            context.runOnClient(client -> client.getSoundManager().addListener(listener));
            for (int initial : new int[] {2, 16, 0, -1}) {
                heard.clear();
                context.waitTicks(2);
                singleplayer.getServer().runOnServer(server -> {
                    var level = server.overworld();
                    var pos = new BlockPos(-2, 80, 0);
                    var station = (io.github.r3neer.lodestonetransit.block.TeleportStationBlockEntity) level.getBlockEntity(pos);
                    var player = server.getPlayerList().getPlayers().getFirst();
                    station.setItem(0, initial == 0 ? ItemStack.EMPTY : new ItemStack(Items.ENDER_PEARL, initial == -1 ? 2 : initial));
                    var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                    if (initial > 0) {
                        var pearl = new ItemStack(Items.ENDER_PEARL);
                        level.getBlockState(pos).useItemOn(pearl, level, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
                        if (station.getItem(0).getCount() != (initial == 16 ? 16 : 3) || pearl.getCount() != (initial == 16 ? 1 : 0)) throw new AssertionError("Incorrect station insertion or full-container consumption");
                    } else {
                        if (initial == -1) {
                            var embedded = new ItemStack(LodestoneTransit.STATION);
                            embedded.set(LodestoneTransit.DESTINATION, TeleportDestination.anchor(UUID.randomUUID()));
                            station.preserveItem(embedded);
                        }
                        level.getBlockState(pos).useWithoutItem(level, player, hit);
                    }
                });
                String expected = switch (initial) {
                    case 2 -> "Ender pearls: 3/16";
                    case 16 -> "Ender pearls: 16/16 · Full";
                    case 0 -> "Ender pearls: 0/16 · Empty";
                    default -> "Linked lodestone is no longer available. · Ender pearls: 1/16";
                };
                context.waitFor(client -> overlay(client).equals(expected));
                if (initial == 16 || initial == 0) {
                    String sound = initial == 16 ? "minecraft:item.bundle.insert_fail" : "minecraft:block.decorated_pot.insert_fail";
                    context.waitFor(client -> heard.contains(sound));
                    if (heard.contains("minecraft:block.respawn_anchor.deplete")) throw new AssertionError("Container rejection used teleport-failure sound");
                }
                context.takeScreenshot("station-feedback-" + initial);
            }
            context.runOnClient(client -> client.getSoundManager().removeListener(listener));
            var reload = new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.CompletableFuture<Void>>();
            var selected = new java.util.concurrent.atomic.AtomicReference<List<String>>();
            var packIds = new java.util.concurrent.atomic.AtomicReference<List<String>>();
            context.runOnClient(client -> {
                var repository = client.getResourcePackRepository(); repository.reload();
                selected.set(List.copyOf(repository.getSelectedIds()));
                packIds.set(repository.getAvailableIds().stream().filter(id -> id.contains("p1kl")).toList());
            });
            if (packIds.get().stream().noneMatch(id -> !id.startsWith("file/") && selected.get().contains(id))) {
                throw new AssertionError("Built-in p1kl compatibility pack is not enabled automatically");
            }
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
