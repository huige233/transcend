package huige233.transcend.init;

import huige233.transcend.enchantment.EnchantmentFLAWLESSEnchantment;
import huige233.transcend.util.Reference;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class ModEnchantment {
    public static final List<Enchantment> ENCHANTMENTS = new ArrayList<Enchantment>();
    public static final Enchantment FLAWLESS = new EnchantmentFLAWLESSEnchantment();

    @SubscribeEvent
    public static void EnchantmentFunction(LivingUpdateEvent event) {
        EntityLivingBase living = event.getEntityLiving();
        int level = EnchantmentHelper.getMaxEnchantmentLevel(FLAWLESS, living);
        BlockPos pos = living.getPosition();
        World world = event.getEntity().world;
    }
}
