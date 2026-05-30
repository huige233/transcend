package com.huige233.transcend.handle;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.network.C2SAscensionAction;
import com.huige233.transcend.network.S2CAscensionSync;
import com.huige233.transcend.network.S2CTreeSync;
import com.huige233.transcend.network.C2STestDummySettingsPack;
import com.huige233.transcend.network.S2CGlitterBatchPack;
import com.huige233.transcend.network.S2CParticleBatchPack;
import com.huige233.transcend.network.S2CRuneBatchPack;
import com.huige233.transcend.network.S2CTotemPack;
import com.huige233.transcend.network.S2CVanillaParticleBatchPack;
import com.huige233.transcend.network.S2CNexusRuleSync;
import com.huige233.transcend.network.C2SCircleAction;
import com.huige233.transcend.network.C2SCircleSettingChange;
import com.huige233.transcend.network.S2CCircleStatus;
import com.huige233.transcend.network.S2CCircleGhostBlocks;
import com.huige233.transcend.network.S2COreRevealPack;
import com.huige233.transcend.network.S2CShaderEffectPack;
import com.huige233.transcend.network.S2CInnateManaSync;
import com.huige233.transcend.network.S2CChunkManaMapPack;
import com.huige233.transcend.network.C2SSpellBookSlotChange;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(Transcend.rl("main"), () -> {return "1.0";}, (s) -> {return true;}, (s) -> {return true;});
    public static int id = 0;

    @SubscribeEvent
    public static void init(FMLCommonSetupEvent event){
        CHANNEL.registerMessage(id++, S2CTotemPack.class, S2CTotemPack::write, S2CTotemPack::new, S2CTotemPack::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, S2CParticleBatchPack.class, S2CParticleBatchPack::write, S2CParticleBatchPack::new, S2CParticleBatchPack::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, S2CVanillaParticleBatchPack.class, S2CVanillaParticleBatchPack::write, S2CVanillaParticleBatchPack::new, S2CVanillaParticleBatchPack::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, S2CRuneBatchPack.class, S2CRuneBatchPack::write, S2CRuneBatchPack::new, S2CRuneBatchPack::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, S2CGlitterBatchPack.class, S2CGlitterBatchPack::write, S2CGlitterBatchPack::new, S2CGlitterBatchPack::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, S2CAscensionSync.class, S2CAscensionSync::write, S2CAscensionSync::new, S2CAscensionSync::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, C2SAscensionAction.class, C2SAscensionAction::write, C2SAscensionAction::new, C2SAscensionAction::run, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, S2CTreeSync.class, S2CTreeSync::write, S2CTreeSync::new, S2CTreeSync::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, C2STestDummySettingsPack.class, C2STestDummySettingsPack::write, C2STestDummySettingsPack::new, C2STestDummySettingsPack::run, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, S2CNexusRuleSync.class, S2CNexusRuleSync::write, S2CNexusRuleSync::new, S2CNexusRuleSync::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, C2SCircleAction.class, C2SCircleAction::write, C2SCircleAction::new, C2SCircleAction::run, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, S2CCircleStatus.class, S2CCircleStatus::write, S2CCircleStatus::new, S2CCircleStatus::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, C2SCircleSettingChange.class, C2SCircleSettingChange::write, C2SCircleSettingChange::new, C2SCircleSettingChange::run, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, S2CCircleGhostBlocks.class, S2CCircleGhostBlocks::write, S2CCircleGhostBlocks::new, S2CCircleGhostBlocks::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, S2COreRevealPack.class, S2COreRevealPack::write, S2COreRevealPack::new, S2COreRevealPack::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, S2CInnateManaSync.class, S2CInnateManaSync::write, S2CInnateManaSync::new, S2CInnateManaSync::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        // Round 39: Shader effect broadcast (server → nearby clients)
        CHANNEL.registerMessage(id++, S2CShaderEffectPack.class, S2CShaderEffectPack::write, S2CShaderEffectPack::new, S2CShaderEffectPack::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        // R78: Spellbook wheel-scroll slot change (client → server)
        CHANNEL.registerMessage(id++, C2SSpellBookSlotChange.class, C2SSpellBookSlotChange::write, C2SSpellBookSlotChange::new, C2SSpellBookSlotChange::run, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        // Chunk mana map GUI (server → client) — /tr_mana_map command 触发
        CHANNEL.registerMessage(id++, S2CChunkManaMapPack.class, S2CChunkManaMapPack::write, S2CChunkManaMapPack::new, S2CChunkManaMapPack::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
