package com.huige233.transcend.tech.energy;


/** 约定物品直接充能接口，允许绕过端口方向和速率限制但仍受储能容量约束。 */
public interface DirectEnergyReceiver {
    
    int receiveExternalEnergy(int amount);
}
