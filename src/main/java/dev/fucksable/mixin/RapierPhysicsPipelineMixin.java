package dev.fucksable.mixin;

import dev.fucksable.ThrottledLogger;
import dev.fucksable.fix.FixRegistry;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.impl.rapier.Rapier3D;
import dev.ryanhcode.sable.physics.impl.rapier.RapierPhysicsPipeline;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

@Mixin(RapierPhysicsPipeline.class)
public abstract class RapierPhysicsPipelineMixin {

    @Shadow(remap = false)
    @Final
    private Int2ObjectMap<ServerSubLevel> activeSubLevels;

    @Unique
    private boolean fucksable$isBodyValid(PhysicsPipelineBody body) {
        if (body == null) {
            return false;
        }
        int id = Rapier3D.getID(body);
        return this.activeSubLevels.containsKey(id);
    }

    @Unique
    private boolean fucksable$isPanicGuardEnabled() {
        return FixRegistry.isEnabled("panic-guard");
    }

    // --- Self-constraint suppression ---
    // Note: addConstraint @Inject is in version-specific mixins:
    // - RapierConstraintSelfFixMixinV1 for Sable 1.x (ServerSubLevel parameters)
    // - RapierConstraintSelfFixMixinV2 for Sable 2.0.3+ (PhysicsPipelineBody parameters)
    // Selection is handled by FuckSableMixinConfigPlugin based on Sable version.

    // --- Velocity queries ---

    @Inject(method = "getLinearVelocity", at = @At("HEAD"), cancellable = true, remap = false)
    private void fucksable$safeGetLinearVelocity(PhysicsPipelineBody body, Vector3d dest, CallbackInfoReturnable<Vector3d> cir) {
        if (!this.fucksable$isPanicGuardEnabled()) return;
        if (!this.fucksable$isBodyValid(body)) {
            ThrottledLogger.warn("rapier-linear-velocity:" + (body != null ? Rapier3D.getID(body) : "null"),
                "Attempted to get linear velocity of invalid/removed body (id={}), returning zero",
                body != null ? Rapier3D.getID(body) : "null");
            cir.setReturnValue(dest.zero());
        }
    }

    @Inject(method = "getAngularVelocity", at = @At("HEAD"), cancellable = true, remap = false)
    private void fucksable$safeGetAngularVelocity(PhysicsPipelineBody body, Vector3d dest, CallbackInfoReturnable<Vector3d> cir) {
        if (!this.fucksable$isPanicGuardEnabled()) return;
        if (!this.fucksable$isBodyValid(body)) {
            ThrottledLogger.warn("rapier-angular-velocity:" + (body != null ? Rapier3D.getID(body) : "null"),
                "Attempted to get angular velocity of invalid/removed body (id={}), returning zero",
                body != null ? Rapier3D.getID(body) : "null");
            cir.setReturnValue(dest.zero());
        }
    }

    // --- Pose query ---

    @Inject(method = "readPose", at = @At("HEAD"), cancellable = true, remap = false)
    private void fucksable$safeReadPose(ServerSubLevel body, Pose3d dest, CallbackInfoReturnable<Pose3d> cir) {
        if (!this.fucksable$isPanicGuardEnabled()) return;
        if (!this.fucksable$isBodyValid(body)) {
            ThrottledLogger.warn("rapier-read-pose:" + (body != null ? Rapier3D.getID(body) : "null"),
                "Attempted to read pose of invalid/removed body (id={}), returning current pose",
                body != null ? Rapier3D.getID(body) : "null");
            cir.setReturnValue(dest);
        }
    }

    // --- Velocity/impulse modifications ---

    @Inject(method = "addLinearAndAngularVelocity", at = @At("HEAD"), cancellable = true, remap = false)
    private void fucksable$safeAddVelocity(PhysicsPipelineBody body, Vector3dc linearVelocity, Vector3dc angularVelocity, CallbackInfo ci) {
        if (!this.fucksable$isPanicGuardEnabled()) return;
        if (!this.fucksable$isBodyValid(body)) {
            ThrottledLogger.warn("rapier-add-velocity:" + (body != null ? Rapier3D.getID(body) : "null"),
                "Attempted to add velocity to invalid/removed body (id={}), skipping",
                body != null ? Rapier3D.getID(body) : "null");
            ci.cancel();
        }
    }

    @Inject(method = "applyLinearAndAngularImpulse", at = @At("HEAD"), cancellable = true, remap = false)
    private void fucksable$safeApplyImpulse(PhysicsPipelineBody body, Vector3dc force, Vector3dc torque, boolean wakeUp, CallbackInfo ci) {
        if (!this.fucksable$isPanicGuardEnabled()) return;
        if (!this.fucksable$isBodyValid(body)) {
            ThrottledLogger.warn("rapier-apply-impulse:" + (body != null ? Rapier3D.getID(body) : "null"),
                "Attempted to apply impulse to invalid/removed body (id={}), skipping",
                body != null ? Rapier3D.getID(body) : "null");
            ci.cancel();
        }
    }

    @Inject(method = "applyImpulse", at = @At("HEAD"), cancellable = true, remap = false)
    private void fucksable$safeApplyForce(PhysicsPipelineBody body, Vector3dc position, Vector3dc force, CallbackInfo ci) {
        if (!this.fucksable$isPanicGuardEnabled()) return;
        if (!this.fucksable$isBodyValid(body)) {
            ThrottledLogger.warn("rapier-apply-force:" + (body != null ? Rapier3D.getID(body) : "null"),
                "Attempted to apply force to invalid/removed body (id={}), skipping",
                body != null ? Rapier3D.getID(body) : "null");
            ci.cancel();
        }
    }

    @Inject(method = "wakeUp", at = @At("HEAD"), cancellable = true, remap = false)
    private void fucksable$safeWakeUp(PhysicsPipelineBody body, CallbackInfo ci) {
        if (!this.fucksable$isPanicGuardEnabled()) return;
        if (!this.fucksable$isBodyValid(body)) {
            ThrottledLogger.warn("rapier-wake-up:" + (body != null ? Rapier3D.getID(body) : "null"),
                "Attempted to wake up invalid/removed body (id={}), skipping",
                body != null ? Rapier3D.getID(body) : "null");
            ci.cancel();
        }
    }

    // --- Teleport ---

    @Inject(method = "teleport", at = @At("HEAD"), cancellable = true, remap = false)
    private void fucksable$safeTeleport(PhysicsPipelineBody body, Vector3dc position, Quaterniondc orientation, CallbackInfo ci) {
        if (!this.fucksable$isPanicGuardEnabled()) return;
        if (!this.fucksable$isBodyValid(body)) {
            ThrottledLogger.warn("rapier-teleport:" + (body != null ? Rapier3D.getID(body) : "null"),
                "Attempted to teleport invalid/removed body (id={}), skipping",
                body != null ? Rapier3D.getID(body) : "null");
            ci.cancel();
        }
    }

    // --- Mass/stats changes ---

    @Inject(method = "onStatsChanged", at = @At("HEAD"), cancellable = true, remap = false)
    private void fucksable$safeOnStatsChanged(ServerSubLevel serverSubLevel, CallbackInfo ci) {
        if (!this.fucksable$isPanicGuardEnabled()) return;
        if (!this.fucksable$isBodyValid(serverSubLevel)) {
            ThrottledLogger.warn("rapier-stats-changed:" + (serverSubLevel != null ? Rapier3D.getID(serverSubLevel) : "null"),
                "Attempted to update stats of invalid/removed body (id={}), skipping",
                serverSubLevel != null ? Rapier3D.getID(serverSubLevel) : "null");
            ci.cancel();
        }
    }
}
