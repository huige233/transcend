package com.mega.uom.coremod.asm;

import com.mega.uom.coremod.FantasyEndingCore;
import com.mega.uom.coremod.FantasyEndingMixinPlugin;
import com.mega.uom.util.entity.ms.GetHealthModifyMethodVisitor;
import com.mega.uom.util.entity.ms.IsAliveModifyMethodVisitor;
import com.mega.uom.util.entity.ms.IsDeadOrDyingMethodVisitor;
import com.mega.uom.util.entity.ms.StaticHealthManagerVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class SoftGetHealthClassVisitor extends ClassVisitor {
    String targetClassName;
    public SoftGetHealthClassVisitor(String targetClassName, int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
        this.targetClassName = targetClassName;
    }
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (GetHealthModifyMethodVisitor.is(name, descriptor)) {
            FantasyEndingMixinPlugin.log("FeCoremod:visit getHealth()F of class:%s", targetClassName);
            FantasyEndingCore.stream.println("FeCoremod:visit getHealth method:"+name+descriptor+" class "+targetClassName);
            return new GetHealthModifyMethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions));
        } else if (IsAliveModifyMethodVisitor.is(name, descriptor)) {
            FantasyEndingMixinPlugin.log("FeCoremod:visit isAlive method:%s%s", name, descriptor);
            FantasyEndingCore.stream.println("FeCoremod:visit isAlive method:"+name+descriptor+" class "+targetClassName);
            return new IsAliveModifyMethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions));
        } else if (IsDeadOrDyingMethodVisitor.is(name, descriptor)) {
            FantasyEndingMixinPlugin.log("FeCoremod:visit isDeadOrDying method:%s%s", name, descriptor);
            FantasyEndingCore.stream.println("FeCoremod:visit isDeadOrDying method:"+name+descriptor+" class "+targetClassName);
            return new IsDeadOrDyingMethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions));
        }
        if (!targetClassName.endsWith(".LivingEntity"))
            return new StaticHealthManagerVisitor(super.visitMethod(access, name, descriptor, signature, exceptions));
        else return super.visitMethod(access, name, descriptor, signature, exceptions);
    }
}
