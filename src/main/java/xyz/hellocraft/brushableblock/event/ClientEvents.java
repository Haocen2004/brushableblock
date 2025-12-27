package xyz.hellocraft.brushableblock.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import xyz.hellocraft.brushableblock.BrushableBlock;
import xyz.hellocraft.brushableblock.block.BrushingManager;
import xyz.hellocraft.brushableblock.mixin.BrushableBlockEntityAccessor;
import xyz.hellocraft.brushableblock.tag.ModTags;

@EventBusSubscriber(modid = BrushableBlock.MODID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = mc.renderBuffers().bufferSource();

        BrushingManager.getActiveEntities().forEach((pos, be) -> {
            BlockState state = mc.level.getBlockState(pos);
            if (state.is(ModTags.Blocks.BRUSHABLE)) {
                
                // 1. Render the emerging item (Vanilla behavior)
                @SuppressWarnings("unchecked")
                BlockEntityRenderer<BrushableBlockEntity> renderer = mc.getBlockEntityRenderDispatcher().getRenderer(be);
                if (renderer != null) {
                    poseStack.pushPose();
                    poseStack.translate(pos.getX() - event.getCamera().getPosition().x, 
                                       pos.getY() - event.getCamera().getPosition().y, 
                                       pos.getZ() - event.getCamera().getPosition().z);
                    int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(mc.level, pos);
                    renderer.render(be, event.getPartialTick().getGameTimeDeltaTicks(), poseStack, bufferSource, light, OverlayTexture.NO_OVERLAY);
                    poseStack.popPose();
                }

                // 2. Render breaking cracks overlay mapping brushCount to 0-3 stages (then to 0-9 crack levels)
                BrushableBlockEntityAccessor accessor = (BrushableBlockEntityAccessor) be;
                int count = accessor.getBrushCount();
                if (count > 0) {
                    // perStage is 2 in VirtualBrushableBlockEntity, totalBrushes is 8.
                    // Map count to 0-3 dusted stages, then scaled to 0-9 breaking stages.
                    int dustedStage = Math.min(3, count / 2); 
                    int breakProgress = Math.min(9, dustedStage * 3);

                    poseStack.pushPose();
                    poseStack.translate(pos.getX() - event.getCamera().getPosition().x, 
                                       pos.getY() - event.getCamera().getPosition().y, 
                                       pos.getZ() - event.getCamera().getPosition().z);
                    
                    renderBlockWithCracks(state, pos, mc, poseStack, bufferSource, breakProgress);

                    poseStack.popPose();
                }
            }
        });
    }

    private static void renderBlockWithCracks(BlockState state, BlockPos pos, Minecraft mc, PoseStack poseStack, MultiBufferSource bufferSource, int breakProgress) {
        Object typeObj = ModelBakery.DESTROY_TYPES.get(breakProgress);
        net.minecraft.client.renderer.RenderType crumblingType;
        if (typeObj instanceof net.minecraft.client.renderer.RenderType) {
            crumblingType = (net.minecraft.client.renderer.RenderType) typeObj;
        } else {
            crumblingType = net.minecraft.client.renderer.RenderType.crumbling((net.minecraft.resources.ResourceLocation) typeObj);
        }

        // Restore micro-scaling to definitively fix Z-fighting
        poseStack.scale(1.001F, 1.001F, 1.001F);
        poseStack.translate(-0.0005, -0.0005, -0.0005);

        VertexConsumer decalConsumer = new SheetedDecalTextureGenerator(
            bufferSource.getBuffer(crumblingType), 
            poseStack.last(), 1.0F
        );

        // Get neighbor light for accurate surface brightness
        BrushableBlockEntity entity = BrushingManager.getVirtualEntity(pos);
        Direction hitDir = (entity != null) ? entity.getHitDirection() : Direction.UP;
        BlockPos lightPos = (hitDir != null) ? pos.relative(hitDir) : pos.above();
        int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(mc.level, lightPos);
        
        // Only render the decal part. This overlays the cracks without re-rendering the base block colors.
        MultiBufferSource progressOnlyBuffer = type -> decalConsumer;

        mc.getBlockRenderer().renderSingleBlock(
            state, 
            poseStack, 
            progressOnlyBuffer, 
            light, 
            OverlayTexture.NO_OVERLAY, 
            ModelData.EMPTY, 
            null
        );
    }
}
