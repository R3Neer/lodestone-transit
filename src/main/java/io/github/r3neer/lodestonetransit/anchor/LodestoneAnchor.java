package io.github.r3neer.lodestonetransit.anchor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

public record LodestoneAnchor(UUID id, GlobalPos position, Optional<Component> name, boolean valid) {
    public static final Codec<LodestoneAnchor> CODEC = RecordCodecBuilder.create(i -> i.group(
        UUIDUtil.CODEC.fieldOf("id").forGetter(LodestoneAnchor::id),
        GlobalPos.CODEC.fieldOf("position").forGetter(LodestoneAnchor::position),
        ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(LodestoneAnchor::name),
        Codec.BOOL.fieldOf("valid").forGetter(LodestoneAnchor::valid)
    ).apply(i, LodestoneAnchor::new));
}
