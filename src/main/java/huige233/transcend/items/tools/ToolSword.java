package huige233.transcend.items.tools;

import huige233.transcend.Main;
import huige233.transcend.init.ModItems;
import huige233.transcend.util.IHasModel;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;


public class ToolSword extends ItemSword implements IHasModel {
    public ToolSword(String name, CreativeTabs tab,ToolMaterial material) {
        super(material);
        setTranslationKey(name);
        setRegistryName(name);
        setCreativeTab(tab);
        ModItems.ITEMS.add(this);
    }
    @Override
    public void registerModels() {
        Main.proxy.registerItemRenderer(this,0,"inventory");
    }
}

