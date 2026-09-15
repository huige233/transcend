package com.huige233.transcend.client;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.entity.TestDummy;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

   
                         
                                               
   
/** 向 Jade 注册测试假人的专属信息提供器。 */
@WailaPlugin(Transcend.MODID)
public class TranscendJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(new DummyProvider(), TestDummy.class);
    }

    
    /** 在 Jade 提示面板追加假人的护盾进度、实体类别、减伤、增益数量与伤害统计。 */
    private static class DummyProvider implements IEntityComponentProvider {
        @Override
        public net.minecraft.resources.ResourceLocation getUid() {
            return Transcend.rl("dummy_info");
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, snownee.jade.api.config.IPluginConfig config) {
            if (!(accessor.getEntity() instanceof TestDummy dummy)) return;

            
            if (dummy.isShieldEnabled() && dummy.getShieldMax() > 0) {
                float ratio = Math.max(0F, Math.min(1F, dummy.getShieldValue() / dummy.getShieldMax()));
                var helper = snownee.jade.api.ui.IElementHelper.get();
                var style = helper.progressStyle();
                style.color(0xFF2A6E8F, 0xFF55CCFF);
                style.textColor(0xFFBFEFFF);
                
                tooltip.add(helper.progress(
                        ratio,
                        Component.literal(String.format("%s / %s",
                                fmt(dummy.getShieldValue()), fmt(dummy.getShieldMax()))),
                        style, snownee.jade.api.ui.IBoxStyle.Empty.INSTANCE, false));
            }

            String kind = kindName(dummy.getKind());
            tooltip.add(Component.literal(String.format("%s: %s%s",
                    Component.translatable("jade.transcend.kind").getString(),
                    ChatFormatting.AQUA, kind)).withStyle(ChatFormatting.GRAY));

            int reducePct = dummy.getResistanceLevel() * 20;
            tooltip.add(Component.literal(String.format("%s: %s%d%%%s   %s: %d",
                    Component.translatable("jade.transcend.resist").getString(),
                    ChatFormatting.AQUA, reducePct, ChatFormatting.GRAY,
                    Component.translatable("jade.transcend.buffs").getString(),
                    dummy.getActiveBuffDescriptions().size())).withStyle(ChatFormatting.GRAY));

            tooltip.add(Component.literal(String.format("%s: %s%.0f%s   %s: %s%.0f%s   %s: %s%d",
                    Component.translatable("jade.transcend.total").getString(),
                    ChatFormatting.YELLOW, dummy.getTotalDamage(), ChatFormatting.GRAY,
                    Component.translatable("jade.transcend.max_hit").getString(),
                    ChatFormatting.RED, dummy.getMaxHit(), ChatFormatting.GRAY,
                    Component.translatable("jade.transcend.hits").getString(),
                    ChatFormatting.WHITE, dummy.getHitCount())).withStyle(ChatFormatting.GRAY));
        }
    }

    private static String kindName(TestDummy.DummyKind kind) {
        return switch (kind) {
            case UNDEAD -> "Undead";
            case ARTHROPOD -> "Arthropod";
            case ILLAGER -> "Illager";
            case WATER -> "Aquatic";
            default -> "Normal";
        };
    }

    
    private static String fmt(float v) {
        if (v >= 1_000_000F) return String.format("%.2fM", v / 1_000_000F);
        if (v >= 10_000F) return String.format("%.1fk", v / 1_000F);
        return String.format("%.0f", v);
    }
}
