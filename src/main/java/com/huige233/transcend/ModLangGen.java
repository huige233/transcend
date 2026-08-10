package com.huige233.transcend;

import com.huige233.transcend.init.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

/** 语言(本地化)文件生成器：产出简体中文等 lang JSON。 */
public class ModLangGen extends LanguageProvider {

    public ModLangGen(PackOutput output,String locale){super(output, Transcend.MODID,locale);}

    @Override
    protected void addTranslations() {
        add(ModItems.epic_ingot.get(),"epic_ingot");
        add(ModItems.normal_ingot.get(),"normal_ingot");
        add(ModItems.transcend_ingot.get(),"transcend_ingot");
    }
}
