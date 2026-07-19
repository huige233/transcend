package com.huige233.transcend.circle;

public interface CircleFunctionExecutor {

    boolean canActivate(CircleFunctionContext ctx);

    void onActivate(CircleFunctionContext ctx);

    void tick(CircleFunctionContext ctx);

    void onDeactivate(CircleFunctionContext ctx);
}
