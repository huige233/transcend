package com.huige233.transcend.agent;

import cpw.mods.modlauncher.api.INameMappingService;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * 抢在 OmniMobs(及梦幻终焉)之后,把 {@code LivingEntity.getHealth/isAlive/isDeadOrDying} 的返回值再包一层。
 *
 * <p><b>为什么这是唯一能赢 omni 的办法</b>:omni 不是普通 coremod —— 它在运行时动态 attach 一个 javaagent,
 * 用 {@code retransformClasses} 改写 getHealth/isAlive/isDeadOrDying(把玩家读成死)。谁<b>最后</b>改返回值谁说了算;
 * mixin、梦幻终焉护盾都在 omni 之前(更里层),所以会被 omni 盖掉。本类直接复用 omni 自己 attach 出来的
 * {@code OmniMobs.instrumentation},把<b>我们的</b> transformer 注册在 omni 之后,于是我们的包装在最外层 → 赢。</p>
 *
 * <p>只包返回值(在 FRETURN/IRETURN 前插一次 invokestatic),不加分支 → 不需要重算 stack map frame,最稳。</p>
 */
public final class TranscendOmniPatch implements ClassFileTransformer {

    private static final String LIVING = "net/minecraft/world/entity/LivingEntity";
    private static final String HOOK = "com/huige233/transcend/agent/TranscendAgentHook";

    private static final String GET_HEALTH = remap("m_21223_");   // getHealth ()F
    private static final String IS_ALIVE = remap("m_6084_");      // isAlive ()Z
    private static final String IS_DEAD = remap("m_21224_");      // isDeadOrDying ()Z

    private static boolean installed;

    private TranscendOmniPatch() {
    }

    private static String remap(String srg) {
        try {
            return ObfuscationReflectionHelper.remapName(INameMappingService.Domain.METHOD, srg);
        } catch (Throwable t) {
            return srg;
        }
    }

    /** 复用 omni attach 出来的 Instrumentation,把本 transformer 注册在它之后并 retransform。omni 不在则直接跳过。 */
    public static synchronized void tryInstall() {
        if (installed) return;
        try {
            Class<?> omni = Class.forName("flashfur.omnimobs.OmniMobs");
            Object inst = omni.getField("instrumentation").get(null);
            if (!(inst instanceof Instrumentation instrumentation)) return;
            instrumentation.addTransformer(new TranscendOmniPatch(), true);
            instrumentation.retransformClasses(Class.forName("net.minecraft.world.entity.LivingEntity"));
            installed = true;
        } catch (Throwable ignored) {
            // omni 未安装 / 无 attach 权限:本层不生效,mixin 兜底其它 mod。
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!LIVING.equals(className)) return null;
        try {
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);

            boolean changed = false;
            for (MethodNode m : cn.methods) {
                if (m.name.equals(GET_HEALTH) && m.desc.equals("()F")) {
                    wrapReturns(m, Opcodes.FRETURN, "health",
                            "(FLnet/minecraft/world/entity/LivingEntity;)F");
                    changed = true;
                } else if (m.name.equals(IS_ALIVE) && m.desc.equals("()Z")) {
                    wrapReturns(m, Opcodes.IRETURN, "alive",
                            "(ZLnet/minecraft/world/entity/LivingEntity;)Z");
                    changed = true;
                } else if (m.name.equals(IS_DEAD) && m.desc.equals("()Z")) {
                    wrapReturns(m, Opcodes.IRETURN, "deadOrDying",
                            "(ZLnet/minecraft/world/entity/LivingEntity;)Z");
                    changed = true;
                }
            }
            if (!changed) return null;

            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
            cn.accept(cw);
            return cw.toByteArray();
        } catch (Throwable t) {
            return null;
        }
    }

    /** 在方法每个 return 前插入 {@code aload 0; invokestatic hook(原返回值, this)} —— 用钩子结果替换返回值。 */
    private static void wrapReturns(MethodNode m, int opcode, String hook, String desc) {
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn.getOpcode() == opcode) {
                InsnList list = new InsnList();
                list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, hook, desc, false));
                m.instructions.insertBefore(insn, list);
            }
        }
    }
}
