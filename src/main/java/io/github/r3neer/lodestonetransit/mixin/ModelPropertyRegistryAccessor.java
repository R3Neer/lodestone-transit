package io.github.r3neer.lodestonetransit.mixin;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.item.properties.conditional.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla has no public registration method for conditional model properties. */
@Mixin(ConditionalItemModelProperties.class)
public interface ModelPropertyRegistryAccessor {
    @Accessor("ID_MAPPER")
    static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ConditionalItemModelProperty>> transit$registry() { throw new AssertionError(); }
}
