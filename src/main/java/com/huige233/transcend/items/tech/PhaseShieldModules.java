package com.huige233.transcend.items.tech;

import com.huige233.transcend.tech.core.TechItemData;
import com.huige233.transcend.tech.shield.TechShieldTierProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/** 保存多个护盾强化组件，兼容旧版单组件；等级校验和扣物品都在装配台服务端执行。 */
public final class PhaseShieldModules {
    public static final String MODULE = "shield_module";
    public static final String INSTALLED = "InstalledShieldModule";
    private static final String MODULES = "ShieldModules";
    private PhaseShieldModules() {}

    private static ListTag entries(@Nullable CompoundTag host) {
        if (host == null) return new ListTag();
        if (host.contains(MODULES, Tag.TAG_LIST)) return host.getList(MODULES, Tag.TAG_COMPOUND).copy();
        ListTag entries = new ListTag();
        if (ShieldModule.byId(host.getString(MODULE)) != null) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", host.getString(MODULE));
            entry.put("Item", host.getCompound(INSTALLED).copy());
            entries.add(entry);
        }
        return entries;
    }

    public static List<ShieldModule> modules(@Nullable CompoundTag host) {
        List<ShieldModule> result = new ArrayList<>();
        ListTag entries = entries(host);
        for (int i = 0; i < entries.size(); i++) {
            ShieldModule module = ShieldModule.byId(entries.getCompound(i).getString("Id"));
            if (module != null && result.stream().noneMatch(m -> m.family().equals(module.family()))) result.add(module);
        }
        return result;
    }

    public static List<ShieldModule> activeModules(ItemStack stack) {
        int limit = TechShieldTierProfile.of(TechItemData.getRank(stack)).slots();
        List<ShieldModule> active = new ArrayList<>();
        Set<String> families = new HashSet<>();
        ListTag entries = entries(stack.getTag());
        for (int i = 0; i < entries.size() && active.size() < limit; i++) {
            ShieldModule module = ShieldModule.byId(entries.getCompound(i).getString("Id"));
            if (PhaseShield.allowsModule(stack, module) && families.add(module.family())) active.add(module);
        }
        return List.copyOf(active);
    }

    @Nullable
    public static ShieldModule module(@Nullable CompoundTag host) {
        List<ShieldModule> modules = modules(host);
        return modules.isEmpty() ? null : modules.get(modules.size() - 1);
    }

    public static float energyMultiplier(ItemStack stack) {
        float multiplier = 1;
        for (ShieldModule module : activeModules(stack)) multiplier *= module.energyMultiplier();
        return Math.max(0.5F, Math.min(2, multiplier));
    }

    public static float capacity(ItemStack stack, float base) {
        float bonus = 0;
        for (ShieldModule module : activeModules(stack)) bonus += Math.min(base * 0.5F, module.capacity());
        return base + bonus;
    }

    public static boolean canInstall(ItemStack stack, ShieldModule module) {
        if (!(stack.getItem() instanceof PhaseShield) || !PhaseShield.allowsModule(stack, module)) return false;
        List<ShieldModule> installed = modules(stack.getTag());
        return entries(stack.getTag()).size() < TechShieldTierProfile.of(TechItemData.getRank(stack)).slots()
                && installed.stream().noneMatch(m -> m.family().equals(module.family()));
    }

    public static boolean install(ItemStack stack, ShieldModule module, CompoundTag serializedItem) {
        if (!canInstall(stack, module)) return false;
        ItemStack component = ItemStack.of(serializedItem);
        if (ShieldModuleItem.getModule(component) != module) return false;
        PhaseShield.initialize(stack);
        CompoundTag host = stack.getOrCreateTag();
        ListTag entries = entries(host);
        CompoundTag entry = new CompoundTag();
        entry.putString("Id", module.id());
        entry.put("Item", component.copyWithCount(1).save(new CompoundTag()));
        entries.add(entry);
        host.put(MODULES, entries);
        host.remove(MODULE);
        host.remove(INSTALLED);
        PhaseShield.applyModule(stack);
        return true;
    }

    /** 按安装顺序逆序拆卸，保留原物品的品质、名称及其余数据。 */
    public static ItemStack removeLast(ItemStack stack) {
        CompoundTag host = stack.getOrCreateTag();
        ListTag entries = entries(host);
        if (entries.isEmpty()) return ItemStack.EMPTY;
        CompoundTag entry = entries.getCompound(entries.size() - 1);
        ShieldModule module = ShieldModule.byId(entry.getString("Id"));
        ItemStack result = ItemStack.of(entry.getCompound("Item"));
        if (result.isEmpty() && module != null) result = ShieldModuleItem.create(module);
        if (result.isEmpty()) return ItemStack.EMPTY;
        entries.remove(entries.size() - 1);
        host.put(MODULES, entries);
        host.remove(MODULE);
        host.remove(INSTALLED);
        PhaseShield.applyModule(stack);
        return result.copyWithCount(1);
    }
}
