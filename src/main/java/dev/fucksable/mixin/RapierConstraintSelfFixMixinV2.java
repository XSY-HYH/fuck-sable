package dev.fucksable.mixin;

import dev.fucksable.ThrottledLogger;
import dev.fucksable.fix.FixRegistry;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.physics.impl.rapier.Rapier3D;
import dev.ryanhcode.sable.physics.impl.rapier.RapierPhysicsPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sable 2.0.3+ 版本的 self-constraint 抑制。
 * <p>
 * Sable 2.0.3 中 RapierPhysicsPipeline.addConstraint 的参数类型从 ServerSubLevel
 * 改为父接口 PhysicsPipelineBody。此 mixin 仅在 Sable 2.0.3+ 上启用
 * （由 FuckSableMixinConfigPlugin 控制）。
 */
@Mixin(RapierPhysicsPipeline.class)
public abstract class RapierConstraintSelfFixMixinV2 {

    @Inject(method = "addConstraint", at = @At("HEAD"), cancellable = true, remap = false)
    private void fucksable$suppressSelfConstraintV2(PhysicsPipelineBody bodyA, PhysicsPipelineBody bodyB, PhysicsConstraintConfiguration<?> configuration, CallbackInfoReturnable<PhysicsConstraintHandle> cir) {
        if (!FixRegistry.isEnabled("constraint-self-fix")) return;

        if (bodyA == bodyB && bodyA != null) {
            String key = String.valueOf(Rapier3D.getID(bodyA));
            ThrottledLogger.warnOnce("self-constraint:" + key,
                "Suppressed self-constraint on body id={} (same SubLevel), returning null. This warning will not repeat.", key);
            cir.setReturnValue(null);
        }
    }
}
