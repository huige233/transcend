package huige233.transcend.init;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class ModRecipes {
    public static void init() {
        GameRegistry.addSmelting(ModBlock.TRANSCEND_BLOCK,new ItemStack(ModBlock.FLAWLESS_BLOCK,1),0.1f);
    }
}
