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
                var pos=new BlockPos(0,80,0); level.setBlockAndUpdate(pos,Blocks.LODESTONE.defaultBlockState());
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
        }
    }
}
