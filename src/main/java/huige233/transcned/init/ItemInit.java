package huige233.transcned.init;

import huige233.transcned.Main;
import huige233.transcned.items.tool.ItemSword;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ItemInit {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Main.MOD_ID);

    public static final RegistryObject<Item> TRANSCEND = register("transcend_ingot",() -> new Item(new Item.Properties().tab(Main.TRANSCEND_TAB)));
    public static final RegistryObject<Item> TRANSCEND_SWORD = register("transcend_sword", ItemSword::new);

    private static <T extends Item> RegistryObject<T> register(final String name,final Supplier<T> item){
        return ITEMS.register(name,item);
    }
}
