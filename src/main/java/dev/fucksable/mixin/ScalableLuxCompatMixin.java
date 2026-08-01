package dev.fucksable.mixin;

import ca.spottedleaf.starlight.common.light.StarLightInterface;
import ca.spottedleaf.starlight.common.light.StarLightLightingProvider;
import dev.fucksable.FuckSable;
import dev.fucksable.fix.FixRegistry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 修复 Sable 与 ScalableLux 的光照引擎兼容性问题。
 * <p>
 * 问题分析：
 * Sable 的 ServerLevelPlot 在构造时，从主世界 LevelLightEngine 读取
 * blockEngine/skyEngine 字段来判断 SubLevel 是否有方块光/天空光
 * （hasBlockLight = blockEngine != null, hasSkyLight = skyEngine != null）。
 * <p>
 * ScalableLux 的 LevelLightEngineMixin.construct 在 BaseLevelLightEngineVanillaInterface
 * （实现了 StarLightLightingProvider）构造后，将 blockEngine/skyEngine 清空为 null，
 * 因为 ScalableLux 用自己的 StarLightInterface 替代 vanilla 光照引擎。
 * <p>
 * 这导致 Sable 误判 SubLevel 无方块光、无天空光（hasBlockLight=false, hasSkyLight=false），
 * SubLevel 光照完全失效，表现为 SubLevel 内部全黑或光照异常。
 * 这也是 Issue #8 报告的 "C2ME OCL 和 Sable 大部分不兼容" 的技术根因。
 * <p>
 * 修复方式：
 * 拦截 ServerLevelPlot 构造函数中 new LevelLightEngine(...) 调用，
 * 当 ScalableLux 存在时，通过 StarLightInterface.hasBlockLight()/hasSkyLight()
 * 重新计算正确的 hasBlockLight/hasSkyLight 参数，确保 SubLevel 光照正常初始化。
 */
@Mixin(targets = "dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot", remap = false)
public abstract class ScalableLuxCompatMixin {

    @Redirect(
        method = "<init>",
        at = @At(
            value = "NEW",
            target = "net/minecraft/world/level/lighting/LevelLightEngine",
            remap = false
        ),
        remap = false
    )
    private LevelLightEngine fucksable$fixLightEngineInit(
        LightChunkGetter chunkGetter, boolean hasBlockLight, boolean hasSkyLight
    ) {
        if (FixRegistry.isEnabled("scalablelux-compat")) {
            if (!FixRegistry.isEnabled("sable-scalablelux-incompat-bypass")) {
                FuckSable.LOGGER.warn(
                    "scalablelux-compat is enabled but its prerequisite 'sable-scalablelux-incompat-bypass' is disabled. " +
                    "Enable 'sable-scalablelux-incompat-bypass' and restart the server for ScalableLux compatibility to work."
                );
                return new LevelLightEngine(chunkGetter, hasBlockLight, hasSkyLight);
            }
            if (chunkGetter.getLevel() instanceof Level level) {
                LevelLightEngine worldEngine = level.getLightEngine();
                if (worldEngine instanceof StarLightLightingProvider provider) {
                    StarLightInterface starLight = provider.scalablelux$getLightEngine();
                    if (starLight != null) {
                        boolean realHasBlock = starLight.hasBlockLight();
                        boolean realHasSky = starLight.hasSkyLight();
                        if (realHasBlock != hasBlockLight || realHasSky != hasSkyLight) {
                            FuckSable.LOGGER.info(
                                "ScalableLux compat: corrected SubLevel light engine flags (block: {}->{}, sky: {}->{})",
                                hasBlockLight, realHasBlock, hasSkyLight, realHasSky
                            );
                        }
                        hasBlockLight = realHasBlock;
                        hasSkyLight = realHasSky;
                    }
                }
            }
        }
        return new LevelLightEngine(chunkGetter, hasBlockLight, hasSkyLight);
    }
}
