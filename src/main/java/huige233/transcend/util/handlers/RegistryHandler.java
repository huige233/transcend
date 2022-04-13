package huige233.transcend.util.handlers;

import huige233.transcend.init.ModEnchantment;
import huige233.transcend.init.ModItems;
import huige233.transcend.util.IHasModel;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber

public class RegistryHandler {
    @SubscribeEvent
    public static void onItemRegister( RegistryEvent.Register<Item> event )
    {
        event.getRegistry().registerAll( ModItems.ITEMS.toArray( new Item[0] ) );
    }


    @SubscribeEvent
    public static void onModelRegister( ModelRegistryEvent event )
    {
        for ( Item item : ModItems.ITEMS )
        {
            if ( item instanceof IHasModel )
            {
                ( (IHasModel) item).registerModels();
            }
        }
    }


    @SubscribeEvent
    public static void onEnchantmentRegister( RegistryEvent.Register<Enchantment> event )
    {
        event.getRegistry().registerAll( ModEnchantment.ENCHANTMENTS.toArray( new Enchantment[0] ) );
    }
}
