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

/**
 * 因果编辑器独立通道：不占用主通道的注册 ID，避免触犯
 * S2COpenTestDummyScreenTest 对主通道 0..21+22 的硬性契约。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class EditorNetwork {
    private static final String PROTOCOL_VERSION = "editor1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Transcend.rl("editor"),
            () -> PROTOCOL_VERSION,
            remote -> PROTOCOL_VERSION.equals(remote),
            remote -> PROTOCOL_VERSION.equals(remote));
    private static int id = 0;

    /** 编辑通道：注册 C2S 编辑包与 S2C 扫描包的逻辑 ID。 */
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