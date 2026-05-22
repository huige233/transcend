package com.huige233.transcend.init;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.items.ItemBase;
import com.huige233.transcend.items.MagicCircleItem;
import com.huige233.transcend.items.MagicCircleItem1;
import com.huige233.transcend.items.MagicCircleItemBase;
import com.huige233.transcend.items.MagicCrystalItem;
import com.huige233.transcend.items.ManaStorageItem;
import com.huige233.transcend.items.TypedManaCrystal;
import com.huige233.transcend.items.CircleEnhanceMaterial;
import com.huige233.transcend.items.SpellBaseItem;
import com.huige233.transcend.items.SpellCarrierItem;
import com.huige233.transcend.items.SpellElementItem;
import com.huige233.transcend.items.SpellEffectItem;
import com.huige233.transcend.items.SpellScrollItem;
import com.huige233.transcend.items.SpellUpgradeStone;
import com.huige233.transcend.items.TranscendWand;
import com.huige233.transcend.items.RuneItem;
import com.huige233.transcend.items.armor.ElementArmor;
import com.huige233.transcend.spell.WandRune;
import net.minecraft.world.item.ArmorItem;
import com.huige233.transcend.spell.SpellCarrier;
import com.huige233.transcend.spell.SpellElement;
import com.huige233.transcend.spell.SpellEffect;
import com.huige233.transcend.items.TranscendShield;
import com.huige233.transcend.client.magic.MagicCircleType;
import com.huige233.transcend.client.magic.MagicCircleNBTHelper;
import com.huige233.transcend.items.AscensionBookItem;
import com.huige233.transcend.items.armor.TranscendArmor;
import com.huige233.transcend.items.curio.AnvilCompat;
import com.huige233.transcend.items.curio.FragmentLan;
import com.huige233.transcend.items.curio.ThunderSkin;
import com.huige233.transcend.items.curio.TranscendCurio;
import com.huige233.transcend.items.curio.TheLastTotem;
import com.huige233.transcend.items.tools.TestSword;
import com.huige233.transcend.items.tools.TranscendSword;
import com.huige233.transcend.items.circle.*;
import com.huige233.transcend.circle.CircleFunctionType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public class ModItems {

    public static final List<Item> ITEMS = new ArrayList<>();

    public static final DeferredRegister<Item> ITEMS_REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, Transcend.MODID);

    public static final RegistryObject<Item> transcend_ingot = ITEMS_REGISTRY.register("transcend_ingot",
            () -> new ItemBase("transcend_ingot"));
    public static final RegistryObject<Item> epic_ingot = ITEMS_REGISTRY.register("epic_ingot",
            () -> new ItemBase("epic_ingot"));
    public static final RegistryObject<Item> normal_ingot = ITEMS_REGISTRY.register("normal_ingot",
            () -> new ItemBase("normal_ingot"));
    public static final RegistryObject<Item> transcend_sword = ITEMS_REGISTRY.register("transcend_sword",
            TranscendSword::new);
    public static final RegistryObject<Item> test_sword = ITEMS_REGISTRY.register("test_sword",
            TestSword::new);
    public static final RegistryObject<Item> transcend_curio = ITEMS_REGISTRY.register("transcend_curio",
            () -> new TranscendCurio(new Item.Properties()));
    public static final RegistryObject<Item> thelasttotem = ITEMS_REGISTRY.register("thelasttotem",
            TheLastTotem::new);

    public static final RegistryObject<Item> transcend_helmet = ITEMS_REGISTRY.register("transcend_helmet",
            () -> new TranscendArmor(ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> transcend_chestplate = ITEMS_REGISTRY.register("transcend_chestplate",
            () -> new TranscendArmor(ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> transcend_leggings = ITEMS_REGISTRY.register("transcend_leggings",
            () -> new TranscendArmor(ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> transcend_boots = ITEMS_REGISTRY.register("transcend_boots",
            () -> new TranscendArmor(ArmorItem.Type.BOOTS));

    public static final RegistryObject<Item> pyro_helmet = ITEMS_REGISTRY.register("pyro_helmet", () -> new ElementArmor(ElementArmor.ElementSet.PYRO, ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> pyro_chestplate = ITEMS_REGISTRY.register("pyro_chestplate", () -> new ElementArmor(ElementArmor.ElementSet.PYRO, ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> pyro_leggings = ITEMS_REGISTRY.register("pyro_leggings", () -> new ElementArmor(ElementArmor.ElementSet.PYRO, ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> pyro_boots = ITEMS_REGISTRY.register("pyro_boots", () -> new ElementArmor(ElementArmor.ElementSet.PYRO, ArmorItem.Type.BOOTS));

    public static final RegistryObject<Item> cryo_helmet = ITEMS_REGISTRY.register("cryo_helmet", () -> new ElementArmor(ElementArmor.ElementSet.CRYO, ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> cryo_chestplate = ITEMS_REGISTRY.register("cryo_chestplate", () -> new ElementArmor(ElementArmor.ElementSet.CRYO, ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> cryo_leggings = ITEMS_REGISTRY.register("cryo_leggings", () -> new ElementArmor(ElementArmor.ElementSet.CRYO, ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> cryo_boots = ITEMS_REGISTRY.register("cryo_boots", () -> new ElementArmor(ElementArmor.ElementSet.CRYO, ArmorItem.Type.BOOTS));

    public static final RegistryObject<Item> storm_helmet = ITEMS_REGISTRY.register("storm_helmet", () -> new ElementArmor(ElementArmor.ElementSet.STORM, ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> storm_chestplate = ITEMS_REGISTRY.register("storm_chestplate", () -> new ElementArmor(ElementArmor.ElementSet.STORM, ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> storm_leggings = ITEMS_REGISTRY.register("storm_leggings", () -> new ElementArmor(ElementArmor.ElementSet.STORM, ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> storm_boots = ITEMS_REGISTRY.register("storm_boots", () -> new ElementArmor(ElementArmor.ElementSet.STORM, ArmorItem.Type.BOOTS));

    public static final RegistryObject<Item> terra_helmet = ITEMS_REGISTRY.register("terra_helmet", () -> new ElementArmor(ElementArmor.ElementSet.TERRA, ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> terra_chestplate = ITEMS_REGISTRY.register("terra_chestplate", () -> new ElementArmor(ElementArmor.ElementSet.TERRA, ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> terra_leggings = ITEMS_REGISTRY.register("terra_leggings", () -> new ElementArmor(ElementArmor.ElementSet.TERRA, ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> terra_boots = ITEMS_REGISTRY.register("terra_boots", () -> new ElementArmor(ElementArmor.ElementSet.TERRA, ArmorItem.Type.BOOTS));

    public static final RegistryObject<Item> arcane_set_helmet = ITEMS_REGISTRY.register("arcane_set_helmet", () -> new ElementArmor(ElementArmor.ElementSet.ARCANE, ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> arcane_set_chestplate = ITEMS_REGISTRY.register("arcane_set_chestplate", () -> new ElementArmor(ElementArmor.ElementSet.ARCANE, ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> arcane_set_leggings = ITEMS_REGISTRY.register("arcane_set_leggings", () -> new ElementArmor(ElementArmor.ElementSet.ARCANE, ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> arcane_set_boots = ITEMS_REGISTRY.register("arcane_set_boots", () -> new ElementArmor(ElementArmor.ElementSet.ARCANE, ArmorItem.Type.BOOTS));

    public static final RegistryObject<Item> abyss_helmet = ITEMS_REGISTRY.register("abyss_helmet", () -> new ElementArmor(ElementArmor.ElementSet.ABYSS, ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> abyss_chestplate = ITEMS_REGISTRY.register("abyss_chestplate", () -> new ElementArmor(ElementArmor.ElementSet.ABYSS, ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> abyss_leggings = ITEMS_REGISTRY.register("abyss_leggings", () -> new ElementArmor(ElementArmor.ElementSet.ABYSS, ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> abyss_boots = ITEMS_REGISTRY.register("abyss_boots", () -> new ElementArmor(ElementArmor.ElementSet.ABYSS, ArmorItem.Type.BOOTS));

    public static final RegistryObject<Item> transcend_shield = ITEMS_REGISTRY.register("transcend_shield",
            TranscendShield::new);

    public static final RegistryObject<Item> anvil_compat = ITEMS_REGISTRY.register("anvil_compat",
            AnvilCompat::new);
    public static final RegistryObject<Item> fragment_lan = ITEMS_REGISTRY.register("fragment_lan",
            FragmentLan::new);
    public static final RegistryObject<Item> thunder_skin = ITEMS_REGISTRY.register("thunder_skin",
            ThunderSkin::new);

    public static final RegistryObject<Item> magic_circle = ITEMS_REGISTRY.register("magic_circle",
            MagicCircleItem::new);
    public static final RegistryObject<Item> magic_circle_alt = ITEMS_REGISTRY.register("magic_circle_alt",
            MagicCircleItem1::new);
    public static final RegistryObject<Item> magic_circle_inferno = ITEMS_REGISTRY.register("magic_circle_inferno",
            () -> new MagicCircleItemBase(MagicCircleType.INFERNO));
    public static final RegistryObject<Item> magic_circle_glacial = ITEMS_REGISTRY.register("magic_circle_glacial",
            () -> new MagicCircleItemBase(MagicCircleType.GLACIAL));
    public static final RegistryObject<Item> magic_circle_sanctum = ITEMS_REGISTRY.register("magic_circle_sanctum",
            () -> new MagicCircleItemBase(MagicCircleType.SANCTUM));
    public static final RegistryObject<Item> magic_circle_gravity = ITEMS_REGISTRY.register("magic_circle_gravity",
            () -> new MagicCircleItemBase(MagicCircleType.GRAVITY));
    public static final RegistryObject<Item> magic_circle_thunder = ITEMS_REGISTRY.register("magic_circle_thunder",
            () -> new MagicCircleItemBase(MagicCircleType.THUNDER));
    public static final RegistryObject<Item> magic_circle_tempest = ITEMS_REGISTRY.register("magic_circle_tempest",
            () -> new MagicCircleItemBase(MagicCircleType.TEMPEST));
    public static final RegistryObject<Item> magic_circle_terra = ITEMS_REGISTRY.register("magic_circle_terra",
            () -> new MagicCircleItemBase(MagicCircleType.TERRA));
    public static final RegistryObject<Item> magic_circle_void = ITEMS_REGISTRY.register("magic_circle_void",
            () -> new MagicCircleItemBase(MagicCircleType.VOID));
    public static final RegistryObject<Item> magic_circle_chrono = ITEMS_REGISTRY.register("magic_circle_chrono",
            () -> new MagicCircleItemBase(MagicCircleType.CHRONO));
    public static final RegistryObject<Item> magic_circle_blood = ITEMS_REGISTRY.register("magic_circle_blood",
            () -> new MagicCircleItemBase(MagicCircleType.BLOOD));
    public static final RegistryObject<Item> magic_circle_divine = ITEMS_REGISTRY.register("magic_circle_divine",
            () -> new MagicCircleItemBase(MagicCircleType.DIVINE));
    public static final RegistryObject<Item> magic_circle_chaos = ITEMS_REGISTRY.register("magic_circle_chaos",
            () -> new MagicCircleItemBase(MagicCircleType.CHAOS));
    public static final RegistryObject<Item> magic_circle_phantom = ITEMS_REGISTRY.register("magic_circle_phantom",
            () -> new MagicCircleItemBase(MagicCircleType.PHANTOM));
    public static final RegistryObject<Item> magic_circle_skybound = ITEMS_REGISTRY.register("magic_circle_skybound",
            () -> new MagicCircleItemBase(MagicCircleType.SKYBOUND));

    public static final RegistryObject<Item> magic_crystal = ITEMS_REGISTRY.register("magic_crystal",
            () -> new MagicCrystalItem(false));
    public static final RegistryObject<Item> refined_magic_crystal = ITEMS_REGISTRY.register("refined_magic_crystal",
            () -> new MagicCrystalItem(true));

    // === Round 17: 多样化法力 — 元素相位水晶 ===
    public static final RegistryObject<Item> aether_crystal = ITEMS_REGISTRY.register("aether_crystal",
            () -> new TypedManaCrystal(TypedManaCrystal.ManaAspect.AETHER));
    public static final RegistryObject<Item> blood_crystal = ITEMS_REGISTRY.register("blood_crystal",
            () -> new TypedManaCrystal(TypedManaCrystal.ManaAspect.BLOOD));
    public static final RegistryObject<Item> cosmic_crystal = ITEMS_REGISTRY.register("cosmic_crystal",
            () -> new TypedManaCrystal(TypedManaCrystal.ManaAspect.COSMIC));
    public static final RegistryObject<Item> tainted_crystal = ITEMS_REGISTRY.register("tainted_crystal",
            () -> new TypedManaCrystal(TypedManaCrystal.ManaAspect.TAINTED));

    public static final RegistryObject<Item> enhance_power = ITEMS_REGISTRY.register("enhance_power",
            () -> new CircleEnhanceMaterial(MagicCircleNBTHelper.EnhanceType.POWER));
    public static final RegistryObject<Item> enhance_duration = ITEMS_REGISTRY.register("enhance_duration",
            () -> new CircleEnhanceMaterial(MagicCircleNBTHelper.EnhanceType.DURATION));
    public static final RegistryObject<Item> enhance_efficiency = ITEMS_REGISTRY.register("enhance_efficiency",
            () -> new CircleEnhanceMaterial(MagicCircleNBTHelper.EnhanceType.EFFICIENCY));
    public static final RegistryObject<Item> enhance_special = ITEMS_REGISTRY.register("enhance_special",
            () -> new CircleEnhanceMaterial(MagicCircleNBTHelper.EnhanceType.SPECIAL));

    public static final RegistryObject<Item> mana_storage = ITEMS_REGISTRY.register("mana_storage",
            ManaStorageItem::new);
    public static final RegistryObject<Item> greater_mana_storage = ITEMS_REGISTRY.register("greater_mana_storage",
            () -> new ManaStorageItem(1024));
    public static final RegistryObject<Item> ancient_mana_vessel = ITEMS_REGISTRY.register("ancient_mana_vessel",
            () -> new ManaStorageItem(4096));

    // === Spell System ===

    public static final RegistryObject<Item> wand_basic = ITEMS_REGISTRY.register("wand_basic",
            () -> new TranscendWand(3, 6));
    public static final RegistryObject<Item> wand_advanced = ITEMS_REGISTRY.register("wand_advanced",
            () -> new TranscendWand(5, 4));
    public static final RegistryObject<Item> wand_master = ITEMS_REGISTRY.register("wand_master",
            () -> new TranscendWand(7, 2));

    public static final RegistryObject<Item> spell_base_basic = ITEMS_REGISTRY.register("spell_base_basic",
            () -> new SpellBaseItem(1));
    public static final RegistryObject<Item> spell_base_advanced = ITEMS_REGISTRY.register("spell_base_advanced",
            () -> new SpellBaseItem(2));
    public static final RegistryObject<Item> spell_base_master = ITEMS_REGISTRY.register("spell_base_master",
            () -> new SpellBaseItem(3));

    public static final RegistryObject<Item> carrier_orb = ITEMS_REGISTRY.register("carrier_orb",
            () -> new SpellCarrierItem(SpellCarrier.ORB));
    public static final RegistryObject<Item> carrier_arrow = ITEMS_REGISTRY.register("carrier_arrow",
            () -> new SpellCarrierItem(SpellCarrier.ARROW));
    public static final RegistryObject<Item> carrier_slash = ITEMS_REGISTRY.register("carrier_slash",
            () -> new SpellCarrierItem(SpellCarrier.SLASH));
    public static final RegistryObject<Item> carrier_beam = ITEMS_REGISTRY.register("carrier_beam",
            () -> new SpellCarrierItem(SpellCarrier.BEAM));
    public static final RegistryObject<Item> carrier_nova = ITEMS_REGISTRY.register("carrier_nova",
            () -> new SpellCarrierItem(SpellCarrier.NOVA));
    public static final RegistryObject<Item> carrier_chain = ITEMS_REGISTRY.register("carrier_chain",
            () -> new SpellCarrierItem(SpellCarrier.CHAIN));
    public static final RegistryObject<Item> carrier_vortex = ITEMS_REGISTRY.register("carrier_vortex",
            () -> new SpellCarrierItem(SpellCarrier.VORTEX));
    public static final RegistryObject<Item> carrier_spike = ITEMS_REGISTRY.register("carrier_spike",
            () -> new SpellCarrierItem(SpellCarrier.SPIKE));
    public static final RegistryObject<Item> carrier_teleport = ITEMS_REGISTRY.register("carrier_teleport",
            () -> new SpellCarrierItem(SpellCarrier.TELEPORT));
    public static final RegistryObject<Item> carrier_trap = ITEMS_REGISTRY.register("carrier_trap",
            () -> new SpellCarrierItem(SpellCarrier.TRAP));
    public static final RegistryObject<Item> carrier_barrier = ITEMS_REGISTRY.register("carrier_barrier",
            () -> new SpellCarrierItem(SpellCarrier.BARRIER));
    public static final RegistryObject<Item> carrier_summon = ITEMS_REGISTRY.register("carrier_summon",
            () -> new SpellCarrierItem(SpellCarrier.SUMMON));
    public static final RegistryObject<Item> carrier_ring = ITEMS_REGISTRY.register("carrier_ring",
            () -> new SpellCarrierItem(SpellCarrier.RING));
    public static final RegistryObject<Item> carrier_breath = ITEMS_REGISTRY.register("carrier_breath",
            () -> new SpellCarrierItem(SpellCarrier.BREATH));
    public static final RegistryObject<Item> carrier_rain = ITEMS_REGISTRY.register("carrier_rain",
            () -> new SpellCarrierItem(SpellCarrier.RAIN));
    public static final RegistryObject<Item> carrier_dash = ITEMS_REGISTRY.register("carrier_dash",
            () -> new SpellCarrierItem(SpellCarrier.DASH));
    public static final RegistryObject<Item> carrier_ground = ITEMS_REGISTRY.register("carrier_ground",
            () -> new SpellCarrierItem(SpellCarrier.GROUND));

    public static final RegistryObject<Item> element_fire = ITEMS_REGISTRY.register("element_fire",
            () -> new SpellElementItem(SpellElement.FIRE));
    public static final RegistryObject<Item> element_ice = ITEMS_REGISTRY.register("element_ice",
            () -> new SpellElementItem(SpellElement.ICE));
    public static final RegistryObject<Item> element_thunder = ITEMS_REGISTRY.register("element_thunder",
            () -> new SpellElementItem(SpellElement.THUNDER));
    public static final RegistryObject<Item> element_wind = ITEMS_REGISTRY.register("element_wind",
            () -> new SpellElementItem(SpellElement.WIND));
    public static final RegistryObject<Item> element_earth = ITEMS_REGISTRY.register("element_earth",
            () -> new SpellElementItem(SpellElement.EARTH));
    public static final RegistryObject<Item> element_void = ITEMS_REGISTRY.register("element_void",
            () -> new SpellElementItem(SpellElement.VOID));
    public static final RegistryObject<Item> element_holy = ITEMS_REGISTRY.register("element_holy",
            () -> new SpellElementItem(SpellElement.HOLY));
    public static final RegistryObject<Item> element_blood = ITEMS_REGISTRY.register("element_blood",
            () -> new SpellElementItem(SpellElement.BLOOD));
    public static final RegistryObject<Item> element_dark = ITEMS_REGISTRY.register("element_dark",
            () -> new SpellElementItem(SpellElement.DARK));
    public static final RegistryObject<Item> element_light = ITEMS_REGISTRY.register("element_light",
            () -> new SpellElementItem(SpellElement.LIGHT));
    public static final RegistryObject<Item> element_poison = ITEMS_REGISTRY.register("element_poison",
            () -> new SpellElementItem(SpellElement.POISON));
    public static final RegistryObject<Item> element_time = ITEMS_REGISTRY.register("element_time",
            () -> new SpellElementItem(SpellElement.TIME));
    public static final RegistryObject<Item> element_space = ITEMS_REGISTRY.register("element_space",
            () -> new SpellElementItem(SpellElement.SPACE));
    public static final RegistryObject<Item> element_nature = ITEMS_REGISTRY.register("element_nature",
            () -> new SpellElementItem(SpellElement.NATURE));
    public static final RegistryObject<Item> element_chaos = ITEMS_REGISTRY.register("element_chaos",
            () -> new SpellElementItem(SpellElement.CHAOS));
    public static final RegistryObject<Item> element_acid = ITEMS_REGISTRY.register("element_acid",
            () -> new SpellElementItem(SpellElement.ACID));
    public static final RegistryObject<Item> element_sonic = ITEMS_REGISTRY.register("element_sonic",
            () -> new SpellElementItem(SpellElement.SONIC));
    public static final RegistryObject<Item> element_eldritch = ITEMS_REGISTRY.register("element_eldritch",
            () -> new SpellElementItem(SpellElement.ELDRITCH));

    public static final RegistryObject<Item> effect_explosion = ITEMS_REGISTRY.register("effect_explosion",
            () -> new SpellEffectItem(SpellEffect.EXPLOSION));
    public static final RegistryObject<Item> effect_piercing = ITEMS_REGISTRY.register("effect_piercing",
            () -> new SpellEffectItem(SpellEffect.PIERCING));
    public static final RegistryObject<Item> effect_split = ITEMS_REGISTRY.register("effect_split",
            () -> new SpellEffectItem(SpellEffect.SPLIT));
    public static final RegistryObject<Item> effect_homing = ITEMS_REGISTRY.register("effect_homing",
            () -> new SpellEffectItem(SpellEffect.HOMING));
    public static final RegistryObject<Item> effect_healing = ITEMS_REGISTRY.register("effect_healing",
            () -> new SpellEffectItem(SpellEffect.HEALING));
    public static final RegistryObject<Item> effect_shield = ITEMS_REGISTRY.register("effect_shield",
            () -> new SpellEffectItem(SpellEffect.SHIELD));
    public static final RegistryObject<Item> effect_chain_lightning = ITEMS_REGISTRY.register("effect_chain_lightning",
            () -> new SpellEffectItem(SpellEffect.CHAIN_LIGHTNING));
    public static final RegistryObject<Item> effect_bounce = ITEMS_REGISTRY.register("effect_bounce",
            () -> new SpellEffectItem(SpellEffect.BOUNCE));
    public static final RegistryObject<Item> effect_delayed = ITEMS_REGISTRY.register("effect_delayed",
            () -> new SpellEffectItem(SpellEffect.DELAYED));
    public static final RegistryObject<Item> effect_amplify = ITEMS_REGISTRY.register("effect_amplify",
            () -> new SpellEffectItem(SpellEffect.AMPLIFY));
    public static final RegistryObject<Item> effect_lifesteal = ITEMS_REGISTRY.register("effect_lifesteal",
            () -> new SpellEffectItem(SpellEffect.LIFESTEAL));
    public static final RegistryObject<Item> effect_quickcast = ITEMS_REGISTRY.register("effect_quickcast",
            () -> new SpellEffectItem(SpellEffect.QUICKCAST));
    public static final RegistryObject<Item> effect_multishot = ITEMS_REGISTRY.register("effect_multishot",
            () -> new SpellEffectItem(SpellEffect.MULTISHOT));
    public static final RegistryObject<Item> effect_slowfield = ITEMS_REGISTRY.register("effect_slowfield",
            () -> new SpellEffectItem(SpellEffect.SLOWFIELD));
    public static final RegistryObject<Item> effect_gravity_well = ITEMS_REGISTRY.register("effect_gravity_well",
            () -> new SpellEffectItem(SpellEffect.GRAVITY_WELL));
    public static final RegistryObject<Item> effect_mark = ITEMS_REGISTRY.register("effect_mark",
            () -> new SpellEffectItem(SpellEffect.MARK));
    public static final RegistryObject<Item> effect_echo = ITEMS_REGISTRY.register("effect_echo",
            () -> new SpellEffectItem(SpellEffect.ECHO));
    public static final RegistryObject<Item> effect_armor_break = ITEMS_REGISTRY.register("effect_armor_break",
            () -> new SpellEffectItem(SpellEffect.ARMOR_BREAK));
    public static final RegistryObject<Item> effect_root = ITEMS_REGISTRY.register("effect_root",
            () -> new SpellEffectItem(SpellEffect.ROOT));
    public static final RegistryObject<Item> effect_blight = ITEMS_REGISTRY.register("effect_blight",
            () -> new SpellEffectItem(SpellEffect.BLIGHT));
    public static final RegistryObject<Item> effect_lingering = ITEMS_REGISTRY.register("effect_lingering",
            () -> new SpellEffectItem(SpellEffect.LINGERING));
    public static final RegistryObject<Item> effect_devour = ITEMS_REGISTRY.register("effect_devour",
            () -> new SpellEffectItem(SpellEffect.DEVOUR));
    public static final RegistryObject<Item> effect_absorb = ITEMS_REGISTRY.register("effect_absorb",
            () -> new SpellEffectItem(SpellEffect.ABSORB));
    public static final RegistryObject<Item> effect_reflect = ITEMS_REGISTRY.register("effect_reflect",
            () -> new SpellEffectItem(SpellEffect.REFLECT));
    public static final RegistryObject<Item> effect_curse = ITEMS_REGISTRY.register("effect_curse",
            () -> new SpellEffectItem(SpellEffect.CURSE));
    public static final RegistryObject<Item> effect_overload = ITEMS_REGISTRY.register("effect_overload",
            () -> new SpellEffectItem(SpellEffect.OVERLOAD));
    public static final RegistryObject<Item> effect_weaken = ITEMS_REGISTRY.register("effect_weaken",
            () -> new SpellEffectItem(SpellEffect.WEAKEN));
    public static final RegistryObject<Item> effect_unstable = ITEMS_REGISTRY.register("effect_unstable",
            () -> new SpellEffectItem(SpellEffect.UNSTABLE));
    public static final RegistryObject<Item> effect_shatter = ITEMS_REGISTRY.register("effect_shatter",
            () -> new SpellEffectItem(SpellEffect.SHATTER));
    public static final RegistryObject<Item> effect_summon_wisp = ITEMS_REGISTRY.register("effect_summon_wisp",
            () -> new SpellEffectItem(SpellEffect.SUMMON_WISP));
    public static final RegistryObject<Item> effect_summon_guardian = ITEMS_REGISTRY.register("effect_summon_guardian",
            () -> new SpellEffectItem(SpellEffect.SUMMON_GUARDIAN));

    public static final RegistryObject<Item> spell_scroll = ITEMS_REGISTRY.register("spell_scroll",
            SpellScrollItem::new);
    // Round 37: 古卷合成基底
    public static final RegistryObject<Item> sealed_scroll = ITEMS_REGISTRY.register("sealed_scroll",
            com.huige233.transcend.items.SealedScrollItem::new);

    // Round 42: 魔力传输水晶（block）+ 绑定器（tool）
    public static final RegistryObject<Item> mana_transmit_crystal = ITEMS_REGISTRY.register("mana_transmit_crystal",
            () -> new net.minecraft.world.item.BlockItem(
                    com.huige233.transcend.init.ModBlocks.MANA_TRANSMIT_CRYSTAL.get(),
                    new net.minecraft.world.item.Item.Properties()));
    public static final RegistryObject<Item> mana_crystal_binder = ITEMS_REGISTRY.register("mana_crystal_binder",
            com.huige233.transcend.items.ManaCrystalBinderItem::new);

    // Round 51: 增幅符文（柱冠位）— 加速 / 节能 / 减损
    public static final RegistryObject<Item> augment_rune_haste = ITEMS_REGISTRY.register("augment_rune_haste",
            () -> new net.minecraft.world.item.BlockItem(
                    com.huige233.transcend.init.ModBlocks.AUGMENT_RUNE_HASTE.get(),
                    new net.minecraft.world.item.Item.Properties()));
    public static final RegistryObject<Item> augment_rune_efficiency = ITEMS_REGISTRY.register("augment_rune_efficiency",
            () -> new net.minecraft.world.item.BlockItem(
                    com.huige233.transcend.init.ModBlocks.AUGMENT_RUNE_EFFICIENCY.get(),
                    new net.minecraft.world.item.Item.Properties()));
    public static final RegistryObject<Item> augment_rune_preservation = ITEMS_REGISTRY.register("augment_rune_preservation",
            () -> new net.minecraft.world.item.BlockItem(
                    com.huige233.transcend.init.ModBlocks.AUGMENT_RUNE_PRESERVATION.get(),
                    new net.minecraft.world.item.Item.Properties()));

    // Round 52: mana_sensor (handheld) + mana_dew (block)
    public static final RegistryObject<Item> mana_sensor = ITEMS_REGISTRY.register("mana_sensor",
            com.huige233.transcend.items.ManaSensorItem::new);
    public static final RegistryObject<Item> mana_dew = ITEMS_REGISTRY.register("mana_dew",
            () -> new net.minecraft.world.item.BlockItem(
                    com.huige233.transcend.init.ModBlocks.MANA_DEW.get(),
                    new net.minecraft.world.item.Item.Properties()));

    // === Round 43: Spell Augment Glyphs (8) — 新生魔艺风 augment ===
    public static final RegistryObject<Item> glyph_amplify = ITEMS_REGISTRY.register("glyph_amplify",
            () -> new com.huige233.transcend.items.SpellGlyphItem(com.huige233.transcend.spell.SpellAugment.AMPLIFY));
    public static final RegistryObject<Item> glyph_dampen = ITEMS_REGISTRY.register("glyph_dampen",
            () -> new com.huige233.transcend.items.SpellGlyphItem(com.huige233.transcend.spell.SpellAugment.DAMPEN));
    public static final RegistryObject<Item> glyph_quickfire = ITEMS_REGISTRY.register("glyph_quickfire",
            () -> new com.huige233.transcend.items.SpellGlyphItem(com.huige233.transcend.spell.SpellAugment.QUICKFIRE));
    public static final RegistryObject<Item> glyph_split = ITEMS_REGISTRY.register("glyph_split",
            () -> new com.huige233.transcend.items.SpellGlyphItem(com.huige233.transcend.spell.SpellAugment.SPLIT));
    public static final RegistryObject<Item> glyph_pierce = ITEMS_REGISTRY.register("glyph_pierce",
            () -> new com.huige233.transcend.items.SpellGlyphItem(com.huige233.transcend.spell.SpellAugment.PIERCE));
    public static final RegistryObject<Item> glyph_chain = ITEMS_REGISTRY.register("glyph_chain",
            () -> new com.huige233.transcend.items.SpellGlyphItem(com.huige233.transcend.spell.SpellAugment.CHAIN));
    public static final RegistryObject<Item> glyph_extend = ITEMS_REGISTRY.register("glyph_extend",
            () -> new com.huige233.transcend.items.SpellGlyphItem(com.huige233.transcend.spell.SpellAugment.EXTEND));
    public static final RegistryObject<Item> glyph_homing = ITEMS_REGISTRY.register("glyph_homing",
            () -> new com.huige233.transcend.items.SpellGlyphItem(com.huige233.transcend.spell.SpellAugment.HOMING));

    // === Round 45: Blood Magic 风格法环辅助 ===
    public static final RegistryObject<Item> sacrificial_knife = ITEMS_REGISTRY.register("sacrificial_knife",
            com.huige233.transcend.items.SacrificialKnifeItem::new);

    // === Round 50: Mana Blossom block item ===
    public static final RegistryObject<Item> mana_blossom = ITEMS_REGISTRY.register("mana_blossom",
            () -> new net.minecraft.world.item.BlockItem(
                    com.huige233.transcend.init.ModBlocks.MANA_BLOSSOM.get(),
                    new net.minecraft.world.item.Item.Properties()));

    public static final RegistryObject<Item> spell_upgrade_stone = ITEMS_REGISTRY.register("spell_upgrade_stone",
            SpellUpgradeStone::new);

    public static final RegistryObject<Item> rune_mana_siphon = ITEMS_REGISTRY.register("rune_mana_siphon", () -> new RuneItem(WandRune.MANA_SIPHON));
    public static final RegistryObject<Item> rune_rapid_fire = ITEMS_REGISTRY.register("rune_rapid_fire", () -> new RuneItem(WandRune.RAPID_FIRE));
    public static final RegistryObject<Item> rune_overcharge = ITEMS_REGISTRY.register("rune_overcharge", () -> new RuneItem(WandRune.OVERCHARGE));
    public static final RegistryObject<Item> rune_spell_echo = ITEMS_REGISTRY.register("rune_spell_echo", () -> new RuneItem(WandRune.SPELL_ECHO));
    public static final RegistryObject<Item> rune_elemental_mastery = ITEMS_REGISTRY.register("rune_elemental_mastery", () -> new RuneItem(WandRune.ELEMENTAL_MASTERY));
    public static final RegistryObject<Item> rune_glass_cannon = ITEMS_REGISTRY.register("rune_glass_cannon", () -> new RuneItem(WandRune.GLASS_CANNON));
    public static final RegistryObject<Item> rune_conservation = ITEMS_REGISTRY.register("rune_conservation", () -> new RuneItem(WandRune.CONSERVATION));
    public static final RegistryObject<Item> rune_chain_caster = ITEMS_REGISTRY.register("rune_chain_caster", () -> new RuneItem(WandRune.CHAIN_CASTER));

    // === Boss Summon Items === (Round 23: stage + circle gated)
    public static final RegistryObject<Item> ancient_glyph = ITEMS_REGISTRY.register("ancient_glyph",
            () -> new com.huige233.transcend.items.BossSummonItem(
                    () -> ModEntities.ELEMENTAL_WARDEN.get(), "tooltip.transcend.ancient_glyph.desc",
                    2, 3, 12));
    public static final RegistryObject<Item> rift_fragment = ITEMS_REGISTRY.register("rift_fragment",
            () -> new com.huige233.transcend.items.BossSummonItem(
                    () -> ModEntities.VOID_WEAVER.get(), "tooltip.transcend.rift_fragment.desc",
                    3, 4, 14));
    public static final RegistryObject<Item> transcendence_core = ITEMS_REGISTRY.register("transcendence_core",
            () -> new com.huige233.transcend.items.BossSummonItem(
                    () -> ModEntities.TRANSCENDENCE_AVATAR.get(), "tooltip.transcend.transcendence_core.desc",
                    4, 5, 16));

    // === Round 19: Apex Great Spells — 终焉级仪式法术 (Stage 4 + T5 circle gated) ===
    public static final RegistryObject<Item> apex_solar_collapse = ITEMS_REGISTRY.register("apex_solar_collapse",
            () -> new com.huige233.transcend.items.ApexGreatSpellItem(
                    com.huige233.transcend.items.ApexGreatSpellItem.ApexType.SOLAR_COLLAPSE));
    public static final RegistryObject<Item> apex_blood_pact = ITEMS_REGISTRY.register("apex_blood_pact",
            () -> new com.huige233.transcend.items.ApexGreatSpellItem(
                    com.huige233.transcend.items.ApexGreatSpellItem.ApexType.BLOOD_PACT));
    public static final RegistryObject<Item> apex_cosmic_anchor = ITEMS_REGISTRY.register("apex_cosmic_anchor",
            () -> new com.huige233.transcend.items.ApexGreatSpellItem(
                    com.huige233.transcend.items.ApexGreatSpellItem.ApexType.COSMIC_ANCHOR));
    public static final RegistryObject<Item> apex_void_unmaking = ITEMS_REGISTRY.register("apex_void_unmaking",
            () -> new com.huige233.transcend.items.ApexGreatSpellItem(
                    com.huige233.transcend.items.ApexGreatSpellItem.ApexType.VOID_UNMAKING));

    // === Round 20: Familiar Pact Items (γ 路线 - 召唤式持续魔法助手) ===
    public static final RegistryObject<Item> pact_aether_wisp = ITEMS_REGISTRY.register("pact_aether_wisp",
            () -> new com.huige233.transcend.items.FamiliarPactItem(
                    com.huige233.transcend.entity.familiar.TranscendFamiliar.FamiliarType.AETHER_WISP));
    public static final RegistryObject<Item> pact_blood_hound = ITEMS_REGISTRY.register("pact_blood_hound",
            () -> new com.huige233.transcend.items.FamiliarPactItem(
                    com.huige233.transcend.entity.familiar.TranscendFamiliar.FamiliarType.BLOOD_HOUND));
    public static final RegistryObject<Item> pact_cosmic_owl = ITEMS_REGISTRY.register("pact_cosmic_owl",
            () -> new com.huige233.transcend.items.FamiliarPactItem(
                    com.huige233.transcend.entity.familiar.TranscendFamiliar.FamiliarType.COSMIC_OWL));
    public static final RegistryObject<Item> pact_tainted_imp = ITEMS_REGISTRY.register("pact_tainted_imp",
            () -> new com.huige233.transcend.items.FamiliarPactItem(
                    com.huige233.transcend.entity.familiar.TranscendFamiliar.FamiliarType.TAINTED_IMP));

    // === Round 24: Aether Travel Stone (β 终局 - 主世界 ↔ Aether Realm 维度) ===
    public static final RegistryObject<Item> aether_travel_stone = ITEMS_REGISTRY.register("aether_travel_stone",
            com.huige233.transcend.items.AetherTravelStoneItem::new);

    // === Round 25: Aether Realm 独占资源 ===
    public static final RegistryObject<Item> aether_essence_ore = ITEMS_REGISTRY.register("aether_essence_ore",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.AETHER_ESSENCE_ORE.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> aether_essence = ITEMS_REGISTRY.register("aether_essence",
            () -> new ItemBase("aether_essence", new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> aether_ingot = ITEMS_REGISTRY.register("aether_ingot",
            () -> new ItemBase("aether_ingot", new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));

    // === Round 26: Boss Progression Currency / Ascension Essence ===
    public static final RegistryObject<Item> warden_essence = ITEMS_REGISTRY.register("warden_essence",
            () -> new ItemBase("warden_essence", new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant()));
    public static final RegistryObject<Item> weaver_essence = ITEMS_REGISTRY.register("weaver_essence",
            () -> new ItemBase("weaver_essence", new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> avatar_essence = ITEMS_REGISTRY.register("avatar_essence",
            () -> new ItemBase("avatar_essence", new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> transcendence_proof = ITEMS_REGISTRY.register("transcendence_proof",
            () -> new ItemBase("transcendence_proof", new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));

    // === Round 27: Spellbook Loadout (α 路线 - IS&S 配装 RPG 化) ===
    public static final RegistryObject<Item> spellbook_apprentice = ITEMS_REGISTRY.register("spellbook_apprentice",
            () -> new com.huige233.transcend.items.SpellBookItem(com.huige233.transcend.items.SpellBookItem.BookTier.APPRENTICE));
    public static final RegistryObject<Item> spellbook_adept = ITEMS_REGISTRY.register("spellbook_adept",
            () -> new com.huige233.transcend.items.SpellBookItem(com.huige233.transcend.items.SpellBookItem.BookTier.ADEPT));
    public static final RegistryObject<Item> spellbook_master = ITEMS_REGISTRY.register("spellbook_master",
            () -> new com.huige233.transcend.items.SpellBookItem(com.huige233.transcend.items.SpellBookItem.BookTier.MASTER));
    public static final RegistryObject<Item> spellbook_archon = ITEMS_REGISTRY.register("spellbook_archon",
            () -> new com.huige233.transcend.items.SpellBookItem(com.huige233.transcend.items.SpellBookItem.BookTier.ARCHON));
    public static final RegistryObject<Item> spellbook_transcendent = ITEMS_REGISTRY.register("spellbook_transcendent",
            () -> new com.huige233.transcend.items.SpellBookItem(com.huige233.transcend.items.SpellBookItem.BookTier.TRANSCENDENT));

    // === Round 28: Aspect Ring Curio - 4 aspect 被动饰品 ===
    public static final RegistryObject<Item> ring_aether = ITEMS_REGISTRY.register("ring_aether",
            () -> new com.huige233.transcend.items.curio.AspectRingItem(
                    com.huige233.transcend.items.curio.AspectRingItem.AspectRingType.AETHER));
    public static final RegistryObject<Item> ring_blood = ITEMS_REGISTRY.register("ring_blood",
            () -> new com.huige233.transcend.items.curio.AspectRingItem(
                    com.huige233.transcend.items.curio.AspectRingItem.AspectRingType.BLOOD));
    public static final RegistryObject<Item> ring_cosmic = ITEMS_REGISTRY.register("ring_cosmic",
            () -> new com.huige233.transcend.items.curio.AspectRingItem(
                    com.huige233.transcend.items.curio.AspectRingItem.AspectRingType.COSMIC));
    public static final RegistryObject<Item> ring_tainted = ITEMS_REGISTRY.register("ring_tainted",
            () -> new com.huige233.transcend.items.curio.AspectRingItem(
                    com.huige233.transcend.items.curio.AspectRingItem.AspectRingType.TAINTED));

    // === Round 29: Aspect Sword - 4 aspect 主题武器 ===
    public static final RegistryObject<Item> sword_aether = ITEMS_REGISTRY.register("sword_aether",
            () -> new com.huige233.transcend.items.tools.AspectSwordItem(
                    com.huige233.transcend.items.tools.AspectSwordItem.AspectSwordType.AETHER));
    public static final RegistryObject<Item> sword_blood = ITEMS_REGISTRY.register("sword_blood",
            () -> new com.huige233.transcend.items.tools.AspectSwordItem(
                    com.huige233.transcend.items.tools.AspectSwordItem.AspectSwordType.BLOOD));
    public static final RegistryObject<Item> sword_cosmic = ITEMS_REGISTRY.register("sword_cosmic",
            () -> new com.huige233.transcend.items.tools.AspectSwordItem(
                    com.huige233.transcend.items.tools.AspectSwordItem.AspectSwordType.COSMIC));
    public static final RegistryObject<Item> sword_tainted = ITEMS_REGISTRY.register("sword_tainted",
            () -> new com.huige233.transcend.items.tools.AspectSwordItem(
                    com.huige233.transcend.items.tools.AspectSwordItem.AspectSwordType.TAINTED));

    // === Round 30: Ancient Manuscript - lore reveal system ===
    public static final RegistryObject<Item> manuscript_world_origin = ITEMS_REGISTRY.register("manuscript_world_origin",
            () -> new com.huige233.transcend.items.AncientManuscriptItem(
                    com.huige233.transcend.items.AncientManuscriptItem.ManuscriptType.WORLD_ORIGIN));
    public static final RegistryObject<Item> manuscript_mana_theory = ITEMS_REGISTRY.register("manuscript_mana_theory",
            () -> new com.huige233.transcend.items.AncientManuscriptItem(
                    com.huige233.transcend.items.AncientManuscriptItem.ManuscriptType.MANA_THEORY));
    public static final RegistryObject<Item> manuscript_aspect_lore = ITEMS_REGISTRY.register("manuscript_aspect_lore",
            () -> new com.huige233.transcend.items.AncientManuscriptItem(
                    com.huige233.transcend.items.AncientManuscriptItem.ManuscriptType.ASPECT_LORE));
    public static final RegistryObject<Item> manuscript_boss_lore = ITEMS_REGISTRY.register("manuscript_boss_lore",
            () -> new com.huige233.transcend.items.AncientManuscriptItem(
                    com.huige233.transcend.items.AncientManuscriptItem.ManuscriptType.BOSS_LORE));
    public static final RegistryObject<Item> manuscript_ascension_lore = ITEMS_REGISTRY.register("manuscript_ascension_lore",
            () -> new com.huige233.transcend.items.AncientManuscriptItem(
                    com.huige233.transcend.items.AncientManuscriptItem.ManuscriptType.ASCENSION_LORE));
    public static final RegistryObject<Item> manuscript_aether_lore = ITEMS_REGISTRY.register("manuscript_aether_lore",
            () -> new com.huige233.transcend.items.AncientManuscriptItem(
                    com.huige233.transcend.items.AncientManuscriptItem.ManuscriptType.AETHER_LORE));

    // === 飞升之书 ===
    public static final RegistryObject<Item> ascension_book = ITEMS_REGISTRY.register("ascension_book",
            AscensionBookItem::new);

    // === 洗点水 ===
    public static final RegistryObject<Item> respec_potion = ITEMS_REGISTRY.register("respec_potion",
            com.huige233.transcend.items.RespecPotionItem::new);

    // === 仪式方块 ===
    public static final RegistryObject<Item> ritual_altar = ITEMS_REGISTRY.register("ritual_altar",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.RITUAL_ALTAR.get(),
                    new Item.Properties()));
    public static final RegistryObject<Item> ritual_pedestal = ITEMS_REGISTRY.register("ritual_pedestal",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.RITUAL_PEDESTAL.get(),
                    new Item.Properties()));

    // === 魔力井 ===
    public static final RegistryObject<Item> mana_well = ITEMS_REGISTRY.register("mana_well",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MANA_WELL.get(),
                    new Item.Properties()));

    // === 水晶方块 ===
    public static final RegistryObject<Item> ancient_crystal = ITEMS_REGISTRY.register("ancient_crystal",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.ANCIENT_CRYSTAL.get(),
                    new Item.Properties()));
    public static final RegistryObject<Item> magic_crystal_block = ITEMS_REGISTRY.register("magic_crystal_block",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MAGIC_CRYSTAL_BLOCK.get(),
                    new Item.Properties()));
    public static final RegistryObject<Item> concentrated_crystal_block = ITEMS_REGISTRY.register("concentrated_crystal_block",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.CONCENTRATED_CRYSTAL_BLOCK.get(),
                    new Item.Properties()));

    public static final RegistryObject<Item> spell_workbench_item = ITEMS_REGISTRY.register("spell_workbench",
            () -> {
                net.minecraft.world.item.BlockItem item = new net.minecraft.world.item.BlockItem(
                        ModBlocks.SPELL_WORKBENCH.get(),
                        new Item.Properties());
                ITEMS.add(item);
                return item;
            });

    // === Circle Structure BlockItems ===
    // Foundation stones
    public static final RegistryObject<Item> ancient_circle_stone = ITEMS_REGISTRY.register("ancient_circle_stone",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.ANCIENT_CIRCLE_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> awakened_circle_stone = ITEMS_REGISTRY.register("awakened_circle_stone",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.AWAKENED_CIRCLE_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> astral_circle_stone = ITEMS_REGISTRY.register("astral_circle_stone",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.ASTRAL_CIRCLE_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> nexus_circle_stone = ITEMS_REGISTRY.register("nexus_circle_stone",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.NEXUS_CIRCLE_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> primordial_circle_stone = ITEMS_REGISTRY.register("primordial_circle_stone",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.PRIMORDIAL_CIRCLE_STONE.get(), new Item.Properties()));

    // Rune stones
    public static final RegistryObject<Item> lesser_rune_stone = ITEMS_REGISTRY.register("lesser_rune_stone",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.LESSER_RUNE_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> awakened_rune_stone = ITEMS_REGISTRY.register("awakened_rune_stone",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.AWAKENED_RUNE_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> greater_rune_stone = ITEMS_REGISTRY.register("greater_rune_stone",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.GREATER_RUNE_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> archon_rune_stone = ITEMS_REGISTRY.register("archon_rune_stone",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.ARCHON_RUNE_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> primordial_rune_stone = ITEMS_REGISTRY.register("primordial_rune_stone",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.PRIMORDIAL_RUNE_STONE.get(), new Item.Properties()));

    // Circle cores
    public static final RegistryObject<Item> circle_core_dormant = ITEMS_REGISTRY.register("circle_core_dormant",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.CIRCLE_CORE_DORMANT.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> circle_core_wellspring = ITEMS_REGISTRY.register("circle_core_wellspring",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.CIRCLE_CORE_WELLSPRING.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> circle_core_sanctuary = ITEMS_REGISTRY.register("circle_core_sanctuary",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.CIRCLE_CORE_SANCTUARY.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> circle_core_dominion = ITEMS_REGISTRY.register("circle_core_dominion",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.CIRCLE_CORE_DOMINION.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> circle_core_waystone = ITEMS_REGISTRY.register("circle_core_waystone",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.CIRCLE_CORE_WAYSTONE.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> circle_core_convergence = ITEMS_REGISTRY.register("circle_core_convergence",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.CIRCLE_CORE_CONVERGENCE.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> circle_core_primordial = ITEMS_REGISTRY.register("circle_core_primordial",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.CIRCLE_CORE_PRIMORDIAL.get(), new Item.Properties().stacksTo(1)));

    // Catalyst plinths
    public static final RegistryObject<Item> catalyst_plinth = ITEMS_REGISTRY.register("catalyst_plinth",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.CATALYST_PLINTH.get(), new Item.Properties()));
    public static final RegistryObject<Item> sealed_catalyst_plinth = ITEMS_REGISTRY.register("sealed_catalyst_plinth",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SEALED_CATALYST_PLINTH.get(), new Item.Properties()));

    // Conduit blocks
    public static final RegistryObject<Item> leyline_conduit_stone = ITEMS_REGISTRY.register("leyline_conduit_stone",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.LEYLINE_CONDUIT_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> aether_channel_marker = ITEMS_REGISTRY.register("aether_channel_marker",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.AETHER_CHANNEL_MARKER.get(), new Item.Properties()));
    public static final RegistryObject<Item> nexus_conduit_gate = ITEMS_REGISTRY.register("nexus_conduit_gate",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.NEXUS_CONDUIT_GATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> primordial_conduit_gate = ITEMS_REGISTRY.register("primordial_conduit_gate",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.PRIMORDIAL_CONDUIT_GATE.get(), new Item.Properties()));

    // Pillars and caps
    public static final RegistryObject<Item> runic_pillar = ITEMS_REGISTRY.register("runic_pillar",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.RUNIC_PILLAR.get(), new Item.Properties()));
    public static final RegistryObject<Item> nexus_obelisk = ITEMS_REGISTRY.register("nexus_obelisk",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.NEXUS_OBELISK.get(), new Item.Properties()));
    public static final RegistryObject<Item> primordial_pylon = ITEMS_REGISTRY.register("primordial_pylon",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.PRIMORDIAL_PYLON.get(), new Item.Properties()));
    public static final RegistryObject<Item> astral_capstone = ITEMS_REGISTRY.register("astral_capstone",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.ASTRAL_CAPSTONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> mana_lantern_cap = ITEMS_REGISTRY.register("mana_lantern_cap",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MANA_LANTERN_CAP.get(), new Item.Properties()));

    // === Circle Tools ===
    public static final RegistryObject<Item> attunement_chisel = ITEMS_REGISTRY.register("attunement_chisel",
            AttunementChiselItem::new);
    public static final RegistryObject<Item> mana_lens = ITEMS_REGISTRY.register("mana_lens",
            ManaLensItem::new);
    public static final RegistryObject<Item> bound_aether_pearl = ITEMS_REGISTRY.register("bound_aether_pearl",
            BoundAetherPearlItem::new);
    public static final RegistryObject<Item> circle_architect_wand = ITEMS_REGISTRY.register("circle_architect_wand",
            CircleArchitectWandItem::new);
    public static final RegistryObject<Item> inscription_quill = ITEMS_REGISTRY.register("inscription_quill",
            InscriptionQuillItem::new);
    public static final RegistryObject<Item> structure_blueprint_scroll = ITEMS_REGISTRY.register("structure_blueprint_scroll",
            StructureBlueprintScrollItem::new);
    public static final RegistryObject<Item> function_imprint_scroll = ITEMS_REGISTRY.register("function_imprint_scroll",
            FunctionImprintScrollItem::new);

    // === Function Sigils ===
    public static final RegistryObject<Item> sigil_leyline_siphon = ITEMS_REGISTRY.register("sigil_leyline_siphon",
            () -> new FunctionSigilItem(CircleFunctionType.LEYLINE_SIPHON));
    public static final RegistryObject<Item> sigil_remote_mana_link = ITEMS_REGISTRY.register("sigil_remote_mana_link",
            () -> new FunctionSigilItem(CircleFunctionType.REMOTE_MANA_LINK));
    public static final RegistryObject<Item> sigil_arcane_amplifier = ITEMS_REGISTRY.register("sigil_arcane_amplifier",
            () -> new FunctionSigilItem(CircleFunctionType.ARCANE_AMPLIFIER));
    public static final RegistryObject<Item> sigil_wellspring_renewal = ITEMS_REGISTRY.register("sigil_wellspring_renewal",
            () -> new FunctionSigilItem(CircleFunctionType.WELLSPRING_RENEWAL));
    public static final RegistryObject<Item> sigil_leyline_convergence = ITEMS_REGISTRY.register("sigil_leyline_convergence",
            () -> new FunctionSigilItem(CircleFunctionType.LEYLINE_CONVERGENCE));
    public static final RegistryObject<Item> sigil_warding_aegis = ITEMS_REGISTRY.register("sigil_warding_aegis",
            () -> new FunctionSigilItem(CircleFunctionType.WARDING_AEGIS));
    public static final RegistryObject<Item> sigil_wayfarers_haste = ITEMS_REGISTRY.register("sigil_wayfarers_haste",
            () -> new FunctionSigilItem(CircleFunctionType.WAYFARERS_HASTE));
    public static final RegistryObject<Item> sigil_deep_sight_veil = ITEMS_REGISTRY.register("sigil_deep_sight_veil",
            () -> new FunctionSigilItem(CircleFunctionType.DEEP_SIGHT_VEIL));
    public static final RegistryObject<Item> sigil_verdant_restoration = ITEMS_REGISTRY.register("sigil_verdant_restoration",
            () -> new FunctionSigilItem(CircleFunctionType.VERDANT_RESTORATION));
    public static final RegistryObject<Item> sigil_sky_mantle = ITEMS_REGISTRY.register("sigil_sky_mantle",
            () -> new FunctionSigilItem(CircleFunctionType.SKY_MANTLE));
    public static final RegistryObject<Item> sigil_weather_edict = ITEMS_REGISTRY.register("sigil_weather_edict",
            () -> new FunctionSigilItem(CircleFunctionType.WEATHER_EDICT));
    public static final RegistryObject<Item> sigil_chrono_loom = ITEMS_REGISTRY.register("sigil_chrono_loom",
            () -> new FunctionSigilItem(CircleFunctionType.CHRONO_LOOM));
    public static final RegistryObject<Item> sigil_quiet_boundary = ITEMS_REGISTRY.register("sigil_quiet_boundary",
            () -> new FunctionSigilItem(CircleFunctionType.QUIET_BOUNDARY));
    public static final RegistryObject<Item> sigil_everlight_mandala = ITEMS_REGISTRY.register("sigil_everlight_mandala",
            () -> new FunctionSigilItem(CircleFunctionType.EVERLIGHT_MANDALA));
    public static final RegistryObject<Item> sigil_twin_horizon_gate = ITEMS_REGISTRY.register("sigil_twin_horizon_gate",
            () -> new FunctionSigilItem(CircleFunctionType.TWIN_HORIZON_GATE));
    public static final RegistryObject<Item> sigil_hearth_stability = ITEMS_REGISTRY.register("sigil_hearth_stability",
            () -> new FunctionSigilItem(CircleFunctionType.HEARTH_STABILITY));
    public static final RegistryObject<Item> sigil_dimensional_anchor = ITEMS_REGISTRY.register("sigil_dimensional_anchor",
            () -> new FunctionSigilItem(CircleFunctionType.DIMENSIONAL_ANCHOR));
    public static final RegistryObject<Item> sigil_elemental_crucible = ITEMS_REGISTRY.register("sigil_elemental_crucible",
            () -> new FunctionSigilItem(CircleFunctionType.ELEMENTAL_CRUCIBLE));
    public static final RegistryObject<Item> sigil_spell_resonance_nexus = ITEMS_REGISTRY.register("sigil_spell_resonance_nexus",
            () -> new FunctionSigilItem(CircleFunctionType.SPELL_RESONANCE_NEXUS));
    public static final RegistryObject<Item> sigil_nexus_gatehouse = ITEMS_REGISTRY.register("sigil_nexus_gatehouse",
            () -> new FunctionSigilItem(CircleFunctionType.NEXUS_GATEHOUSE));
    public static final RegistryObject<Item> sigil_primordial_synchrony = ITEMS_REGISTRY.register("sigil_primordial_synchrony",
            () -> new FunctionSigilItem(CircleFunctionType.PRIMORDIAL_SYNCHRONY));
    public static final RegistryObject<Item> sigil_verdant_reaping = ITEMS_REGISTRY.register("sigil_verdant_reaping",
            () -> new FunctionSigilItem(CircleFunctionType.VERDANT_REAPING));
    public static final RegistryObject<Item> sigil_mineral_convergence = ITEMS_REGISTRY.register("sigil_mineral_convergence",
            () -> new FunctionSigilItem(CircleFunctionType.MINERAL_CONVERGENCE));
    public static final RegistryObject<Item> sigil_brood_hearth = ITEMS_REGISTRY.register("sigil_brood_hearth",
            () -> new FunctionSigilItem(CircleFunctionType.BROOD_HEARTH));
    public static final RegistryObject<Item> sigil_aegis_lattice = ITEMS_REGISTRY.register("sigil_aegis_lattice",
            () -> new FunctionSigilItem(CircleFunctionType.AEGIS_LATTICE));
    public static final RegistryObject<Item> sigil_sentinel_alarm = ITEMS_REGISTRY.register("sigil_sentinel_alarm",
            () -> new FunctionSigilItem(CircleFunctionType.SENTINEL_ALARM));
    public static final RegistryObject<Item> sigil_trapweaver_relay = ITEMS_REGISTRY.register("sigil_trapweaver_relay",
            () -> new FunctionSigilItem(CircleFunctionType.TRAPWEAVER_RELAY));
    public static final RegistryObject<Item> sigil_covenant_reservoir = ITEMS_REGISTRY.register("sigil_covenant_reservoir",
            () -> new FunctionSigilItem(CircleFunctionType.COVENANT_RESERVOIR));
    public static final RegistryObject<Item> sigil_concordant_banner = ITEMS_REGISTRY.register("sigil_concordant_banner",
            () -> new FunctionSigilItem(CircleFunctionType.CONCORDANT_BANNER));
    public static final RegistryObject<Item> sigil_cartographers_eye = ITEMS_REGISTRY.register("sigil_cartographers_eye",
            () -> new FunctionSigilItem(CircleFunctionType.CARTOGRAPHERS_EYE));
    public static final RegistryObject<Item> sigil_biome_resonance = ITEMS_REGISTRY.register("sigil_biome_resonance",
            () -> new FunctionSigilItem(CircleFunctionType.BIOME_RESONANCE));
    public static final RegistryObject<Item> sigil_arcanist_forge_field = ITEMS_REGISTRY.register("sigil_arcanist_forge_field",
            () -> new FunctionSigilItem(CircleFunctionType.ARCANIST_FORGE_FIELD));
    public static final RegistryObject<Item> sigil_restoration_halo = ITEMS_REGISTRY.register("sigil_restoration_halo",
            () -> new FunctionSigilItem(CircleFunctionType.RESTORATION_HALO));
    public static final RegistryObject<Item> sigil_prismatic_attunement = ITEMS_REGISTRY.register("sigil_prismatic_attunement",
            () -> new FunctionSigilItem(CircleFunctionType.PRISMATIC_ATTUNEMENT));
    public static final RegistryObject<Item> sigil_aurora_theatre = ITEMS_REGISTRY.register("sigil_aurora_theatre",
            () -> new FunctionSigilItem(CircleFunctionType.AURORA_THEATRE));
    public static final RegistryObject<Item> sigil_void_bore = ITEMS_REGISTRY.register("sigil_void_bore",
            () -> new FunctionSigilItem(CircleFunctionType.VOID_BORE));

    // === Circle Blueprints ===
    public static final RegistryObject<Item> circle_blueprint_fragment = ITEMS_REGISTRY.register("circle_blueprint_fragment",
            () -> new CircleBlueprintItem(CircleBlueprintItem.BlueprintType.FRAGMENT));
    public static final RegistryObject<Item> circle_blueprint_page = ITEMS_REGISTRY.register("circle_blueprint_page",
            () -> new CircleBlueprintItem(CircleBlueprintItem.BlueprintType.PAGE));
    public static final RegistryObject<Item> complete_circle_schematic = ITEMS_REGISTRY.register("complete_circle_schematic",
            () -> new CircleBlueprintItem(CircleBlueprintItem.BlueprintType.SCHEMATIC));

    // === Ancient Spell Scrolls ===
    public static final RegistryObject<Item> scroll_solar_judgement = ITEMS_REGISTRY.register("scroll_solar_judgement",
            () -> new AncientSpellScrollItem("solar_judgement"));
    public static final RegistryObject<Item> scroll_leyline_eruption = ITEMS_REGISTRY.register("scroll_leyline_eruption",
            () -> new AncientSpellScrollItem("leyline_eruption"));
    public static final RegistryObject<Item> scroll_chronal_stillness = ITEMS_REGISTRY.register("scroll_chronal_stillness",
            () -> new AncientSpellScrollItem("chronal_stillness"));
    public static final RegistryObject<Item> scroll_sovereign_aegis = ITEMS_REGISTRY.register("scroll_sovereign_aegis",
            () -> new AncientSpellScrollItem("sovereign_aegis"));
    public static final RegistryObject<Item> scroll_thousand_league_return = ITEMS_REGISTRY.register("scroll_thousand_league_return",
            () -> new AncientSpellScrollItem("thousand_league_return"));
    public static final RegistryObject<Item> scroll_void_exile = ITEMS_REGISTRY.register("scroll_void_exile",
            () -> new AncientSpellScrollItem("void_exile_mandate"));
    public static final RegistryObject<Item> scroll_storm_king = ITEMS_REGISTRY.register("scroll_storm_king",
            () -> new AncientSpellScrollItem("storm_king_writ"));
    public static final RegistryObject<Item> scroll_worldmender = ITEMS_REGISTRY.register("scroll_worldmender",
            () -> new AncientSpellScrollItem("worldmender_edict"));
    public static final RegistryObject<Item> scroll_eclipse_veil = ITEMS_REGISTRY.register("scroll_eclipse_veil",
            () -> new AncientSpellScrollItem("eclipse_veil"));
    public static final RegistryObject<Item> scroll_avatar_fall = ITEMS_REGISTRY.register("scroll_avatar_fall",
            () -> new AncientSpellScrollItem("avatar_fall"));

    // === Ancient Spell Scrolls (Expansion Pack) ===
    public static final RegistryObject<Item> scroll_unbroken_arsenal = ITEMS_REGISTRY.register("scroll_unbroken_arsenal",
            () -> new AncientSpellScrollItem("unbroken_arsenal"));
    public static final RegistryObject<Item> scroll_ordered_vault = ITEMS_REGISTRY.register("scroll_ordered_vault",
            () -> new AncientSpellScrollItem("ordered_vault"));
    public static final RegistryObject<Item> scroll_oreblood_revelation = ITEMS_REGISTRY.register("scroll_oreblood_revelation",
            () -> new AncientSpellScrollItem("oreblood_revelation"));
    public static final RegistryObject<Item> scroll_paper_legion = ITEMS_REGISTRY.register("scroll_paper_legion",
            () -> new AncientSpellScrollItem("paper_legion"));
    public static final RegistryObject<Item> scroll_unremembered_fog = ITEMS_REGISTRY.register("scroll_unremembered_fog",
            () -> new AncientSpellScrollItem("unremembered_fog"));
    public static final RegistryObject<Item> scroll_inverted_heaven = ITEMS_REGISTRY.register("scroll_inverted_heaven",
            () -> new AncientSpellScrollItem("inverted_heaven"));
    public static final RegistryObject<Item> scroll_eighteenfold_dragon = ITEMS_REGISTRY.register("scroll_eighteenfold_dragon",
            () -> new AncientSpellScrollItem("eighteenfold_dragon"));
    public static final RegistryObject<Item> scroll_leyline_resync = ITEMS_REGISTRY.register("scroll_leyline_resync",
            () -> new AncientSpellScrollItem("leyline_resync"));
    public static final RegistryObject<Item> scroll_forbidden_hollow_quarry = ITEMS_REGISTRY.register("scroll_forbidden_hollow_quarry",
            () -> new AncientSpellScrollItem("forbidden_hollow_quarry"));
    public static final RegistryObject<Item> scroll_forbidden_black_sun = ITEMS_REGISTRY.register("scroll_forbidden_black_sun",
            () -> new AncientSpellScrollItem("forbidden_black_sun"));

    // === Mana Reservoir BlockItems ===
    public static final RegistryObject<Item> mana_reservoir = ITEMS_REGISTRY.register("mana_reservoir",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MANA_RESERVOIR.get(), new Item.Properties()));
    public static final RegistryObject<Item> greater_mana_reservoir = ITEMS_REGISTRY.register("greater_mana_reservoir",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.GREATER_MANA_RESERVOIR.get(), new Item.Properties()));

    public static final RegistryObject<Item> mana_spreader = ITEMS_REGISTRY.register("mana_spreader",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MANA_SPREADER.get(), new Item.Properties()));

    // === Round 21: Mana Conduit BlockItem ===
    public static final RegistryObject<Item> mana_conduit = ITEMS_REGISTRY.register("mana_conduit",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MANA_CONDUIT.get(), new Item.Properties()));

    // === Round 22: Functional Mana BlockItems ===
    public static final RegistryObject<Item> mana_furnace = ITEMS_REGISTRY.register("mana_furnace",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MANA_FURNACE.get(), new Item.Properties()));
    public static final RegistryObject<Item> mana_sentinel = ITEMS_REGISTRY.register("mana_sentinel",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MANA_SENTINEL.get(), new Item.Properties()));
    public static final RegistryObject<Item> mana_harvester = ITEMS_REGISTRY.register("mana_harvester",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MANA_HARVESTER.get(), new Item.Properties()));
    public static final RegistryObject<Item> mana_generator = ITEMS_REGISTRY.register("mana_generator",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MANA_GENERATOR.get(), new Item.Properties()));

    // === Round 01: Aether Line ===
    // 以太碎片 — 古代飞升者残留以太能量的可见形态。
    public static final RegistryObject<Item> aether_shard = ITEMS_REGISTRY.register("aether_shard",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> aether_ore = ITEMS_REGISTRY.register("aether_ore",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.AETHER_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> deepslate_aether_ore = ITEMS_REGISTRY.register("deepslate_aether_ore",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.DEEPSLATE_AETHER_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> nether_aether_ore = ITEMS_REGISTRY.register("nether_aether_ore",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.NETHER_AETHER_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> aether_block = ITEMS_REGISTRY.register("aether_block",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.AETHER_BLOCK.get(), new Item.Properties()));

    // === Round 18: 魔力水晶矿 BlockItems ===
    public static final RegistryObject<Item> magic_crystal_ore = ITEMS_REGISTRY.register("magic_crystal_ore",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MAGIC_CRYSTAL_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> deepslate_magic_crystal_ore = ITEMS_REGISTRY.register("deepslate_magic_crystal_ore",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.DEEPSLATE_MAGIC_CRYSTAL_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> nether_magic_crystal_ore = ITEMS_REGISTRY.register("nether_magic_crystal_ore",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.NETHER_MAGIC_CRYSTAL_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> mana_lantern = ITEMS_REGISTRY.register("mana_lantern",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MANA_LANTERN.get(), new Item.Properties()));
    public static final RegistryObject<Item> aether_glass = ITEMS_REGISTRY.register("aether_glass",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.AETHER_GLASS.get(), new Item.Properties()));

    // === Round 07: Magical Building Set ===
    public static final RegistryObject<Item> runed_stone_bricks = ITEMS_REGISTRY.register("runed_stone_bricks",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.RUNED_STONE_BRICKS.get(), new Item.Properties()));
    public static final RegistryObject<Item> aether_bricks = ITEMS_REGISTRY.register("aether_bricks",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.AETHER_BRICKS.get(), new Item.Properties()));
    public static final RegistryObject<Item> polished_aether = ITEMS_REGISTRY.register("polished_aether",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.POLISHED_AETHER.get(), new Item.Properties()));
    public static final RegistryObject<Item> resonant_floor_tile = ITEMS_REGISTRY.register("resonant_floor_tile",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.RESONANT_FLOOR_TILE.get(), new Item.Properties()));

    // === Round 08: Element-Themed Lanterns ===
    public static final RegistryObject<Item> pyro_lantern = ITEMS_REGISTRY.register("pyro_lantern",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.PYRO_LANTERN.get(), new Item.Properties()));
    public static final RegistryObject<Item> cryo_lantern = ITEMS_REGISTRY.register("cryo_lantern",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.CRYO_LANTERN.get(), new Item.Properties()));
    public static final RegistryObject<Item> storm_lantern = ITEMS_REGISTRY.register("storm_lantern",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.STORM_LANTERN.get(), new Item.Properties()));
    public static final RegistryObject<Item> void_lantern = ITEMS_REGISTRY.register("void_lantern",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.VOID_LANTERN.get(), new Item.Properties()));

    // === Round 01: Nexus Core gap fix ===
    // 法则枢纽核心 — 此前注册了方块但缺 BlockItem，导致 creative 无法获取。
    // stacksTo(1) + 罕见，强调其是终局/世界生成方块。
    public static final RegistryObject<Item> nexus_core = ITEMS_REGISTRY.register("nexus_core",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.NEXUS_CORE.get(),
                    new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

    // === 执笔者套装 (Inscriber's Regalia) ===
    // 三件套：头巾 / 长袍 / 主笔 — 均为 Curio 饰品，单件被动 + 套装效果。
    public static final RegistryObject<Item> inscriber_hood = ITEMS_REGISTRY.register("inscriber_hood",
            com.huige233.transcend.items.curio.InscriberHood::new);
    public static final RegistryObject<Item> inscriber_robe = ITEMS_REGISTRY.register("inscriber_robe",
            com.huige233.transcend.items.curio.InscriberRobe::new);
    public static final RegistryObject<Item> inscriber_stylus = ITEMS_REGISTRY.register("inscriber_stylus",
            com.huige233.transcend.items.curio.InscriberStylus::new);

    public static void register(IEventBus eventBus) {
        ITEMS_REGISTRY.register(eventBus);
    }
}
