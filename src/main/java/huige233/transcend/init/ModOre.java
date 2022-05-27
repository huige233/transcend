package huige233.transcend.init;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.oredict.OreDictionary;

public class ModOre {
    public ModOre(FMLPreInitializationEvent event) {
        ItemStack transcend = new ItemStack(ModItems.TRANSCEND);
        ItemStack flawless = new ItemStack(ModItems.FLAWLESS);
        OreDictionary.registerOre("transcend",transcend);
        OreDictionary.registerOre("flawless",flawless);
    }
}
