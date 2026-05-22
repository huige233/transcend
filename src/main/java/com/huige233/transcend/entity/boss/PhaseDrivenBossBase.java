package com.huige233.transcend.entity.boss;

import com.huige233.transcend.client.renderer.ShaderSpellRenderer;
import com.huige233.transcend.lib.effect.ShaderTaskOrchestrator;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 分阶段 Boss 通用基类。
 * <p>
 * 提供两类通用能力：
 * 1) 阶段阈值状态机（按血量百分比自动切阶段）
 * 2) 竞技场特效编排（基于 ShaderTaskOrchestrator）
 */
public abstract class PhaseDrivenBossBase extends AbstractTranscendBoss {

    private final List<PhaseThresholdRule> phaseRules = new ArrayList<>();

    protected PhaseDrivenBossBase(EntityType<? extends Monster> type, Level level,
                                  Component displayName, BossEvent.BossBarColor barColor) {
        super(type, level, displayName, barColor);
    }

    /**
     * 注册“当前阶段 -> 下一阶段”的血量阈值规则。
     *
     * @param fromPhase     当前阶段
     * @param maxHealthRate 最大血量比例阈值（例如 0.6 表示 <=60%）
     * @param toPhase       目标阶段
     */
    protected final void addPhaseRule(BossPhase fromPhase, float maxHealthRate, BossPhase toPhase) {
        phaseRules.add(new PhaseThresholdRule(fromPhase, maxHealthRate, toPhase));
    }

    /**
     * 运行血量阈值状态机。
     * Boss 转阶段条件：假血条耗尽（<=0）且真实HP被锁定在1。
     * addPhaseRule 中的 maxHealthRate 不再作为提前转阶段的依据，
     * 而是仅用于确定"当前阶段→下一阶段"的映射关系。
     *
     * @return 若发生阶段切换则返回 true
     */
    protected final boolean runPhaseStateMachine() {
        if (phaseRules.isEmpty()) {
            return false;
        }
        // 只在血量被打到1HP锁血时才转阶段
        if (this.getHealth() > 1.0F) {
            return false;
        }
        for (PhaseThresholdRule rule : phaseRules) {
            if (this.getCurrentPhase() == rule.fromPhase) {
                setPhase(rule.toPhase);
                return true;
            }
        }
        return false;
    }

    /**
     * 覆盖父类竞技场特效逻辑：改为交给任务编排器定时触发。
     */
    @Override
    protected void tickArenaAura() {
        UUID key = this.getUUID();
        if (!this.isInArenaDimension() || !this.isAlive()) {
            ShaderTaskOrchestrator.stop(key);
            return;
        }

        ShaderTaskOrchestrator.ensureRepeating(key, 0, 80, pulse -> {
            if (!this.isAlive() || this.level() == null || !this.isInArenaDimension()) {
                ShaderTaskOrchestrator.stop(key);
                return;
            }
            float er = this.currentElement != null ? this.currentElement.particleR : 1.0f;
            float eg = this.currentElement != null ? this.currentElement.particleG : 1.0f;
            float eb = this.currentElement != null ? this.currentElement.particleB : 1.0f;
            float auraRadius = 5.0f + this.currentPhase.ordinal() * 1.2f;
            ShaderSpellRenderer.addCircle(
                    new Vec3(this.getX(), this.getY() + 0.08, this.getZ()),
                    auraRadius, er, eg, eb, 50, 36, "hexagram");

            if ((pulse & 1) == 1 && this.getTarget() != null && this.getTarget().isAlive()) {
                ShaderSpellRenderer.addSpellEffect(
                        new Vec3(this.getX(), this.getEyeY(), this.getZ()),
                        new Vec3(this.getTarget().getX(), this.getTarget().getEyeY(), this.getTarget().getZ()),
                        er, eg, eb, 20, "beam");
            }
        });
    }

    @Override
    public void die(DamageSource source) {
        ShaderTaskOrchestrator.stop(this.getUUID());
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        ShaderTaskOrchestrator.stop(this.getUUID());
        super.remove(reason);
    }

    /**
     * 阶段阈值规则。
     */
    private record PhaseThresholdRule(BossPhase fromPhase, float maxHealthRate, BossPhase toPhase) {
    }
}
