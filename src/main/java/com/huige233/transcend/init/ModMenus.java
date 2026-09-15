package com.huige233.transcend.init;

import com.huige233.transcend.Transcend;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** 注册装配、培育、发电、研究和储能设备的菜单类型与网络创建工厂。 */
public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Transcend.MODID);
    public static final net.minecraftforge.registries.RegistryObject<MenuType<com.huige233.transcend.menu.AssemblyMenu>> ASSEMBLY = MENUS.register("assembly_table", () -> net.minecraftforge.common.extensions.IForgeMenuType.create(com.huige233.transcend.menu.AssemblyMenu::new));
    public static final net.minecraftforge.registries.RegistryObject<MenuType<com.huige233.transcend.menu.BlackHoleSeedBreederMenu>> BLACK_HOLE_SEED_BREEDER = MENUS.register("black_hole_seed_breeder", () -> net.minecraftforge.common.extensions.IForgeMenuType.create(com.huige233.transcend.menu.BlackHoleSeedBreederMenu::new));
    public static final net.minecraftforge.registries.RegistryObject<MenuType<com.huige233.transcend.menu.MiniUniverseGeneratorMenu>> MINI_UNIVERSE_GENERATOR = MENUS.register("mini_universe_generator", () -> net.minecraftforge.common.extensions.IForgeMenuType.create(com.huige233.transcend.menu.MiniUniverseGeneratorMenu::new));
    public static final net.minecraftforge.registries.RegistryObject<MenuType<com.huige233.transcend.menu.ResearchStationMenu>> RESEARCH_STATION = MENUS.register("research_station", () -> net.minecraftforge.common.extensions.IForgeMenuType.create(com.huige233.transcend.menu.ResearchStationMenu::new));
    public static final net.minecraftforge.registries.RegistryObject<MenuType<com.huige233.transcend.menu.GeneratorMenu>> GENERATOR = MENUS.register("generator", () -> net.minecraftforge.common.extensions.IForgeMenuType.create(com.huige233.transcend.menu.GeneratorMenu::new));
    public static final net.minecraftforge.registries.RegistryObject<MenuType<com.huige233.transcend.menu.LongStorageMenu>> LONG_STORAGE = MENUS.register("long_storage", () -> net.minecraftforge.common.extensions.IForgeMenuType.create(com.huige233.transcend.menu.LongStorageMenu::new));
    public static void register(IEventBus eventBus) { MENUS.register(eventBus); }
}
