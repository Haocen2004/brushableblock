package xyz.hellocraft.brushableblock.registration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SetTransformedBlockFunction extends LootItemConditionalFunction {
    public static final MapCodec<SetTransformedBlockFunction> CODEC = RecordCodecBuilder.mapCodec(
            instance -> commonFields(instance).and(
                    instance.group(
                            Codec.STRING.fieldOf("block").forGetter(f -> f.blockId),
                            Codec.unboundedMap(Codec.STRING, TransformedBlockData.PROPERTY_VALUE_CODEC).optionalFieldOf("properties", Map.of()).forGetter(f -> f.properties),
                            DataComponentPatch.CODEC.optionalFieldOf("components").forGetter(f -> f.components)
                    )
            ).apply(instance, SetTransformedBlockFunction::new)
    );

    private final String blockId;
    private final Map<String, String> properties;
    private final Optional<DataComponentPatch> components;

    protected SetTransformedBlockFunction(List<LootItemCondition> conditions, String blockId, Map<String, String> properties, Optional<DataComponentPatch> components) {
        super(conditions);
        this.blockId = blockId;
        this.properties = properties;
        this.components = components;
    }

    @Override
    public @NotNull LootItemFunctionType<SetTransformedBlockFunction> getType() {
        return ModContent.SET_TRANSFORMED_BLOCK.get();
    }

    @Override
    protected @NotNull ItemStack run(ItemStack stack, @NotNull LootContext context) {
        stack.set(ModContent.TRANSFORMED_BLOCK.get(), new TransformedBlockData(this.blockId, this.properties, this.components));
        return stack;
    }
}
