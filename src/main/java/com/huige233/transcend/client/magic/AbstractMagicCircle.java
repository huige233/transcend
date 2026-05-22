package com.huige233.transcend.client.magic;

import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.util.EntityCompatUtil;
import com.huige233.transcend.network.S2CGlitterBatchPack;
import com.huige233.transcend.network.S2CParticleBatchPack;
import com.huige233.transcend.network.S2CRuneBatchPack;
import com.huige233.transcend.network.S2CVanillaParticleBatchPack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AbstractMagicCircle {

    protected final ServerLevel level;
    protected final Vec3 center;
    protected int age = 0;
    protected int maxAge = 100;
    protected boolean removed = false;

    protected float baseR = 0, baseG = 1, baseB = 1;
    protected float accentR = 0.5F, accentG = 0.3F, accentB = 1.0F;

    protected float powerMultiplier = 1.0F;
    protected float radiusMultiplier = 1.0F;
    protected int specialLevel = 0;
    protected UUID ownerUUID = null;

    protected static final Vector3f UP = new Vector3f(0, 1, 0);

    public AbstractMagicCircle(ServerLevel level, Vec3 center) {
        this.level = level;
        this.center = center;
    }

    public AbstractMagicCircle withColor(float r, float g, float b) {
        this.baseR = r;
        this.baseG = g;
        this.baseB = b;
        return this;
    }

    public AbstractMagicCircle withAccentColor(float r, float g, float b) {
        this.accentR = r;
        this.accentG = g;
        this.accentB = b;
        return this;
    }

    public AbstractMagicCircle withMaxAge(int ticks) {
        this.maxAge = ticks;
        return this;
    }

    public AbstractMagicCircle withPowerMultiplier(float mult) {
        this.powerMultiplier = mult;
        return this;
    }

    public AbstractMagicCircle withRadiusMultiplier(float mult) {
        this.radiusMultiplier = mult;
        return this;
    }

    public AbstractMagicCircle withSpecialLevel(int level) {
        this.specialLevel = level;
        return this;
    }

    public AbstractMagicCircle withOwner(UUID uuid) {
        this.ownerUUID = uuid;
        return this;
    }

    protected Player getOwnerPlayer() {
        return ownerUUID != null ? level.getPlayerByUUID(ownerUUID) : null;
    }

    protected DamageSource adaptDamageSourceForTarget(LivingEntity target, DamageSource fallback) {
        return EntityCompatUtil.adaptGaiaDamageSource(level, getOwnerPlayer(), target, fallback);
    }

    protected boolean hurtCompat(LivingEntity target, DamageSource fallback, float amount) {
        return target.hurt(adaptDamageSourceForTarget(target, fallback), amount);
    }

    protected boolean shouldAffect(Entity entity) {
        if (!entity.isAlive()) return false;
        if (ownerUUID != null && entity.getUUID().equals(ownerUUID)) return false;
        if (entity instanceof com.huige233.transcend.entity.SpellGuardian) return false;
        if (entity instanceof com.huige233.transcend.entity.SpellWisp) return false;
        if (ownerUUID != null && entity instanceof com.huige233.transcend.entity.boss.AbstractTranscendBoss) {
            Entity owner = level.getEntity(ownerUUID);
            if (owner instanceof com.huige233.transcend.entity.boss.AbstractTranscendBoss ownerBoss
                    && entity instanceof com.huige233.transcend.entity.boss.AbstractTranscendBoss targetBoss
                    && !ownerBoss.getFaction().isHostileTo(targetBoss.getFaction())) {
                return false;
            }
        }
        return true;
    }

    protected <T extends Mob> List<T> getMobsInRadius(Class<T> clazz, double radius) {
        AABB area = new AABB(center.x - radius, center.y - 2, center.z - radius,
                center.x + radius, center.y + 4, center.z + radius);
        return level.getEntitiesOfClass(clazz, area, mob -> {
            if (!shouldAffect(mob)) return false;
            double dx = mob.getX() - center.x;
            double dz = mob.getZ() - center.z;
            return Math.sqrt(dx * dx + dz * dz) <= radius;
        });
    }

    public boolean isRemoved() {
        return removed;
    }

    public int getAge() {
        return age;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public Vec3 getCenter() {
        return center;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public final void tick() {
        if (removed) return;
        if (age >= maxAge) {
            onRemove();
            removed = true;
            return;
        }

        float scaleFactor;
        if (age < 10) {
            scaleFactor = age / 10.0F;
        } else if (age > maxAge - 15) {
            scaleFactor = (maxAge - age) / 15.0F;
        } else {
            scaleFactor = 1.0F;
        }

        tickParticles(scaleFactor);
        tickEffect(scaleFactor);

        // Submit shader circle geometry every 6 ticks for denser shader presentation.
        if (age % 6 == 0) {
            submitShaderCircle(scaleFactor);
        }

        age++;
    }

    protected void submitShaderCircle(float scale) {
        float radius = getBaseRadius() * radiusMultiplier * scale;
        if (radius < 0.5F) return;

        com.huige233.transcend.client.renderer.ShaderSpellRenderer.addCircle(
                center, radius, baseR, baseG, baseB,
                12, 32, getCirclePattern());
    }

    protected float getBaseRadius() { return 5.0F; }
    protected String getCirclePattern() { return "hexagram"; }
    protected boolean preferShaderMagic() { return true; }

    protected abstract void tickParticles(float scaleFactor);

    protected abstract void tickEffect(float scaleFactor);

    protected abstract void onRemove();

    protected void sendBatch(List<S2CParticleBatchPack.ParticleEntry> entries,
                             float r, float g, float b, float scale, int lifetime) {
        if (entries.isEmpty()) return;
        if (preferShaderMagic()) {
            emitShaderFromDustBatch(entries, r, g, b, scale, lifetime);
            return;
        }
        S2CParticleBatchPack packet = new S2CParticleBatchPack(
                entries,
                new Vector3f(r, g, b),
                scale,
                lifetime,
                true
        );
        NetworkHandler.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        center.x, center.y, center.z, 64, level.dimension())),
                packet
        );
    }

    protected void sendVanillaBatch(List<S2CVanillaParticleBatchPack.VanillaParticleEntry> entries, String particleId) {
        if (entries.isEmpty()) return;
        if (preferShaderMagic()) {
            float[] color = vanillaParticleColor(particleId);
            emitShaderFromVanillaBatch(entries, color[0], color[1], color[2]);
            return;
        }
        S2CVanillaParticleBatchPack packet = new S2CVanillaParticleBatchPack(entries, particleId);
        NetworkHandler.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        center.x, center.y, center.z, 64, level.dimension())),
                packet
        );
    }

    protected void sendRuneBatch(List<S2CRuneBatchPack.RuneEntry> entries,
                                 float r, float g, float b, float scale, int lifetime, boolean glow) {
        if (entries.isEmpty()) return;
        if (preferShaderMagic()) {
            emitShaderFromRuneBatch(entries, r, g, b, scale, lifetime);
            return;
        }
        S2CRuneBatchPack packet = new S2CRuneBatchPack(
                entries,
                new Vector3f(r, g, b),
                scale,
                lifetime,
                glow
        );
        NetworkHandler.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        center.x, center.y, center.z, 64, level.dimension())),
                packet
        );
    }

    protected void sendGlitterBatch(List<S2CGlitterBatchPack.GlitterEntry> entries,
                                     float r, float g, float b, float scale, int lifetime, boolean glow) {
        if (entries.isEmpty()) return;
        if (preferShaderMagic()) {
            emitShaderFromGlitterBatch(entries, r, g, b, scale, lifetime);
            return;
        }
        S2CGlitterBatchPack packet = new S2CGlitterBatchPack(
                entries,
                new Vector3f(r, g, b),
                scale,
                lifetime,
                glow
        );
        NetworkHandler.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        center.x, center.y, center.z, 64, level.dimension())),
                packet
        );
    }

    protected void sendBatchAsGlitter(List<S2CParticleBatchPack.ParticleEntry> dustEntries,
                                       float r, float g, float b, float scale, int lifetime) {
        if (dustEntries.isEmpty()) return;
        List<S2CGlitterBatchPack.GlitterEntry> glitterEntries = new ArrayList<>(dustEntries.size());
        for (S2CParticleBatchPack.ParticleEntry e : dustEntries) {
            glitterEntries.add(new S2CGlitterBatchPack.GlitterEntry(e.x, e.y, e.z, e.xd, e.yd, e.zd));
        }
        sendGlitterBatch(glitterEntries, r, g, b, scale, lifetime, true);
    }

    private void emitShaderFromDustBatch(List<S2CParticleBatchPack.ParticleEntry> entries,
                                         float r, float g, float b, float scale, int lifetime) {
        if ((age & 1) != 0) {
            return;
        }
        Vec3 c = centroid(entries);
        float radius = estimateRadiusFromDust(entries, c, scale);
        int life = Math.max(8, Math.min(40, lifetime + 2));
        com.huige233.transcend.client.renderer.ShaderSpellRenderer.addCircle(
                c, radius, r, g, b, life, 28, getCirclePattern());
        if (entries.size() > 24 && age % 8 == 0) {
            com.huige233.transcend.client.renderer.ShaderSpellRenderer.addShockwave(
                    c, radius * 0.9F, r, g, b, Math.max(10, life - 4));
        }
    }

    private void emitShaderFromRuneBatch(List<S2CRuneBatchPack.RuneEntry> entries,
                                         float r, float g, float b, float scale, int lifetime) {
        if (entries.isEmpty()) {
            return;
        }
        int limit = Math.min(3, entries.size());
        for (int i = 0; i < limit; i++) {
            S2CRuneBatchPack.RuneEntry e = entries.get((age + i) % entries.size());
            Vec3 from = new Vec3(e.x, e.y, e.z);
            Vec3 to = new Vec3(e.targetX, e.targetY, e.targetZ);
            com.huige233.transcend.client.renderer.ShaderSpellRenderer.addSpellEffect(
                    from, to, r, g, b, Math.max(10, Math.min(30, lifetime + 8)), "beam");
        }
        if (age % 6 == 0) {
            S2CRuneBatchPack.RuneEntry first = entries.get(0);
            com.huige233.transcend.client.renderer.ShaderSpellRenderer.addShieldRipple(
                    new Vec3(first.x, first.y, first.z), Math.max(0.8F, scale * 1.5F), r, g, b, 18);
        }
    }

    private void emitShaderFromGlitterBatch(List<S2CGlitterBatchPack.GlitterEntry> entries,
                                            float r, float g, float b, float scale, int lifetime) {
        if (entries.isEmpty()) {
            return;
        }
        Vec3 c = centroidGlitter(entries);
        float radius = estimateRadiusFromGlitter(entries, c, scale);
        com.huige233.transcend.client.renderer.ShaderSpellRenderer.addShieldRipple(
                c, radius, r, g, b, Math.max(10, Math.min(32, lifetime + 6)));
    }

    private void emitShaderFromVanillaBatch(List<S2CVanillaParticleBatchPack.VanillaParticleEntry> entries,
                                            float r, float g, float b) {
        Vec3 c = centroidVanilla(entries);
        float radius = estimateRadiusFromVanilla(entries, c, 1.0F);
        com.huige233.transcend.client.renderer.ShaderSpellRenderer.addShockwave(
                c, radius, r, g, b, 16);
    }

    private static float[] vanillaParticleColor(String particleId) {
        if (particleId == null) return new float[]{0.9F, 0.9F, 0.9F};
        return switch (particleId) {
            case "flame", "smoke", "campfire_cosy_smoke" -> new float[]{1.0F, 0.5F, 0.15F};
            case "snowflake" -> new float[]{0.75F, 0.9F, 1.0F};
            case "witch", "portal" -> new float[]{0.6F, 0.2F, 0.8F};
            case "enchant", "end_rod" -> new float[]{1.0F, 0.95F, 0.6F};
            case "soul_fire_flame" -> new float[]{0.25F, 0.85F, 1.0F};
            default -> new float[]{0.9F, 0.9F, 0.9F};
        };
    }

    private static Vec3 centroid(List<S2CParticleBatchPack.ParticleEntry> entries) {
        double sx = 0, sy = 0, sz = 0;
        for (S2CParticleBatchPack.ParticleEntry e : entries) {
            sx += e.x;
            sy += e.y;
            sz += e.z;
        }
        double inv = 1.0 / entries.size();
        return new Vec3(sx * inv, sy * inv, sz * inv);
    }

    private static Vec3 centroidGlitter(List<S2CGlitterBatchPack.GlitterEntry> entries) {
        double sx = 0, sy = 0, sz = 0;
        for (S2CGlitterBatchPack.GlitterEntry e : entries) {
            sx += e.x;
            sy += e.y;
            sz += e.z;
        }
        double inv = 1.0 / entries.size();
        return new Vec3(sx * inv, sy * inv, sz * inv);
    }

    private static Vec3 centroidVanilla(List<S2CVanillaParticleBatchPack.VanillaParticleEntry> entries) {
        double sx = 0, sy = 0, sz = 0;
        for (S2CVanillaParticleBatchPack.VanillaParticleEntry e : entries) {
            sx += e.x;
            sy += e.y;
            sz += e.z;
        }
        double inv = 1.0 / entries.size();
        return new Vec3(sx * inv, sy * inv, sz * inv);
    }

    private static float estimateRadiusFromDust(List<S2CParticleBatchPack.ParticleEntry> entries, Vec3 center, float scale) {
        double maxDist = 0;
        for (S2CParticleBatchPack.ParticleEntry e : entries) {
            double dx = e.x - center.x;
            double dz = e.z - center.z;
            maxDist = Math.max(maxDist, Math.sqrt(dx * dx + dz * dz));
        }
        return (float) Math.max(0.7, maxDist + scale * 0.6F);
    }

    private static float estimateRadiusFromGlitter(List<S2CGlitterBatchPack.GlitterEntry> entries, Vec3 center, float scale) {
        double maxDist = 0;
        for (S2CGlitterBatchPack.GlitterEntry e : entries) {
            double dx = e.x - center.x;
            double dz = e.z - center.z;
            maxDist = Math.max(maxDist, Math.sqrt(dx * dx + dz * dz));
        }
        return (float) Math.max(0.6, maxDist + scale * 0.4F);
    }

    private static float estimateRadiusFromVanilla(List<S2CVanillaParticleBatchPack.VanillaParticleEntry> entries, Vec3 center, float scale) {
        double maxDist = 0;
        for (S2CVanillaParticleBatchPack.VanillaParticleEntry e : entries) {
            double dx = e.x - center.x;
            double dz = e.z - center.z;
            maxDist = Math.max(maxDist, Math.sqrt(dx * dx + dz * dz));
        }
        return (float) Math.max(0.8, maxDist + scale * 0.5F);
    }
}
