## v1.7.25

### 测试功能标记机制 / Experimental Fix Tagging Mechanism

在 `FixEntry` 中新增 `experimental` 字段，允许将修复项标记为测试功能。标记后在 `/fucksable` 命令列表和详情中以黄色 `[测试]` / `[Experimental]` 标签显示。已将 `world-height-override` 标记为测试功能。

Added `experimental` field to `FixEntry`, allowing fixes to be tagged as experimental. Tagged fixes display a yellow `[测试]` / `[Experimental]` label in the `/fucksable` command list and details. `world-height-override` is now tagged as experimental.

### 兼容性 / Compatibility

- Sable 1.x 和 2.x / Sable 1.x and 2.x
- NeoForge 1.21.1
- Mohist/Youer 混合服务端 / Mohist/Youer hybrid servers
- ScalableLux（光照优化）兼容 / ScalableLux (lighting optimization) compatible
- c2me 兼容 / c2me compatible

## v1.7.24

### SubLevelStorage storageCache 并发崩溃修复 / SubLevelStorage storageCache Concurrent Crash Fix (Issue #20)

修复 `SubLevelStorage.getRegionStorageFile` 中 `Long2ObjectLinkedOpenHashMap`（storageCache）并发访问导致 `ArrayIndexOutOfBoundsException: Index -1 out of bounds for length N` 崩溃的问题。

Fixed `ArrayIndexOutOfBoundsException: Index -1 out of bounds for length N` crash caused by concurrent access to `Long2ObjectLinkedOpenHashMap` (storageCache) in `SubLevelStorage.getRegionStorageFile`.

**问题 / Issue**: 当 `async-save` 修复项启用时，`attemptSaveSubLevel` 在异步线程执行访问 storageCache。若主线程同时通过 `getOrLoadHoldingChunk` → `attemptLoadSubLevel` → `getRegionStorageFile` 访问 storageCache，会导致 FastUtil 链表 map 的 prev/next 指针状态损坏，最终抛出 `ArrayIndexOutOfBoundsException`。

When `async-save` fix is enabled, `attemptSaveSubLevel` runs on async thread accessing storageCache. If main thread simultaneously accesses it via `getOrLoadHoldingChunk` → `attemptLoadSubLevel` → `getRegionStorageFile`, the FastUtil linked-map's prev/next pointer state gets corrupted, ultimately throwing `ArrayIndexOutOfBoundsException`.

**修复 / Fix**: 新增 `SubLevelStorageRegionCacheSyncMixin`，用 `synchronized(map)` 保护 `getAndMoveToFirst`、`removeLast`、`putAndMoveToFirst` 三个链表修改操作，确保并发安全。

Added `SubLevelStorageRegionCacheSyncMixin` to protect `getAndMoveToFirst`, `removeLast`, `putAndMoveToFirst` chain-modification operations with `synchronized(map)`, ensuring concurrent safety.

### 兼容性 / Compatibility

- Sable 1.x 和 2.x / Sable 1.x and 2.x
- NeoForge 1.21.1
- Mohist/Youer 混合服务端 / Mohist/Youer hybrid servers
- ScalableLux（光照优化）兼容 / ScalableLux (lighting optimization) compatible
- c2me 兼容 / c2me compatible

## v1.7.23

### async-save 存档卡顿回归修复 / async-save Save Freeze Regression Fix

修复 v1.7.12 起的"存档时服务器卡顿"回归。v1.7.12 在 `saveAll` 返回前 join 所有异步磁盘 IO，导致主线程在每次存档时阻塞等待全部磁盘 IO 完成，表现为存档时服务器明显卡顿。

Fixed the "server freezes during saves" regression introduced in v1.7.12. v1.7.12 joined all async disk IO before `saveAll` returned, blocking the main thread on every save while waiting for all disk IO to finish.

**修复 / Fix**: 改为"非阻塞提交 + 屏障 drain"模式：`saveAll` 只把磁盘 IO 提交到异步线程且不在 `saveAll` 内阻塞；任何后续磁盘读取（`getOrLoadHoldingChunk`）以及下一次 `saveAll` / `close` 之前先 drain 未完成的异步 IO，既避免主线程与异步线程并发访问非线程安全的 `regionCache`/`storageCache`，又让磁盘 IO 与主线程序列化流水线重叠。

Changed to a "non-blocking submit + drain barrier" model: `saveAll` only submits disk IO to the async thread and never blocks inside `saveAll`; before any later disk read (`getOrLoadHoldingChunk`) and before the next `saveAll` / `close`, pending async IO is drained. This keeps the non-thread-safe `regionCache`/`storageCache` from being accessed concurrently while letting disk IO overlap with main-thread serialization.

