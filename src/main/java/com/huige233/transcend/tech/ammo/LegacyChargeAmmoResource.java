package com.huige233.transcend.tech.ammo;

import com.huige233.transcend.tech.TechConfig;
import com.huige233.transcend.tech.core.TechItemData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;


/** 使用迁移后的枪械内部蓄能支付兼容弹药装填，并提供蓄能恢复操作。 */
public final class LegacyChargeAmmoResource implements AmmoResource {
    public static final LegacyChargeAmmoResource INSTANCE = new LegacyChargeAmmoResource();

    private LegacyChargeAmmoResource() {
    }

    @Override public String id() { return BuiltInAmmoTypes.CHARGE_ID; }
    @Override public Component displayName() { return Component.translatable("ammo.transcend.charge"); }
    @Override public long available(Player holder, ItemStack gun) { return (long) Math.floor(TechItemData.getGunCharge(gun)); }

    @Override
    public long consume(Player holder, ItemStack gun, long cost) {
        if (cost < 0 || available(holder, gun) < cost) return 0L;
        TechItemData.setGunCharge(gun, TechItemData.getGunCharge(gun) - cost);
        return cost;
    }

    @Override public boolean isAvailable(Player holder) { return true; }

    public void regenerate(ItemStack gun) {
        TechItemData.setGunCharge(gun, Math.min(TechConfig.chargeAmmoMax(),
                TechItemData.getGunCharge(gun) + (float) TechConfig.chargeAmmoRegenPerTick()));
    }
}
