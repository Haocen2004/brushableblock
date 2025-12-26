package xyz.hellocraft.brushableblock.mixin;

import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BrushableBlockEntity.class)
public interface BrushableBlockEntityAccessor {

    @Accessor("lootTable")
    void setLootTable(net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> lootTable);

    @Accessor("brushCount")
    int getBrushCount();

    @Accessor("brushCount")
    void setBrushCount(int count);

    @Accessor("item")
    void setBrushedItem(net.minecraft.world.item.ItemStack item);

    @Accessor("hitDirection")
    void setHitDirection(net.minecraft.core.Direction direction);
}
