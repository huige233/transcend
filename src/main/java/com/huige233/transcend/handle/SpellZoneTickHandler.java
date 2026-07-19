package com.huige233.transcend.handle;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.spell.ElementReaction;
import com.huige233.transcend.spell.SpellElement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SpellZoneTickHandler {

    public static final String TAG_VORTEX = "transcend_vortex";
    public static final String TAG_TRAP   = "transcend_trap";

    private static final String KEY_CASTER  = "caster_uuid";
    private static final String KEY_ELEMENT = "element";
    private static final String KEY_DAMAGE  = "damage";
    private static final String KEY_SPELL_TIER = "spell_tier";

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;

        long tick = level.getGameTime();

        if (tick % 5 != 0) return;

        List<AreaEffectCloud> clouds = new java.util.ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e instanceof AreaEffectCloud aec) {
                CompoundTag pdata = aec.getPersistentData();
                if (pdata.contains(TAG_VORTEX) || pdata.contains(TAG_TRAP)) clouds.add(aec);
            }
        }

        for (AreaEffectCloud cloud : clouds) {
            if (cloud.isRemoved()) continue;
            CompoundTag pdata = cloud.getPersistentData();

            if (pdata.contains(TAG_VORTEX)) {
                tickVortex(level, cloud, pdata.getCompound(TAG_VORTEX));
            } else if (pdata.contains(TAG_TRAP)) {
                tickTrap(level, cloud, pdata.getCompound(TAG_TRAP));
            }
        }
    }

    private static void tickVortex(ServerLevel level, AreaEffectCloud cloud, CompoundTag data) {
        Vec3 center = cloud.position();
        float damage = data.getFloat(KEY_DAMAGE);
        SpellElement element = parseElement(data);
        LivingEntity caster = resolveCaster(level, data);
        int spellTier = Math.max(1, data.getInt(KEY_SPELL_TIER));

        double pullRadius = cloud.getRadius();

        AABB area = new AABB(center.x - pullRadius, center.y - pullRadius, center.z - pullRadius,
                             center.x + pullRadius, center.y + pullRadius, center.z + pullRadius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> !(e instanceof Player p && p.isCreative()) && !e.equals(caster));

        for (LivingEntity target : targets) {
            double dist = target.distanceTo(cloud);
            if (dist > pullRadius) continue;

            Vec3 toCenter = center.subtract(target.position()).normalize();
            double pullStrength = 0.15 + (1.0 - dist / pullRadius) * 0.15;
            Vec3 newVel = target.getDeltaMovement().add(toCenter.scale(pullStrength));
            target.setDeltaMovement(newVel.x, Math.min(newVel.y + 0.05, 0.5), newVel.z);
            target.hurtMarked = true;

            if (level.getGameTime() % 20 == 0 && element != null && caster != null) {
                ElementReaction.tryReaction(target, element, spellTier, damage * 0.3F, caster);
            }
        }
    }

    private static void tickTrap(ServerLevel level, AreaEffectCloud cloud, CompoundTag data) {
        Vec3 center = cloud.position();
        float damage = data.getFloat(KEY_DAMAGE);
        SpellElement element = parseElement(data);
        LivingEntity caster = resolveCaster(level, data);
        int spellTier = Math.max(1, data.getInt(KEY_SPELL_TIER));

        double triggerRadius = 2.0;
        AABB area = cloud.getBoundingBox().inflate(triggerRadius);

        List<LivingEntity> triggers = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> !(e instanceof Player p && p.isCreative()) && !e.equals(caster)
                     && e.distanceTo(cloud) <= triggerRadius);

        if (triggers.isEmpty()) return;

        AABB blastArea = cloud.getBoundingBox().inflate(2.5);
        List<LivingEntity> blastTargets = level.getEntitiesOfClass(LivingEntity.class, blastArea,
                e -> !(e instanceof Player p && p.isCreative()));
        if (element != null && caster != null) {
            for (LivingEntity t : blastTargets) {
                ElementReaction.tryReaction(t, element, spellTier, damage, caster);
            }
        }

        cloud.discard();
    }

    public static AreaEffectCloud spawnVortex(ServerLevel level, Vec3 pos,
                                               SpellElement element, float damage,
                                               int spellTier, LivingEntity caster, double radius) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, pos.x, pos.y, pos.z);
        cloud.setRadius((float) radius);
        cloud.setDuration(60);
        cloud.setRadiusPerTick(0);
        cloud.setWaitTime(0);
        cloud.setOwner(caster);
        cloud.setNoGravity(true);

        CompoundTag vData = new CompoundTag();
        if (caster != null) vData.putString(KEY_CASTER, caster.getUUID().toString());
        if (element != null) vData.putString(KEY_ELEMENT, element.id);
        vData.putFloat(KEY_DAMAGE, damage);
        vData.putInt(KEY_SPELL_TIER, Math.max(1, spellTier));
        cloud.getPersistentData().put(TAG_VORTEX, vData);

        level.addFreshEntity(cloud);
        return cloud;
    }

    public static AreaEffectCloud spawnTrap(ServerLevel level, Vec3 pos,
                                             SpellElement element, float damage,
                                             int spellTier, LivingEntity caster) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, pos.x, pos.y, pos.z);
        cloud.setRadius(0.5F);
        cloud.setDuration(1200);
        cloud.setRadiusPerTick(0);
        cloud.setWaitTime(0);
        cloud.setOwner(caster);
        cloud.setNoGravity(true);

        CompoundTag tData = new CompoundTag();
        if (caster != null) tData.putString(KEY_CASTER, caster.getUUID().toString());
        if (element != null) tData.putString(KEY_ELEMENT, element.id);
        tData.putFloat(KEY_DAMAGE, damage);
        tData.putInt(KEY_SPELL_TIER, Math.max(1, spellTier));
        cloud.getPersistentData().put(TAG_TRAP, tData);

        level.addFreshEntity(cloud);
        return cloud;
    }

    private static SpellElement parseElement(CompoundTag data) {
        String id = data.getString(KEY_ELEMENT);
        for (SpellElement e : SpellElement.values()) {
            if (e.id.equals(id)) return e;
        }
        return null;
    }

    private static LivingEntity resolveCaster(ServerLevel level, CompoundTag data) {
        if (!data.contains(KEY_CASTER)) return null;
        try {
            UUID uuid = UUID.fromString(data.getString(KEY_CASTER));
            Entity e = level.getPlayerByUUID(uuid);
            if (e instanceof LivingEntity le) return le;

            for (Entity ent : level.getAllEntities()) {
                if (ent.getUUID().equals(uuid) && ent instanceof LivingEntity le) return le;
            }
        } catch (IllegalArgumentException ignored) {}
        return null;
    }
}
