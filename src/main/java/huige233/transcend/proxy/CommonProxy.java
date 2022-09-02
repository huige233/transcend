package huige233.transcend.proxy;

import huige233.transcend.compat.Avartiabreak;
import huige233.transcend.compat.PsiCompat;
import huige233.transcend.compat.tinkers.TiCConfig;
import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {
    public void registerItemRenderer( Item item, int meta, String id )
    {
    }


    public void preInit( FMLPreInitializationEvent event )
    {
        if(Loader.isModLoaded("psi")){
            MinecraftForge.EVENT_BUS.register(PsiCompat.class);
        }
        if(Loader.isModLoaded("avartia")) {
            Avartiabreak.enabled = true;
        }
        if(Loader.isModLoaded("tconstruct")){
            TiCConfig.setup();
            TiCConfig.setRenderInfo();
            //MinecraftForge.EVENT_BUS.register(TiCConfig.setup());
        }
    }
}
