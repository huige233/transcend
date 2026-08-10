package com.huige233.transcend.circle;

/** 法阵功能执行器接口。 */
public interface CircleFunctionExecutor {

    boolean canActivate(CircleFunctionContext ctx);

    void onActivate(CircleFunctionContext ctx);

    void tick(CircleFunctionContext ctx);

    void onDeactivate(CircleFunctionContext ctx);
}
