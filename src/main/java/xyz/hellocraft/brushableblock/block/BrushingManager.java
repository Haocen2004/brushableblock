package xyz.hellocraft.brushableblock.block;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import cy.jdkdigital.jearchaeology.JEArchaeology;
import cy.jdkdigital.jearchaeology.recipe.BrushingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import xyz.hellocraft.brushableblock.BrushableBlock;
import xyz.hellocraft.brushableblock.block.entity.VirtualBrushableBlockEntity;
import xyz.hellocraft.brushableblock.tag.ModTags;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BrushingManager {
    // Track virtual entities for rendering and logic
    private static final Map<BlockPos, BrushableBlockEntity> VIRTUAL_ENTITIES = new ConcurrentHashMap<>();
    
    // Track when they were last accessed to clean up idle ones
    private static final Map<BlockPos, Long> LAST_ACCESS = new ConcurrentHashMap<>();

    private static final UUID BRUSHER_PLAYER_UUID = UUID.nameUUIDFromBytes("jea_brusher_player".getBytes(StandardCharsets.UTF_8));
    private static List<RecipeHolder<?>> cachedBrushingRecipes = new ArrayList<>();

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

    public static List<RecipeHolder<?>> getAllBrushingRecipes(ServerLevel level) {
        if (level != null && cachedBrushingRecipes.isEmpty()) {

            List<RecipeHolder<?>> recipeList = new ArrayList<>();

            Map<ResourceKey<LootTable>, Pair<String, Ingredient>> tables = new HashMap<>();

            // get all tagged block
            BuiltInRegistries.BLOCK.getTag(ModTags.Blocks.BRUSHABLE).ifPresent(tag -> tag.forEach(block -> {
                ResourceKey<LootTable> lootTableKey = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(BrushableBlock.MODID, "brushing/" + BuiltInRegistries.BLOCK.getKey(block.value()).getPath()));
                if (!tables.containsKey(lootTableKey)) {
                    tables.put(lootTableKey, Pair.of(BuiltInRegistries.BLOCK.getKey(block.value()).getPath(), Ingredient.of(block.value().asItem())));
                }
            }));


            Player fakePlayer = FakePlayerFactory.get(level, new GameProfile(BRUSHER_PLAYER_UUID, "jea_brusher_player"));
            LootParams lootparams = (new LootParams.Builder(level)).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(new BlockPos(0, 0, 0))).withLuck(1.0f).withParameter(LootContextParams.THIS_ENTITY, fakePlayer).create(LootContextParamSets.CHEST);
            tables.forEach((lootTableKey, pair) -> {
                Map<Item, ItemStack> items = new HashMap<>();
                var table = level.getServer().reloadableRegistries().getLootTable(lootTableKey);
                if (!table.equals(LootTable.EMPTY)) {
                    for (int i = 0; i < 600; i++) {
                        table.getRandomItems(lootparams).forEach(itemStack -> {
                            if (!items.containsKey(itemStack.getItem())) {
                                items.put(itemStack.getItem(), itemStack);
                            }
                        });
                    }
                }
                String locationName = pair.getFirst();
                if (items.size() > 64) {
                    recipeList.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(JEArchaeology.MODID, locationName + "_4"), new BrushingRecipe(Ingredient.of(items.values().stream().limit(42).skip(21).toList().toArray(new ItemStack[0])), 1f, pair.getSecond())));
                    recipeList.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(JEArchaeology.MODID, locationName + "_3"), new BrushingRecipe(Ingredient.of(items.values().stream().skip(42).toList().toArray(new ItemStack[0])), 1f, pair.getSecond())));
                }
                if (items.size() > 32) {
                    recipeList.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(JEArchaeology.MODID, locationName + "_2"), new BrushingRecipe(Ingredient.of(items.values().stream().limit(21).toList().toArray(new ItemStack[0])), 1f, pair.getSecond())));
                    recipeList.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(JEArchaeology.MODID, locationName + "_1"), new BrushingRecipe(Ingredient.of(items.values().stream().skip(21).toList().toArray(new ItemStack[0])), 1f, pair.getSecond())));
                } else {
                    recipeList.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(JEArchaeology.MODID, locationName), new BrushingRecipe(Ingredient.of(items.values().toArray(new ItemStack[0])), 1f, pair.getSecond())));
                }
            });
            cachedBrushingRecipes = recipeList;
        }
        return cachedBrushingRecipes;
    }

}
