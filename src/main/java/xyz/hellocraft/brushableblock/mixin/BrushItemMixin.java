package xyz.hellocraft.brushableblock.mixin;

import net.minecraft.world.item.BrushItem;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.hellocraft.brushableblock.block.BrushingManager;
import xyz.hellocraft.brushableblock.tag.ModTags;

@Mixin(BrushItem.class)
public class BrushItemMixin {

    @Redirect(
        method = "onUseTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;")
    )
    private net.minecraft.world.level.block.Block redirectGetBlock(BlockState state) {
        if (state.is(ModTags.Blocks.BRUSHABLE)) {
            return net.minecraft.world.level.block.Blocks.SUSPICIOUS_SAND;
        }
        return state.getBlock();
    }

    @Redirect(
        method = "onUseTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;")
    )
    private net.minecraft.world.level.block.entity.BlockEntity redirectGetBlockEntity(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModTags.Blocks.BRUSHABLE)) {
                return BrushingManager.getOrCreateVirtualEntity(level, pos, state);
            }
        }
        return be;
    }

}
