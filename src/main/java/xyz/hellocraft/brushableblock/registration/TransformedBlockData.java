package xyz.hellocraft.brushableblock.registration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;

import java.util.Map;
import java.util.Optional;

public record TransformedBlockData(String blockId, Map<String, String> properties, Optional<DataComponentPatch> components) {
    
    public static final Codec<String> PROPERTY_VALUE_CODEC = Codec.withAlternative(
            Codec.withAlternative(Codec.STRING, Codec.BOOL, String::valueOf),
            Codec.INT, String::valueOf
    );

    public static final Codec<TransformedBlockData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("block").forGetter(TransformedBlockData::blockId),
            Codec.unboundedMap(Codec.STRING, PROPERTY_VALUE_CODEC).optionalFieldOf("properties", Map.of()).forGetter(TransformedBlockData::properties),
            DataComponentPatch.CODEC.optionalFieldOf("components").forGetter(TransformedBlockData::components)
    ).apply(instance, TransformedBlockData::new));
}
