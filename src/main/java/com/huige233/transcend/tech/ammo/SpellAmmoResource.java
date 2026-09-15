package com.huige233.transcend.tech.ammo;

import com.Polarice3.Goety.common.capabilities.soulenergy.ISoulEnergy;
import com.Polarice3.Goety.common.capabilities.soulenergy.SEProvider;
import com.huige233.transcend.tech.TechConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;


/** 按配置优先级选择单个外部法术资源提供方，为法术弹装填扣除魔力或灵魂能量。 */
public final class SpellAmmoResource implements AmmoResource {
    public static final SpellAmmoResource INSTANCE = new SpellAmmoResource();

    private SpellAmmoResource() {
    }

    @Override public String id() { return BuiltInAmmoTypes.SPELL_ID; }
    @Override public Component displayName() { return Component.translatable("ammo.transcend.spell"); }

    @Override
    public long available(Player holder, ItemStack gun) {
        Provider provider = select(holder, 0L);
        return provider == null ? 0L : provider.available(holder);
    }

    @Override
    public long consume(Player holder, ItemStack gun, long cost) {
        if (cost < 0 || cost > Integer.MAX_VALUE) return 0L;
        Provider provider = select(holder, cost);
        return provider != null && provider.consume(holder, cost) ? cost : 0L;
    }

    @Override
    public boolean isAvailable(Player holder) {
        return select(holder, 0L) != null;
    }

    private static Provider select(Player player, long required) {
        if (player == null || ModList.get() == null) return null;
        for (String id : TechConfig.spellProviderPriority()) {
            Provider provider = switch (id) {
                case "irons_spellbooks" -> ISS;
                case "goety" -> GOETY;
                default -> null;
            };
            if (provider != null && ModList.get().isLoaded(provider.modId())
                    && provider.available(player) >= required) {
                return provider;
            }
        }
        return null;
    }

    /** 约定外部法术模组的标识、玩家资源余额查询和装填费用扣除操作。 */
    private interface Provider {
        String modId();
        long available(Player player);
        boolean consume(Player player, long cost);
    }

    private static final Provider ISS = new Provider() {
        @Override public String modId() { return "irons_spellbooks"; }
        @Override public long available(Player player) {
            try {
                MagicData data = MagicData.getPlayerMagicData(player);
                return data == null ? 0L : Math.max(0L, (long) Math.floor(data.getMana()));
            } catch (Throwable ignored) {
                return 0L;
            }
        }
        @Override public boolean consume(Player player, long cost) {
            try {
                MagicData data = MagicData.getPlayerMagicData(player);
                if (data == null || data.getMana() < cost) return false;
                data.setMana(data.getMana() - cost);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }
    };

    private static final Provider GOETY = new Provider() {
        @Override public String modId() { return "goety"; }
        @Override public long available(Player player) {
            return player.getCapability(SEProvider.CAPABILITY)
                    .map(ISoulEnergy::getSoulEnergy).map(value -> (long) Math.max(0, value)).orElse(0L);
        }
        @Override public boolean consume(Player player, long cost) {
            return player.getCapability(SEProvider.CAPABILITY).map(energy -> {
                if (energy.getSoulEnergy() < cost) return false;
                energy.setSoulEnergy((int) (energy.getSoulEnergy() - cost));
                return true;
            }).orElse(false);
        }
    };
}
