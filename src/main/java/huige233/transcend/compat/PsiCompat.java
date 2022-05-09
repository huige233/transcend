package huige233.transcend.compat;

import huige233.transcend.init.ModItems;
import huige233.transcend.util.ArmorUtils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import vazkii.psi.api.PsiAPI;
import vazkii.psi.api.cad.ICAD;
import vazkii.psi.api.cad.RegenPsiEvent;
import vazkii.psi.common.core.handler.PlayerDataHandler;

public class PsiCompat {
    public static boolean enabled = false;

    public static void onPlayerAttack(EntityPlayer player, EntityPlayer attacker) {
        if(player.world.isRemote) return;
        PlayerDataHandler.PlayerData data = PlayerDataHandler.get(attacker);
        ItemStack cadItem = PsiAPI.getPlayerCAD(attacker);
        if(cadItem != null && cadItem.getItem() instanceof ICAD) {
            ICAD cad = (ICAD) cadItem.getItem();
            int storedPsi = cad.getStoredPsi(cadItem);
            data.deductPsi(data.getAvailablePsi() + storedPsi, 200, true);
        }else {
            data.deductPsi(data.getAvailablePsi(), 200, true);
        }

    }


    public static void hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase player) {
        if(player.world.isRemote) {return;}
        if(target instanceof EntityPlayer) {
            EntityPlayer p = (EntityPlayer) target;
            if(ArmorUtils.fullEquipped(p)){
                target.setHealth(target.getHealth()-4);
                return;
            }
            if(p.getHeldItem(EnumHand.MAIN_HAND) != null && p.getHeldItem(EnumHand.MAIN_HAND).getItem()== ModItems.TRANSCEND_SWORD && p.isHandActive()) {
                return;
            }
        }
        EntityPlayer t = (EntityPlayer) target;
        PlayerDataHandler.PlayerData data = PlayerDataHandler.get(t);
        ItemStack cadItem = PsiAPI.getPlayerCAD(t);
        if(cadItem != null && cadItem.getItem() instanceof ICAD) {
            ICAD cad = (ICAD) cadItem.getItem();
            int storedPsi = cad.getStoredPsi(cadItem);
            data.deductPsi((data.getAvailablePsi() + storedPsi)*10, 2000, true);
        }else {
            data.deductPsi(data.getAvailablePsi()*10, 2000, true);
        }
    }

    @SubscribeEvent
    public static void onPsiRegen(RegenPsiEvent event) {
        EntityPlayer player = event.getPlayer();
        //if(player.world.isRemote) return;
        if(ArmorUtils.fullEquipped(player)){
            event.setRegenCooldown(0);
            event.addRegen(5000);
        }
    }


}
