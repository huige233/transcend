package huige233.transcend.proxy;

import huige233.transcend.compat.PsiCompat;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {
    public void registerItemRenderer( Item item, int meta, String id )
    {
    }


    public void preInit( FMLPreInitializationEvent event )
    {
    }


    public void init( FMLInitializationEvent event )
    {
        if(Loader.isModLoaded("psi")){
            PsiCompat.enabled = true;
        }
    }
}
