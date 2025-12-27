package xyz.hellocraft.brushableblock.registration;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import xyz.hellocraft.brushableblock.BrushableBlock;

import java.util.function.Supplier;

public class ModContent {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BrushableBlock.MODID);
    public static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, BrushableBlock.MODID);

    public static final Supplier<DataComponentType<TransformedBlockData>> TRANSFORMED_BLOCK = DATA_COMPONENTS.register("transformed_block", 
            () -> DataComponentType.<TransformedBlockData>builder().persistent(TransformedBlockData.CODEC).build());

    // We'll define the function class in another file
    public static final Supplier<LootItemFunctionType<SetTransformedBlockFunction>> SET_TRANSFORMED_BLOCK = LOOT_FUNCTIONS.register("set_transformed_block",
            () -> new LootItemFunctionType<>(SetTransformedBlockFunction.CODEC));

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
        LOOT_FUNCTIONS.register(modEventBus);
    }
}
