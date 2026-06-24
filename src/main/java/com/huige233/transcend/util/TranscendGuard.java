package com.huige233.transcend.util;

import com.huige233.transcend.items.TranscendShield;
import com.huige233.transcend.items.tools.TranscendSword;
import com.huige233.transcend.mixinitf.ITranscendMarked;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;


public final class TranscendGuard {

    private TranscendGuard() {
    }

    /** 背包/手持/盔甲位任意处有 TranscendSword 即视为持剑(与盾的判定一致)。 */
    private static boolean hasTranscendSword(Player player) {
        if (player.getMainHandItem().getItem() instanceof TranscendSword) return true;
        if (player.getOffhandItem().getItem() instanceof TranscendSword) return true;
        for (ItemStack s : player.getInventory().items) {
            if (s.getItem() instanceof TranscendSword) return true;
        }
        return false;
    }


    public static boolean isProtected(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) return false;
        if (!(entity instanceof Player player)) return false;
        // 实体构造期:LivingEntity 父类构造函数里就会 setHealth(maxHealth),此时 Player.inventory
        // 还没初始化(为 null)。若此时去查装备会 NPE,导致玩家存档读取失败、进游戏报「无效的玩家数据」。
        if (player.getInventory() == null) return false;
        if (entity instanceof ITranscendMarked marked && marked.transcend$isMarked()) return false;
        // 防御来源:穿满 TranscendArmor、或背包(含手持/盔甲位)任意处有 TranscendShield / TranscendSword。
        // 剑现在和盾一样「在背包即生效」,享有方法级防秒杀 + 每 tick 变量回溯 + getHealth 锁。
        return ArmorUtils.fullEquipped(player)
                || TranscendShield.hasTranscendShield(player)
                || hasTranscendSword(player);
    }


    /**
     * 受保护玩家「无视穿血」总闸:任何会<b>降低</b>血量、或把血量<b>污染成 NaN</b> 的 setHealth 都拦下。
     *
     * <p>UOM 等的 {@code actuallyHurt} 直伤是 {@code setHealth(getHealth()-amount)} 小额连击磨血,
     * 血量始终 &gt;0 以绕过「只拦致死」的旧守卫;NaN 则用来让 {@code health>0}/{@code ≤0} 判定全失效来破锁血。
     * 这里对受保护玩家把「降低 / NaN」一律拦死,而回血 / 回满(增大)放行,不影响 reassert 与正常恢复。</p>
     */
    public static boolean blocksPierceSetHealth(LivingEntity entity, float health) {
        if (!isProtected(entity)) return false;
        return Float.isNaN(health) || health < entity.getHealth();
    }
}
