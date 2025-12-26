package xyz.hellocraft.brushableblock.registration;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SetTransformedBlockFunction extends LootItemConditionalFunction {
    public static final MapCodec<SetTransformedBlockFunction> CODEC = RecordCodecBuilder.mapCodec(
            instance -> commonFields(instance).and(
                    com.mojang.serialization.Codec.STRING.fieldOf("block").forGetter(f -> f.blockId)
            ).apply(instance, SetTransformedBlockFunction::new)
    );

    private final String blockId;

    protected SetTransformedBlockFunction(List<LootItemCondition> conditions, String blockId) {
        super(conditions);
        this.blockId = blockId;
    }

    @Override
    public @NotNull LootItemFunctionType<SetTransformedBlockFunction> getType() {
        return ModContent.SET_TRANSFORMED_BLOCK.get();
    }

    @Override
    protected @NotNull ItemStack run(ItemStack stack, @NotNull LootContext context) {
        stack.set(ModContent.TRANSFORMED_BLOCK.get(), this.blockId);
        return stack;
    }
}
