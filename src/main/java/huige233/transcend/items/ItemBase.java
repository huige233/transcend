package huige233.transcend.items;

import huige233.transcend.Main;
import huige233.transcend.init.ModItems;
import huige233.transcend.util.IHasModel;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class ItemBase extends Item implements IHasModel {
    public ItemBase(String name, CreativeTabs tab) {
        setTranslationKey(name);
        setRegistryName(name);
        setCreativeTab(tab);
        ModItems.ITEMS.add(this);
    }

    @Override
    public void registerModels() {
        Main.proxy.registerItemRenderer(this, 0, "inventory");
    }

    public EnumRarity getRarity(ItemStack stack )
    {
        return(ModItems.COSMIC_RARITY);
    }
}
