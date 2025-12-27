package xyz.hellocraft.brushableblock;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(BrushableBlock.MODID)
public class BrushableBlock {
    public static final String MODID = "brushableblock";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BrushableBlock(IEventBus modEventBus, ModContainer modContainer) {
        xyz.hellocraft.brushableblock.registration.ModContent.register(modEventBus);
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, xyz.hellocraft.brushableblock.config.ModConfig.SPEC);
    }
}
