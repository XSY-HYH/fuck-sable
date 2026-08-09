package dev.fucksable.mixin;

import dev.fucksable.fix.FixEntry;
import dev.fucksable.fix.FixRegistry;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 实验性：覆盖原版世界高度上限。
 * <p>
 * 通过拦截 Level.getMaxBuildHeight() 和 Level.getMinBuildHeight()，
 * 允许用户自定义世界构建高度范围。
 * <p>
 * 注意：这是实验性功能，可能与依赖原版高度的其他 mod 不兼容。
 * 默认关闭，启用前请充分测试。
 */
@Mixin(Level.class)
public class WorldHeightOverrideMixin {

    @Inject(method = "getMaxBuildHeight", at = @At("HEAD"), cancellable = true)
    private void fucksable$overrideMaxBuildHeight(CallbackInfoReturnable<Integer> cir) {
        if (!FixRegistry.isEnabled("world-height-override")) return;
        cir.setReturnValue(fucksable$getIntOption("maxBuildHeight", 320));
    }

    @Inject(method = "getMinBuildHeight", at = @At("HEAD"), cancellable = true)
    private void fucksable$overrideMinBuildHeight(CallbackInfoReturnable<Integer> cir) {
        if (!FixRegistry.isEnabled("world-height-override")) return;
        cir.setReturnValue(fucksable$getIntOption("minBuildHeight", -64));
    }

    @Unique
    private static int fucksable$getIntOption(String key, int defaultValue) {
        FixEntry entry = FixRegistry.getFix("world-height-override");
        if (entry == null) return defaultValue;
        Object val = entry.getOption(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) {
            try { return Integer.parseInt((String) val); } catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }
}
