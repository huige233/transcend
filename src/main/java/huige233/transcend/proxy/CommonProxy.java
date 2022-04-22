package huige233.transcend.proxy;

import huige233.transcend.compat.Avartiabreak;
import huige233.transcend.compat.PsiCompat;
import huige233.transcend.compat.TinkersCompat;
import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
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
            MinecraftForge.EVENT_BUS.register(PsiCompat.class);
        }
        if(Loader.isModLoaded("avartia")) {
            Avartiabreak.enabled = true;
        }
        if(Loader.isModLoaded("tconstruct")) {

        }
        if(Loader.isModLoaded("tconstruct")){
            TinkersCompat.enabled = true;
            new TinkersCompat();
        }
    }
}
