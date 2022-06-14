package huige233.transcend.util;

import huige233.transcend.init.ModItems;
import huige233.transcend.items.tools.ToolSword;
import huige233.transcend.items.tools.ToolWarp;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
public class ToolTip {

    public ToolTip(){
    }
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event){
        if(Loader.isModLoaded("thaumcraft")){
            if(event.getItemStack().getItem() instanceof ToolWarp){
                for(int x=0;x<event.getToolTip().size();x++){
                    if(event.getToolTip().get(x).contains(I18n.translateToLocal("tooltip.warp_sword1.desc"))){
                        event.getToolTip().set(x, TextFormatting.GOLD+I18n.translateToLocal("tooltip.warp_sword2.desc"));
                        return;
                    }
                }
            }
        }
        if(event.getItemStack().getItem() instanceof ToolSword) {
            for (int x = 0; x < event.getToolTip().size(); ++x) {
                if (((String) event.getToolTip().get(x)).contains(I18n.translateToLocal("attribute.name.generic.attackDamage")) || ((String) event.getToolTip().get(x)).contains(I18n.translateToLocal("Attack Damage"))) {
                    if (event.getItemStack().getItem() == ModItems.TRANSCEND_SWORD) {
                        event.getToolTip().set(x, TextFormatting.BLUE + "+" + TextUtils.makeFabulous(I18n.translateToLocal("tip.transcend")) + " " + TextFormatting.BLUE + I18n.translateToLocal("attribute.name.generic.attackDamage"));
                        event.getToolTip().set(x+1, TextFormatting.BLUE + "+" + TextUtils.makeFabulous(I18n.translateToLocal("tip.transcend")) + " " + TextFormatting.BLUE + I18n.translateToLocal("attribute.name.generic.reachDistance"));
                        return;
                    }
                }
            }
        }
    }
}
