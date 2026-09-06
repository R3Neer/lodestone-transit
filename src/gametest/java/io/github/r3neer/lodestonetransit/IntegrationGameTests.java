package io.github.r3neer.lodestonetransit;

import io.github.r3neer.lodestonetransit.anchor.*;
import io.github.r3neer.lodestonetransit.block.*;
import io.github.r3neer.lodestonetransit.compat.AlexsMobsCompat;
import io.github.r3neer.lodestonetransit.name.DestinationNaming;
import io.github.r3neer.lodestonetransit.teleport.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.*;

public final class IntegrationGameTests {
    private static void check(boolean value,String message) { if(!value) throw new net.minecraft.gametest.framework.GameTestAssertException(Component.literal(message),0); }
    @GameTest(padding=24,maxTicks=100) public void vanillaPistonMovesNamedAnchor(GameTestHelper h) {
        var level=h.getLevel(); var pos=h.absolutePos(new BlockPos(2,2,2));
        level.setBlockAndUpdate(pos,Blocks.LODESTONE.defaultBlockState()); var registry=AnchorRegistry.get(level.getServer()); var a=registry.adopt(level,pos); registry.rename(a,Component.literal("North Tower"));
        var compass=new ItemStack(Items.COMPASS); compass.set(LodestoneTransit.DESTINATION,TeleportDestination.anchor(a.id()));
        level.setBlockAndUpdate(pos.west(),Blocks.PISTON.defaultBlockState().setValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING,Direction.EAST));
        level.setBlockAndUpdate(pos.west().below(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(8,()->{
            check(level.getBlockState(pos.east()).is(Blocks.LODESTONE),"real piston pushed lodestone");
            var moved=registry.get(a.id()); check(moved.valid() && moved.position().pos().equals(pos.east()),"piston hook updated anchor");
            check(moved.name().orElseThrow().getString().equals("North Tower"),"name travels with anchor");
            CompassAnchors.update(compass,level); check(compass.get(DataComponents.LODESTONE_TRACKER).target().orElseThrow().pos().equals(pos.east()),"compass updates after storage");
            h.succeed();
        });
    }
    @GameTest(padding=24,maxTicks=100) public void hoppersInsertAndExtract(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,2,2)); var level=h.getLevel();
        level.setBlockAndUpdate(pos,LodestoneTransit.STATION.defaultBlockState());
        level.setBlockAndUpdate(pos.above(),Blocks.HOPPER.defaultBlockState()); level.setBlockAndUpdate(pos.below(),Blocks.HOPPER.defaultBlockState());
        var input=(HopperBlockEntity)level.getBlockEntity(pos.above()); var output=(HopperBlockEntity)level.getBlockEntity(pos.below());
        input.setItem(0,new ItemStack(Items.ENDER_PEARL,4)); input.setItem(1,new ItemStack(Items.DIAMOND));
        h.runAfterDelay(45,()->{ check(output.countItem(Items.ENDER_PEARL)==4,"hoppers transferred all pearls through station"); check(input.countItem(Items.DIAMOND)==1 && output.countItem(Items.DIAMOND)==0,"non-pearls stay in input"); h.succeed(); });
    }
    @GameTest(padding=24,maxTicks=100) public void pushierPistonPreservesStation(GameTestHelper h) {
        if(!FabricLoader.getInstance().isModLoaded("pushierpistons")) { h.succeed(); return; }
        var pos=h.absolutePos(new BlockPos(2,2,2)); var level=h.getLevel(); level.setBlockAndUpdate(pos,LodestoneTransit.STATION.defaultBlockState());
        var station=(TeleportStationBlockEntity)level.getBlockEntity(pos); var item=new ItemStack(LodestoneTransit.STATION); var destination=TeleportDestination.anchor(UUID.randomUUID());
        item.set(LodestoneTransit.DESTINATION,destination); item.set(DataComponents.CUSTOM_NAME,Component.literal("Emergency exit")); station.preserveItem(item); station.setItem(0,new ItemStack(Items.ENDER_PEARL,13));
        level.setBlockAndUpdate(pos.west(),Blocks.PISTON.defaultBlockState().setValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING,Direction.EAST)); level.setBlockAndUpdate(pos.west().below(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(8,()-> {
            check(level.getBlockEntity(pos.east()) instanceof TeleportStationBlockEntity,"Pushier Pistons moved block entity"); var moved=(TeleportStationBlockEntity)level.getBlockEntity(pos.east());
            check(moved.getItem(0).getCount()==13 && destination.equals(moved.destination()),"movement retains inventory and destination");
            check(moved.asItem().get(DataComponents.CUSTOM_NAME).getString().equals("Emergency exit"),"movement retains custom name"); h.succeed();
        });
    }
    @GameTest(padding=24) public void optionalCarverUsesOneDurability(GameTestHelper h) {
        if(!AlexsMobsCompat.available()) { check(AlexsMobsCompat.catalyst(new ItemStack(LodestoneTransit.CORE)),"core route available without Alexs Mobs"); h.succeed(); return; }
        var carver=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("alexsmobs","dimensional_carver"))); check(AlexsMobsCompat.catalyst(carver),"installed carver recognized");
        check(!AlexsMobsCompat.catalyst(new ItemStack(LodestoneTransit.CORE)),"core route disabled"); var remaining=AlexsMobsCompat.remainder(carver);
        check(remaining.getCount()==1 && remaining.getDamageValue()==carver.getDamageValue()+1,"carver loses exactly one durability"); h.succeed();
    }
    @GameTest(padding=24) public void automaticAndManualNames(GameTestHelper h) {
        var id=UUID.randomUUID(); var position=GlobalPos.of(h.getLevel().dimension(),h.absolutePos(BlockPos.ZERO));
        DestinationNaming.CLIENT_ANCHORS.put(id,new LodestoneAnchor(id,position,Optional.of(Component.literal("Home")),true));
        var device=new ItemStack(LodestoneTransit.TELEPORTER); device.set(LodestoneTransit.DESTINATION,TeleportDestination.anchor(id));
        check(DestinationNaming.destination(device).getString().equals("Home"),"automatic destination name");
        DestinationNaming.CLIENT_ANCHORS.put(id,new LodestoneAnchor(id,position,Optional.of(Component.literal("Tower")),true));
        check(DestinationNaming.destination(device).getString().equals("Tower"),"rename updates existing stack");
        device.set(DataComponents.CUSTOM_NAME,Component.literal("Escape")); check(device.getHoverName().getString().equals("Escape"),"manual item name overrides inherited name");
        check(DestinationNaming.destination(device).getString().equals("Tower"),"manual name does not hide destination");
        DestinationNaming.CLIENT_ANCHORS.remove(id); h.succeed();
    }
    @SuppressWarnings("removal")
    @GameTest(padding=32,maxTicks=100) public void dimensionalMountAndLeashChain(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel(); var horse=h.spawn(EntityTypes.HORSE,1,2,1); p.snapTo(horse.position()); p.startRiding(horse,true,false);
        horse.setTamed(true); horse.setNoAi(true);
        var cow=h.spawn(EntityTypes.COW,2,2,1); var sheep=h.spawn(EntityTypes.SHEEP,3,2,1); cow.setLeashedTo(p,true); sheep.setLeashedTo(cow,true);
        var target=h.getLevel().getServer().getLevel(Level.END); var center=new BlockPos(600,80,600);
        for(var pos:BlockPos.betweenClosed(center.offset(-6,-1,-6),center.offset(6,6,6))) target.setBlockAndUpdate(pos,pos.getY()==79 ? Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
        h.runAfterDelay(2,()-> {
            check(cow.getLeashHolder()==p && sheep.getLeashHolder()==cow,"fixture starts with intact leash chain");
            var group=new TeleportGroup(p); var landing=SafeLandingFinder.find(target,horse,Vec3.atBottomCenterOf(center),5,List.of()).orElseThrow(); check(group.travel(target,landing),"dimensional mount trip succeeds");
            check(p.getVehicle()!=null && p.getVehicle().getUUID().equals(horse.getUUID()) && p.level()==target,"immediate cross-dimensional mount relation");
        });
        h.succeedWhen(()-> {
            var arrivedHorse=target.getEntity(horse.getUUID()); var arrivedCow=target.getEntity(cow.getUUID()); var arrivedSheep=target.getEntity(sheep.getUUID());
            h.assertTrue(arrivedHorse!=null && p.getVehicle()==arrivedHorse && p.level()==target,Component.literal("cross-dimensional mount registration: lookup="+arrivedHorse+", vehicle="+p.getVehicle()+", dimension="+p.level().dimension()));
            check(arrivedCow instanceof Leashable first && first.getLeashHolder()==p && arrivedSheep instanceof Leashable second && second.getLeashHolder()==arrivedCow,"cross-dimensional chain reconstruction: cow="+arrivedCow+", cow holder="+(arrivedCow instanceof Leashable c ? c.getLeashHolder() : null)+", sheep="+arrivedSheep+", sheep holder="+(arrivedSheep instanceof Leashable s ? s.getLeashHolder() : null)+", player="+p);
            check(h.getLevel().getEntity(horse.getUUID())==null && h.getLevel().getEntity(cow.getUUID())==null,"no origin duplicates");
        });
    }
}
