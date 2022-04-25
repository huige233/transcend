package huige233.transcend.proxy;

import huige233.transcend.compat.Avartiabreak;
import huige233.transcend.compat.BotaniaCompat;
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
        if(Loader.isModLoaded("tconstruct")){
            MinecraftForge.EVENT_BUS.register(TinkersCompat.class);
            if(Loader.isModLoaded("conarm")){
                TinkersCompat.enabled1=true;
            }
        }
        if(Loader.isModLoaded("botania")){
            MinecraftForge.EVENT_BUS.register(BotaniaCompat.class);
        }

    }
}
