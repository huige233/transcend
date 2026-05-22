package com.huige233.transcend;

import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.PlayerAscensionData;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import com.huige233.transcend.init.ModEntities;
import com.huige233.transcend.init.ModParticles;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegisterEvent;

import java.util.Objects;

public class ModSetup {
    public static void registers(IEventBus modEventBus){
        TranscendAttributes.ATTRIBUTES.register(modEventBus);
        ModEntities.register(modEventBus);
        ModParticles.register(modEventBus);
        modEventBus.addListener(ModSetup::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerAscensionData.class);
    }
}
