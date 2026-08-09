package dev.fucksable.mixin;

import dev.fucksable.fix.FixEntry;
import dev.fucksable.fix.FixRegistry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 实验性：覆盖原版世界高度上限。
 * <p>
 * LevelHeightAccessor 接口的方法分布：
 * - getMaxBuildHeight() 是 default 方法，Level 类没有 override，所以必须拦截接口的 default 方法。
 * - getMinBuildHeight() 是抽象方法，Level 类可能 override 也可能没有。
 *   mixin 类（interface）extends LevelHeightAccessor 后，default 方法实现会被注入到接口。
 *   如果 Level override 了 getMinBuildHeight()，注入的 default 方法不影响 Level（需要额外的 LevelMinBuildHeightMixin）。
 *   如果 Level 没有 override，注入的 default 方法直接生效。
 * <p>
 * 通过 instanceof Level 检查确保只影响 Level 及其子类（ServerLevel/ClientLevel），
 * 不影响 LevelChunk 等其他实现类。
 * <p>
 * 注意：interface mixin 要求 mixin 类本身必须是 interface（不是 class），
 * 且用 extends 关键字声明目标接口。mixin 框架通过检查 mixin 类本身是 class 还是 interface
 * 来决定 SubType（Standard / Interface）。如果 mixin 类是 class，
 * 即使 implements 目标接口，SubType 仍然是 Standard，会导致 prepare 阶段
 * validateTarget 时因 target 是 interface 报
 * "@Mixin target type mismatch: ... is an interface" 错误。
 * <p>
 * 注意：这是实验性功能，可能与依赖原版高度的其他 mod 不兼容。
 * 默认关闭，启用前请充分测试。
 */
@Mixin(LevelHeightAccessor.class)
public interface WorldHeightOverrideMixin extends LevelHeightAccessor {

    @Inject(method = "getMaxBuildHeight()I", at = @At("HEAD"), cancellable = true)
    default void fucksable$overrideMaxBuildHeight(CallbackInfoReturnable<Integer> cir) {
        if (!FixRegistry.isEnabled("world-height-override")) return;
        if (!((Object) this instanceof Level)) return;
        cir.setReturnValue(fucksable$getIntOption("maxBuildHeight", 320));
    }

    /**
     * 实现 LevelHeightAccessor 的抽象方法 getMinBuildHeight()。
     * 这个 default 方法实现会被注入到接口。
     * 如果 Level override 了 getMinBuildHeight()，此 default 方法不影响 Level，
     * 需要 LevelMinBuildHeightMixin 额外处理。
     */
    @Override
    default int getMinBuildHeight() {
        if (FixRegistry.isEnabled("world-height-override") && (Object) this instanceof Level) {
            return fucksable$getIntOption("minBuildHeight", -64);
        }
        return -64;
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
