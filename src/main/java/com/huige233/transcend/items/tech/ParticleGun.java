package com.huige233.transcend.items.tech;

import com.huige233.transcend.ModRarities;
import com.huige233.transcend.entity.projectile.ParticleBolt;
import com.huige233.transcend.tech.TechConfig;
import com.huige233.transcend.tech.ammo.BuiltInAmmoTypes;
import com.huige233.transcend.tech.attribute.TechAttribute;
import com.huige233.transcend.tech.combat.PenetrationProfile;
import com.huige233.transcend.tech.core.TechItemData;
import com.huige233.transcend.tech.gun.GunInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

   
                                                                                                    
                                                                                             
   
/** 处理粒子枪即时射击交互，委托运行实例管理弹药与热量，并组合模块属性生成弹体。 */
public class ParticleGun extends Item {
    
    public static final String NBT_CHARGE = "EnergyCharge";

    public ParticleGun() {
        super(new Properties().rarity(ModRarities.COSMIC).stacksTo(1).fireResistant());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        com.huige233.transcend.tech.ammo.Magazine magazine = new com.huige233.transcend.tech.ammo.Magazine();
        com.huige233.transcend.tech.attribute.AttributeContainer attributes =
                new com.huige233.transcend.tech.attribute.AttributeContainer();
        TechItemData.loadMagazine(stack, magazine);
        TechItemData.loadAttributes(stack, attributes);
        com.huige233.transcend.tech.ammo.AmmoType ammo = com.huige233.transcend.tech.ammo.BuiltInAmmoTypes
                .byId(magazine.loadedAmmoId());
        float charge = readCharge(stack);
        float heat = TechItemData.getHeat(stack);
        float heatCapacity = Math.max(0.001F, (float) attributes.getValue(TechAttribute.HEAT_CAPACITY));
        tooltip.add(Component.translatable("item.transcend.particle_gun").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.transcend.particle_gun.charge",
                TechTooltipNumbers.whole(charge), TechTooltipNumbers.whole(TechConfig.chargeAmmoMax()))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.transcend.particle_gun.magazine",
                magazine.charges(), ammo == null ? "-" : ammo.displayName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.particle_gun.heat",
                TechTooltipNumbers.whole(heat), TechTooltipNumbers.whole(heatCapacity))
                .withStyle(heat >= heatCapacity ? ChatFormatting.RED : ChatFormatting.GOLD));
        GunConfig.appendTooltip(stack, tooltip);
        tooltip.add(Component.translatable("tooltip.transcend.particle_gun.use").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.particle_gun.ammo_key").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("tooltip.transcend.particle_gun.install_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    
    public static float getCharge(ItemStack stack) {
        return readCharge(stack);
    }

    private static float readCharge(ItemStack stack) {
        if (TechItemData.hasGunCharge(stack)) return Math.min(TechConfig.chargeAmmoMax(), TechItemData.getGunCharge(stack));
        net.minecraft.nbt.CompoundTag tag = stack.getTag();
        return tag == null ? 0.0F : Math.min(TechConfig.chargeAmmoMax(), Math.max(0.0F, tag.getFloat(NBT_CHARGE)));
    }

    

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack gunStack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && !GunConfig.hasScope(gunStack)) {
            if (!level.isClientSide) player.displayClientMessage(
                    Component.translatable("tooltip.transcend.gun_module.install"), true);
            return InteractionResultHolder.pass(gunStack);
        }

        if (level.isClientSide) {
            
            return InteractionResultHolder.consume(gunStack);
        }
        GunInstance gun = GunInstance.of(gunStack);
        if (gun.isOverheated()) {
            player.displayClientMessage(Component.translatable("msg.transcend.particle_gun.overheated")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(gunStack);
        }
        if (!gun.ensureRoundForUse(player)) {
            player.displayClientMessage(Component.translatable("msg.transcend.particle_gun.no_ammo")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(gunStack);
        }
        fire(level, player, gunStack, gun);
        return InteractionResultHolder.consume(gunStack);
    }

    
    private void fire(Level level, Player player, ItemStack gunStack, GunInstance gun) {
        if (gun.isOverheated() || !gun.hasRoundReady()) return;

        
        
        float base = com.huige233.transcend.tech.combat.ShotAttributes.damageBase(
                TechConfig.particleGunDamage(), TechConfig.attributeBase(TechAttribute.DAMAGE_BASE),
                gun.techAttributes().getModifiers(TechAttribute.DAMAGE_BASE));
        com.huige233.transcend.tech.ammo.AmmoType loadedAmmo = gun.loadedAmmo();
        if (loadedAmmo == null) return;
        com.huige233.transcend.tech.ammo.BoltVisual visual = loadedAmmo.visual();
        com.huige233.transcend.tech.ammo.BoltBehavior behavior = loadedAmmo.behavior(gun.techAttributes());
        float damage = finiteFloat(base * GunConfig.damageMult(gunStack)
                * (float) gun.attribute(TechAttribute.DAMAGE_MULT) * loadedAmmo.damageMultiplier());
        float speed = finiteFloat(TechConfig.particleGunBoltSpeed() * GunConfig.speedMult(gunStack)
                * gun.attribute(TechAttribute.BOLT_SPEED) * loadedAmmo.speedMultiplier());
        GunModule ammoModule = GunConfig.getAmmo(gunStack);
        int penetration = finiteInt(gun.attribute(TechAttribute.ARMOR_PIERCE)
                + BuiltInAmmoTypes.penetrationStrength(loadedAmmo.id()) + ammoModule.penetrationBonus());
        int shieldPenetration = finiteInt(gun.attribute(TechAttribute.SHIELD_PENETRATION)
                + BuiltInAmmoTypes.penetrationStrength(loadedAmmo.id()) + ammoModule.penetrationBonus());
        boolean sirius = GunConfig.hasSirius(gunStack);
        int bossPenetration = finiteInt(gun.attribute(TechAttribute.BOSS_PENETRATION));
        float bossPercent = finiteFloat(gun.attribute(TechAttribute.BOSS_DAMAGE_PERCENT));
        PenetrationProfile profile = new PenetrationProfile(penetration, shieldPenetration, bossPenetration, bossPercent);

        behavior = new com.huige233.transcend.tech.ammo.BoltBehavior(
                behavior.pierce + ammoModule.entityPierceBonus(),
                Math.max(behavior.splashRadius, ammoModule.splashRadius()),
                behavior.explode || ammoModule == GunModule.AMMO_EXPLOSIVE);
        
        if (!gun.commitShot()) return;
        Vec3 look = player.getLookAngle();
        int count = com.huige233.transcend.tech.combat.CombatNumbers.shotCount(
                gun.attribute(TechAttribute.SHOT_COUNT), ammoModule.projectileCount());
        profile = com.huige233.transcend.tech.combat.ShotPenetration.perProjectile(profile, sirius, count);
        double moduleSpread = com.huige233.transcend.tech.combat.CombatNumbers.spread(gun.attribute(TechAttribute.ACCURACY_SPREAD));
        double spread = Math.max(0.0D, (player.isShiftKeyDown() && GunConfig.hasScope(gunStack) ? 4.0D : 8.0D) + moduleSpread);
        for (int i = 0; i < count; i++) {
            Vec3 direction = spreadDirection(look, player.getYRot(), (i - (count - 1) * 0.5D) * spread);
            shootBolt(level, player, direction, speed, damage, visual, behavior, behavior.explode, profile, sirius);
        }

        boolean silenced = GunConfig.getMuzzle(gunStack) == GunModule.MUZZLE_SILENCER
                || gun.attribute(TechAttribute.SILENCED) > 0.0D;
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, silenced ? 0.08F : 0.6F, 1.0F);
        player.getCooldowns().addCooldown(this, GunConfig.cooldownTicks(gunStack));
    }

    private static void shootBolt(Level level, Player player, Vec3 direction, float speed, float damage,
                                  com.huige233.transcend.tech.ammo.BoltVisual visual,
                                  com.huige233.transcend.tech.ammo.BoltBehavior behavior, boolean explosive,
                                  PenetrationProfile profile, boolean sirius) {
        ParticleBolt.shoot(level, player, direction, speed, damage, visual.color, visual.size,
                behavior.pierce, behavior.splashRadius, explosive, profile, sirius);
    }

    private static int finiteInt(double value) {
        return com.huige233.transcend.tech.combat.CombatNumbers.strength(value);
    }

    private static float finiteFloat(double value) {
        return com.huige233.transcend.tech.combat.CombatNumbers.nonNegative(value);
    }

    static Vec3 spreadDirection(Vec3 look, float yaw, double degrees) {
        if (!Double.isFinite(look.x) || !Double.isFinite(look.y) || !Double.isFinite(look.z)) look = new Vec3(0, 0, 1);
        double radians = Math.toRadians(Double.isFinite(degrees) ? degrees % 360.0D : 0.0D);
        double yawRadians = Math.toRadians(Float.isFinite(yaw) ? yaw : 0.0F);
        Vec3 right = new Vec3(Math.cos(yawRadians), 0.0D, Math.sin(yawRadians));
        return look.normalize().scale(Math.cos(radians)).add(right.scale(Math.sin(radians))).normalize();
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level,
                              @NotNull net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide) GunInstance.of(stack).tick(level);
    }

    @Override public boolean isBarVisible(@NotNull ItemStack stack) {
        return readCharge(stack) < TechConfig.chargeAmmoMax();
    }
    @Override public int getBarWidth(@NotNull ItemStack stack) {
        return (int) (readCharge(stack) / Math.max(1.0F, TechConfig.chargeAmmoMax()) * 13.0F);
    }
    @Override public int getBarColor(@NotNull ItemStack stack) {
        return readCharge(stack) >= TechConfig.chargeAmmoMax() ? 0xFF00FFFF : 0xFF0088CC;
    }
    @Override public boolean isDamageable(ItemStack stack) { return false; }
    @Override public boolean isFoil(@NotNull ItemStack stack) {
        return GunConfig.damageMult(stack) != 1.0F || GunConfig.cooldownMult(stack) != 1.0F
                || GunConfig.speedMult(stack) != 1.0F;
    }
}
