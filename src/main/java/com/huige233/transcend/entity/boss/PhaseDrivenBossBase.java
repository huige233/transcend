package com.huige233.transcend.entity.boss;

import com.huige233.transcend.lib.effect.ShaderTaskOrchestrator;
import com.huige233.transcend.visual.ServerVisualBroadcaster;
import net.minecraft.server.level.ServerLevel;
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

public abstract class PhaseDrivenBossBase extends AbstractTranscendBoss {

    private final List<PhaseThresholdRule> phaseRules = new ArrayList<>();

    protected PhaseDrivenBossBase(EntityType<? extends Monster> type, Level level,
                                  Component displayName, BossEvent.BossBarColor barColor) {
        super(type, level, displayName, barColor);
    }

    protected final void addPhaseRule(BossPhase fromPhase, float maxHealthRate, BossPhase toPhase) {
        phaseRules.add(new PhaseThresholdRule(fromPhase, maxHealthRate, toPhase));
    }

    protected final boolean runPhaseStateMachine() {
        if (phaseRules.isEmpty()) {
            return false;
        }

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

    @Override
    protected void tickArenaAura() {
        UUID key = this.getUUID();
        if (!this.isInArenaDimension() || !this.isAlive()) {
            ShaderTaskOrchestrator.stop(key);
            return;
        }

        ShaderTaskOrchestrator.ensureRepeating(key, 0, 80, pulse -> {
            if (!this.isAlive() || !(this.level() instanceof ServerLevel sl) || !this.isInArenaDimension()) {
                ShaderTaskOrchestrator.stop(key);
                return;
            }
            float er = this.currentElement != null ? this.currentElement.getParticleR() : 1.0f;
            float eg = this.currentElement != null ? this.currentElement.getParticleG() : 1.0f;
            float eb = this.currentElement != null ? this.currentElement.getParticleB() : 1.0f;
            float auraRadius = 5.0f + this.currentPhase.ordinal() * 1.2f;
            ServerVisualBroadcaster.circle(sl,
                    new Vec3(this.getX(), this.getY() + 0.08, this.getZ()),
                    auraRadius, er, eg, eb, 50, 36, "hexagram");

            if ((pulse & 1) == 1 && this.getTarget() != null && this.getTarget().isAlive()) {
                ServerVisualBroadcaster.beam(sl,
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

    private record PhaseThresholdRule(BossPhase fromPhase, float maxHealthRate, BossPhase toPhase) {
    }
}
