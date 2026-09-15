package com.huige233.transcend.client;

import com.huige233.transcend.tech.ammo.AmmoType;


/** 保存粒子枪界面使用的弹药、充能和热量快照，并计算充能与热量占比。 */
final class ParticleGunHudSnapshot {
    final AmmoType ammo;
    final int charges;
    final int capacity;
    final float charge;
    final float chargeCapacity;
    final float heat;
    final float heatCapacity;

    ParticleGunHudSnapshot(AmmoType ammo, int charges, int capacity, float charge,
                           float chargeCapacity, float heat, float heatCapacity) {
        this.ammo = ammo;
        this.charges = charges;
        this.capacity = capacity;
        this.charge = charge;
        this.chargeCapacity = chargeCapacity;
        this.heat = heat;
        this.heatCapacity = heatCapacity;
    }

    float chargeRatio() {
        return charge / chargeCapacity;
    }

    float heatRatio() {
        return heat / heatCapacity;
    }
}
