package com.huige233.transcend.client.magic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class MagicCircleFactory {

    public static AbstractMagicCircle create(MagicCircleType type, ServerLevel level, Vec3 center) {
        return create(type, level, center, 1.0F, 1.0F, 0, null);
    }

    public static AbstractMagicCircle create(MagicCircleType type, ServerLevel level, Vec3 center,
                                              float powerMult, float durationMult) {
        return create(type, level, center, powerMult, durationMult, 0, null);
    }

    public static AbstractMagicCircle create(MagicCircleType type, ServerLevel level, Vec3 center,
                                              float powerMult, float durationMult, int specialLvl) {
        return create(type, level, center, powerMult, durationMult, specialLvl, null);
    }

    public static AbstractMagicCircle create(MagicCircleType type, ServerLevel level, Vec3 center,
                                              float powerMult, float durationMult, int specialLvl, UUID ownerUUID) {
        AbstractMagicCircle effect = switch (type) {
            case ARCANE -> new MagicCircleEffectServer(level, center);
            case ELDRITCH -> new MagicCircleEffectServer1(level, center);
            case INFERNO -> new InfernoCircleEffect(level, center);
            case GLACIAL -> new GlacialCircleEffect(level, center);
            case SANCTUM -> new SanctumCircleEffect(level, center);
            case GRAVITY -> new GravityCircleEffect(level, center);
            case THUNDER -> new ThunderCircleEffect(level, center);
            case TEMPEST -> new TempestCircleEffect(level, center);
            case TERRA -> new TerraCircleEffect(level, center);
            case VOID -> new VoidCircleEffect(level, center);
            case CHRONO -> new ChronoCircleEffect(level, center);
            case BLOOD -> new BloodCircleEffect(level, center);
            case DIVINE -> new DivineCircleEffect(level, center);
            case CHAOS -> new ChaosCircleEffect(level, center);
            case PHANTOM -> new PhantomCircleEffect(level, center);
            case SKYBOUND -> new SkyboundCircleEffect(level, center);
        };

        float radiusMult = 1.0F + (powerMult - 1.0F) * 0.3F;

        return effect
                .withColor(type.baseR, type.baseG, type.baseB)
                .withAccentColor(type.accentR, type.accentG, type.accentB)
                .withMaxAge((int) (type.duration * durationMult))
                .withPowerMultiplier(powerMult)
                .withRadiusMultiplier(radiusMult)
                .withSpecialLevel(specialLvl)
                .withOwner(ownerUUID);
    }
}
