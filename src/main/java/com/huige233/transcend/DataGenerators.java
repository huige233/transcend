package com.huige233.transcend;

import com.huige233.transcend.block.ModBlockModelGen;
import com.huige233.transcend.init.ModItemModelGen;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

/** 为数据生成事件注册物品和方块模型生成器，并保留手工维护的语言文件。 */
@Mod.EventBusSubscriber(modid = Transcend.MODID,bus=Mod.EventBusSubscriber.Bus.MOD)

public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event){
        DataGenerator generator=event.getGenerator();
        PackOutput output=generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider>lookupProvider=event.getLookupProvider();
        ExistingFileHelper helper=event.getExistingFileHelper();

        generator.addProvider(event.includeClient(),new ModItemModelGen(output,helper));
        
        
        generator.addProvider(event.includeClient(),new ModBlockModelGen(output,helper));

    }
}
