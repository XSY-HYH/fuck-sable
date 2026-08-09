package dev.fucksable.mixin;

import dev.fucksable.ThrottledLogger;
import dev.fucksable.fix.FixRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 修复玩家坐标超出世界边界导致服务器崩溃的问题。
 * <p>
 * 问题分析：
 * Sable 的 SubLevel 物理系统可能将玩家推出世界边界，
 * 导致后续的坐标计算和区块加载出现异常，最终引发服务器崩溃。
 * <p>
 * 修复方式：
 * 在 ServerPlayer.tick 中检查玩家 X/Z 坐标是否超出世界边界，
 * 如果超出则立刻将其拉回最近的边界点。
 * 不再限制 Y 坐标，玩家可以自由飞出世界高度。
 */
@Mixin(ServerPlayer.class)
public class PlayerPositionGuardMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void fucksable$clampToWorldBorder(CallbackInfo ci) {
        if (!FixRegistry.isEnabled("player-position-guard")) return;

        ServerPlayer self = (ServerPlayer) (Object) this;
        Vec3 pos = self.position();

        // 世界边界检查（仅 X/Z 轴，拉回边界+5，避免紧贴边界）
        WorldBorder border = self.level().getWorldBorder();
        double minX = border.getMinX() + 5.0;
        double maxX = border.getMaxX() - 5.0;
        double minZ = border.getMinZ() + 5.0;
        double maxZ = border.getMaxZ() - 5.0;

        boolean outOfBounds = false;
        double clampedX = pos.x;
        double clampedZ = pos.z;

        if (clampedX < minX) { clampedX = minX; outOfBounds = true; }
        if (clampedX > maxX) { clampedX = maxX; outOfBounds = true; }
        if (clampedZ < minZ) { clampedZ = minZ; outOfBounds = true; }
        if (clampedZ > maxZ) { clampedZ = maxZ; outOfBounds = true; }

        if (outOfBounds) {
            ThrottledLogger.warn("player-position:" + self.getUUID(),
                "Player {} was out of world bounds at ({}, {}, {}), clamping X/Z to ({}, {})",
                self.getName().getString(), pos.x, pos.y, pos.z, clampedX, clampedZ);
            self.setPos(clampedX, pos.y, clampedZ);
            self.setDeltaMovement(new Vec3(0.0, 0.0, 0.0));
        }
    }
}
