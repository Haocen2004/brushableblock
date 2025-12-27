package xyz.hellocraft.brushableblock.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import xyz.hellocraft.brushableblock.BrushableBlock;
import xyz.hellocraft.brushableblock.block.BrushingManager;
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
            // Check if the block is still there and is brushable
            if (mc.level.getBlockState(pos).is(ModTags.Blocks.BRUSHABLE)) {
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
            }
        });
    }
}
