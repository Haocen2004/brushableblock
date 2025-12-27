package xyz.hellocraft.brushableblock.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<OverlayMode> OVERLAY_MODE = BUILDER
            .comment("Choose the brushing progress overlay mode")
            .defineEnum("overlayMode", OverlayMode.NONE);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public enum OverlayMode {
        NONE,
        VANILLA,
        CUSTOM
    }
}
