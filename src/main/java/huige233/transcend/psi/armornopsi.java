package huige233.transcend.psi;
import huige233.transcend.init.ModItems;
import huige233.transcend.util.IHasModel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import slimeknights.mantle.typesafe.config.Optional;
import vazkii.psi.api.PsiAPI;
import vazkii.psi.api.spell.ISpellImmune;

public class armornopsi extends ItemArmor implements ISpellImmune {
    public armornopsi(ArmorMaterial materialIn, int renderIndexIn, EntityEquipmentSlot equipmentSlotIn) {
        super(materialIn, renderIndexIn, equipmentSlotIn);
    }
    @CapabilityInject(ISpellImmune.class)
    @Override
    public void onArmorTick(World world, EntityPlayer player, ItemStack itemStack ) {
        if (this.armorType == EntityEquipmentSlot.HEAD){
            ISpellImmune.immunity(player).isImmune();
        }
    }

    @Override
    public boolean isImmune() {
        return true;
    }
}
