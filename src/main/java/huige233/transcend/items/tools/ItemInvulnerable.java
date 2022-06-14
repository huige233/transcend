package huige233.transcend.items.tools;

import huige233.transcend.Main;
import huige233.transcend.init.ModItems;
import huige233.transcend.util.ItemNBTHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ItemInvulnerable extends ItemSword {
    public ItemInvulnerable(String name, ToolMaterial material) {
        super(material);
        setTranslationKey(name);
        setRegistryName(name);
        setCreativeTab(Main.TranscendTab);
        ModItems.ITEMS.add(this);
    }

    @Override
    public boolean onLeftClickEntity(@NotNull ItemStack stack, @NotNull EntityPlayer player, Entity entity) {
        if (!player.world.isRemote) {
            entity.setEntityInvulnerable(!entity.getIsInvulnerable());
            return true;
        }
        return false;
    }
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag){
        tooltip.add(TextFormatting.BLUE+I18n.translateToLocal("tooltip.invulnerable"));
    }
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> stack) {
        if(tab == Main.TranscendTab) {
            ItemStack itemstack = new ItemStack(this);
            ItemNBTHelper.setInt(itemstack, "HideFlags", 3);
            stack.add(itemstack);
        }
    }
}