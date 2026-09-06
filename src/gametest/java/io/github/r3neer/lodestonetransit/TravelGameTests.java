package io.github.r3neer.lodestonetransit;

import io.github.r3neer.lodestonetransit.anchor.*;
import io.github.r3neer.lodestonetransit.block.TeleportStationBlockEntity;
import io.github.r3neer.lodestonetransit.item.*;
import io.github.r3neer.lodestonetransit.teleport.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import java.util.*;

@SuppressWarnings("removal")
public final class TravelGameTests {
    private static void check(boolean value, String message) { if (!value) throw new net.minecraft.gametest.framework.GameTestAssertException(Component.literal(message),0); }
    private static ServerPlayer player(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel(); p.snapTo(h.absoluteVec(new Vec3(1.5, 2, 1.5))); return p;
    }
    private static void platform(net.minecraft.server.level.ServerLevel level, BlockPos center) {
        for (var pos : BlockPos.betweenClosed(center.offset(-6,-1,-6), center.offset(6,5,6))) level.setBlock(pos, pos.getY() == center.getY()-1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
    }
    private static TeleportDestination anchor(GameTestHelper h, BlockPos pos) {
        h.getLevel().setBlockAndUpdate(pos, LodestoneTransit.CALIBRATED_LODESTONE.defaultBlockState()); return TeleportDestination.anchor(AnchorRegistry.get(h.getLevel().getServer()).adopt(h.getLevel(), pos).id());
    }
    private static void stationAttempt(GameTestHelper h, ServerPlayer player, TeleportDestination destination, boolean dimensional, int fuel) {
        var pos = h.absolutePos(new BlockPos(1,1,1)); var block = dimensional ? LodestoneTransit.DIMENSIONAL_STATION : LodestoneTransit.STATION;
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        var be = (TeleportStationBlockEntity)h.getLevel().getBlockEntity(pos);
        var embedded = new ItemStack(block); embedded.set(LodestoneTransit.DESTINATION, destination); be.preserveItem(embedded);
        be.setItem(0, fuel == 0 ? ItemStack.EMPTY : new ItemStack(Items.ENDER_PEARL, fuel));
        h.getLevel().getBlockState(pos).useWithoutItem(h.getLevel(), player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        check(be.getItem(0).getCount() == Math.max(0,fuel-1), "fuel debited once before resolution");
    }
    @GameTest(padding=32) public void brokenAnchorConsumesFuel(GameTestHelper h) {
        var p = player(h); var original = p.position();
        stationAttempt(h,p,TeleportDestination.anchor(UUID.randomUUID()),false,2);
        check(p.position().equals(original), "broken anchor must not move player"); h.succeed();
    }
    @GameTest(padding=32) public void missingDeathConsumesFuel(GameTestHelper h) {
        var p = player(h); p.setLastDeathLocation(Optional.empty()); var original=p.position();
        stationAttempt(h,p,TeleportDestination.death(),false,1);
        check(p.position().equals(original), "missing death must not move player"); h.succeed();
    }
    @GameTest(padding=32) public void normalCrossDimensionConsumesFuel(GameTestHelper h) {
        var p = player(h); var original=p.position(); p.setLastDeathLocation(Optional.of(GlobalPos.of(Level.NETHER,new BlockPos(0,80,0))));
        stationAttempt(h,p,TeleportDestination.death(),false,1);
        check(p.level()==h.getLevel() && p.position().equals(original), "normal device rejects other dimension"); h.succeed();
    }
    @GameTest(padding=32) public void noFuelCannotTravel(GameTestHelper h) {
        var p = player(h); var original=p.position(); stationAttempt(h,p,TeleportDestination.spawn(),true,0);
        check(p.position().equals(original), "no free teleport"); h.succeed();
    }
    @GameTest(padding=32) public void solidDestinationConsumesFuel(GameTestHelper h) {
        var p=player(h); var original=p.position(); var center=h.absolutePos(new BlockPos(18,8,1));
        for (var pos:BlockPos.betweenClosed(center.offset(-6,-6,-6),center.offset(6,8,6))) h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        var target=anchor(h,center); stationAttempt(h,p,target,false,1);
        check(p.position().equals(original),"no geometric space leaves player in origin"); h.succeed();
    }
    @GameTest(padding=32) public void normalSameDimensionTravels(GameTestHelper h) {
        var p=player(h); var center=h.absolutePos(new BlockPos(18,4,1)); platform(h.getLevel(),center);
        var target=anchor(h,center); stationAttempt(h,p,target,false,1);
        check(p.position().distanceTo(Vec3.atBottomCenterOf(center))<=5,"same-dimensional arrival within five blocks"); h.succeed();
    }
    @GameTest(padding=32) public void dimensionalStationTravels(GameTestHelper h) {
        var p=player(h); var nether=h.getLevel().getServer().getLevel(Level.NETHER); var center=new BlockPos(400,80,400); platform(nether,center);
        p.setLastDeathLocation(Optional.of(GlobalPos.of(Level.NETHER,center))); stationAttempt(h,p,TeleportDestination.death(),true,1);
        check(p.level()==nether && p.position().distanceTo(Vec3.atBottomCenterOf(center))<=5,"dimensional station reaches Nether"); h.succeed();
    }
    @GameTest(padding=32,maxTicks=100) public void portableCapacityReadinessAndNoDoubleDebit(GameTestHelper h) {
        var p=player(h); var stack=new ItemStack(LodestoneTransit.TELEPORTER); stack.set(LodestoneTransit.DESTINATION,TeleportDestination.anchor(UUID.randomUUID()));
        p.getAbilities().instabuild=false;
        p.setItemInHand(InteractionHand.MAIN_HAND,stack); p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.ENDER_PEARL,8));
        EquipReadiness.tick(p); check(!EquipReadiness.ready(p,InteractionHand.MAIN_HAND),"equip starts unarmed");
        p.setShiftKeyDown(true);
        for(int tick=1;tick<=5;tick++) { final int step=tick; h.runAfterDelay(tick,()-> {
            ((TeleporterItem)stack.getItem()).use(p.level(),p,InteractionHand.MAIN_HAND);
            check(stack.getOrDefault(LodestoneTransit.CHARGES,0)==Math.min(step,4),"capacity capped at four");
        }); }
        h.runAfterDelay(22,()-> {
            p.setShiftKeyDown(false); check(EquipReadiness.ready(p,InteractionHand.MAIN_HAND),"ready after twenty ticks");
            check(p.getOffhandItem().getCount()==4,"loading consumes exactly four pearls and does not consume a fifth at capacity");
            p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.ENDER_PEARL,2));
            check(EquipReadiness.ready(p,InteractionHand.MAIN_HAND),"changing the other hand does not reset a continuously held device");
            var origin=p.position(); ((TeleporterItem)stack.getItem()).use(p.level(),p,InteractionHand.MAIN_HAND); ((TeleporterItem)stack.getItem()).use(p.level(),p,InteractionHand.MAIN_HAND);
            check(stack.get(LodestoneTransit.CHARGES)==3 && p.position().equals(origin),"failed attempt consumes exactly one despite same-tick spam");
            EquipReadiness.reset(p); check(!EquipReadiness.ready(p,InteractionHand.MAIN_HAND),"reselect resets readiness");
        });
        h.runAfterDelay(23,()-> {
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.ENDER_PEARL,2)); p.setItemInHand(InteractionHand.OFF_HAND,stack); p.setShiftKeyDown(true);
            net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(p,p.level(),InteractionHand.MAIN_HAND);
            check(stack.get(LodestoneTransit.CHARGES)==4 && p.getMainHandItem().getCount()==1,"reversed hands insert one pearl without throwing or double consuming"); h.succeed();
        });
    }
    @GameTest(padding=32,maxTicks=100) public void mountAndRecursiveLeashesTravel(GameTestHelper h) {
        var p=player(h); var horse=h.spawn(EntityTypes.HORSE,2,2,2); p.startRiding(horse,true,false);
        var cow=h.spawn(EntityTypes.COW,3,2,2); var sheep=h.spawn(EntityTypes.SHEEP,4,2,2); var llama=h.spawn(EntityTypes.LLAMA,5,2,2);
        cow.setLeashedTo(p,true); sheep.setLeashedTo(cow,true); llama.setLeashedTo(sheep,true);
        var center=h.absolutePos(new BlockPos(18,4,1)); platform(h.getLevel(),center);
        h.runAfterDelay(2,()-> {
            var group=new TeleportGroup(p); var landing=SafeLandingFinder.find(h.getLevel(),horse,Vec3.atBottomCenterOf(center),5,List.of()).orElseThrow();
            check(group.travel(h.getLevel(),landing),"mounted group travels"); check(p.getVehicle()==horse,"mount retained");
            check(cow.position().distanceTo(horse.position())<=3.1 && sheep.position().distanceTo(horse.position())<=3.1 && llama.position().distanceTo(horse.position())<=3.1,"recursive chain arrives nearby");
            check(cow.getLeashHolder()==p && sheep.getLeashHolder()==cow && llama.getLeashHolder()==sheep,"recursive links preserved");
            check(!cow.getBoundingBox().intersects(sheep.getBoundingBox()),"secondary arrivals do not overlap"); h.succeed();
        });
    }
    @GameTest(padding=32) public void oversizedMountCannotFit(GameTestHelper h) {
        var p=player(h); var horse=h.spawn(EntityTypes.HORSE,2,2,2); p.startRiding(horse,true,false);
        var center=h.absolutePos(new BlockPos(18,8,1));
        for(var pos:BlockPos.betweenClosed(center.offset(-7,-6,-7),center.offset(7,10,7))) h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,Blocks.AIR.defaultBlockState()); h.getLevel().setBlockAndUpdate(center.above(),Blocks.AIR.defaultBlockState());
        check(SafeLandingFinder.find(h.getLevel(),horse,Vec3.atBottomCenterOf(center),5,List.of()).isEmpty(),"horse and rider do not fit narrow shaft"); h.succeed();
    }
    @GameTest(padding=32) public void oversizedSecondaryLeavesSubtreeBehind(GameTestHelper h) {
        var p=player(h); var cow=h.spawn(EntityTypes.COW,3,2,2); var sheep=h.spawn(EntityTypes.SHEEP,4,2,2); cow.setLeashedTo(p,true); sheep.setLeashedTo(cow,true);
        var original=cow.position(); var center=h.absolutePos(new BlockPos(18,8,1));
        for(var pos:BlockPos.betweenClosed(center.offset(-7,-6,-7),center.offset(7,10,7))) h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,Blocks.AIR.defaultBlockState()); h.getLevel().setBlockAndUpdate(center.above(),Blocks.AIR.defaultBlockState());
        cow.setBoundingBox(AABB.ofSize(cow.position().add(0,2,0),4,4,4));
        check(new TeleportGroup(p).travel(h.getLevel(),Vec3.atBottomCenterOf(center)),"main trip succeeds");
        check(cow.position().equals(original) && cow.getLeashHolder()==null && sheep.getLeashHolder()==cow,"only subtree boundary lead breaks"); h.succeed();
    }
}
