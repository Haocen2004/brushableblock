package xyz.hellocraft.brushableblock.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import xyz.hellocraft.brushableblock.BrushableBlock;
import xyz.hellocraft.brushableblock.block.BrushingManager;

@EventBusSubscriber(modid = BrushableBlock.MODID)
public class CommonEvents {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        BrushingManager.tick(event.getLevel());
    }
}
