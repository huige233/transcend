package com.huige233.transcend.init;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.block.SpellWorkbenchMenu;
import com.huige233.transcend.block.circle.CircleCoreMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Transcend.MODID);

    public static final RegistryObject<MenuType<SpellWorkbenchMenu>> SPELL_WORKBENCH_MENU =
            MENUS.register("spell_workbench_menu",
                    () -> IForgeMenuType.create((id, inv, data) -> new SpellWorkbenchMenu(id, inv, data)));

    public static final RegistryObject<MenuType<CircleCoreMenu>> CIRCLE_CORE_MENU =
            MENUS.register("circle_core_menu",
                    () -> IForgeMenuType.create((id, inv, data) -> new CircleCoreMenu(id, inv, data)));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
