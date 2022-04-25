package huige233.transcend.compat.tinkers;

import huige233.transcend.init.ModItems;
import huige233.transcend.lib.TranscendDamageSources;
import huige233.transcend.util.ArmorUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.materials.*;
import slimeknights.tconstruct.library.smeltery.Cast;
import slimeknights.tconstruct.library.tinkering.MaterialItem;
import slimeknights.tconstruct.library.tools.IToolPart;
import slimeknights.tconstruct.library.traits.AbstractTrait;
import slimeknights.tconstruct.library.utils.HarvestLevels;
import slimeknights.tconstruct.library.utils.TagUtil;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import static slimeknights.tconstruct.library.materials.MaterialTypes.HEAD;

public class TiCConfig {
    public static class TiCMaterials{
        public static final AbstractTrait flawlesstrait = new TraitFlawless();
        public static Material flawless = new Material("flawless", -1);
        public static final AbstractTrait transcendtratt =  new TraitTranscend();
        public static Material transcend = new Material("transcend",-1);

        public static void setup() {
            HarvestLevels.harvestLevelNames.put(32, TextFormatting.GRAY + "Flawless");
            flawless.addItem(ModItems.FLAWLESS);
            flawless.setRepresentativeItem(ModItems.FLAWLESS);
            flawless.setCastable(true);
            flawless.setCraftable(true);
            flawless.addTrait(flawlesstrait, HEAD);
            TinkerRegistry.addMaterialStats(flawless,
                    new HeadMaterialStats(9999, 100.0f, 2000.0f, 32),
                    new HandleMaterialStats(10.0f, 9999),
                    new ExtraMaterialStats(9999));
            new BowMaterialStats(15.0F, 15.0F, 10F);


            HarvestLevels.harvestLevelNames.put(99, TextFormatting.GRAY + "Transcend");
            transcend.addItem(ModItems.TRANSCEND);
            transcend.setRepresentativeItem(ModItems.TRANSCEND);
            transcend.setCastable(true);
            transcend.setCraftable(true);
            transcend.addTrait(transcendtratt, HEAD);
            TinkerRegistry.addMaterialStats(transcend,
                new HeadMaterialStats(9999, 100.0f, 9999.0f, 99),
                new HandleMaterialStats(100.0f, 9999),
                new ExtraMaterialStats(9999));
            new BowMaterialStats(45.0F, 45.0F, 50F);

            TinkerRegistry.integrate(flawless).preInit();
            TinkerRegistry.integrate(transcend).preInit();

            registerToolParts(flawless);
            registerToolParts(transcend);
        }

        private static void registerToolParts(Material material)
        {
            Fluid fluid = material.getFluid();

            for(IToolPart toolPart : TinkerRegistry.getToolParts())
            {
                if(!toolPart.canBeCasted())
                    continue;

                if(!toolPart.canUseMaterial(material))
                    continue;

                if(toolPart instanceof MaterialItem)
                {
                    ItemStack stack = toolPart.getItemstackWithMaterial(material);

                    ItemStack originCast = Cast.setTagForPart(new ItemStack(TinkerSmeltery.cast), stack.getItem());

                    if(fluid != null)
                    {
                        TinkerRegistry.registerMelting(stack, fluid, toolPart.getCost());
                        TinkerRegistry.registerTableCasting(stack, originCast, fluid, toolPart.getCost());
                    }
                }
            }
        }
        public static void setRenderInfo()
        {
            flawless.setRenderInfo(-1);
            transcend.setRenderInfo(-1);
        }
    }
    public static class TraitFlawless extends AbstractTrait
    {
        public TraitFlawless()
        {
            super("flawless", TextFormatting.DARK_GRAY);
        }

        @Override
        public void onArmorTick(ItemStack tool, World world, EntityPlayer player) {
        }

        @Override
        public float damage(ItemStack tool, EntityLivingBase player, EntityLivingBase target, float damage, float newDamage, boolean isCritical) {
            return 0;
        }

        @Override
        public void applyEffect(NBTTagCompound rootCompound,NBTTagCompound modifierTag) {
            NBTTagCompound toolTag = TagUtil.getToolTag(rootCompound);
            toolTag.setInteger("FreeModifiers", 100);
            rootCompound.setBoolean("Unbreakable", true);
            TagUtil.setToolTag(rootCompound, toolTag);
        }
    }

    public static class TraitTranscend extends AbstractTrait
    {
        public TraitTranscend() {super("transcend",TextFormatting.DARK_GRAY);}

        @Override
        public void applyEffect(NBTTagCompound rootCompound,NBTTagCompound modifierTag) {
            NBTTagCompound toolTag = TagUtil.getToolTag(rootCompound);
            toolTag.setInteger("FreeModifiers", 100);
            rootCompound.setBoolean("Unbreakable", true);
            TagUtil.setToolTag(rootCompound, toolTag);
        }

        @Override
        public void afterHit(ItemStack tool, EntityLivingBase player, EntityLivingBase target, float damageDealt, boolean wasCritical, boolean wasHit) {
            if(target instanceof EntityPlayer) {
                EntityPlayer player1 = (EntityPlayer) target;
                if(ArmorUtils.fullEquipped(player1)) {
                    target.setHealth(target.getMaxHealth()-4);
                }
            }
            target.attackEntityFrom((new TranscendDamageSources(player)).setDamageAllowedInCreativeMode().setDamageBypassesArmor().setDamageIsAbsolute(),Float.MAX_VALUE);
            target.setHealth(0);
            target.setDead();
        }

        @Override
        public boolean isCriticalHit(ItemStack tool, EntityLivingBase player, EntityLivingBase target) {
            if(target instanceof EntityPlayer) {
                EntityPlayer p = (EntityPlayer) target;
                if(ArmorUtils.fullEquipped(p)){
                    target.setHealth(target.getHealth()-4);
                }
            }
            target.attackEntityFrom((new TranscendDamageSources(player)).setDamageAllowedInCreativeMode().setDamageBypassesArmor().setDamageIsAbsolute(),Float.MAX_VALUE);
            target.setHealth(0);
            target.setDead();
            target.getCombatTracker().trackDamage(new TranscendDamageSources(player),Float.MAX_VALUE,Float.MAX_VALUE);
            target.onDeath(new EntityDamageSource("transcend",player));
            target.isDead = true;
            return false;
        }
    }
}
