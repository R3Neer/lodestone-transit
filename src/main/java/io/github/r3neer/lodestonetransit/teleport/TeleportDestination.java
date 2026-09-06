package io.github.r3neer.lodestonetransit.teleport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;

/** Coordinates are only a migration hint for pre-existing vanilla compasses. */
public record TeleportDestination(Kind kind, Optional<UUID> anchorId, Optional<GlobalPos> legacyTarget) {
    public enum Kind { WORLD_SPAWN, LODESTONE_ANCHOR, LAST_DEATH }
    public static final Codec<TeleportDestination> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.comapFlatMap(s -> {
            try { return com.mojang.serialization.DataResult.success(Kind.valueOf(s.toUpperCase(java.util.Locale.ROOT))); }
            catch (IllegalArgumentException ex) { return com.mojang.serialization.DataResult.error(() -> "Unknown destination type: " + s); }
        }, k -> k.name().toLowerCase(java.util.Locale.ROOT)).fieldOf("type").forGetter(TeleportDestination::kind),
        UUIDUtil.CODEC.optionalFieldOf("anchor_id").forGetter(TeleportDestination::anchorId),
        GlobalPos.CODEC.optionalFieldOf("legacy_target").forGetter(TeleportDestination::legacyTarget)
    ).apply(i, TeleportDestination::new));
    public static TeleportDestination spawn() { return new TeleportDestination(Kind.WORLD_SPAWN, Optional.empty(), Optional.empty()); }
    public static TeleportDestination death() { return new TeleportDestination(Kind.LAST_DEATH, Optional.empty(), Optional.empty()); }
    public static TeleportDestination anchor(UUID id) { return new TeleportDestination(Kind.LODESTONE_ANCHOR, Optional.of(id), Optional.empty()); }
}
