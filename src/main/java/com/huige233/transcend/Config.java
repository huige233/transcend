package com.huige233.transcend;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

   
                                         
                                      
   
/** 定义实体编辑器的字节码扫描配置，并在配置加载时同步运行时开关。 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    
    private static final ForgeConfigSpec.BooleanValue EDITOR_ASM = BUILDER
            .comment("Entity editor: run bytecode (ASM) read/write analysis for fields to infer their meaning.",
                    "Uses ASM to scan entity class files; on complex/obfuscated classes it can hurt scan latency.",
                    "Default OFF. Toggle affects the next scan.")
            .define("editorAsmScan", false);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean editorAsmScan;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        editorAsmScan = EDITOR_ASM.get();
    }
}
