package com.huige233.transcend.ascension.resource;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.MageClass;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.network.S2CClassResourceSync;
import com.huige233.transcend.spell.SpellElement;
import com.huige233.transcend.spell.SpellDamageService;
import com.huige233.transcend.util.EntityCompatUtil;
import com.huige233.transcend.world.nexus.NexusWorldPenalty;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
/** 职业资源处理：推送/消耗/回复。 */
public class ClassResourceHandler {

    private static final float HEAT_DECAY_PER_TICK = 0.10f;
    private static final float OVERHEAT_SELF_DAMAGE = 2.0f;
    private static final int   WHITE_HOT_DURATION = 100;
    private static final float WHITE_HOT_REARM_HEAT = 60.0f;
    private static final float WATER_CAST_COOLING = 15.0f;

    private static final float CHARGE_MOVE_GAIN_PER_BLOCK = 5f;
    private static final float CHARGE_LEAK_PER_TICK = 0.1f;
    private static final double STORM_MOVE_THRESHOLD = 0.05D;

    private static final float HUNGER_RISE_PER_TICK = 0.05f;
    private static final float HUNGER_DEVOUR_DAMAGE = 1.5f;

    private static final int   SEDIMENT_STAND_TICKS = 20;
    private static final float TOPPLE_DAMAGE_PER_LAYER = 3.0f;
    private static final double TOPPLE_MOVE_THRESHOLD = 0.02D;

    private static final float TEMPO_DECAY_PER_TICK = 0.02f;
    private static final float CHRONO_RELEASE_COST = 40.0f;
    private static final int MAX_RELEASE_TARGETS = 12;

    private static final Map<UUID, double[]> LAST_POS = new ConcurrentHashMap<>();

    private static final Map<UUID, Integer> STAND_TICKS = new ConcurrentHashMap<>();

