package com.huige233.transcend.handle;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.network.C2SGeneratorOutputPacket;
import com.huige233.transcend.network.C2SMiniUniverseOverclockPacket;
import com.huige233.transcend.network.C2SResearchStartPacket;
import com.huige233.transcend.network.C2SReloadParticleGunAmmo;
import com.huige233.transcend.network.C2STestDummySettingsPack;
import com.huige233.transcend.network.C2STogglePhaseFlight;
import com.huige233.transcend.network.S2CGlitterBatchPack;
import com.huige233.transcend.network.S2COpenTestDummyScreen;
import com.huige233.transcend.network.S2CParticleBatchPack;
import com.huige233.transcend.network.S2CPhaseFlightSync;
import com.huige233.transcend.network.S2CPhaseShieldState;
import com.huige233.transcend.network.S2CShaderEffectPack;
import com.huige233.transcend.network.S2CRuneBatchPack;
import com.huige233.transcend.network.S2CResearchStationPacket;
import com.huige233.transcend.network.S2CTotemPack;
import com.huige233.transcend.network.S2CVanillaParticleBatchPack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/** 建立模组主网络通道并按固定编号和传输方向注册特效、装备与机器交互数据包。 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1.5";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Transcend.rl("main"), () -> PROTOCOL_VERSION,
            remoteVersion -> PROTOCOL_VERSION.equals(remoteVersion),
            remoteVersion -> PROTOCOL_VERSION.equals(remoteVersion));
    public static int id = 0;

    @SubscribeEvent
    public static void init(FMLCommonSetupEvent event){
        CHANNEL.registerMessage(id++, S2CShaderEffectPack.class, S2CShaderEffectPack::write, S2CShaderEffectPack::new, S2CShaderEffectPack::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, S2CTotemPack.class, S2CTotemPack::write, S2CTotemPack::new, S2CTotemPack::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, S2CParticleBatchPack.class, S2CParticleBatchPack::write, S2CParticleBatchPack::new, S2CParticleBatchPack::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, S2CVanillaParticleBatchPack.class, S2CVanillaParticleBatchPack::write, S2CVanillaParticleBatchPack::new, S2CVanillaParticleBatchPack::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, S2CRuneBatchPack.class, S2CRuneBatchPack::write, S2CRuneBatchPack::new, S2CRuneBatchPack::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, S2CGlitterBatchPack.class, S2CGlitterBatchPack::write, S2CGlitterBatchPack::new, S2CGlitterBatchPack::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, C2STestDummySettingsPack.class, C2STestDummySettingsPack::write, C2STestDummySettingsPack::new, C2STestDummySettingsPack::run, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        id++;                                                                                

        CHANNEL.registerMessage(id++, S2COpenTestDummyScreen.class, S2COpenTestDummyScreen::write, S2COpenTestDummyScreen::new, S2COpenTestDummyScreen::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, C2STogglePhaseFlight.class, C2STogglePhaseFlight::write, C2STogglePhaseFlight::new, C2STogglePhaseFlight::run, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, S2CPhaseFlightSync.class, S2CPhaseFlightSync::write, S2CPhaseFlightSync::new, S2CPhaseFlightSync::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, S2CPhaseShieldState.class, S2CPhaseShieldState::write, S2CPhaseShieldState::new, S2CPhaseShieldState::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, C2SGeneratorOutputPacket.class, C2SGeneratorOutputPacket::write, C2SGeneratorOutputPacket::new, C2SGeneratorOutputPacket::run, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, C2SMiniUniverseOverclockPacket.class, C2SMiniUniverseOverclockPacket::write, C2SMiniUniverseOverclockPacket::new, C2SMiniUniverseOverclockPacket::run, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, C2SReloadParticleGunAmmo.class, C2SReloadParticleGunAmmo::write, C2SReloadParticleGunAmmo::new, C2SReloadParticleGunAmmo::run, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, C2SResearchStartPacket.class, C2SResearchStartPacket::write, C2SResearchStartPacket::new, C2SResearchStartPacket::run, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, S2CResearchStationPacket.class, S2CResearchStationPacket::write, S2CResearchStationPacket::new, S2CResearchStationPacket::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
