package com.huige233.transcend.init;

import com.huige233.transcend.Transcend;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItemModelGen extends ItemModelProvider {
    public static final String GENERATED = "item/generated";

    public ModItemModelGen(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Transcend.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        itemGenerateModel(ModItems.normal_ingot.get(), resourceItem(itemName(ModItems.normal_ingot.get())));
        itemGenerateModel(ModItems.epic_ingot.get(), resourceItem(itemName(ModItems.epic_ingot.get())));
        itemGenerateModel(ModItems.transcend_ingot.get(), resourceItem(itemName(ModItems.transcend_ingot.get())));
        itemGenerateModel(ModItems.ascension_book.get(), resourceItem(itemName(ModItems.ascension_book.get())));
    }

    public void itemGenerateModel(Item item, ResourceLocation texture) {
        withExistingParent(itemName(item), GENERATED).texture("layer0", texture);
    }

    public String itemName(Item item) {
        return ForgeRegistries.ITEMS.getKey(item).getPath();
    }

    public ResourceLocation resourceItem(String path) {
        return new ResourceLocation(Transcend.MODID, "item/" + path);
    }
}
