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
import xyz.hellocraft.brushableblock.block.entity.VirtualBrushableBlockEntity;
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
            // Render if the block is natively brushable OR if there's an active virtual entity already managing it
            // (Allows the overlay/item to linger for a moment even if the block transformed)
            boolean isBrushable = state.is(ModTags.Blocks.BRUSHABLE);
            
            if (isBrushable || be instanceof VirtualBrushableBlockEntity) {
                
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
                xyz.hellocraft.brushableblock.config.ModConfig.OverlayMode mode = xyz.hellocraft.brushableblock.config.ModConfig.OVERLAY_MODE.get();
                if (mode == xyz.hellocraft.brushableblock.config.ModConfig.OverlayMode.NONE) {
                    return;
                }

                BrushableBlockEntityAccessor accessor = (BrushableBlockEntityAccessor) be;
                int count = accessor.getBrushCount();
                if (count > 0) {
                    // perStage is 2 in VirtualBrushableBlockEntity, max is 8.
                    // Map count to 0-3 stages to match archaeology 'dusted' property.
                    int stage = Math.min(3, count / 2); 

                    poseStack.pushPose();
                    poseStack.translate(pos.getX() - event.getCamera().getPosition().x, 
                                       pos.getY() - event.getCamera().getPosition().y, 
                                       pos.getZ() - event.getCamera().getPosition().z);
                    
                    renderBlockWithCracks(state, pos, mc, poseStack, bufferSource, stage);

                    poseStack.popPose();
                }
            }
        });
    }

    private static void renderBlockWithCracks(BlockState state, BlockPos pos, Minecraft mc, PoseStack poseStack, MultiBufferSource bufferSource, int stage) {
        net.minecraft.client.renderer.RenderType overlayType;

        if (xyz.hellocraft.brushableblock.config.ModConfig.OVERLAY_MODE.get() == xyz.hellocraft.brushableblock.config.ModConfig.OverlayMode.VANILLA) {
            // Map 0-3 stages to 0-9 vanilla breaking stages
            int vanillaStage = Math.min(9, stage * 3);
            Object typeObj = ModelBakery.DESTROY_TYPES.get(vanillaStage);
            if (typeObj instanceof net.minecraft.client.renderer.RenderType) {
                overlayType = (net.minecraft.client.renderer.RenderType) typeObj;
            } else {
                overlayType = net.minecraft.client.renderer.RenderType.crumbling((net.minecraft.resources.ResourceLocation) typeObj);
            }
        } else {
            // Support custom textures: brushableblock:textures/gui/brushing_overlay_<0-3>.png
            // Note: Customs use 'crumbling' RenderType to get the same blending effect
            net.minecraft.resources.ResourceLocation customLoc = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                BrushableBlock.MODID, "textures/block/brushing_overlay_" + stage + ".png");
            overlayType = net.minecraft.client.renderer.RenderType.crumbling(customLoc);
        }

        // Restore micro-scaling to definitively fix Z-fighting
        poseStack.scale(1.001F, 1.001F, 1.001F);
        poseStack.translate(-0.0005, -0.0005, -0.0005);

        VertexConsumer decalConsumer = new SheetedDecalTextureGenerator(
            bufferSource.getBuffer(overlayType), 
            poseStack.last(), 1.0F
        );

        // Get neighbor light for accurate surface brightness
        BrushableBlockEntity entity = BrushingManager.getVirtualEntity(pos);
        Direction hitDir = (entity != null) ? entity.getHitDirection() : Direction.UP;
        BlockPos lightPos = (hitDir != null) ? pos.relative(hitDir) : pos.above();
        int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(mc.level, lightPos);
        
        // Only render the decal part.
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
