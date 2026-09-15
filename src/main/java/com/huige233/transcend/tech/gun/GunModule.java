package com.huige233.transcend.tech.gun;

import com.huige233.transcend.tech.attribute.TechAttrModifier;
import net.minecraft.network.chat.Component;

import java.util.List;


/** 约定枪械模块的标识、安装槽位、显示名称和属性修饰器列表。 */
public interface GunModule {
    String id();
    ModuleSlot slot();
    Component displayName();
    default List<TechAttrModifier> modifiers() { return List.of(); }
}
