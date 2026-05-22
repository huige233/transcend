package com.huige233.transcend.circle;

/**
 * 法环功能执行器接口。
 * 每种 CircleFunctionType 对应一个实现类。
 */
public interface CircleFunctionExecutor {
    /** 检查是否可以激活（结构、催化剂、魔力是否满足条件） */
    boolean canActivate(CircleFunctionContext ctx);

    /** 激活时回调 */
    void onActivate(CircleFunctionContext ctx);

    /** 每20tick调用一次（服务端） */
    void tick(CircleFunctionContext ctx);

    /** 关闭时回调 */
    void onDeactivate(CircleFunctionContext ctx);
}
