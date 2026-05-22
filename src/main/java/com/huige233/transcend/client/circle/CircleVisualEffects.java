package com.huige233.transcend.client.circle;

import com.huige233.transcend.circle.CircleFunctionType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * 法环视觉效果 — 粒子和音效。
 * 由核心BE的客户端tick或渲染器调用。
 */
public class CircleVisualEffects {
    
    /**
     * 在法环核心周围生成运转粒子（客户端每tick调用）。
     * 根据功能类型选择不同粒子。
     */
    public static void spawnActiveParticles(Level level, BlockPos corePos, 
                                            CircleFunctionType function, int tier, long gameTime) {
        if (level.getGameTime() % 4 != 0) return; // 每4tick一次，减少性能开销
        
        RandomSource random = level.getRandom();
        double cx = corePos.getX() + 0.5;
        double cy = corePos.getY() + 1.0;
        double cz = corePos.getZ() + 0.5;
        float radius = 1.0f + tier * 0.5f;
        
        // 环形粒子
        int particleCount = 2 + tier;
        for (int i = 0; i < particleCount; i++) {
            double angle = gameTime * 0.05 + i * (2 * Math.PI / particleCount);
            double px = cx + radius * Math.cos(angle);
            double pz = cz + radius * Math.sin(angle);
            
            ParticleOptions particle = getParticleForFunction(function);
            level.addParticle(particle, px, cy + random.nextDouble() * 0.3, pz, 0, 0.02, 0);
        }
        
        // 中心上升粒子
        if (gameTime % 20 == 0) {
            level.addParticle(ParticleTypes.ENCHANT, cx, cy, cz, 0, 0.1, 0);
        }
    }
    
    private static ParticleOptions getParticleForFunction(CircleFunctionType function) {
        if (function == null || function.getCategory() == null) {
            return ParticleTypes.END_ROD;
        }
        
        switch (function.getCategory()) {
            case MANA_LOGISTICS: return ParticleTypes.SOUL_FIRE_FLAME;
            case PLAYER_BUFF: return ParticleTypes.HAPPY_VILLAGER;
            case WORLD_INTERACTION: return ParticleTypes.ENCHANT;
            case ADVANCED: return ParticleTypes.PORTAL;
            case FARMING: return ParticleTypes.COMPOSTER;
            case DEFENSE: return ParticleTypes.CRIT;
            // DANGEROUS case mapped to default or smoke if it exists in CircleCategory
            default: return ParticleTypes.END_ROD;
        }
    }
    
    /**
     * 播放法环环境音效（每80tick）
     */
    public static void playAmbientSound(Level level, BlockPos pos, long gameTime) {
        if (gameTime % 80 != 0) return;
        level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.3f, 1.2f, false);
    }
}