package com.huige233.transcend;

import com.huige233.transcend.init.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

public class ModLangGen extends LanguageProvider {

    public ModLangGen(PackOutput output,String locale){super(output, Transcend.MODID,locale);}

    @Override
    protected void addTranslations() {
        add(ModItems.epic_ingot.get(),"epic_ingot");
        add(ModItems.normal_ingot.get(),"normal_ingot");
        add(ModItems.transcend_ingot.get(),"transcend_ingot");
    }
}
