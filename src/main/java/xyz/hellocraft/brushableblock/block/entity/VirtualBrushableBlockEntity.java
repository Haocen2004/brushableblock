package xyz.hellocraft.brushableblock.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import xyz.hellocraft.brushableblock.mixin.BrushableBlockEntityAccessor;

public class VirtualBrushableBlockEntity extends BrushableBlockEntity {
    private final BlockState originalState;
    private final int totalBrushes = 8;
    private final int perStage = 2;

    public VirtualBrushableBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
        this.originalState = state;
    }

    @Override
    public BlockState getBlockState() {
        // Trick the renderer by returning a state that has the 'dusted' property
        // We use suspicious sand as a template
        int count = ((BrushableBlockEntityAccessor)this).getBrushCount();
        return Blocks.SUSPICIOUS_SAND.defaultBlockState()
                .setValue(BlockStateProperties.DUSTED, Math.min(3, count / perStage));
    }

    @Override
    public boolean brush(long gameTime, Player player, Direction direction) {
        if (this.level == null) return false;

        BrushableBlockEntityAccessor accessor = (BrushableBlockEntityAccessor) this;
        accessor.setHitDirection(direction);

        if (this.level.isClientSide) {
            // Client side: Update progress for animation
            int count = accessor.getBrushCount();
            if (count < totalBrushes) {
                accessor.setBrushCount(count + 1);
            }
            return true;
        }

        // Server side
        if (this.getItem().isEmpty()) {
            this.unpackLootTable(player);
            // Sync to clients so they can render the emerging item
            this.level.sendBlockUpdated(this.worldPosition, this.originalState, this.originalState, 3);
        }

        int count = accessor.getBrushCount();
        if (count % perStage == 0) {
            this.level.playSound(null, this.worldPosition, net.minecraft.sounds.SoundEvents.BRUSH_SAND, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        if (count < totalBrushes) {
            accessor.setBrushCount(count + 1);
            return true;
        }

        // Drop item
        ItemStack stack = this.getItem();
        if (!stack.isEmpty()) {
            this.level.playSound(null, this.worldPosition, net.minecraft.sounds.SoundEvents.BRUSH_SAND_COMPLETED, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            
            // Check for block transformation
            String transformedBlockId = stack.get(xyz.hellocraft.brushableblock.registration.ModContent.TRANSFORMED_BLOCK.get());
            if (transformedBlockId != null) {
                net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.parse(transformedBlockId);
                net.minecraft.world.level.block.Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(rl);
                if (block != net.minecraft.world.level.block.Blocks.AIR) {
                    this.level.setBlock(this.worldPosition, block.defaultBlockState(), 3);
                }
            }

            Direction dropDir = direction.getOpposite();
            double x = (double)this.worldPosition.getX() + 0.5D + (double)dropDir.getStepX() * 0.35D;
            double y = (double)this.worldPosition.getY() + 0.5D + (double)dropDir.getStepY() * 0.35D;
            double z = (double)this.worldPosition.getZ() + 0.5D + (double)dropDir.getStepZ() * 0.35D;
            
            // If transformation changed the block, the BE might be invalid now, but we are a virtual BE 
            // from BrushingManager, so we can still finish our logic.
            net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(this.level, x, y, z, stack.copy());
            itemEntity.setDefaultPickUpDelay();
            this.level.addFreshEntity(itemEntity);
            accessor.setBrushedItem(ItemStack.EMPTY);
            // Final sync to clear the item on clients
            this.level.sendBlockUpdated(this.worldPosition, this.originalState, this.originalState, 3);
        }

        return false;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }
}
