package xyz.hellocraft.brushableblock.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import xyz.hellocraft.brushableblock.BrushableBlock;
import xyz.hellocraft.brushableblock.block.entity.VirtualBrushableBlockEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BrushingManager {
    // Track virtual entities for rendering and logic
    private static final Map<BlockPos, BrushableBlockEntity> VIRTUAL_ENTITIES = new ConcurrentHashMap<>();
    
    // Track when they were last accessed to clean up idle ones
    private static final Map<BlockPos, Long> LAST_ACCESS = new ConcurrentHashMap<>();

    public static BrushableBlockEntity getOrCreateVirtualEntity(Level level, BlockPos pos, BlockState state) {
        BlockPos immutablePos = pos.immutable();
        LAST_ACCESS.put(immutablePos, level.getGameTime());
        
        return VIRTUAL_ENTITIES.computeIfAbsent(immutablePos, p -> {
            VirtualBrushableBlockEntity be = new VirtualBrushableBlockEntity(p, state);
            be.setLevel(level);
            
            // Set loot table based on block id: brushableblock:brushing/<block_path>
            net.minecraft.resources.ResourceLocation blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> lootTableKey = 
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, 
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BrushableBlock.MODID, "brushing/" + blockId.getPath()));

            ((xyz.hellocraft.brushableblock.mixin.BrushableBlockEntityAccessor)be).setLootTable(lootTableKey);
            
            return be;
        });
    }


    public static Map<BlockPos, BrushableBlockEntity> getActiveEntities() {
        return VIRTUAL_ENTITIES;
    }

    public static void tick(Level level) {
        long time = level.getGameTime();
        
        // 1. Gradually decrease brush count for idle entities
        VIRTUAL_ENTITIES.forEach((pos, be) -> {
            if (be.getLevel() != level) return;
            
            long lastTime = LAST_ACCESS.getOrDefault(pos, 0L);
            long idleTicks = time - lastTime;
            
            if (idleTicks > 40) { // Start shrinking after 2 seconds of inactivity
                xyz.hellocraft.brushableblock.mixin.BrushableBlockEntityAccessor accessor = (xyz.hellocraft.brushableblock.mixin.BrushableBlockEntityAccessor) be;
                int count = accessor.getBrushCount();
                if (count > 0) {
                    if (time % 5 == 0) accessor.setBrushCount(count - 1);
                    
                    // Periodically sync to client to update the shrinking animation
                    if (!level.isClientSide && count % 2 == 0) {
                        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                    }
                }
            }
        });

        // 2. Cleanup entities that are fully shrunk (count 0) and idle for 10+ ticks
        LAST_ACCESS.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            BrushableBlockEntity be = VIRTUAL_ENTITIES.get(pos);
            
            if (be != null && be.getLevel() == level) {
                int count = ((xyz.hellocraft.brushableblock.mixin.BrushableBlockEntityAccessor)be).getBrushCount();
                long idleTicks = time - entry.getValue();
                
                if (count == 0 && idleTicks > 40) {
                    VIRTUAL_ENTITIES.remove(pos);
                    return true;
                }
                return false;
            }
            return be == null; // Clean up entry if BE is gone
        });
    }
}
