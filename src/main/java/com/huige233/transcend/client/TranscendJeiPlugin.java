package com.huige233.transcend.client;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.items.tech.GunModule;
import com.huige233.transcend.items.tech.GunModuleItem;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;


/** 向 JEI 注册装配配方类别和配方，并按模块数据区分枪械与护盾模块物品子类型。 */
@JeiPlugin
public final class TranscendJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() { return Transcend.rl("jei"); }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AssemblyJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(mezz.jei.api.registration.IRecipeRegistration registration) {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level != null) {
            var recipes = minecraft.level.getRecipeManager().getAllRecipesFor(
                    com.huige233.transcend.tech.assembly.AssemblyRecipeRegistration.TYPE.get());
            registration.addRecipes(AssemblyJeiCategory.TYPE, recipes);
        }
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(ModItems.gun_module.get(), (stack, context) -> {
            GunModule module = GunModuleItem.getModule(stack);
            return module == null ? "unknown" : module.id;
        });
        registration.registerSubtypeInterpreter(ModItems.shield_module.get(), (stack, context) -> {
            var module = com.huige233.transcend.items.tech.ShieldModuleItem.getModule(stack);
            return module == null ? "unknown" : module.id();
        });
    }
}
