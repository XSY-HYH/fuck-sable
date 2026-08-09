package dev.fucksable.mixin;

import dev.fucksable.ThrottledLogger;
import dev.fucksable.fix.FixEntry;
import dev.fucksable.fix.FixRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 防止整数极限坐标的方块破坏导致服务器卡死和崩溃。
 *
 * 问题来源：某些 mod 物品触发方块破坏时，坐标计算溢出至 Integer.MIN_VALUE / MAX_VALUE，
 * 导致大量区块被加载/生成、光照更新范围极大，最终服务器卡死或崩溃。
 *
 * 修复方式：在 Level.setBlock 和 Level.destroyBlock 入口处检查坐标是否超出合理范围，
 * 超出则跳过方块操作并记录警告（通过 ThrottledLogger 节流）。
 * 坐标范围（xLimit, yMin, yMax）从 block-destroy-coordinate-guard 修复项的 options 读取，
 * 可通过 config.json 配置。
 */
@Mixin(Level.class)
public class LevelDestroyBlockGuardMixin {

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
    private void fucksable$guardSetBlockBounds(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        if (!FixRegistry.isEnabled("block-destroy-coordinate-guard")) return;
        if (fucksable$isCoordinateExtreme(pos)) {
            fucksable$warnExtremeCoordinate("setBlock", pos);
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z", at = @At("HEAD"), cancellable = true)
    private void fucksable$guardDestroyBlockBounds(BlockPos pos, boolean dropBlock, net.minecraft.world.entity.Entity entity, int recursionLevel, CallbackInfoReturnable<Boolean> cir) {
        if (!FixRegistry.isEnabled("block-destroy-coordinate-guard")) return;
        if (fucksable$isCoordinateExtreme(pos)) {
            fucksable$warnExtremeCoordinate("destroyBlock", pos);
            cir.setReturnValue(false);
        }
    }

    @Unique
    private static int fucksable$getIntOption(String key, int defaultValue) {
        FixEntry entry = FixRegistry.getFix("block-destroy-coordinate-guard");
        if (entry == null) return defaultValue;
        Object val = entry.getOption(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) {
            try { return Integer.parseInt((String) val); } catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }

    @Unique
    private static boolean fucksable$isCoordinateExtreme(BlockPos pos) {
        int xLimit = fucksable$getIntOption("xLimit", 30_000_000);
        int yMin = fucksable$getIntOption("yMin", -512);
        int yMax = fucksable$getIntOption("yMax", 1024);
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        return Math.abs(x) > xLimit || Math.abs(z) > xLimit || y < yMin || y > yMax;
    }

    @Unique
    private static void fucksable$warnExtremeCoordinate(String operation, BlockPos pos) {
        int xLimit = fucksable$getIntOption("xLimit", 30_000_000);
        int yMin = fucksable$getIntOption("yMin", -512);
        int yMax = fucksable$getIntOption("yMax", 1024);
        ThrottledLogger.warn("extreme-coord:" + operation,
            "Blocked {} at extreme coordinate {} (likely integer overflow from a modded item). " +
            "Current limits: x/z limit={}, y range=[{}, {}].",
            operation, pos, xLimit, yMin, yMax
        );
    }
}
