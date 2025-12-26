package xyz.hellocraft.brushableblock.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.hellocraft.brushableblock.tag.ModTags;

import java.util.Set;

@Mixin(BlockEntityType.class)
public class BlockEntityTypeMixin {
    @Shadow
    private Set<Block> validBlocks;

    @Inject(
        method = "isValid",
        at = @At("RETURN"),
        cancellable = true
    )
    public void isValidMixin(BlockState state, CallbackInfoReturnable<Boolean> ci) {

        if (state.is(ModTags.Blocks.BRUSHABLE)) {
            if ( validBlocks.stream().anyMatch(block ->
                    block.getDescriptionId().equals("block.minecraft.suspicious_sand") ||
                    block.getDescriptionId().equals("block.minecraft.suspicious_gravel"))
            ) {
                ci.setReturnValue(true);
            }
        }
    }
}
