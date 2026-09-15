package com.huige233.transcend;

import com.huige233.transcend.init.ModEntities;
import com.huige233.transcend.init.ModParticles;
import com.huige233.transcend.tech.api.TechCapabilities;
import net.minecraftforge.eventbus.api.IEventBus;


/** 将自定义属性、实体、粒子和科技能力注册接入模组事件总线。 */
public class ModSetup {
    public static void registers(IEventBus modEventBus){
        TranscendAttributes.ATTRIBUTES.register(modEventBus);
        ModEntities.register(modEventBus);
        ModParticles.register(modEventBus);
        modEventBus.addListener(TechCapabilities::register);
    }
}
