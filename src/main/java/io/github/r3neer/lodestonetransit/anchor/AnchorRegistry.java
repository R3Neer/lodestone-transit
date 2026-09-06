package io.github.r3neer.lodestonetransit.anchor;

import com.mojang.serialization.Codec;
import java.util.*;
import io.github.r3neer.lodestonetransit.LodestoneTransit;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.*;

/** One server-owned registry; the secondary index never resurrects invalid identities. */
public final class AnchorRegistry extends SavedData {
    public static final Codec<AnchorRegistry> CODEC = LodestoneAnchor.CODEC.listOf().xmap(AnchorRegistry::new, r -> List.copyOf(r.anchors.values()));
    public static final SavedDataType<AnchorRegistry> TYPE = new SavedDataType<>(LodestoneTransit.id("anchors"), AnchorRegistry::new, CODEC, null);
    private final Map<UUID, LodestoneAnchor> anchors = new HashMap<>();
    private final Map<GlobalPos, UUID> positions = new HashMap<>();
    private final Set<GlobalPos> moving = new HashSet<>();
    private MinecraftServer server;
    public AnchorRegistry() {}
    private AnchorRegistry(List<LodestoneAnchor> values) {
        for (var a : values) { anchors.put(a.id(), a); if (a.valid()) positions.put(a.position(), a.id()); }
    }
    public static AnchorRegistry get(MinecraftServer server) {
        var state = server.overworld().getDataStorage().computeIfAbsent(TYPE); state.server = server; return state;
    }
    public Collection<LodestoneAnchor> all() { return List.copyOf(anchors.values()); }
    public LodestoneAnchor get(UUID id) { return anchors.get(id); }
    public LodestoneAnchor at(GlobalPos pos) { return anchors.get(positions.get(pos)); }
    public LodestoneAnchor adopt(ServerLevel level, BlockPos pos) {
        if (!AnchorBlocks.isAnchor(level.getBlockState(pos))) return null;
        var global = GlobalPos.of(level.dimension(), pos.immutable());
        var existing = at(global); if (existing != null) return existing;
        var anchor = new LodestoneAnchor(UUID.randomUUID(), global, Optional.empty(), true);
        put(anchor); return anchor;
    }
    private void put(LodestoneAnchor anchor) {
        anchors.put(anchor.id(), anchor);
        if (anchor.valid()) positions.put(anchor.position(), anchor.id());
        setDirty();
        if (server != null) AnchorNetworking.broadcast(server, anchor);
    }
    public void rename(LodestoneAnchor anchor, Component name) { put(new LodestoneAnchor(anchor.id(), anchor.position(), Optional.of(name.copy()), anchor.valid())); }
    public void invalidate(GlobalPos pos) {
        if (moving.contains(pos)) return;
        var anchor = at(pos);
        if (anchor != null) { positions.remove(pos); put(new LodestoneAnchor(anchor.id(), pos, anchor.name(), false)); }
    }
    /** Called using the successful piston resolver's actual list, before it changes blocks. */
    public void beginMove(ServerLevel level, List<BlockPos> blocks, Direction direction) {
        var updates = new ArrayList<LodestoneAnchor>();
        for (var pos : blocks) {
            var from = GlobalPos.of(level.dimension(), pos);
            moving.add(from); moving.add(GlobalPos.of(level.dimension(), pos.relative(direction)));
            var a = at(from);
            if (a != null) updates.add(new LodestoneAnchor(a.id(), GlobalPos.of(level.dimension(), pos.relative(direction)), a.name(), true));
        }
        for (var a : updates) positions.remove(anchors.get(a.id()).position());
        updates.forEach(this::put);
    }
    public void endMove() { moving.clear(); }
}
