package com.huige233.transcend.gear.forge;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.gear.GearCategory;
import com.huige233.transcend.gear.GearForgeData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TriggerAffixHandler {

    private static final long CD_LAST_DASH   = 60 * 20L;
    private static final long CD_DEATH_ECHO  = 5 * 60 * 20L;
    private static final long CD_PULSE       = 30 * 20L;
    private static final long CD_AEGIS_AURA  = 60 * 20L;
    private static final long CD_OVERFLOW    = 20 * 20L;

    private static final float EMBER_EXPLOSION_RADIUS = 1.5f;
    private static final float REPRISE_HEAL = 1.0f;
    private static final float HARMONIC_PER_STACK = 0.50f;
    private static final int   HARMONIC_MAX_STACKS = 3;
    private static final float SANGUINE_RADIUS = 5.0f;
    private static final int   SANGUINE_DURATION_TICKS = 60;
    private static final long  SOUL_REAP_ENERGY_PER_KILL = 1L;
    private static final float THORNBACK_REFLECT = 0.30f;
    private static final int   LAST_DASH_DURATION = 60;
    private static final float AEGIS_HEAL_AMOUNT = 1.0f;
    private static final float DEATH_ECHO_KNOCKBACK_RADIUS = 5.0f;
    private static final float PULSE_AOE_RADIUS = 1.5f;
    private static final float PULSE_AOE_DAMAGE_FRACTION = 0.50f;
    private static final float AEGIS_AURA_AMOUNT = 1.0f;
    private static final float AEGIS_AURA_RADIUS = 5.0f;
    private static final float OVERFLOW_MULTIPLIER = 2.00f;

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof Player killer)) return;
        if (!(killer.level() instanceof ServerLevel serverLevel)) return;

        ItemStack weapon = killer.getItemInHand(InteractionHand.MAIN_HAND);
        applyOnKill(serverLevel, killer, victim, weapon);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
            applyOnKill(serverLevel, killer, victim, killer.getItemBySlot(slot));
        }
    }

    private static void applyOnKill(ServerLevel level, Player killer, LivingEntity victim, ItemStack stack) {
        if (stack.isEmpty() || !GearForgeData.isInPipeline(stack)) return;
        var data = GearForgeData.getTriggerAffix(stack);
        if (data == null) return;
        TriggerAffixKind kind = TriggerAffixKind.byId(data.affixId());
        if (kind == null || kind.category != TriggerAffixKind.Category.ON_KILL) return;

        Vec3 pos = victim.position();
        switch (kind) {
            case EMBER -> {
                level.explode(killer, pos.x, pos.y, pos.z, EMBER_EXPLOSION_RADIUS,
                        true, Level.ExplosionInteraction.NONE);
            }
            case REPRISE -> {
                killer.heal(REPRISE_HEAL);
            }
            case HARMONIC -> {

                if (GearCategory.classify(stack) == GearCategory.WEAPON) {
                    int cur = GearForgeData.getHarmonicStacks(stack);
                    GearForgeData.setHarmonicStacks(stack, Math.min(HARMONIC_MAX_STACKS, cur + 1));
                }
            }
            case SANGUINE -> {
                AABB box = new AABB(pos.add(-SANGUINE_RADIUS, -SANGUINE_RADIUS, -SANGUINE_RADIUS),
                                    pos.add( SANGUINE_RADIUS,  SANGUINE_RADIUS,  SANGUINE_RADIUS));
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e != killer && e != victim && e.isAlive());
                for (LivingEntity target : targets) {
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, SANGUINE_DURATION_TICKS, 1));
                }
            }
            case SOUL_REAP -> {
                PlayerAscensionData pdata = AscensionCapability.get(killer);
                if (pdata != null) {
                    pdata.addSoulEnergy(SOUL_REAP_ENERGY_PER_KILL);
                }
            }
            default -> {}
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim == null) return;
        Level level = victim.level();
        if (level.isClientSide) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        float amount = event.getAmount();
        if (amount <= 0) return;

        if (event.getSource().getEntity() instanceof Player attacker) {
            ItemStack weapon = attacker.getItemInHand(InteractionHand.MAIN_HAND);
            if (!weapon.isEmpty() && GearForgeData.isInPipeline(weapon)
                    && GearCategory.classify(weapon) == GearCategory.WEAPON) {

                int harmonicStacks = GearForgeData.getHarmonicStacks(weapon);
                if (harmonicStacks > 0) {
                    amount *= (1.0f + HARMONIC_PER_STACK * harmonicStacks);
                    GearForgeData.setHarmonicStacks(weapon, 0);
                }

                var trig = GearForgeData.getTriggerAffix(weapon);
                if (trig != null) {
                    TriggerAffixKind kind = TriggerAffixKind.byId(trig.affixId());
                    long now = serverLevel.getGameTime();

                    if (kind == TriggerAffixKind.PULSE) {
                        long last = GearForgeData.getTriggerCd(weapon, kind.id);
                        if (now - last >= CD_PULSE) {

                            applyAoe(serverLevel, attacker, victim, amount, PULSE_AOE_RADIUS, PULSE_AOE_DAMAGE_FRACTION);
                            GearForgeData.setTriggerCd(weapon, kind.id, now);
                        }
                    } else if (kind == TriggerAffixKind.OVERFLOW) {
                        long last = GearForgeData.getTriggerCd(weapon, kind.id);
                        if (now - last >= CD_OVERFLOW) {
                            amount *= OVERFLOW_MULTIPLIER;
                            GearForgeData.setTriggerCd(weapon, kind.id, now);
                        }
                    }
                }
            }
        }

        if (victim instanceof Player player) {
            long now = serverLevel.getGameTime();

            ItemStack[] candidates = new ItemStack[]{
                    player.getItemInHand(InteractionHand.MAIN_HAND),
                    player.getItemBySlot(EquipmentSlot.HEAD),
                    player.getItemBySlot(EquipmentSlot.CHEST),
                    player.getItemBySlot(EquipmentSlot.LEGS),
                    player.getItemBySlot(EquipmentSlot.FEET),
            };

            for (ItemStack stack : candidates) {
                if (stack.isEmpty() || !GearForgeData.isInPipeline(stack)) continue;
                var trig = GearForgeData.getTriggerAffix(stack);
                if (trig == null) continue;
                TriggerAffixKind kind = TriggerAffixKind.byId(trig.affixId());
                if (kind == null || kind.category != TriggerAffixKind.Category.ON_HURT) continue;

                switch (kind) {
                    case THORNBACK -> {
                        if (event.getSource().getEntity() instanceof LivingEntity attacker
                                && attacker != player) {
                            float reflect = amount * THORNBACK_REFLECT;
                            attacker.hurt(player.damageSources().thorns(player), reflect);
                        }
                    }
                    case LAST_DASH -> {
                        float hpAfter = player.getHealth() - amount;
                        float threshold = player.getMaxHealth() * 0.30f;
                        long last = GearForgeData.getTriggerCd(stack, kind.id);
                        if (hpAfter < threshold && now - last >= CD_LAST_DASH) {
                            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, LAST_DASH_DURATION, 2));
                            GearForgeData.setTriggerCd(stack, kind.id, now);
                        }
                    }
                    case AEGIS_HEAL -> {
                        float absorbBefore = player.getAbsorptionAmount();

                        if (absorbBefore > 0 && amount >= absorbBefore) {
                            player.heal(AEGIS_HEAL_AMOUNT);
                        }
                    }
                    case DEATH_ECHO -> {
                        float hpAfter = player.getHealth() - amount;
                        long last = GearForgeData.getTriggerCd(stack, kind.id);
                        if (hpAfter <= 0 && now - last >= CD_DEATH_ECHO) {

                            float clampedAmount = Math.max(0, player.getHealth() - 1.0f);
                            event.setAmount(clampedAmount);
                            applyKnockback(serverLevel, player, DEATH_ECHO_KNOCKBACK_RADIUS);
                            GearForgeData.setTriggerCd(stack, kind.id, now);
                            amount = clampedAmount;
                        }
                    }
                    default -> {}
                }
            }
        }

        if (amount != event.getAmount()) {
            event.setAmount(Math.max(0.0f, amount));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        long now = serverLevel.getGameTime();

        ItemStack[] candidates = new ItemStack[]{
                player.getItemInHand(InteractionHand.MAIN_HAND),
                player.getItemBySlot(EquipmentSlot.HEAD),
                player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.FEET),
        };

        for (ItemStack stack : candidates) {
            if (stack.isEmpty() || !GearForgeData.isInPipeline(stack)) continue;
            var trig = GearForgeData.getTriggerAffix(stack);
            if (trig == null) continue;
            TriggerAffixKind kind = TriggerAffixKind.byId(trig.affixId());
            if (kind != TriggerAffixKind.AEGIS_AURA) continue;

            long last = GearForgeData.getTriggerCd(stack, kind.id);
            if (now - last < CD_AEGIS_AURA) continue;

            Vec3 pos = player.position();
            AABB box = new AABB(pos.add(-AEGIS_AURA_RADIUS, -AEGIS_AURA_RADIUS, -AEGIS_AURA_RADIUS),
                                pos.add( AEGIS_AURA_RADIUS,  AEGIS_AURA_RADIUS,  AEGIS_AURA_RADIUS));
            List<Player> nearby = serverLevel.getEntitiesOfClass(Player.class, box,
                    p -> p != null && p.isAlive());
            for (Player ally : nearby) {
                float curAbs = ally.getAbsorptionAmount();
                ally.setAbsorptionAmount(Math.max(curAbs, AEGIS_AURA_AMOUNT));
            }
            GearForgeData.setTriggerCd(stack, kind.id, now);
        }
    }

    private static void applyAoe(ServerLevel level, Player attacker, LivingEntity victim,
                                  float baseDamage, float radius, float fraction) {
        Vec3 pos = victim.position();
        AABB box = new AABB(pos.add(-radius, -radius, -radius), pos.add(radius, radius, radius));
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != attacker && e != victim && e.isAlive());
        float aoeAmount = baseDamage * fraction;
        DamageSource src = attacker.damageSources().playerAttack(attacker);
        for (LivingEntity target : targets) {
            target.hurt(src, aoeAmount);
        }
    }

    private static void applyKnockback(ServerLevel level, Player center, float radius) {
        Vec3 pos = center.position();
        AABB box = new AABB(pos.add(-radius, -radius, -radius), pos.add(radius, radius, radius));
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != center && e.isAlive());
        for (LivingEntity target : targets) {
            Vec3 dir = target.position().subtract(pos).normalize();
            target.setDeltaMovement(target.getDeltaMovement().add(dir.x * 1.0, 0.4, dir.z * 1.0));
            target.hurtMarked = true;
        }
    }
}
