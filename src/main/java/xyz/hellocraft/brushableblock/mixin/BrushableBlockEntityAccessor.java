package xyz.hellocraft.brushableblock.mixin;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BrushableBlockEntity.class)
public interface BrushableBlockEntityAccessor {

    @Accessor("lootTable")
    void setLootTable(ResourceKey<LootTable> lootTable);

    @Accessor("brushCount")
    int getBrushCount();

    @Accessor("brushCount")
    void setBrushCount(int count);

    @Accessor("item")
    void setBrushedItem(ItemStack item);

    @Accessor("hitDirection")
    void setHitDirection(Direction direction);
}
