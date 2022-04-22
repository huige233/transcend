package huige233.transcend.compat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.common.Mod;
import morph.avaritia.init.ModItems;

@Mod.EventBusSubscriber
public class Avartiabreak {
    public static boolean enabled = false;

    public static void onPlayerAttack(EntityPlayer player,EntityPlayer attacker) {
        if(!enabled) return;
        if(player.world.isRemote) return;
        NonNullList<ItemStack> armor = player.inventory.armorInventory;
        if(armor.get(3).getItem() ==ModItems.infinity_helmet && armor.get(2).getItem() ==ModItems.infinity_chestplate && armor.get(1).getItem() ==ModItems.infinity_pants && armor.get(0).getItem() ==ModItems.infinity_boots) {
            player.setHealth(0);
        }
    }
}
