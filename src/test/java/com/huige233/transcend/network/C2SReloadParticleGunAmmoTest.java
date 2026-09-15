package com.huige233.transcend.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** 检查粒子枪装填请求的内置弹药标识编码及载荷消耗，并拒绝空白、未知和超长标识。 */
class C2SReloadParticleGunAmmoTest {
    @Test
    void roundTripsEachBuiltInAmmoIdWithoutUnreadBytes() {
        for (String id : new String[] {"charge", "energy", "spell"}) {
            ByteBuf backing = Unpooled.buffer();
            try {
                FriendlyByteBuf buffer = new FriendlyByteBuf(backing);
                new C2SReloadParticleGunAmmo(id).write(buffer);
                C2SReloadParticleGunAmmo decoded = new C2SReloadParticleGunAmmo(buffer);
                assertSame(com.huige233.transcend.tech.ammo.BuiltInAmmoTypes.byId(id),
                        C2SReloadParticleGunAmmo.requestedAmmo(id));
                assertEquals(0, backing.readableBytes());
            } finally {
                backing.release();
            }
        }
    }

    @Test
    void rejectsUnknownBlankAndOversizedIdsBeforeReloadHandling() {
        assertNull(C2SReloadParticleGunAmmo.requestedAmmo(null));
        assertNull(C2SReloadParticleGunAmmo.requestedAmmo(""));
        assertNull(C2SReloadParticleGunAmmo.requestedAmmo("not_registered"));
        assertNull(C2SReloadParticleGunAmmo.requestedAmmo("x".repeat(17)));
    }
}
