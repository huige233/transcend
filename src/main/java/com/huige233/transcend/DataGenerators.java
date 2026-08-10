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

@Mod.EventBusSubscriber(modid = Transcend.MODID,bus=Mod.EventBusSubscriber.Bus.MOD)
/** 数据生成入口：为方块/物品/配方等批量注册 provider 数据。 */
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event){
        DataGenerator generator=event.getGenerator();
        PackOutput output=generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider>lookupProvider=event.getLookupProvider();
        ExistingFileHelper helper=event.getExistingFileHelper();

        generator.addProvider(event.includeClient(),new ModItemModelGen(output,helper));
        generator.addProvider(event.includeClient(),new ModLangGen(output,"en_US"));
        generator.addProvider(event.includeClient(),new ModBlockModelGen(output,helper));

    }
}
