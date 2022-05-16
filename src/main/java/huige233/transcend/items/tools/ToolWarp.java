package huige233.transcend.items.tools;

import huige233.transcend.Main;
import huige233.transcend.compat.ThaumcraftSword;
import huige233.transcend.init.ModItems;
import huige233.transcend.util.IHasModel;
import huige233.transcend.util.TextUtils;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;

import java.util.List;

public class ToolWarp extends ItemSword implements IHasModel {
    public ToolWarp(String name, CreativeTabs tab, ToolMaterial material) {
        super(material);
        setTranslationKey(name);
        setRegistryName(name);
        setCreativeTab(tab);
        ModItems.ITEMS.add(this);
    }
    @Override
    public void registerModels() {
        Main.proxy.registerItemRenderer(this,0,"inventory");
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        if (Loader.isModLoaded("thaumcraft")) {
            ThaumcraftSword.warpsword(stack,target);
            stack.setItemDamage(stack.getItemDamage()-1);
            return true;
        }
        stack.setItemDamage(stack.getItemDamage()-1);
        return true;
    }
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag){
        tooltip.add(TextFormatting.DARK_GRAY+I18n.translateToLocal("tooltip.warp_sword1.desc"));
    }
}
