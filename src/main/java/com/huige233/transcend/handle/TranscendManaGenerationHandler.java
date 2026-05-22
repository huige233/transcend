package com.huige233.transcend.handle;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.spell.SpellProjectile;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TranscendManaGenerationHandler {

    private static final float CHANCE = 0.05F;
    private static final float AMOUNT = 1.0F;
    private static final String ROLL_TAG = "transcend_mana_roll_done";

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        ServerLevel level = (ServerLevel) event.getEntity().level();
        BlockPos pos = event.getEntity().blockPosition();
        maybeAddChunkMana(level, pos);
    }

    @SubscribeEvent
    public static void onSpellProjectileJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getEntity() instanceof SpellProjectile projectile)) return;
        if (projectile.getPersistentData().getBoolean(ROLL_TAG)) return;
        projectile.getPersistentData().putBoolean(ROLL_TAG, true);
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = projectile.blockPosition();
        maybeAddChunkMana(level, pos);
    }

    private static void maybeAddChunkMana(ServerLevel level, BlockPos pos) {
        if (level.random.nextFloat() >= CHANCE) return;
        ChunkPos chunkPos = new ChunkPos(pos);
        ChunkManaSavedData data = ChunkManaSavedData.get(level);
        float currentMana = data.getMana(chunkPos);
        data.setMana(chunkPos, currentMana + AMOUNT);
    }
}