**感谢 / Thanks**: 感谢 [Variapolis](https://github.com/Variapolis) 的 PR #19 贡献。/ Thanks to [Variapolis](https://github.com/Variapolis) for the PR #19 contribution.

### 兼容性 / Compatibility

- Sable 1.x 和 2.x / Sable 1.x and 2.x
- NeoForge 1.21.1
- Mohist/Youer 混合服务端 / Mohist/Youer hybrid servers
- ScalableLux（光照优化）兼容 / ScalableLux (lighting optimization) compatible
- c2me 兼容 / c2me compatible

## v1.7.22

### world-height-override mixin 注入失败修复 / world-height-override Mixin Injection Failure Fix (re-published)

`world-height-override` 功能经历了两轮修复才最终可用。

`world-height-override` went through two rounds of fixes before working correctly.

**v1.7.21 问题 / v1.7.21 issue**: `WorldHeightOverrideMixin` 拦截 `Level.getMaxBuildHeight()` 失败导致游戏启动崩溃。`getMaxBuildHeight()` 和 `getMinBuildHeight()` 是 `LevelHeightAccessor` 接口的 default 方法，`Level` 类没有 override 它们，所以拦截 `Level` 类找不到目标方法（`Scanned 0 target(s)`）。改为拦截 `LevelHeightAccessor` 接口，通过 `instanceof Level` 检查确保只影响 `Level` 及其子类。

`WorldHeightOverrideMixin` crashed on startup because `getMaxBuildHeight()` and `getMinBuildHeight()` are default methods of `LevelHeightAccessor` interface, not overridden by `Level`, so injecting into `Level` scanned 0 targets. Changed to target `LevelHeightAccessor` interface with `instanceof Level` guard.

**v1.7.22 原始版本问题（已撤回）/ v1.7.22 original issue (withdrawn)**: 改用 `@Mixin(targets = "...")` 字符串 targets 写法后，mixin 框架在 prepare 阶段无法从字符串推断 target 类型，默认按 Standard (class) SubType 处理，`validateTarget` 时因 target 实际是 interface 报 `@Mixin target type mismatch: ... is an interface` 错误。改为 `@Mixin(LevelHeightAccessor.class)` 直接引用接口类对象，让 mixin 框架从 Class 对象自动识别为 Interface SubType。

After switching to `@Mixin(targets = "...")` string syntax, mixin framework could not infer target type from string at prepare phase, defaulted to Standard (class) SubType, and `validateTarget` failed with `@Mixin target type mismatch: ... is an interface`. Changed to `@Mixin(LevelHeightAccessor.class)` direct class reference so mixin framework auto-detects Interface SubType from Class object.

### 兼容性 / Compatibility

- Sable 1.x 和 2.x / Sable 1.x and 2.x
- NeoForge 1.21.1
- Mohist/Youer 混合服务端 / Mohist/Youer hybrid servers
- ScalableLux（光照优化）兼容 / ScalableLux (lighting optimization) compatible
- c2me 兼容 / c2me compatible

## v1.7.12

### async-save PalettedContainer 多线程崩溃修复 / async-save PalettedContainer Multithreading Crash Fix

修复 `async-save` 把整个 `saveAll` 重定向到异步线程，导致 `PalettedContainer.pack` 触发 ThreadingDetector 崩溃的问题。改为统一"主线程序列化 + 异步磁盘 I/O"模式：序列化在主线程执行，磁盘 I/O 提交到异步线程。

Fixed `async-save` redirecting entire `saveAll` to async thread causing `PalettedContainer.pack` ThreadingDetector crash. Changed to unified "main-thread serialization + async disk I/O" model: serialization runs on main thread, disk I/O submitted to async thread.

**为什么序列化必须在主线程 / Why serialization must be on main thread**:

`PalettedContainer` 内部有 ThreadingDetector，不允许跨线程访问。原来的方案把整个 `saveAll`（包括序列化）放异步线程，触发了 ThreadingDetector 崩溃。磁盘 I/O 是真正的阻塞操作（会触发看门狗），序列化是 CPU 操作（不会阻塞线程），所以只把磁盘 I/O 放异步线程即可。

`PalettedContainer` has an internal ThreadingDetector that disallows cross-thread access. The old approach put entire `saveAll` (including serialization) on async thread, triggering ThreadingDetector crash. Disk I/O is a true blocking operation (triggers watchdog), serialization is a CPU operation (doesn't block thread), so only disk I/O needs to be on async thread.

### 整数极限方块破坏坐标防护 / Integer-Overflow Block Destruction Coordinate Guard (Issue #14)

新增 `block-destroy-coordinate-guard` 修复项，防止某些 mod 物品触发方块破坏时坐标计算溢出至 `Integer.MIN_VALUE`/`MAX_VALUE`，导致大量区块被加载、光照更新范围极大，最终服务器卡死或崩溃。

Added `block-destroy-coordinate-guard` fix: prevents server crash when modded items trigger block destruction at integer-overflow coordinates (`Integer.MIN_VALUE`/`MAX_VALUE`), which causes massive chunk loading and lighting updates.

**配置 / Configuration**:

坐标范围可通过 `config/fucksable/config.json` 的 `fixParams.block-destroy-coordinate-guard` 节点调整：

Coordinate limits are configurable via `fixParams.block-destroy-coordinate-guard` in `config/fucksable/config.json`:

```json
"fixParams": {
  "block-destroy-coordinate-guard": {
    "xLimit": 30000000,
    "yMin": -512,
    "yMax": 1024
  }
}
```

如果调整后需要恢复默认值，使用命令 `/fucksable block-destroy-coordinate-guard reset`。

To reset to defaults after adjustment, use command `/fucksable block-destroy-coordinate-guard reset`.

### 修复项参数恢复命令 / Fix Options Reset Command

新增 `/fucksable <fix> reset` 命令，恢复指定修复项的参数到默认值。适用于用户手动修改 `config.json` 中的 `fixParams` 后参数写错、需要恢复的情况。

Added `/fucksable <fix> reset` command to reset a fix's options to defaults. Useful when users manually edit `fixParams` in `config.json` and need to recover from invalid values.

### 兼容性 / Compatibility

- Sable 1.x 和 2.x / Sable 1.x and 2.x
- NeoForge 1.21.1
- Mohist/Youer 混合服务端 / Mohist/Youer hybrid servers
- ScalableLux（光照优化）兼容 / ScalableLux (lighting optimization) compatible
- c2me 兼容 / c2me compatible

## v1.7.10

### ScalableLux 不兼容声明绕过 / ScalableLux Incompatibility Declaration Bypass

使用 NeoForge 官方的 `fml.toml` 依赖覆盖（`[dependencyOverrides]`）机制，绕过 Sable 对 ScalableLux 的 `type = "incompatible"` 声明，避免 NeoForge ModSorter 在启动阶段直接拒绝加载。fs 启动时自动检查并写入 `fml.toml`，无需用户手动配置。

Uses NeoForge's official `fml.toml` dependency override (`[dependencyOverrides]`) mechanism to bypass Sable's `type = "incompatible"` declaration against ScalableLux, preventing NeoForge ModSorter from aborting startup. fs automatically checks and writes `fml.toml` on startup, no manual configuration required.

**如何让 NeoForge 忽略 Sable 对 ScalableLux 的不兼容检查 / How to make NeoForge ignore Sable's incompatibility with ScalableLux**:

fs 启动时会自动在 `<游戏目录>/config/fml.toml` 写入以下配置（若不存在）：

fs automatically writes the following to `<game_dir>/config/fml.toml` on startup (if not present):

```toml
[dependencyOverrides]
sable = ["-scalablelux"]
```

如果你想手动配置，在 `config/fml.toml` 末尾添加上述两行即可。`-scalablelux` 表示移除 Sable 对 ScalableLux 的所有依赖约束（包括 INCOMPATIBLE 声明）。

To configure manually, add the above two lines to the end of `config/fml.toml`. `-scalablelux` removes all dependency constraints from Sable against ScalableLux (including the INCOMPATIBLE declaration).

**注意 / Note**: 如果 fs 和 ScalableLux 同时首次安装，fs 没有机会执行自动写入（游戏在 ModSorter 阶段就崩溃）。此时请先单独启动一次 fs，再安装 ScalableLux。/ If fs and ScalableLux are installed simultaneously for the first time, fs has no chance to auto-write (game crashes at ModSorter stage). In this case, launch the game with fs alone first, then install ScalableLux.

### ScalableLux 兼容性修复项 / ScalableLux Compatibility Fixes

fs 提供两个与 ScalableLux 相关的修复项：

fs provides two ScalableLux-related fixes:

1. **`sable-scalablelux-incompat-bypass`**（默认不启用 / disabled by default）: 绕过 Sable 的 `neoforge.mods.toml` 中对 ScalableLux 的 `type = "incompatible"` 声明，防止 NeoForge ModSorter 在启动阶段报 `Mod sable is incompatible with scalablelux` 并拒绝启动。fs 启动时自动写入 `fml.toml` 的 `[dependencyOverrides]` 配置。/ Bypasses Sable's `type = "incompatible"` declaration against ScalableLux, preventing NeoForge ModSorter from aborting startup with `Mod sable is incompatible with scalablelux`. fs automatically writes the `[dependencyOverrides]` config to `fml.toml` on startup.

2. **`scalablelux-compat`**（默认启用 / enabled by default）: 修复 ScalableLux 存在时 Sable SubLevel 光照完全失效的问题。ScalableLux 清空了主世界 `LevelLightEngine` 的 `blockEngine`/`skyEngine` 字段，导致 Sable 误判 SubLevel 无方块光、无天空光。本修复拦截 `ServerLevelPlot` 构造函数中的 `new LevelLightEngine(...)` 调用，通过 `StarLightInterface.hasBlockLight()/hasSkyLight()` 重新计算正确的光照参数。/ Fixes Sable SubLevel lighting being completely disabled when ScalableLux is installed. ScalableLux clears the `blockEngine`/`skyEngine` fields of the main world `LevelLightEngine`, causing Sable to misjudge SubLevel as having no block light and no sky light. This fix intercepts `new LevelLightEngine(...)` in `ServerLevelPlot` constructor and recalculates correct light parameters via `StarLightInterface.hasBlockLight()/hasSkyLight()`.

**前置依赖关系 / Prerequisite dependency**:

`scalablelux-compat` 依赖 `sable-scalablelux-incompat-bypass`。如果 `scalablelux-compat` 启用但 `sable-scalablelux-incompat-bypass` 未启用，fs 会在日志中输出警告，提醒用户先启用 `sable-scalablelux-incompat-bypass` 并重启服务器。

`scalablelux-compat` depends on `sable-scalablelux-incompat-bypass`. If `scalablelux-compat` is enabled but `sable-scalablelux-incompat-bypass` is not, fs will log a warning reminding the user to enable `sable-scalablelux-incompat-bypass` and restart the server.

**使用步骤 / Usage steps**:

1. 在配置中启用 `sable-scalablelux-incompat-bypass`，重启服务器（fs 会自动写入 `fml.toml` 的依赖覆盖配置）/ Enable `sable-scalablelux-incompat-bypass` in config, restart server (fs will auto-write the `fml.toml` dependency override)
2. 确认 ScalableLux 正常加载后，`scalablelux-compat` 会自动生效（默认已启用）/ After confirming ScalableLux loads correctly, `scalablelux-compat` will work automatically (enabled by default)

### ScalableLux 兼容性 mixin 注入修复 / ScalableLux Compat Mixin Injection Fix

修复 `ScalableLuxCompatMixin` 的 `@At("NEW")` target 格式不正确导致 mixin 扫描 0 个目标，simulated mod 加载时崩溃。将 target 从 `new <类名>(<描述符>)V` 改为纯类名。

Fixed `ScalableLuxCompatMixin` `@At("NEW")` target format causing mixin to scan 0 targets and crash when simulated mod loads. Changed target from `new <class>(<descriptor>)V` to plain class name.

### 控制台刷屏修复 / Console Log Spam Fix

修复无物理结构时 `Attempted to teleport invalid/removed body (id=0), skipping` 警告每 tick 刷屏的问题，警告改为 60 秒节流窗口。

Fixed `Attempted to teleport invalid/removed body (id=0), skipping` warning spamming console every tick when no physics structures exist. Warning now throttled to once per 60s window.

### 兼容性 / Compatibility

- Sable 1.x 和 2.x / Sable 1.x and 2.x
- NeoForge 1.21.1
- Mohist/Youer 混合服务端 / Mohist/Youer hybrid servers
- ScalableLux（光照优化）兼容 / ScalableLux (lighting optimization) compatible
- c2me 兼容 / c2me compatible

## v1.7.9

### ScalableLux 兼容性 mixin `@At("NEW")` target 格式修复 / ScalableLux Compat Mixin `@At("NEW")` Target Format Fix

修复 v1.7.8 中 `ScalableLuxCompatMixin` 的 `@At("NEW")` target 使用了错误的方法描述符格式，导致 mixin 扫描 0 个目标，服务端启动崩溃（Issue #13）。

Fixed v1.7.8 `ScalableLuxCompatMixin` `@At("NEW")` target using incorrect method descriptor format, causing mixin to scan 0 targets and crash server startup (Issue #13).

**问题 / Issue**: `@At("NEW")` 要求 `new <类名>(<参数描述符>)V` 格式，但代码误用了 `L<类名>;<init>(<参数描述符>)V` 方法描述符格式，mixin 无法匹配 `new LevelLightEngine(...)` 调用点，报 `Scanned 0 target(s). No refMap loaded.`。

`@At("NEW")` requires `new <class>(<descriptor>)V` format, but code mistakenly used `L<class>;<init>(<descriptor>)V` method descriptor format, mixin could not match `new LevelLightEngine(...)` call site, reporting `Scanned 0 target(s). No refMap loaded.`.

**修复 / Fix**: 将 target 改为 `new net/minecraft/world/level/lighting/LevelLightEngine(Lnet/minecraft/world/level/chunk/LightChunkGetter;ZZ)V`。

Changed target to `new net/minecraft/world/level/lighting/LevelLightEngine(Lnet/minecraft/world/level/chunk/LightChunkGetter;ZZ)V`.

### 兼容性 / Compatibility

- Sable 1.x 和 2.x / Sable 1.x and 2.x
- NeoForge 1.21.1
- Mohist/Youer 混合服务端 / Mohist/Youer hybrid servers
- ScalableLux（光照优化）兼容 / ScalableLux (lighting optimization) compatible
- c2me 兼容 / c2me compatible

## v1.7.8

### ScalableLux 兼容性 mixin 注入失败修复 / ScalableLux Compat Mixin Injection Failure Fix

修复 v1.7.7 中 `ScalableLuxCompatMixin` 因缺少 `remap = false` 注解导致 NeoForge 启动时 mixin 注入失败崩溃的问题（Issue #10）。

Fixed crash on startup caused by `ScalableLuxCompatMixin` missing `remap = false` annotation in v1.7.7, which made mixin injection fail on NeoForge (Issue #10).

**问题 / Issue**: NeoForge moddev 不生成 refmap，`@At("NEW")` target `new LevelLightEngine(...)` 无法解析，触发 `MixinTransformerError: Critical injection failure: Redirector fucksable$fixLightEngineInit ... Scanned 0 target(s). No refMap loaded`，服务端启动崩溃。

NeoForge moddev does not generate refmap; the `@At("NEW")` target `new LevelLightEngine(...)` could not be resolved, triggering `MixinTransformerError: Critical injection failure: Redirector fucksable$fixLightEngineInit ... Scanned 0 target(s). No refMap loaded` and crashing server startup.

**修复 / Fix**: 在 `@Mixin`、`@At`、`@Redirect` 注解均添加 `remap = false`，并将 `@Mixin(ServerLevelPlot.class)` 改为 `@Mixin(targets = "dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot", remap = false)`。

Added `remap = false` to `@Mixin`, `@At`, and `@Redirect` annotations, and changed `@Mixin(ServerLevelPlot.class)` to `@Mixin(targets = "dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot", remap = false)`.

### 兼容性 / Compatibility

- Sable 1.x 和 2.x / Sable 1.x and 2.x
- NeoForge 1.21.1
- Mohist/Youer 混合服务端 / Mohist/Youer hybrid servers
- ScalableLux（光照优化）兼容 / ScalableLux (lighting optimization) compatible
- c2me 兼容 / c2me compatible

## v1.7.7

### ScalableLux 兼容性修复 / ScalableLux Compatibility Fix

修复 ScalableLux 与 Sable 的光照引擎不兼容问题（Issue #8）。此前 ScalableLux 与 Sable 同时安装时，SubLevel 光照完全失效，导致 C2ME OCL 和 Sable 大部分不兼容。

Fixed ScalableLux incompatibility with Sable's light engine (Issue #8). Previously, when ScalableLux and Sable were installed together, SubLevel lighting was completely disabled, making C2ME OCL and Sable largely incompatible.

**问题 / Issue**: Sable 的 `ServerLevelPlot` 构造函数从主世界 `LevelLightEngine` 读取 `blockEngine`/`skyEngine` 字段判断 SubLevel 是否有方块光/天空光。ScalableLux 的 `LevelLightEngineMixin.construct` 清空了这两个字段（用 `StarLightInterface` 替代 vanilla 光照引擎），导致 Sable 误判 SubLevel 无方块光、无天空光，SubLevel 光照完全失效。

Sable's `ServerLevelPlot` constructor reads the `blockEngine`/`skyEngine` fields of the main world `LevelLightEngine` to determine if the SubLevel has block light / sky light. ScalableLux's `LevelLightEngineMixin.construct` clears these fields (replacing vanilla light engine with `StarLightInterface`), causing Sable to misjudge SubLevel as having no block light and no sky light, completely disabling SubLevel lighting.

**修复 / Fix**: 新增 `scalablelux-compat` 修复项，拦截 `ServerLevelPlot` 构造函数中 `new LevelLightEngine(...)` 调用，当 ScalableLux 存在时通过 `StarLightInterface.hasBlockLight()/hasSkyLight()` 重新计算正确的光照参数。

Added `scalablelux-compat` fix: intercepts `new LevelLightEngine(...)` in `ServerLevelPlot` constructor; when ScalableLux is present, recalculates correct light parameters via `StarLightInterface.hasBlockLight()/hasSkyLight()`.

### 物理结构崩溃修复 / Physics Structure Crash Fix

修复 simulated mod 的 `EndSeaPhysics.physicsTick` 调用 `RigidBodyHandle.getLinearVelocity` 访问已移除的 rapier native body 导致 `RuntimeException: Body has been removed` 崩溃。新增 `RigidBodyHandleMixin`，拦截 `getLinearVelocity` 和 `getAngularVelocity` 方法，try-catch 捕获 `RuntimeException` 并返回零向量，复用 `panic-guard` 修复项开关。

Fixed `RuntimeException: Body has been removed` crash when simulated mod's `EndSeaPhysics.physicsTick` calls `RigidBodyHandle.getLinearVelocity` on a removed rapier native body. Added `RigidBodyHandleMixin`: intercepts `getLinearVelocity` and `getAngularVelocity`, catches `RuntimeException` and returns zero vector, reuses the `panic-guard` fix toggle.

### 配置文件修复 / Config File Fix

修复配置文件不存在时未重新生成的问题，以及执行命令修改配置后未立即保存的问题。

Fixed config not regenerating when missing, and config not saving immediately after command modification.

### 兼容性 / Compatibility

- Sable 1.x 和 2.x / Sable 1.x and 2.x
- NeoForge 1.21.1
- Mohist/Youer 混合服务端 / Mohist/Youer hybrid servers
- ScalableLux（光照优化）兼容 / ScalableLux (lighting optimization) compatible
- c2me 兼容 / c2me compatible

## v1.7.6

### c2me 兼容性修复 / c2me Compatibility Fix

修复 fucksable 的 `async-save` 修复项与 c2me 的 `preventAsyncEntityUnload` mixin 冲突问题。

Fixed conflict between fucksable's `async-save` fix and c2me's `preventAsyncEntityUnload` mixin.

**问题 / Issue**: fucksable 把 `SubLevelHoldingChunkMap.saveAll` 整体放到异步 IO 线程执行，异步线程执行 `processUnloads` → `removeSubLevel` → `removeEntity` 时，c2me 检测到异步线程调用 `ChunkMap.removeEntity` 并抛出 `ConcurrentModificationException: Async entity unload`，导致 sub-level 保存流程中断。

fucksable ran `saveAll` entirely on async IO thread; when the async thread called `processUnloads` → `removeSubLevel` → `removeEntity`, c2me detected async `ChunkMap.removeEntity` call and threw `ConcurrentModificationException: Async entity unload`, interrupting the sub-level save flow.

**修复 / Fix**: 检测 c2me 存在时，`saveAll` 在主线程执行（unload 部分不触发 c2me 冲突），但磁盘 IO（`attemptSaveSubLevel`、`attemptSaveHoldingChunk`）提交到异步 IO 线程执行，`saveAll` 返回前等待所有异步磁盘 IO 完成。

When c2me is present, `saveAll` runs on main thread (unload does not trigger c2me conflict), but disk IO (`attemptSaveSubLevel`, `attemptSaveHoldingChunk`) is submitted to async IO thread; `saveAll` waits for all async disk IO to complete before returning.

### 兼容性 / Compatibility

- Sable 1.x 和 2.x / Sable 1.x and 2.x
- NeoForge 1.21.1
- Mohist/Youer 混合服务端 / Mohist/Youer hybrid servers
- c2me（Concurrent Chunk Management Engine）兼容 / c2me compatible

## v1.7.5

### 变更 / Changes

代码现在开源到 [XSY-Team/fuck-sable](https://github.com/XSY-Team/fuck-sable) 组织仓库，与个人仓库 [OLKMO/FuckSable-Unofficial](https://github.com/OLKMO/FuckSable-Unofficial) 同步维护。

Code is now open-sourced to the [XSY-Team/fuck-sable](https://github.com/XSY-Team/fuck-sable) org repo, maintained in sync with the personal repo [OLKMO/FuckSable-Unofficial](https://github.com/OLKMO/FuckSable-Unofficial).

- **jar 产物重命名 / jar artifact renamed**: `FuckSable-Unofficial-x.x.x.jar` → `fuck-sable-x.x.x.jar`，与新仓库名一致。/ Renamed to match the new repo name.
- **UpdateChecker 双源查询 / UpdateChecker dual-source query**: 更新检查器现在同时查询 `OLKMO/FuckSable-Unofficial` 和 `XSY-Team/fuck-sable` 两个仓库的 latest release，取版本号更高的作为更新提示。单个仓库查询失败不影响另一个。/ Update checker now queries both repos for the latest release and uses the higher version. Failure of one query does not affect the other.
- **README 链接调整 / README link updates**: README 中的图片 URL 和 Releases 链接主链接改为 `XSY-Team/fuck-sable`，同时保留 `OLKMO/FuckSable-Unofficial` 作为备用下载源。/ README links now point to `XSY-Team/fuck-sable` as main, with `OLKMO/FuckSable-Unofficial` as backup.

### 注意 / Note

v1.7.5 同时在 `XSY-Team/fuck-sable` 和 `OLKMO/FuckSable-Unofficial` 两个仓库发布，内容一致。

v1.7.5 is released on both `XSY-Team/fuck-sable` and `OLKMO/FuckSable-Unofficial` repos with identical content.

## v1.7.4

### 修复 / Bug Fix

修复 v1.7.3 中 `entity-lookup-remove-guard` mixin 注入失败导致服务器启动崩溃的问题。

Fix `entity-lookup-remove-guard` mixin injection failure that crashed server startup in v1.7.3.

### 崩溃信息 / Crash

```
org.spongepowered.asm.mixin.transformer.throwables.MixinTransformerError
Caused by: InjectionError: Critical injection failure:
  Redirector fucksable$safeEntityLookupRemove(Lnet/minecraft/world/level/entity/EntityLookup;Lnet/minecraft/world/entity/Entity;)V
  in fucksable.mixins.json:PersistentEntitySectionManagerStopTrackingGuardMixin
  failed injection check, (0/1) succeeded. Scanned 0 target(s).
```

### 根因 / Root Cause

`@At` 的 target 描述符错误地使用了 `Entity` 作为参数类型，但 `EntityLookup<T extends EntityAccess>` 的 `remove(T)` 和 `PersistentEntitySectionManager.stopTracking(T)` 在编译后由于泛型擦除，实际签名是 `(Lnet/minecraft/world/level/entity/EntityAccess;)V`。mixin 找不到匹配的调用点（Scanned 0 targets），导致 `MixinTransformerError` 让整个服务端启动崩溃。

The `@At` target descriptor incorrectly used `Entity` as the parameter type, but `EntityLookup<T extends EntityAccess>.remove(T)` and `PersistentEntitySectionManager.stopTracking(T)` are erased to `(Lnet/minecraft/world/level/entity/EntityAccess;)V` at compile time due to Java generics erasure. Mixin could not find any matching INVOKE site (Scanned 0 targets), causing `MixinTransformerError` and crashing the server at startup.

### 修复方式 / How

- target 描述符改为 `(Lnet/minecraft/world/level/entity/EntityAccess;)V`
- handler 方法参数改为 `EntityAccess`
- Change target descriptor to `(Lnet/minecraft/world/level/entity/EntityAccess;)V`
- Change handler parameter type to `EntityAccess`

### 注意 / Note

v1.7.3 是有问题的版本，请勿使用，请直接升级到 v1.7.4。

v1.7.3 is broken, please skip it and use v1.7.4 instead.

## v1.7.3

## 新增修复 / New Fix: `entity-lookup-remove-guard`

拦截 `PersistentEntitySectionManager.stopTracking` 中的 `EntityLookup.remove` 调用，捕获 `Int2ObjectLinkedOpenHashMap.fixPointers` 抛出的 `ArrayIndexOutOfBoundsException`，避免单个实体移除失败导致整个服务器 tick 崩溃。

Catches `ArrayIndexOutOfBoundsException` thrown by `Int2ObjectLinkedOpenHashMap.fixPointers` inside `EntityLookup.remove` during `PersistentEntitySectionManager.stopTracking`, preventing single-entity removal failures from crashing the server tick loop.

### 修复的崩溃 / Crash being fixed

```
java.lang.ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 513
    at it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap.fixPointers(Int2ObjectLinkedOpenHashMap.java:979)
    at it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap.removeEntry(Int2ObjectLinkedOpenHashMap.java:263)
    at it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap.remove(Int2ObjectLinkedOpenHashMap.java:372)
    at net.minecraft.world.level.entity.EntityLookup.remove(EntityLookup.java:52)
    at net.minecraft.world.level.entity.PersistentEntitySectionManager.stopTracking(PersistentEntitySectionManager.java:157)
    ...
    at net.minecraft.server.MinecraftServer.tickServer(MinecraftServer.java:1383)
```

### 根因 / Root cause

Sable 的 SubLevel 实体管理（跨维度/多线程）破坏了 `EntityLookup` 内部 `Int2ObjectLinkedOpenHashMap` 的链表状态。当某个 entry 的 `prev`/`next` 指针被设为 `-1`（哨兵值表示"无链接"）却被当作数组下标访问时，`fixPointers` 抛出 `Index -1 out of bounds for length N`。异常沿 `PersistentEntitySectionManager.stopTracking` → `updateChunkStatus` → `ChunkMap.onFullChunkStatusChange` → `ChunkHolder.demoteFullChunk` → `MinecraftServer.tickServer` 一路传播，导致 tick 循环崩溃。

Sable's SubLevel entity management (cross-dimension / multi-threaded) corrupts the internal linked-map state of `EntityLookup`'s backing `Int2ObjectLinkedOpenHashMap`. When an entry's `prev` / `next` pointer is set to `-1` (sentinel meaning "no link") but later accessed as an array index, `fixPointers` throws `Index -1 out of bounds for length N`. The exception propagates up through `PersistentEntitySectionManager.stopTracking` -> `updateChunkStatus` -> `ChunkMap.onFullChunkStatusChange` -> `ChunkHolder.demoteFullChunk` -> `MinecraftServer.tickServer`, crashing the tick loop.

### 实现方式 / How

- 在 `PersistentEntitySectionManager.stopTracking` 中的 `EntityLookup.remove` 调用上加 `@Redirect`
- 在调用点捕获 `ArrayIndexOutOfBoundsException`（以及其他 `Throwable`）
- 每次发生时输出一条 `WARN` 日志（包含实体引用），便于排查
- tick 继续正常运行，仅跳过出错实体的移除操作

- `@Redirect` on the `EntityLookup.remove` invocation inside `PersistentEntitySectionManager.stopTracking`
- Catches `ArrayIndexOutOfBoundsException` (and any other `Throwable`) at the call site
- Emits a single `WARN` log per occurrence with the entity reference for diagnosis
- Tick continues normally; only the offending entity's removal is skipped

### 注意事项 / Caveats

这是**治标修复**。它让服务器保持运行但不会修复 `Int2ObjectLinkedOpenHashMap` 的底层状态——损坏的 entry 仍然存在，可能在后续 `remove` 调用中再次出现。真正的修复应在 Sable 的实体卸载 / SubLevel 实体追踪代码中（参见 `sable.mixins.json:entity.entity_unloading.PersistentEntitySectionManagerMixin` 和 `sable.mixins.json:entity.server_entities_tick.ChunkMapMixin`）。

This is a **symptomatic** fix. It keeps the server alive but does not repair the underlying `Int2ObjectLinkedOpenHashMap` state — the corrupted entry remains and may surface again on subsequent `remove` calls. The true fix belongs in Sable's entity unloading / SubLevel entity tracking code (see `sable.mixins.json:entity.entity_unloading.PersistentEntitySectionManagerMixin` and `sable.mixins.json:entity.server_entities_tick.ChunkMapMixin`).

### 兼容性 / Compatibility

- Mixin 目标 / Mixin target: `net.minecraft.world.level.entity.PersistentEntitySectionManager`
- Redirect 目标 / Redirect target: `stopTracking` 中的 `EntityLookup.remove(Entity)` 调用 / `EntityLookup.remove(Entity)` invocation in `stopTracking`
- Sable 的 `PersistentEntitySectionManagerMixin` 使用 `@Inject` 注入在不同方法（`processChunkUnload`）上，因此不会发生 handler 冲突 / Sable's `PersistentEntitySectionManagerMixin` uses `@Inject` on a different method (`processChunkUnload`), so there is no handler conflict.

## v1.7.2 - UDP 无效数据包防护 / UDP Invalid Packet Guard

### 新增修复 / New Fix: `udp-invalid-packet-guard`

在 `SableUDPPacketDecoder.decode` 头部静默丢弃 packet ID 越界的 UDP 数据包（如旧版 Minecraft 服务器列表 ping 的 packet ID 254），而不是让 Sable 抛出 `IOException("Received an invalid packet ID: 254")`。

Silently drops UDP packets with invalid packet IDs (e.g. legacy Minecraft server list ping packet ID 254) at the head of `SableUDPPacketDecoder.decode`, instead of letting Sable throw `IOException("Received an invalid packet ID: 254")`.

### 原因 / Why

没有这个防护时，Sable 读取第一个字节作为 packet ID，发现它 `>= SableUDPPacketType.VALUES.length`，抛出 `IOException`。异常沿 Netty pipeline 作为 `DecoderException` 向上传播，被 Sable 的 channel handler 捕获后输出 `Server UDP channel caught exception` ERROR 日志——每当有人 ping 或扫描 Sable 的 UDP 端口时都会重复刷屏。

Without this guard, Sable reads the first byte as a packet ID, sees it is `>= SableUDPPacketType.VALUES.length`, and throws `IOException`. The exception propagates up the Netty pipeline as a `DecoderException`, gets caught by Sable's channel handler, and produces a recurring `Server UDP channel caught exception` ERROR log entry every time someone pings or scans the Sable UDP port.

### 实现方式 / How

- 在 `decode` HEAD 处 `@Inject`
- `@Inject` at `decode` HEAD
- peek 第一个字节，不消费 `readerIndex`
- Peek the first byte without consuming `readerIndex`
- 若 packet ID 超过 `SableUDPPacketType.VALUES.length`，cancel decode 调用
- If packet ID exceeds `SableUDPPacketType.VALUES.length`, cancel the decode call
- 合法 ID 上界通过反射从 `SableUDPPacketType.VALUES` 读取（首次调用后缓存），反射失败时回退到 5（Sable 1.2.2 有 6 个 packet type）
- Valid ID upper bound is read via reflection from `SableUDPPacketType.VALUES` (cached after first call), falls back to 5 (Sable 1.2.2 has 6 packet types) if reflection fails

## v1.7.1

### Bug 修复 / Bug Fixes

修复在 Mohist/Youer 1.21.1 专用服务端上由 `ReEntrantTransformerError: Re-entrance error` in `FuckSableMixinConfigPlugin` 引起的服务器启动崩溃：

Fix server startup crash on **Mohist/Youer 1.21.1** dedicated servers caused by `ReEntrantTransformerError: Re-entrance error` in `FuckSableMixinConfigPlugin`:

- **`ArtifactVersion.compareTo` 的 `NoSuchMethodException`**: 在混合服务端（Mohist/Youer）上，`ArtifactVersion` 的 `compareTo` 是 `Comparable<ArtifactVersion>` 桥接方法，参数类型被擦除为 `Object`，所以 `getMethod("compareTo", artifactVersionClass)` 找不到它。改为直接使用 `((Comparable) version).compareTo(threshold)` 通过 JVM 多态派发。/ **`NoSuchMethodException` on `ArtifactVersion.compareTo`**: on hybrid servers (Mohist/Youer), `ArtifactVersion`'s `compareTo` is a `Comparable<ArtifactVersion>` bridge method whose erased parameter type is `Object`, so `getMethod("compareTo", artifactVersionClass)` failed to find it. Replaced with a direct `((Comparable) version).compareTo(threshold)` call dispatched via JVM polymorphism.

- **`detectByClassSignature` 中的 `ReEntrantTransformerError`**: fallback 使用 `Class.forName("dev.ryanhcode.sable.physics.impl.rapier.RapierPhysicsPipeline")` 在 mixin prepare 阶段重新进入了 mixin transformer（在 transformer 仍在准备时加载了一个被 mixin 处理的类）。重写 `detectByClassSignature` 使用 `ClassLoader.getResourceAsStream` + ASM `ClassReader` 直接从字节码解析方法描述符，不触发任何类加载。/ **`ReEntrantTransformerError` in `detectByClassSignature`**: the fallback used `Class.forName("dev.ryanhcode.sable.physics.impl.rapier.RapierPhysicsPipeline")` during the mixin prepare phase, which re-entered the mixin transformer (loading a mixin-processed class while the transformer was still preparing). Rewrote `detectByClassSignature` to use `ClassLoader.getResourceAsStream` + ASM `ClassReader` to parse method descriptors directly from class bytecode, without triggering any class loading.

## v1.7.0

`OLKMO/FuckSable-Unofficial` GitHub 仓库的首次发布。汇总了 1.6.11–1.6.14 期间所有未发布的修复，并新增跨版本 Sable 1.x/2.x 支持。

First release on the `OLKMO/FuckSable-Unofficial` GitHub repository. Rolls up all unreleased fixes from 1.6.11–1.6.14 plus cross-version Sable 1.x/2.x support.

### 重大变更 / Major Changes

- **跨版本 Sable 支持 / Cross-version Sable support**: 重写 `FuckSableMixinConfigPlugin` 版本检测。改用 `ModList.getMods()` 在运行时读取 Sable mod 版本，并添加类签名 fallback 检查 `RapierPhysicsPipeline.addConstraint` 参数类型。修复了 `NoSuchMethodException: ModFileInfo.getModInfos()` bug——该 bug 在所有 Sable 版本上都会静默禁用 V1/V2 自约束修复 mixin。/ Rewritten `FuckSableMixinConfigPlugin` version detection. Now uses `ModList.getMods()` to read the Sable mod version at runtime, with a class-signature fallback that inspects `RapierPhysicsPipeline.addConstraint` parameter types. Fixes the `NoSuchMethodException: ModFileInfo.getModInfos()` bug that silently disabled both V1/V2 constraint self-fix mixins on every Sable version.

- **按版本自适应的约束自修复 / Version-specific constraint self-fix**: 新增 `RapierConstraintSelfFixMixinV1`（Sable 1.x，`ServerSubLevel` 参数）和 `RapierConstraintSelfFixMixinV2`（Sable 2.x，`PhysicsPipelineBody` 参数）。正确的 mixin 由上面的插件自动选择，因此单个 FuckSable 构建现在可同时在 Sable 1.2.x 和 2.0.x 上运行，无需编译时依赖。/ Added `RapierConstraintSelfFixMixinV1` (Sable 1.x, `ServerSubLevel` params) and `RapierConstraintSelfFixMixinV2` (Sable 2.x, `PhysicsPipelineBody` params). The correct mixin is auto-selected by the plugin above, so a single FuckSable build now runs on both Sable 1.2.x and 2.0.x without compile-time dependencies.

### 新增修复 / New Fixes

- `ServerLevelSendBlockUpdateMixin`: 当目标 plot holder 不存在时取消 `sendBlockUpdated` 调用。防止在 Sable 2.0.x 上出现 `UnsupportedOperationException: Cannot change blocks in nonexistent plot holder` 崩溃。/ Cancels `sendBlockUpdated` when the target plot holder is missing. Prevents `UnsupportedOperationException: Cannot change blocks in nonexistent plot holder` crash on Sable 2.0.x.

- `SubLevelStorageLogSpamMixin`: 将 "Couldn't find sub-level at index N" ERROR 日志限流为同一 chunk+index 每 60 秒输出一次。避免 sub-level 存储条目损坏时日志刷屏。/ Throttles "Couldn't find sub-level at index N" ERROR log to once per 60s per chunk+index. Stops log flooding when sub-level storage entries are corrupted.

- `FrogportItemExtractLimitMixin`: 当相邻库存槽位数超过 256 时跳过 `ItemHelper.extract`。防止 FrogportBlockEntity 扫描超大型漏斗链 / Create 仓库导致服务器卡死数秒。/ Skips `ItemHelper.extract` when adjacent inventory exceeds 256 slots. Prevents multi-second server freezes caused by FrogportBlockEntity scanning huge hopper chains / Create warehouses.

- `CttPostTickTimeoutGuardMixin`: 在 CTT `postTick` 中给 `Future.get()` 加 10 秒超时。若异步火车工作线程卡住（例如 Sable 物理自约束死循环），future 会被取消并打 WARN 日志，而不是让主线程挂起触发 Watchdog 崩溃。/ 10s timeout on `Future.get()` in CTT `postTick`. If the async train worker is stuck (e.g. Sable physics self-constraint loop), the future is cancelled and a warning is logged instead of hanging the main thread and triggering a Watchdog crash.

### 变更 / Changes

- `PlayerPositionGuardMixin`: 世界边界钳制放宽到 ±5（原 +1）。Y 轴钳制改为仅创造模式生效——生存模式玩家正常坠落，创造模式玩家被拉回 `minBuildHeight + 5` 之上。/ World-border clamp relaxed to ±5 (was +1). Y-axis clamp is now creative-only — survival players fall normally, creative players are pulled back above `minBuildHeight + 5`.

- 更新检查器现在查询 `OLKMO/FuckSable-Unofficial` Releases API。/ Update checker now queries the `OLKMO/FuckSable-Unofficial` Releases API.

- 构建产物重命名为 `FuckSable-Unofficial-1.7.0.jar`。/ Built jar is now named `FuckSable-Unofficial-1.7.0.jar`.

## v1.6.14

### Bug Fixes
- Fix `FuckSableMixinConfigPlugin` version detection: use `ModList.getMods()` + class signature fallback (fixes `NoSuchMethodException` that disabled both V1/V2 constraint self-fix mixins)

### New Fixes
- Add `ctt-posttick-timeout-guard`: 10s timeout on `Future.get()` in CTT `postTick` to prevent Watchdog server crash
- Add `RapierConstraintSelfFixMixinV1/V2`: version-specific mixins for Sable 1.x and 2.x `addConstraint`, auto-selected by `FuckSableMixinConfigPlugin`

## v1.6.13

### New Fixes
- Add `frogport-extract-limit`: skip `ItemHelper.extract` when adjacent inventory exceeds 256 slots to prevent server freeze

### Changes
- Update `player-position-guard`: clamp to world border+5, creative-only Y-axis clamp (survival falls normally)

## v1.6.12

### New Fixes
- Add `sublevel-load-log-spam-fix`: throttle "Couldn't find sub-level" ERROR log to once per 60s per chunk+index

## v1.6.11

### New Fixes
- Add `ServerLevelSendBlockUpdateMixin`: cancel `sendBlockUpdated` when plot holder missing to prevent crash on Sable 2.0.x

## v1.6.10

### Bug Fixes
- Prevent server crash when `TrackGraph.removeNode` triggers `Train.detachFromTracks` on a train with corrupted state (null `TravellingPoint.edge`): skips `TrainMigration` creation for points with null edge instead of throwing `NullPointerException` in `TrainMigration` constructor

## v1.6.9

### Bug Fixes
- Prevent server crash when Create train navigation searches with a null TrackNode (corrupted train state from CTT concurrent issues): `TrackGraph.getConnectionsFrom` returns empty Map instead of null to avoid NullPointerException in `Navigation.search`

## v1.6.8

### Bug Fixes
- Fix Vista camera chunk loading incompatibility with Sable physics structures: project ViewFinder SubLevel coordinates to world coordinates before force-loading chunks, preventing TPS drop and infinite loading loops
- Fix `SteamVentValueBoxTransformMixin` crash on Aeronautics 1.3.0+: remove `@Shadow direction` field (removed in upstream), use reflection to set direction field for cross-version compatibility

## v1.6.7

### Bug Fixes
- Fix CTT log spam fix mixin crash: correct `Logger.warn` target signature from `(String, Object)` to `(String, Throwable)`
- Fix `RapierPhysicsPipelineMixin` crash: remove unused `poseCache` `@Shadow` field that doesn't exist on some Sable versions
- Fix startup animation character misalignment

## v1.6.6

### Bug Fixes
- Suppress repeated CTT (CreateThreadedTrains) warning logs when train calculation fails — only logs once per error type

## v1.6.5

### Bug Fixes
- Fix physics structures spamming logs when repeatedly out of world bounds — now only warns once per SubLevel, silences subsequent clamps

### Changes
- Add Discord community link to README

## v1.6.4

### Bug Fixes
- Fix `ParticleEngine.crack()` method signature mismatch causing client crash (fixes #1)
- Fix `RapierPhysicsPipelineMixin` crash: `sceneId` field removed, `cache` renamed to `poseCache` in Sable 2.0.2+ (fixes #2)
- Update `SteamVentValueBoxTransformMixin` to also cover `fromSide` method for Aeronautics compat (fixes #3)

### Changes
- Change 18 fix entries from `Side.SERVER` to `Side.BOTH` so fixes also work in singleplayer (integrated server)

## v1.6.3

### Bug Fixes
- Fix crash on Sable 2.0.2+: `@Shadow field sceneId was not located in RapierPhysicsPipeline` (field removed in upstream)
- Remove fstemp3/fsban/fslook features (conflicted with core functionality)
- Restore auto-update to config-controlled behavior

## v1.6.0

### Breaking Changes
- Remove 16 low-impact performance optimization mixins to reduce compatibility risks
- Add FixEntry.Side mechanism (SERVER/CLIENT/BOTH) so fixes only apply on their target side

### New Fixes
- Light engine bounds guard: prevent crash when SubLevel sections exceed world height limits
- Player position guard: clamp player position to world border when coordinates exceed boundaries
- Physics ticket guard: prevent crash from DistanceManager internal state corruption
- Copycats compat: prevent crash when Copycats blocks missing facing property
- SubLevel entity getter guard: prevent server freeze from abnormally large AABBs

### Bug Fixes
- Fix typewriter sneak-click not opening config GUI (@Overwrite -> @WrapMethod)
- Add try-catch to CarryOn placement/teleport projection to prevent crashes
