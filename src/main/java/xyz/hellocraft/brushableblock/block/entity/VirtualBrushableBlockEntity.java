package xyz.hellocraft.brushableblock.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import xyz.hellocraft.brushableblock.mixin.BrushableBlockEntityAccessor;
import xyz.hellocraft.brushableblock.registration.TransformedBlockData;

import java.util.Map;

import static xyz.hellocraft.brushableblock.registration.ModContent.TRANSFORMED_BLOCK;

public class VirtualBrushableBlockEntity extends BrushableBlockEntity {
    private final BlockState originalState;
    private final int totalBrushes = 8;
    private final int perStage = 2;

    public VirtualBrushableBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
        this.originalState = state;
    }

    @Override
    public @NotNull BlockState getBlockState() {
        // Trick the renderer by returning a state that has the 'dusted' property
        // We use suspicious sand as a template
        int count = ((BrushableBlockEntityAccessor) this).getBrushCount();
        return Blocks.SUSPICIOUS_SAND.defaultBlockState().setValue(BlockStateProperties.DUSTED, Math.min(3, count / perStage));
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
            return false;
        }

        // Server side
        if (this.getItem().isEmpty()) {
            this.unpackLootTable(player);
            // Sync to clients so they can render the emerging item
            this.level.sendBlockUpdated(this.worldPosition, this.originalState, this.originalState, 3);
        }

        int count = accessor.getBrushCount();
        if (count % perStage == 0) {
            this.level.playSound(null, this.worldPosition, SoundEvents.BRUSH_SAND, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        if (count < totalBrushes) {
            accessor.setBrushCount(count + 1);
            return false;
        }

        // Drop item
        ItemStack stack = this.getItem();
        if (!stack.isEmpty()) {
            this.level.playSound(null, this.worldPosition, SoundEvents.BRUSH_SAND_COMPLETED, SoundSource.BLOCKS, 1.0F, 1.0F);

            // Check for block transformation
            TransformedBlockData data = stack.get(TRANSFORMED_BLOCK.get());
            if (data != null) {
                ResourceLocation rl = ResourceLocation.parse(data.blockId());
                Block block = BuiltInRegistries.BLOCK.get(rl);
                if (block != Blocks.AIR) {
                    BlockState newState = block.defaultBlockState();
                    if (!data.properties().isEmpty()) {
                        StateDefinition<Block, BlockState> definition = block.getStateDefinition();
                        for (Map.Entry<String, String> entry : data.properties().entrySet()) {
                            Property<?> property = definition.getProperty(entry.getKey());
                            if (property != null) {
                                newState = setValueHelper(newState, property, entry.getValue());
                            }
                        }
                    }


                    if ((newState.hasProperty(BlockStateProperties.WATERLOGGED) && newState.getValue(BlockStateProperties.WATERLOGGED)) || block.equals(Blocks.WATER)) {

                        if (level.dimensionType().ultraWarm()) {

                            if (block.equals(Blocks.WATER)) {
                                newState = Blocks.AIR.defaultBlockState();
                            } else {
                                newState = newState.setValue(BlockStateProperties.WATERLOGGED, false);
                            }
                            playWaterEvaporation(this.level, this.worldPosition);

                        } else {
                            this.level.scheduleTick(this.worldPosition, Fluids.WATER, Fluids.WATER.getTickDelay(this.level));
                        }
                    }

                    this.level.setBlock(this.worldPosition, newState, Block.UPDATE_ALL);

                    data.components().ifPresent(patch -> {
                        BlockEntity be = this.level.getBlockEntity(this.worldPosition);
                        if (be != null) {
                            Item item = block.asItem();
                            if (item != Items.AIR) {
                                ItemStack dummyStack = new ItemStack(item);
                                dummyStack.applyComponents(patch);
                                be.applyComponentsFromItemStack(dummyStack);
                            }
                        }
                    });

                }

                stack.remove(TRANSFORMED_BLOCK.get());

            }

            if (!stack.isEmpty()) {
                double d0 = EntityType.ITEM.getWidth();
                double d1 = 1.0 - d0;
                double d2 = d0 / 2.0;
                BlockPos blockpos = this.worldPosition.relative(direction, 1);
                double d3 = (double) blockpos.getX() + 0.5 * d1 + d2;
                double d4 = (double) blockpos.getY() + 0.5 + (double) (EntityType.ITEM.getHeight() / 2.0F);
                double d5 = (double) blockpos.getZ() + 0.5 * d1 + d2;
                ItemEntity itementity = new ItemEntity(this.level, d3, d4, d5, stack.split(this.level.random.nextInt(21) + 10));
                itementity.setDeltaMovement(Vec3.ZERO);
                this.level.addFreshEntity(itementity);
                accessor.setBrushedItem(ItemStack.EMPTY);
            }

            return true;
        }

        return false;
    }

    private void playWaterEvaporation(Level level, BlockPos pos) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            for (int i = 0; i < 8; i++) {
                serverLevel.sendParticles(
                        ParticleTypes.LARGE_SMOKE,
                        (double) pos.getX() + level.random.nextDouble(),
                        (double) pos.getY() + level.random.nextDouble(),
                        (double) pos.getZ() + level.random.nextDouble(),
                        1,
                        0.0, 0.0, 0.0,
                        0.0
                );
            }
        }

        level.playSound(
                null,
                pos,
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,
                0.5F,
                2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F
        );
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> BlockState setValueHelper(BlockState state, Property<?> property, String value) {
        Property<T> p = (Property<T>) property;
        return p.getValue(value).map(v -> state.setValue(p, v)).orElse(state);
    }
}