    private static final Map<UUID, Float> LAST_SYNCED_VALUE = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_SYNCED_OVERFLOW = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_SYNCED_WINDOW = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer sp)) return;

        PlayerAscensionData data = AscensionCapability.get(sp);
        MageClass mageClass = data.getMageClass();
        if (mageClass == MageClass.NONE || mageClass == MageClass.OMNISCIENT) return;

        ClassResourceType resType = ClassResourceType.forClass(mageClass);
        if (resType == null) return;

        if (NexusWorldPenalty.isClassResourceFrozen(sp)) return;

        ClassResourceData res = data.getClassResource();
        res.set(res.getValue(), resType);
        UUID uuid = sp.getUUID();

        switch (mageClass) {
            case PYROMANCER   -> tickPyromancer(sp, data, res, resType);
            case CRYOMANCER   -> tickCryomancer(sp, data, res, resType);
            case STORMCALLER  -> tickStormcaller(sp, data, res, resType, uuid);
            case ABYSSWALKER  -> tickAbysswalker(sp, data, res, resType);
            case EARTHSHAPER  -> tickEarthshaper(sp, data, res, resType, uuid);
            case CHRONOWEAVER -> tickChronoweaver(sp, data, res, resType);
            default -> {}
        }

        LAST_POS.put(uuid, new double[]{sp.getX(), sp.getY(), sp.getZ()});

        syncIfDirty(sp, res, resType, uuid);
    }

    private static void tickPyromancer(ServerPlayer sp, PlayerAscensionData data,
                                       ClassResourceData res, ClassResourceType type) {
        res.tickWindow();

        if (res.isFull(type) && !res.isWindowActive()) {
            if (!res.isOverflowing()) {
                res.startOverflow();
            } else {
                res.tickOverflow();
            }
            if (res.getOverflowTicks() % 20 == 0) {
                sp.hurt(sp.damageSources().onFire(), OVERHEAT_SELF_DAMAGE);
            }
        } else {
            res.endOverflow();
            if (!res.isWindowActive() && res.getValue() > 0.0F) {
                res.subtract(HEAT_DECAY_PER_TICK, type);
            }
        }

        if (res.getValue() < WHITE_HOT_REARM_HEAT) {
            res.armWindow();
        }
    }

    private static void tickCryomancer(ServerPlayer sp, PlayerAscensionData data,
                                       ClassResourceData res, ClassResourceType type) {

        if (res.isFull(type)) {
            if (!res.isOverflowing()) res.startOverflow();
            res.tickOverflow();
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 0, false, false));
        } else {
            res.endOverflow();
        }

    }

    private static void tickStormcaller(ServerPlayer sp, PlayerAscensionData data,
                                        ClassResourceData res, ClassResourceType type, UUID uuid) {
        double[] last = LAST_POS.get(uuid);
        if (last != null) {
            double dx = sp.getX() - last[0];
            double dy = sp.getY() - last[1];
            double dz = sp.getZ() - last[2];
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (ClassResourceMath.isGroundMovement(
                    dist, sp.onGround(), sp.isSpectator(), STORM_MOVE_THRESHOLD)) {
                float gain = (float) (dist * CHARGE_MOVE_GAIN_PER_BLOCK);
                res.add(gain, type);
            } else {
                if (res.getValue() > 0) {
                    res.subtract(CHARGE_LEAK_PER_TICK, type);
                }
            }
        }

    }

    private static void tickAbysswalker(ServerPlayer sp, PlayerAscensionData data,
                                        ClassResourceData res, ClassResourceType type) {

        res.add(HUNGER_RISE_PER_TICK, type);

        if (res.isFull(type)) {
            if (!res.isOverflowing()) res.startOverflow();
            res.tickOverflow();
            if (res.getOverflowTicks() % 20 == 0) {
                sp.hurt(sp.damageSources().magic(), HUNGER_DEVOUR_DAMAGE);
            }
        } else {
            res.endOverflow();
        }
    }

    private static void tickEarthshaper(ServerPlayer sp, PlayerAscensionData data,
                                        ClassResourceData res, ClassResourceType type, UUID uuid) {
        double[] last = LAST_POS.get(uuid);
        boolean standing = false;
        boolean toppleMovement = false;
        if (last != null) {
            double dx = sp.getX() - last[0];
            double dz = sp.getZ() - last[2];
            double dist = Math.sqrt(dx * dx + dz * dz);
            standing = sp.onGround() && !sp.isSpectator() && dist < TOPPLE_MOVE_THRESHOLD;
            toppleMovement = ClassResourceMath.isGroundMovement(
                    dist, sp.onGround(), sp.isSpectator(), TOPPLE_MOVE_THRESHOLD);
        }

        if (standing) {

            int ticks = STAND_TICKS.getOrDefault(uuid, 0) + 1;
            STAND_TICKS.put(uuid, ticks);

            if (ticks % SEDIMENT_STAND_TICKS == 0 && !res.isFull(type)) {
                res.add(1f, type);
            }
        } else {
            if (toppleMovement && res.getValue() > 0) {
                releaseEarthshaperTopple(sp, res.getValue());
                res.set(0f, type);
            }
            STAND_TICKS.put(uuid, 0);
        }
    }

    private static void tickChronoweaver(ServerPlayer sp, PlayerAscensionData data,
                                         ClassResourceData res, ClassResourceType type) {

        if (res.getValue() > 0) {
            res.subtract(TEMPO_DECAY_PER_TICK, type);
        }

    }

    public static void onCast(Player player, SpellElement element) {
        if (player.level().isClientSide) return;
        PlayerAscensionData data = AscensionCapability.get(player);
        MageClass mc = data.getMageClass();
        ClassResourceType type = ClassResourceType.forClass(mc);
        if (type == null) return;
        ClassResourceData res = data.getClassResource();

        switch (mc) {
            case PYROMANCER -> {
                if (element == SpellElement.WATER) {
                    res.subtract(WATER_CAST_COOLING, type);
                    res.endOverflow();
                    if (res.getValue() < WHITE_HOT_REARM_HEAT) res.armWindow();
                } else if (ClassResourceMath.isCanonicalClassMatch(mc, element)) {
                    float previous = res.getValue();
                    res.add(10f, type);
                    if (res.isWindowReady() && !res.isWindowActive()
                            && ClassResourceMath.crossedThreshold(previous, res.getValue(), type.getThreshold())) {
                        res.startWindow(WHITE_HOT_DURATION);
                        res.disarmWindow();
                    }
                }
            }
            case CRYOMANCER -> {
                if (ClassResourceMath.isCanonicalClassMatch(mc, element)) res.add(1f, type);
            }
            case CHRONOWEAVER -> res.add(5f, type);
            default -> {}
        }
    }

    public static boolean isCanonicalClassMatch(Player player, SpellElement element) {
        return ClassResourceMath.isCanonicalClassMatch(
                AscensionCapability.get(player).getMageClass(), element);
    }

    public static void onDealDamage(Player player, float amount) {
        if (player.level().isClientSide) return;
        PlayerAscensionData data = AscensionCapability.get(player);
        MageClass mc = data.getMageClass();
        ClassResourceType type = ClassResourceType.forClass(mc);
        if (type == null) return;
        ClassResourceData res = data.getClassResource();

        switch (mc) {
            case ABYSSWALKER -> {

                float reduction = Math.min(10f, amount * 0.5f);
                res.subtract(reduction, type);
            }
            case CHRONOWEAVER -> res.add(3f, type);
            default -> {}
        }
    }

    public static void onTakeDamage(Player player, float amount) {
        if (player.level().isClientSide) return;
        PlayerAscensionData data = AscensionCapability.get(player);
        MageClass mc = data.getMageClass();
        ClassResourceType type = ClassResourceType.forClass(mc);
        if (type == null) return;
        ClassResourceData res = data.getClassResource();

        switch (mc) {
            case CRYOMANCER -> res.add(1f, type);
            case CHRONOWEAVER -> res.add(2f, type);
            default -> {}
        }
    }

    public static void onKill(Player player, boolean isBoss) {
        if (player.level().isClientSide) return;
        PlayerAscensionData data = AscensionCapability.get(player);
        MageClass mc = data.getMageClass();
        ClassResourceType type = ClassResourceType.forClass(mc);
        if (type == null) return;
        ClassResourceData res = data.getClassResource();

        switch (mc) {
            case ABYSSWALKER -> {

                float reduction = isBoss ? 50f : 15f;
                res.subtract(reduction, type);
            }
            default -> {}
        }
    }

    public static int cryomancerShatter(Player player) {
        if (player.level().isClientSide) return 0;
        PlayerAscensionData data = AscensionCapability.get(player);
        if (data.getMageClass() != MageClass.CRYOMANCER) return 0;
        ClassResourceData res = data.getClassResource();
        int layers = (int) ClassResourceMath.clampFinite(
                res.getValue(), ClassResourceType.FROST_ARMOR.getMaxValue());
        if (layers <= 0) return 0;
        res.reset();
        return layers;
    }

    public static float stormcallerDischarge(Player player) {
        if (player.level().isClientSide) return 0f;
        PlayerAscensionData data = AscensionCapability.get(player);
        if (data.getMageClass() != MageClass.STORMCALLER) return 0f;
        ClassResourceData res = data.getClassResource();
        float charge = ClassResourceMath.clampFinite(
                res.getValue(), ClassResourceType.CHARGE.getMaxValue());
        if (charge <= 0) return 0f;
        res.set(0f, ClassResourceType.CHARGE);
        return charge;
    }

    public static boolean chronoweaverConsume(Player player, float cost) {
        if (player.level().isClientSide) return false;
        if (!Float.isFinite(cost) || cost < 0.0F) return false;
        PlayerAscensionData data = AscensionCapability.get(player);
        if (data.getMageClass() != MageClass.CHRONOWEAVER) return false;
        ClassResourceData res = data.getClassResource();
        if (res.getValue() < cost) return false;
        res.subtract(cost, ClassResourceType.TEMPO);
        return true;
    }

    public static boolean activateClassSkill(ServerPlayer player) {
        if (player == null || player.level().isClientSide) return false;
        PlayerAscensionData data = AscensionCapability.get(player);
        ClassResourceData resource = data.getClassResource();
        ClassResourceType type = ClassResourceType.forClass(data.getMageClass());
        if (type == null) return false;
        resource.set(resource.getValue(), type);
        boolean activated = switch (data.getMageClass()) {
            case CRYOMANCER -> activateCryomancerNova(player, resource);
            case STORMCALLER -> activateStormcallerDischarge(player, resource);
            case CHRONOWEAVER -> activateChronoweaverAcceleration(player, resource);
            default -> false;
        };
        if (activated) syncNow(player);
        return activated;
    }

    private static boolean activateCryomancerNova(ServerPlayer player, ClassResourceData resource) {
        int layers = (int) ClassResourceMath.clampFinite(
                resource.getValue(), ClassResourceType.FROST_ARMOR.getMaxValue());
        if (layers <= 0) return false;
        resource.reset();
        double radius = Math.min(6.0, 2.0 + layers * 0.4);
        int spellTier = AscensionCapability.get(player).getSpellTier();
        SpellElement element = canonicalClassElement(player);
        for (LivingEntity target : nearbyTargets(player, radius, MAX_RELEASE_TARGETS)) {
            int damageLayers = (int) ClassResourceMath.clampFinite(
                    layers, ClassResourceType.FROST_ARMOR.getMaxValue());
            SpellDamageService.deal(player.serverLevel(), player, player, target, element,
                    spellTier, 1.5F * damageLayers, true);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    60 + damageLayers * 10, Math.min(3, damageLayers / 3), false, true));
            target.setTicksFrozen(target.getTicksFrozen() + damageLayers * 20);
        }
        player.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 0.5,
                player.getZ(), 24 + layers * 4, radius * 0.45, 0.6, radius * 0.45, 0.05);
        return true;
    }

    private static boolean activateStormcallerDischarge(ServerPlayer player, ClassResourceData resource) {
        float charge = ClassResourceMath.clampFinite(
                resource.getValue(), ClassResourceType.CHARGE.getMaxValue());
        if (charge <= 0.0F) return false;
        int targetLimit = Math.min(6, Math.max(1, (int) Math.ceil(charge / 17.0F)));
        List<LivingEntity> targets = nearbyTargets(player, 10.0, targetLimit);
        if (targets.isEmpty()) return false;
        resource.set(0.0F, ClassResourceType.CHARGE);
        int spellTier = AscensionCapability.get(player).getSpellTier();
        SpellElement element = canonicalClassElement(player);
        for (LivingEntity target : targets) {
            float damageCharge = ClassResourceMath.clampFinite(
                    charge, ClassResourceType.CHARGE.getMaxValue());
            float damage = Math.min(11.0F, 3.0F + damageCharge * 0.08F);
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(player.serverLevel());
            if (bolt != null) {
                bolt.moveTo(Vec3.atBottomCenterOf(target.blockPosition()));
                bolt.setVisualOnly(true);
                player.serverLevel().addFreshEntity(bolt);
            }
            SpellDamageService.deal(player.serverLevel(), player, player, target, element,
                    spellTier, damage, true);
        }
        return true;
    }

    private static boolean activateChronoweaverAcceleration(ServerPlayer player, ClassResourceData resource) {
        if (resource.getValue() < CHRONO_RELEASE_COST) return false;
        resource.subtract(CHRONO_RELEASE_COST, ClassResourceType.TEMPO);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0, false, true));
        player.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0,
                player.getZ(), 32, 0.7, 1.0, 0.7, 0.08);
        return true;
    }

    private static void releaseEarthshaperTopple(ServerPlayer player, float layers) {
        layers = ClassResourceMath.clampFinite(layers, ClassResourceType.SEDIMENT.getMaxValue());
        if (layers <= 0.0F) return;
        double radius = Math.min(5.0, 2.0 + layers * 0.375);
        int spellTier = AscensionCapability.get(player).getSpellTier();
        SpellElement element = canonicalClassElement(player);
        for (LivingEntity target : nearbyTargets(player, radius, MAX_RELEASE_TARGETS)) {
            float damageLayers = ClassResourceMath.clampFinite(
                    layers, ClassResourceType.SEDIMENT.getMaxValue());
            SpellDamageService.deal(player.serverLevel(), player, player, target, element,
                    spellTier, TOPPLE_DAMAGE_PER_LAYER * damageLayers, true);
            Vec3 away = target.position().subtract(player.position());
            if (away.horizontalDistanceSqr() > 0.0001) {
                Vec3 knockback = away.normalize().scale(Math.min(0.85, 0.2 + damageLayers * 0.08));
                target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.2, knockback.z));
                target.hurtMarked = true;
            }
        }
        player.serverLevel().sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.2,
                player.getZ(), 12 + (int) layers * 3, radius * 0.4, 0.2, radius * 0.4, 0.03);
    }

    private static List<LivingEntity> nearbyTargets(ServerPlayer player, double radius, int limit) {
        AABB area = player.getBoundingBox().inflate(radius);
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, area,
                        target -> target != player && target.isAlive()
                                && target.distanceToSqr(player) <= radius * radius
                                && !EntityCompatUtil.isProtectedPlayer(target))
                .stream()
                .sorted(Comparator.comparingDouble(target -> target.distanceToSqr(player)))
                .limit(limit)
                .toList();
    }

    private static SpellElement canonicalClassElement(Player player) {
        return java.util.Objects.requireNonNull(
                SpellElement.getById(AscensionCapability.get(player).getMageClass().primaryElement),
                "Mage class has an invalid primary element");
    }

    public static void pyromancerCoolDown(Player player, float amount) {
        if (player.level().isClientSide) return;
        PlayerAscensionData data = AscensionCapability.get(player);
        if (data.getMageClass() != MageClass.PYROMANCER) return;
        ClassResourceData res = data.getClassResource();
        res.subtract(amount, ClassResourceType.HEAT);
        res.endOverflow();
    }

    public static float getPyromancerDamageBonus(Player player) {
        PlayerAscensionData data = AscensionCapability.get(player);
        if (data.getMageClass() != MageClass.PYROMANCER) return 0f;
        ClassResourceData res = data.getClassResource();

        return res.getValue() / ClassResourceType.HEAT.getMaxValue() * 0.6f;
    }

    public static boolean isPyromancerWhiteHot(Player player) {
        PlayerAscensionData data = AscensionCapability.get(player);
        if (data.getMageClass() != MageClass.PYROMANCER) return false;
        return data.getClassResource().isWindowActive();
    }

    public static float getAbysswalkerBonus(Player player) {
        PlayerAscensionData data = AscensionCapability.get(player);
        if (data.getMageClass() != MageClass.ABYSSWALKER) return 0f;
        ClassResourceData res = data.getClassResource();
        return 1.0f - (res.getValue() / ClassResourceType.HUNGER.getMaxValue());
    }

    public static float getEarthshaperDamageReduction(Player player) {
        PlayerAscensionData data = AscensionCapability.get(player);
        if (data.getMageClass() != MageClass.EARTHSHAPER) return 0f;
        ClassResourceData res = data.getClassResource();
        return Math.min(0.4f, res.getValue() * 0.05f);
    }

    public static float getSpellDamageMultiplier(Player player, SpellElement element) {
        PlayerAscensionData data = AscensionCapability.get(player);
        return ClassResourceMath.spellDamageMultiplier(
                data.getMageClass(), element, data.getClassResource().getValue());
    }

    private static void syncIfDirty(ServerPlayer sp, ClassResourceData res,
                                    ClassResourceType type, UUID uuid) {
        float curVal = res.getValue();
        int curOverflow = res.getOverflowTicks();
        int curWindow = res.getWindowTicks();

        Float lastVal = LAST_SYNCED_VALUE.get(uuid);
        Integer lastOverflow = LAST_SYNCED_OVERFLOW.get(uuid);
        Integer lastWindow = LAST_SYNCED_WINDOW.get(uuid);

        boolean dirty = lastVal == null
                || Math.abs(lastVal - curVal) > 0.01f
                || !Integer.valueOf(curOverflow).equals(lastOverflow)
                || !Integer.valueOf(curWindow).equals(lastWindow);

        if (dirty) {
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    new S2CClassResourceSync(type, res));
            LAST_SYNCED_VALUE.put(uuid, curVal);
            LAST_SYNCED_OVERFLOW.put(uuid, curOverflow);
            LAST_SYNCED_WINDOW.put(uuid, curWindow);
        }
    }

    public static void syncNow(ServerPlayer player) {
        PlayerAscensionData data = AscensionCapability.get(player);
        ClassResourceType type = ClassResourceType.forClass(data.getMageClass());
        if (type == null) return;
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new S2CClassResourceSync(type, data.getClassResource()));
        LAST_SYNCED_VALUE.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        LAST_POS.remove(uuid);
        STAND_TICKS.remove(uuid);
        LAST_SYNCED_VALUE.remove(uuid);
        LAST_SYNCED_OVERFLOW.remove(uuid);
        LAST_SYNCED_WINDOW.remove(uuid);
    }
}
