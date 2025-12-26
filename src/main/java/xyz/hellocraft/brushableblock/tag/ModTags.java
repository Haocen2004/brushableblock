package xyz.hellocraft.brushableblock.tag;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import xyz.hellocraft.brushableblock.BrushableBlock;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> BRUSHABLE = tag("brushable");

        private static TagKey<Block> tag(String name) {
            return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(BrushableBlock.MODID, name));
        }
    }
}
