package com.huige233.transcend.handle;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.network.C2SEntityEditPacket;
import com.huige233.transcend.network.S2CEntityScanPacket;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

   
                                                 
   
/** 建立因果编辑器独立网络通道并注册实体编辑请求和扫描结果数据包。 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class EditorNetwork {
    private static final String PROTOCOL_VERSION = "editor1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Transcend.rl("editor"),
            () -> PROTOCOL_VERSION,
            remote -> PROTOCOL_VERSION.equals(remote),
            remote -> PROTOCOL_VERSION.equals(remote));
    private static int id = 0;

    
    @SubscribeEvent
    public static void init(FMLCommonSetupEvent event) {
        CHANNEL.registerMessage(id++, C2SEntityEditPacket.class,
                C2SEntityEditPacket::write, C2SEntityEditPacket::new,
                C2SEntityEditPacket::run, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, S2CEntityScanPacket.class,
                S2CEntityScanPacket::write, S2CEntityScanPacket::new,
                S2CEntityScanPacket::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}