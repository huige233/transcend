package com.huige233.transcend.tech.assembly;

import com.huige233.transcend.Transcend;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 注册装配配方类型及其序列化器，并将注册表接入模组事件总线。 */
public final class AssemblyRecipeRegistration {
    private static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, Transcend.MODID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Transcend.MODID);
    public static final RegistryObject<RecipeType<AssemblyRecipe>> TYPE = TYPES.register("assembly", () -> new RecipeType<>() {
        @Override public String toString() { return "transcend:assembly"; }
    });
    public static final RegistryObject<RecipeSerializer<AssemblyRecipe>> SERIALIZER =
            SERIALIZERS.register("assembly", AssemblyRecipe.Serializer::new);

    private AssemblyRecipeRegistration() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
    }
}
