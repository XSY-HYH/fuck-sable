package dev.fucksable.mixin;

import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.foundation.item.ItemHelper;
import dev.fucksable.ThrottledLogger;
import dev.fucksable.fix.FixRegistry;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

/**
 * 优化 FrogportBlockEntity 拉取物品时的卡死问题。
 * <p>
 * 问题分析：
 * FrogportBlockEntity.lazyTick 调用 tryPullingFromOwnAndAdjacentInventories，
 * 后者对自身库存 + 最多 5 个相邻方向库存调用 tryPullingFrom → ItemHelper.extract。
 * extract 会遍历 IItemHandler 的所有槽位调用 getStackInSlot + extractItem。
 * 当相邻 inventory 是大型仓库（漏斗链连接的箱子阵列、Create 仓库等，槽位数可能上千）时，
 * 单次 extract 遍历耗时极长，叠加多个 frogport 同步触发，导致主线程卡死 10+ 秒。
 * <p>
 * 修复方式：
 * @Redirect 拦截 tryPullingFrom 中的 ItemHelper.extract 调用，
 * 当目标 inventory 槽位数超过阈值（默认 256）时跳过本次提取并降级为 debug 日志。
 * 正常 frogport 的输入库存（箱子、漏斗）槽位数远小于阈值，不受影响；
 * 仅切断异常大型 inventory 的遍历路径。
 * <p>
 * 这是性能优化而非逻辑修复：被跳过的 frogport 在下次 lazyTick 仍会尝试拉取，
 * 玩家可通过缩小相邻库存规模恢复正常工作。
 */
@Mixin(FrogportBlockEntity.class)
public class FrogportItemExtractLimitMixin {

    private static final int MAX_SLOTS = 256;

    @Redirect(
        method = "tryPullingFrom",
        at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/item/ItemHelper;extract(Lnet/neoforged/neoforge/items/IItemHandler;Ljava/util/function/Predicate;Z)Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack fucksable$limitExtractSlots(IItemHandler handler, Predicate<ItemStack> predicate, boolean copy) {
        if (!FixRegistry.isEnabled("frogport-extract-limit")) {
            return ItemHelper.extract(handler, predicate, copy);
        }

        int slots;
        try {
            slots = handler.getSlots();
        } catch (Throwable t) {
            // 下游 inventory 异常，直接跳过
            return ItemStack.EMPTY;
        }

        if (slots <= MAX_SLOTS) {
            return ItemHelper.extract(handler, predicate, copy);
        }

        // 槽位数超限，跳过并通过 ThrottledLogger 节流告警
        ThrottledLogger.warn("frogport-extract-limit",
            "Frogport adjacent inventory has {} slots (limit {}), skipping extract to prevent server freeze. Reduce inventory size or move frogport.",
            slots, MAX_SLOTS
        );
        return ItemStack.EMPTY;
    }
}
