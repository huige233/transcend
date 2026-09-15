package com.huige233.transcend.client;

import com.huige233.transcend.entity.TestDummy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;


/** 根据网络包提供的实体编号查找测试假人，并在客户端打开其设置界面。 */
public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    
    public static void openTestDummyScreen(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(entityId);
        if (!(entity instanceof TestDummy dummy)) {
            return;
        }
        minecraft.setScreen(new TestDummyScreen(dummy));
    }
}
