package com.huige233.transcend.gear.forge;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.gear.GearForgeData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeSetBonusHandler {

    private static final int CHECK_INTERVAL = 20;
    private static final int EFFECT_DURATION = 40;
    private static final int ASPECT_THRESHOLD = 4;
    private static final int SOCKET_THRESHOLD = 8;

    private static final UUID UUID_ASPECT_EARTH_ARMOR  = UUID.fromString("b1000000-0001-0001-0001-000000000001");
    private static final UUID UUID_ASPECT_WIND_SPEED   = UUID.fromString("b1000000-0002-0002-0002-000000000002");

    private static final UUID UUID_SOCKET_SHARP_DMG    = UUID.fromString("b2000000-0001-0001-0001-000000000001");
    private static final UUID UUID_SOCKET_WARD_TOUGH   = UUID.fromString("b2000000-0002-0002-0002-000000000002");
    private static final UUID UUID_SOCKET_LEECH_KB     = UUID.fromString("b2000000-0003-0003-0003-000000000003");
    private static final UUID UUID_SOCKET_SPARK_LUCK   = UUID.fromString("b2000000-0004-0004-0004-000000000004");
    private static final UUID UUID_SOCKET_SWIFT_SPEED  = UUID.fromString("b2000000-0005-0005-0005-000000000005");
    private static final UUID UUID_SOCKET_FOCUS_HP     = UUID.fromString("b2000000-0006-0006-0006-000000000006");

    private static final UUID UUID_BLESSING_STAR_LUCK  = UUID.fromString("b3000000-0003-0003-0003-000000000003");
    private static final UUID UUID_BLESSING_SUN_ATTACK = UUID.fromString("b3000000-0001-0001-0001-000000000001");
    private static final UUID UUID_BLESSING_MOON_ATTACK = UUID.fromString("b3000000-0002-0002-0002-000000000002");

    private static final double EARTH_ARMOR        = 2.0;
    private static final double WIND_SPEED         = 0.05;
    private static final double SHARP_SET_DMG      = 1.0;
    private static final double WARD_SET_TOUGH     = 2.0;
    private static final double LEECH_SET_KB       = 1.0;
    private static final double SPARK_SET_LUCK     = 1.0;
    private static final double SWIFT_SET_SPEED    = 0.05;
    private static final double FOCUS_SET_HP       = 4.0;
    private static final double STAR_LUCK          = 1.0;
    private static final double SUN_DAY_ATTACK     = 1.0;
    private static final double MOON_NIGHT_ATTACK  = 1.0;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % CHECK_INTERVAL != 0) return;

        Map<AspectKind, Integer> aspectCounts = new EnumMap<>(AspectKind.class);
        Map<ResonanceKind, Integer> socketCounts = new EnumMap<>(ResonanceKind.class);
        Map<CelestialKind, Integer> blessingCounts = new EnumMap<>(CelestialKind.class);

        ItemStack[] slots = collectSlots(player);
        for (ItemStack item : slots) {
            if (item.isEmpty() || !GearForgeData.isInPipeline(item)) continue;

            var crucible = GearForgeData.getCrucible(item);
            if (crucible != null) {
                AspectDef def = AspectRegistry.byId(crucible.aspect());
                if (def != null && def != AspectRegistry.INDETERMINATE) {
                    aspectCounts.merge(def.dominant(), 1, Integer::sum);
                }
            }

            for (var sock : GearForgeData.getSockets(item)) {
                ResonanceKind k = ResonanceKind.byId(sock.crystalId());
                if (k != null) socketCounts.merge(k, 1, Integer::sum);
            }

            var blessing = GearForgeData.getCelestial(item);
            if (blessing != null) {
                BlessingDef bd = BlessingRegistry.byId(blessing.blessing());
                if (bd != null && bd != BlessingRegistry.INDETERMINATE) {
                    blessingCounts.merge(bd.dominant(), 1, Integer::sum);
                }
            }
        }

        applyAspectBonus(player, aspectCounts);

        applySocketBonus(player, socketCounts);

        applyBlessingBonus(player, blessingCounts);
    }

    private static ItemStack[] collectSlots(Player player) {
        return new ItemStack[]{
                player.getItemBySlot(EquipmentSlot.MAINHAND),
                player.getItemBySlot(EquipmentSlot.HEAD),
                player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.FEET),
        };
    }

    private static void applyAspectBonus(Player p, Map<AspectKind, Integer> counts) {
        boolean fire   = counts.getOrDefault(AspectKind.FIRE,   0) >= ASPECT_THRESHOLD;
        boolean water  = counts.getOrDefault(AspectKind.WATER,  0) >= ASPECT_THRESHOLD;
        boolean earth  = counts.getOrDefault(AspectKind.EARTH,  0) >= ASPECT_THRESHOLD;
        boolean wind   = counts.getOrDefault(AspectKind.WIND,   0) >= ASPECT_THRESHOLD;
        boolean spirit = counts.getOrDefault(AspectKind.SPIRIT, 0) >= ASPECT_THRESHOLD;
        boolean voidk  = counts.getOrDefault(AspectKind.VOID,   0) >= ASPECT_THRESHOLD;

        if (fire) p.addEffect(makeAmbient(MobEffects.FIRE_RESISTANCE));

        if (water) {
            p.addEffect(makeAmbient(MobEffects.WATER_BREATHING));
            p.addEffect(makeAmbient(MobEffects.DOLPHINS_GRACE));
        }

        manageModifier(p, Attributes.ARMOR, UUID_ASPECT_EARTH_ARMOR,
                "transcend.set.aspect.earth", earth, EARTH_ARMOR,
                AttributeModifier.Operation.ADDITION);

        manageModifier(p, Attributes.MOVEMENT_SPEED, UUID_ASPECT_WIND_SPEED,
                "transcend.set.aspect.wind.speed", wind, WIND_SPEED,
                AttributeModifier.Operation.MULTIPLY_BASE);
        if (wind) p.addEffect(makeAmbient(MobEffects.JUMP));

        if (spirit) p.addEffect(makeAmbient(MobEffects.NIGHT_VISION));

        if (voidk && p.isShiftKeyDown()) {
            p.addEffect(makeAmbient(MobEffects.INVISIBILITY));
        }
    }

    private static void applySocketBonus(Player p, Map<ResonanceKind, Integer> counts) {
        boolean sharpSet  = counts.getOrDefault(ResonanceKind.SHARPNESS, 0) >= SOCKET_THRESHOLD;
        boolean wardSet   = counts.getOrDefault(ResonanceKind.WARD,      0) >= SOCKET_THRESHOLD;
        boolean leechSet  = counts.getOrDefault(ResonanceKind.LEECH,     0) >= SOCKET_THRESHOLD;
        boolean sparkSet  = counts.getOrDefault(ResonanceKind.SPARK,     0) >= SOCKET_THRESHOLD;
        boolean swiftSet  = counts.getOrDefault(ResonanceKind.SWIFTNESS, 0) >= SOCKET_THRESHOLD;
        boolean focusSet  = counts.getOrDefault(ResonanceKind.FOCUS,     0) >= SOCKET_THRESHOLD;

        manageModifier(p, Attributes.ATTACK_DAMAGE, UUID_SOCKET_SHARP_DMG,
                "transcend.set.socket.sharpness", sharpSet, SHARP_SET_DMG,
                AttributeModifier.Operation.ADDITION);

        manageModifier(p, Attributes.ARMOR_TOUGHNESS, UUID_SOCKET_WARD_TOUGH,
                "transcend.set.socket.ward", wardSet, WARD_SET_TOUGH,
                AttributeModifier.Operation.ADDITION);

        manageModifier(p, Attributes.KNOCKBACK_RESISTANCE, UUID_SOCKET_LEECH_KB,
                "transcend.set.socket.leech", leechSet, LEECH_SET_KB,
                AttributeModifier.Operation.ADDITION);

        manageModifier(p, Attributes.LUCK, UUID_SOCKET_SPARK_LUCK,
                "transcend.set.socket.spark", sparkSet, SPARK_SET_LUCK,
                AttributeModifier.Operation.ADDITION);

        manageModifier(p, Attributes.MOVEMENT_SPEED, UUID_SOCKET_SWIFT_SPEED,
                "transcend.set.socket.swiftness", swiftSet, SWIFT_SET_SPEED,
                AttributeModifier.Operation.MULTIPLY_BASE);

        manageModifier(p, Attributes.MAX_HEALTH, UUID_SOCKET_FOCUS_HP,
                "transcend.set.socket.focus", focusSet, FOCUS_SET_HP,
                AttributeModifier.Operation.ADDITION);
    }

    private static void applyBlessingBonus(Player p, Map<CelestialKind, Integer> counts) {

        boolean sunSet   = counts.getOrDefault(CelestialKind.SUN,   0) >= 2;
        boolean moonSet  = counts.getOrDefault(CelestialKind.MOON,  0) >= 2;
        boolean starSet  = counts.getOrDefault(CelestialKind.STAR,  0) >= 2;
        boolean abyssSet = counts.getOrDefault(CelestialKind.ABYSS, 0) >= 2;

        boolean sunActive = sunSet && p.level().isDay();
        manageModifier(p, Attributes.ATTACK_DAMAGE, UUID_BLESSING_SUN_ATTACK,
                "transcend.set.blessing.sun", sunActive, SUN_DAY_ATTACK,
                AttributeModifier.Operation.ADDITION);

        boolean moonActive = moonSet && p.level().isNight();
        manageModifier(p, Attributes.ATTACK_DAMAGE, UUID_BLESSING_MOON_ATTACK,
                "transcend.set.blessing.moon", moonActive, MOON_NIGHT_ATTACK,
                AttributeModifier.Operation.ADDITION);

        manageModifier(p, Attributes.LUCK, UUID_BLESSING_STAR_LUCK,
                "transcend.set.blessing.star", starSet, STAR_LUCK,
                AttributeModifier.Operation.ADDITION);

        if (abyssSet && p.isInWater()) {
            p.addEffect(makeAmbient(MobEffects.WATER_BREATHING));
        }
    }

    private static MobEffectInstance makeAmbient(net.minecraft.world.effect.MobEffect effect) {

        return new MobEffectInstance(effect, EFFECT_DURATION, 0, true, false, false);
    }

    private static void manageModifier(Player p, Attribute attr, UUID uuid, String name,
                                        boolean activate, double amount,
                                        AttributeModifier.Operation op) {
        if (attr == null) return;
        AttributeInstance instance = p.getAttribute(attr);
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(uuid);
        if (activate) {
            if (existing == null) {
                instance.addTransientModifier(new AttributeModifier(uuid, name, amount, op));
            }

        } else {
            if (existing != null) instance.removeModifier(uuid);
        }
    }
}
