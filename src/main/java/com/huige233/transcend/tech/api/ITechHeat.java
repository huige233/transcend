package com.huige233.transcend.tech.api;


/** 约定科技设备的热量查询、积热、散热、过热判断和冷却进度接口。 */
public interface ITechHeat {
    float heat();
    float heatCapacity();
    void addHeat(float amount);
    void tickCooling();
    boolean isOverheated();
    float cooldownProgress();
}
