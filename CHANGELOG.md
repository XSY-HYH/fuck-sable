# Changelog

All notable changes to FuckSable will be documented in this file.

## [1.7.20] - 2026-08-04

### 重构 / Refactor

- **统一日志节流代理 / Unified log throttle proxy**: 创建 `ThrottledLogger` 统一输出代理类，按 key 限制日志输出频率（默认 60 秒窗口），支持永久去重（`warnOnce`）和转发到外部 Logger（`warnTo`/`errorTo`）。删除各 mixin 中分散的节流字段和逻辑，统一改为调用 `ThrottledLogger`。/ Created `ThrottledLogger` unified output proxy class, throttles log output by key (default 60s window), supports permanent dedup (`warnOnce`) and forwarding to external Logger (`warnTo`/`errorTo`). Removed scattered throttle fields and logic across mixins, unified to use `ThrottledLogger`.
  - 改造的文件 / Modified files: `RapierPhysicsPipelineMixin`, `LevelDestroyBlockGuardMixin`, `FrogportItemExtractLimitMixin`, `SubLevelStorageLogSpamMixin`, `CttLogSpamFixMixin`, `RapierConstraintSelfFixMixinV1`, `RapierConstraintSelfFixMixinV2`
  - `RapierPhysicsPipelineMixin` 中之前未节流的 warn 调用（`readPose`、`addVelocity`、`applyImpulse`、`applyForce`、`wakeUp`、`onStatsChanged`）现在也通过 `ThrottledLogger` 节流。/ Previously unthrottled warn calls in `RapierPhysicsPipelineMixin` (`readPose`, `addVelocity`, `applyImpulse`, `applyForce`, `wakeUp`, `onStatsChanged`) are now also throttled via `ThrottledLogger`.

## [1.7.19] - 2026-08-04

### Bug 修复 / Bug Fixes

- **velocity 查询警告刷屏修复 / velocity query warning spam fix**: 修复 `getLinearVelocity` 和 `getAngularVelocity` 在访问无效/已移除的物理 body 时每 tick 输出警告导致控制台刷屏的问题。改为 60 秒节流窗口，窗口内只输出一次警告，和 `teleport` 警告的处理方式一致。/ Fixed `getLinearVelocity` and `getAngularVelocity` warning spamming console every tick when accessing invalid/removed physics bodies. Changed to 60s throttle window, only logging once per window, consistent with the `teleport` warning handling.

## [1.7.18] - 2026-08-01

### Bug 修复 / Bug Fixes

- **i18n 中文乱码修复 / i18n Chinese garbled text fix**: 修复 `LanguageManager` 读写语言文件时未指定 UTF-8 字符集导致中文乱码的问题。/ Fixed `LanguageManager` not specifying UTF-8 charset when reading/writing language files, causing garbled Chinese text.

### 变更 / Changes

- **硬编码中文国际化 / Hardcoded Chinese i18n**: 将 `FuckSableCommand`、`LevelSetBlockMonitorMixin`、`BlockUpdateMonitorMixin` 中的硬编码中文输出改为 i18n 调用，新增对应的翻译键到 `zh.yml` 和 `en.yml`（语言包版本升至 1.3）。/ Replaced hardcoded Chinese strings in `FuckSableCommand`, `LevelSetBlockMonitorMixin`, `BlockUpdateMonitorMixin` with i18n calls, added corresponding translation keys to `zh.yml` and `en.yml` (lang pack version bumped to 1.3).

## [1.7.17] - 2026-08-01

### Bug 修复 / Bug Fixes

- **`/fucksable all reset` 命令修复 / `/fucksable all reset` command fix**: 修复 `resetFixOptions` 方法漏了处理 `all` 参数导致提示"all不是配置项"的问题。现在 `/fucksable all reset` 会遍历所有修复项并重置 options 到默认值。/ Fixed `resetFixOptions` method not handling the `all` argument, causing "all is not a valid fix" error. Now `/fucksable all reset` iterates all fixes and resets their options to defaults.

## [1.7.16] - 2026-08-01

### 变更 / Changes

- **配置文件自动迁移 / Config file auto-migration**: 修复从老版本升级时配置文件不会自动补全新增选项的问题。现在每次启动都会重新保存配置文件，确保新增的可配置项（如 `yMaxMargin`）自动写入已有配置文件，保留用户已有的配置值。/ Fixed config file not auto-populating new options when upgrading from older versions. Config file is now re-saved on every startup to ensure new configurable options (e.g. `yMaxMargin`) are automatically written to existing config files, preserving user's existing values.
  - 之前只在配置文件不存在时才重新生成。/ Previously only regenerated when config file was missing.

## [1.7.15] - 2026-08-01

### 变更 / Changes

- **player-position-guard 与 physics-staff-drag-clipfix Y 轴上限可配置化 / player-position-guard and physics-staff-drag-clipfix Y-axis upper bound configurable (Issue #17)**: 将两个修复项中硬编码的 `getMaxBuildHeight() + 1000` 改为可通过配置文件调整的 `yMaxMargin` 选项（默认 1000）。/ Changed the hardcoded `getMaxBuildHeight() + 1000` in both fixes to a configurable `yMaxMargin` option (default 1000) via config file.
  - 适用于调整了物理参数导致玩家/物理结构会飞到较高 Y 轴的服务器。/ Suitable for servers where physics tweaks cause players/structures to fly to higher Y levels.
  - 配置路径：`config/fucksable/config.json` 的 `fixParams.player-position-guard.yMaxMargin` 和 `fixParams.physics-staff-drag-clipfix.yMaxMargin`。/ Config path: `fixParams.player-position-guard.yMaxMargin` and `fixParams.physics-staff-drag-clipfix.yMaxMargin` in `config/fucksable/config.json`.
  - 可通过 `/fucksable <fix> reset` 命令恢复默认值。/ Can reset to default via `/fucksable <fix> reset` command.

## [1.7.14] - 2026-08-01

### 变更 / Changes

- **ScalableLux 兼容性光照修复 / ScalableLux compat lighting fix (PR #16)**: 修复 `ScalableLuxCompatMixin` 中 `chunkGetter instanceof Level` 判断在 SubLevel 场景下失败导致 ScalableLux 兼容修复逻辑不执行、SubLevel 光照失效的问题。SubLevel 传入的 `LightChunkGetter` 本身可能不是 `Level` 实例（而是包装器），但 `getLevel()` 返回真正的 `Level`。改为 `chunkGetter.getLevel() instanceof Level` 后能正确识别。/ Fixed `ScalableLuxCompatMixin` `chunkGetter instanceof Level` check failing in SubLevel scenarios, causing ScalableLux compat logic to be skipped and SubLevel lighting to break. The `LightChunkGetter` passed for SubLevels may not be a `Level` instance itself (it's a wrapper), but `getLevel()` returns the real `Level`. Changed to `chunkGetter.getLevel() instanceof Level` for correct detection.
  - 感谢 [Variapolis](https://github.com/Variapolis) 的贡献。/ Thanks to [Variapolis](https://github.com/Variapolis) for the contribution.

## [1.7.13] - 2026-07-31

### 变更 / Changes

- **block-destroy-coordinate-guard mixin 注入失败修复 / block-destroy-coordinate-guard mixin injection failure fix (Issue #15)**: 修复 `LevelDestroyBlockGuardMixin` 中 `destroyBlock` 方法签名错误导致 mixin 注入失败、游戏启动崩溃的问题。1.21.1 中 `Level.destroyBlock` 的真实签名是 4 个参数 `destroyBlock(BlockPos, boolean, Entity, int)`，之前错误地使用了 3 个参数的签名。/ Fixed `LevelDestroyBlockGuardMixin` crash caused by incorrect `destroyBlock` method signature. In 1.21.1, `Level.destroyBlock` has 4 parameters `destroyBlock(BlockPos, boolean, Entity, int)`, but the mixin incorrectly used a 3-parameter signature.

## [1.7.12] - 2026-07-31

### 变更 / Changes

- **async-save PalettedContainer 多线程崩溃修复 / async-save PalettedContainer multithreading crash fix**: 修复 `async-save` 把整个 `saveAll` 重定向到异步线程导致 `PalettedContainer.pack` 触发 ThreadingDetector 崩溃的问题。改为统一"主线程序列化 + 异步磁盘 I/O"模式：序列化（`PalettedContainer.pack` → NBT 编码）在主线程执行，磁盘 I/O（`attemptSaveSubLevel`/`attemptSaveHoldingChunk`）提交到异步线程执行。/ Fixed `async-save` redirecting entire `saveAll` to async thread causing `PalettedContainer.pack` ThreadingDetector crash. Changed to unified "main-thread serialization + async disk I/O" model: serialization (`PalettedContainer.pack` → NBT encoding) runs on main thread, disk I/O (`attemptSaveSubLevel`/`attemptSaveHoldingChunk`) submitted to async thread.
  - 移除 c2me 特殊处理，统一行为。/ Removed c2me special handling, unified behavior.

- **整数极限方块破坏坐标防护 / Integer-overflow block destruction coordinate guard (Issue #14)**: 新增 `block-destroy-coordinate-guard` 修复项，拦截 `Level.setBlock` 和 `Level.destroyBlock`，当坐标超出可配置范围（默认 X/Z ±30M, Y -512~1024）时跳过操作并记录警告（60 秒节流）。/ Added `block-destroy-coordinate-guard` fix: intercepts `Level.setBlock` and `Level.destroyBlock`, skips operations when coordinates exceed configurable limits (default X/Z ±30M, Y -512~1024) and logs throttled warnings (60s).
  - 坐标范围可通过 `config/fucksable/config.json` 的 `fixParams.block-destroy-coordinate-guard` 节点调整。/ Coordinate limits are configurable via `fixParams.block-destroy-coordinate-guard` in `config/fucksable/config.json`.

- **修复项参数恢复命令 / Fix options reset command**: 新增 `/fucksable <fix> reset` 命令，恢复指定修复项的参数到默认值。/ Added `/fucksable <fix> reset` command to reset a fix's options to defaults.

## [1.7.10] - 2026-07-31

### 变更 / Changes

- **ScalableLux 不兼容声明绕过 / ScalableLux incompatibility declaration bypass**: 使用 NeoForge 官方的 `fml.toml` 依赖覆盖（`[dependencyOverrides]`）机制，绕过 Sable 的 `neoforge.mods.toml` 中对 ScalableLux 的 `type = "incompatible"` 声明，避免 NeoForge ModSorter 在启动阶段直接拒绝加载。/ Uses NeoForge's official `fml.toml` dependency override (`[dependencyOverrides]`) mechanism to bypass Sable's `type = "incompatible"` declaration against ScalableLux in `neoforge.mods.toml`, preventing NeoForge ModSorter from aborting startup.
  - fs 启动时自动检查并写入 `fml.toml`，无需用户手动配置。/ fs automatically checks and writes `fml.toml` on startup, no manual configuration required.
  - 替代了 v1.7.9 中复杂且易出问题的 CoreMod 方案。/ Replaces the complex and error-prone CoreMod approach from v1.7.9.

- **ScalableLux 兼容性 mixin 注入修复 / ScalableLux compat mixin injection fix**: 修复 `ScalableLuxCompatMixin` 的 `@At("NEW")` target 格式不正确导致 mixin 扫描 0 个目标，simulated mod 加载时崩溃（`Scanned 0 target(s)`）。/ Fixed `ScalableLuxCompatMixin` `@At("NEW")` target format causing mixin to scan 0 targets and crash when simulated mod loads (`Scanned 0 target(s)`).
  - 将 target 从 `new <类名>(<描述符>)V` 改为纯类名 `net/minecraft/world/level/lighting/LevelLightEngine`。/ Changed target from `new <class>(<descriptor>)V` to plain class name `net/minecraft/world/level/lighting/LevelLightEngine`.

- **控制台刷屏修复 / Console log spam fix**: 修复无物理结构时 `Attempted to teleport invalid/removed body (id=0), skipping` 警告每 tick 刷屏的问题。/ Fixed `Attempted to teleport invalid/removed body (id=0), skipping` warning spamming console every tick when no physics structures exist.
  - 警告改为 60 秒节流窗口，窗口内只输出一次。/ Warning now throttled to once per 60s window.

## [1.7.9] - 2026-07-31

### Bug 修复 / Bug Fixes

- **ScalableLux 兼容性 mixin `@At("NEW")` target 格式修复 / ScalableLux compat mixin `@At("NEW")` target format fix**: 修复 v1.7.8 中 `ScalableLuxCompatMixin` 的 `@At("NEW")` target 使用了错误的方法描述符格式 `L<类名>;<init>(<参数描述符>)V`，导致 mixin 扫描 0 个目标（`Scanned 0 target(s). No refMap loaded.`），服务端启动崩溃（Issue #13）。/ Fixed v1.7.8 `ScalableLuxCompatMixin` `@At("NEW")` target using incorrect method descriptor format `L<class>;<init>(<descriptor>)V`, causing mixin to scan 0 targets (`Scanned 0 target(s). No refMap loaded.`) and crash server startup (Issue #13).
  - **问题 / Issue**: `@At("NEW")` 要求 `new <类名>(<参数描述符>)V` 格式，但代码误用了 `L<类名>;<init>(<参数描述符>)V` 方法描述符格式，mixin 无法匹配 `new LevelLightEngine(...)` 调用点。/ `@At("NEW")` requires `new <class>(<descriptor>)V` format, but code mistakenly used `L<class>;<init>(<descriptor>)V` method descriptor format, mixin could not match `new LevelLightEngine(...)` call site.
  - **修复 / Fix**: 将 target 改为 `new net/minecraft/world/level/lighting/LevelLightEngine(Lnet/minecraft/world/level/chunk/LightChunkGetter;ZZ)V`。/ Changed target to `new net/minecraft/world/level/lighting/LevelLightEngine(Lnet/minecraft/world/level/chunk/LightChunkGetter;ZZ)V`.

## [1.7.8] - 2026-07-31

### Bug 修复 / Bug Fixes

- **ScalableLux 兼容性 mixin 注入失败修复 / ScalableLux compat mixin injection failure fix**: 修复 v1.7.7 中 `ScalableLuxCompatMixin` 因缺少 `remap = false` 注解导致 NeoForge 启动时 mixin 注入失败崩溃的问题（Issue #10）。/ Fixed crash on startup caused by `ScalableLuxCompatMixin` missing `remap = false` annotation in v1.7.7, which made mixin injection fail on NeoForge (Issue #10).
  - **问题 / Issue**: NeoForge moddev 不生成 refmap，`@At("NEW")` target `new LevelLightEngine(...)` 无法解析，触发 `MixinTransformerError: Critical injection failure: Redirector fucksable$fixLightEngineInit ... Scanned 0 target(s). No refMap loaded`，服务端启动崩溃。/ NeoForge moddev does not generate refmap; the `@At("NEW")` target `new LevelLightEngine(...)` could not be resolved, triggering `MixinTransformerError: Critical injection failure: Redirector fucksable$fixLightEngineInit ... Scanned 0 target(s). No refMap loaded` and crashing server startup.
  - **修复 / Fix**: 在 `@Mixin`、`@At`、`@Redirect` 注解均添加 `remap = false`，并将 `@Mixin(ServerLevelPlot.class)` 改为 `@Mixin(targets = "dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot", remap = false)`，与项目其他 mixin 保持一致。/ Added `remap = false` to `@Mixin`, `@At`, and `@Redirect` annotations, and changed `@Mixin(ServerLevelPlot.class)` to `@Mixin(targets = "dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot", remap = false)`, consistent with other mixins in the project.

## [1.7.7] - 2026-07-30

### 变更 / Changes

- **ScalableLux 兼容性修复 / ScalableLux compatibility fix**: 修复 ScalableLux 与 Sable 的光照引擎不兼容问题（Issue #8）。/ Fixed ScalableLux incompatibility with Sable's light engine (Issue #8).
  - **问题 / Issue**: Sable 的 `ServerLevelPlot` 构造函数从主世界 `LevelLightEngine` 读取 `blockEngine`/`skyEngine` 字段判断 SubLevel 是否有方块光/天空光。ScalableLux 的 `LevelLightEngineMixin.construct` 清空了这两个字段（用 `StarLightInterface` 替代 vanilla 光照引擎），导致 Sable 误判 SubLevel 无方块光、无天空光，SubLevel 光照完全失效。/ Sable's `ServerLevelPlot` constructor reads the `blockEngine`/`skyEngine` fields of the main world `LevelLightEngine` to determine if the SubLevel has block light / sky light. ScalableLux's `LevelLightEngineMixin.construct` clears these fields (replacing vanilla light engine with `StarLightInterface`), causing Sable to misjudge SubLevel as having no block light and no sky light, completely disabling SubLevel lighting.
  - **修复 / Fix**: 新增 `scalablelux-compat` 修复项，拦截 `ServerLevelPlot` 构造函数中 `new LevelLightEngine(...)` 调用，当 ScalableLux 存在时通过 `StarLightInterface.hasBlockLight()/hasSkyLight()` 重新计算正确的光照参数。/ Added `scalablelux-compat` fix: intercepts `new LevelLightEngine(...)` in `ServerLevelPlot` constructor; when ScalableLux is present, recalculates correct light parameters via `StarLightInterface.hasBlockLight()/hasSkyLight()`.

- **物理结构崩溃修复 / Physics structure crash fix**: 修复 simulated mod 的 `EndSeaPhysics.physicsTick` 调用 `RigidBodyHandle.getLinearVelocity` 访问已移除的 rapier native body 导致 `RuntimeException: Body has been removed` 崩溃。/ Fixed `RuntimeException: Body has been removed` crash when simulated mod's `EndSeaPhysics.physicsTick` calls `RigidBodyHandle.getLinearVelocity` on a removed rapier native body.
  - 新增 `RigidBodyHandleMixin`，拦截 `getLinearVelocity` 和 `getAngularVelocity` 方法，try-catch 捕获 `RuntimeException` 并返回零向量，复用 `panic-guard` 修复项开关。/ Added `RigidBodyHandleMixin`: intercepts `getLinearVelocity` and `getAngularVelocity`, catches `RuntimeException` and returns zero vector, reuses the `panic-guard` fix toggle.

- **配置文件修复 / Config file fix**: 修复配置文件不存在时未重新生成的问题，以及执行命令修改配置后未立即保存的问题。/ Fixed config not regenerating when missing, and config not saving immediately after command modification.
  - 配置保存时记录修复项的显式启用状态（`isExplicitlyEnabled`）而非 `isEnabled`，避免环境条件未满足的修复项被误写为 false。/ Config now saves explicit enable state (`isExplicitlyEnabled`) instead of `isEnabled`, preventing fixes with unmet environment conditions from being written as false.
  - 首次启动或配置文件被删除时立即重新生成。/ Regenerates immediately on first launch or when config is deleted.

## [1.7.6] - 2026-07-30

### 变更 / Changes
- **c2me 兼容性修复 / c2me compatibility fix**: 修复 fucksable 的 `async-save` 修复项与 c2me 的 `preventAsyncEntityUnload` mixin 冲突问题。/ Fixed conflict between fucksable's `async-save` fix and c2me's `preventAsyncEntityUnload` mixin.
  - **问题 / Issue**: fucksable 把 `SubLevelHoldingChunkMap.saveAll` 整体放到异步 IO 线程执行，异步线程执行 `processUnloads` → `removeSubLevel` → `removeEntity` 时，c2me 检测到异步线程调用 `ChunkMap.removeEntity` 并抛出 `ConcurrentModificationException: Async entity unload`，导致 sub-level 保存流程中断。/ fucksable ran `saveAll` entirely on async IO thread; when the async thread called `processUnloads` → `removeSubLevel` → `removeEntity`, c2me detected async `ChunkMap.removeEntity` call and threw `ConcurrentModificationException: Async entity unload`, interrupting the sub-level save flow.
  - **修复 / Fix**: 检测 c2me 存在时，`saveAll` 在主线程执行（unload 部分不触发 c2me 冲突），但磁盘 IO（`attemptSaveSubLevel`、`attemptSaveHoldingChunk`）提交到异步 IO 线程执行，`saveAll` 返回前等待所有异步磁盘 IO 完成。/ When c2me is present, `saveAll` runs on main thread (unload does not trigger c2me conflict), but disk IO (`attemptSaveSubLevel`, `attemptSaveHoldingChunk`) is submitted to async IO thread; `saveAll` waits for all async disk IO to complete before returning.
  - **影响 / Impact**: c2me 存在时保留异步磁盘 IO 性能，同时避免实体卸载冲突。c2me 不存在时行为不变。/ Preserves async disk IO performance when c2me is present while avoiding entity unload conflict. Behavior unchanged when c2me is absent.

## [1.7.5] - 2026-07-19

### 变更 / Changes
- **开源到 XSY-Team 组织 / Open-sourced to XSY-Team org**: 代码现在同时托管在 [XSY-Team/fuck-sable](https://github.com/XSY-Team/fuck-sable)（主仓库）和 [OLKMO/FuckSable-Unofficial](https://github.com/OLKMO/FuckSable-Unofficial)（个人仓库）。/ Code is now hosted on both [XSY-Team/fuck-sable](https://github.com/XSY-Team/fuck-sable) (main) and [OLKMO/FuckSable-Unofficial](https://github.com/OLKMO/FuckSable-Unofficial) (personal). 两个仓库内容一致，同步维护。/ Both repos have identical content and are maintained in sync.
- **jar 产物重命名 / jar artifact renamed**: `FuckSable-Unofficial-x.x.x.jar` → `fuck-sable-x.x.x.jar`，与新仓库名一致。/ Renamed to match the new repo name.
- **UpdateChecker 双源查询 / UpdateChecker dual-source query**: 更新检查器现在同时查询 `OLKMO/FuckSable-Unofficial` 和 `XSY-Team/fuck-sable` 两个仓库的 latest release，取版本号更高的作为更新提示。单个仓库查询失败不影响另一个。/ Update checker now queries both `OLKMO/FuckSable-Unofficial` and `XSY-Team/fuck-sable` repos for the latest release, using the higher version as the update notification. Failure of one repo query does not affect the other.
- **README 链接调整 / README link updates**: README 中的图片 URL 和 Releases 链接主链接改为 `XSY-Team/fuck-sable`，同时保留 `OLKMO/FuckSable-Unofficial` 作为备用下载源。/ README image URL and Releases links now point to `XSY-Team/fuck-sable` as the main source, with `OLKMO/FuckSable-Unofficial` kept as a backup download source.

## [1.7.4] - 2026-07-19

### Bug 修复 / Bug Fixes
- 修复 v1.7.3 中 `entity-lookup-remove-guard` mixin 注入失败导致服务器启动崩溃的问题（`MixinTransformerError: Critical injection failure: Redirector fucksable$safeEntityLookupRemove ... Scanned 0 target(s)`）。`@At` 的 target 描述符错误使用了 `Entity` 作为参数类型，但 `EntityLookup<T extends EntityAccess>.remove(T)` 和 `PersistentEntitySectionManager.stopTracking(T)` 由于 Java 泛型擦除，编译后实际签名是 `(Lnet/minecraft/world/level/entity/EntityAccess;)V`。现在 target 描述符和 handler 签名都使用 `EntityAccess`。/ Fix `entity-lookup-remove-guard` mixin injection failure that crashed server startup in v1.7.3 (`MixinTransformerError: Critical injection failure: Redirector fucksable$safeEntityLookupRemove ... Scanned 0 target(s)`). The `@At` target descriptor incorrectly used `Entity` as the parameter type, but `EntityLookup<T extends EntityAccess>.remove(T)` and `PersistentEntitySectionManager.stopTracking(T)` are erased to `(Lnet/minecraft/world/level/entity/EntityAccess;)V` at compile time. Now the target descriptor and handler signature use `EntityAccess`.

## [1.7.3] - 2026-07-19

### 新增修复 / New Fixes
- 新增 `entity-lookup-remove-guard`：拦截 `PersistentEntitySectionManager.stopTracking` 中 `EntityLookup.remove` 调用里 `Int2ObjectLinkedOpenHashMap.fixPointers` 抛出的 `ArrayIndexOutOfBoundsException`，避免单个实体移除失败导致整个服务器 tick 崩溃。根因是 Sable 的 SubLevel 实体管理破坏了 EntityLookup 内部链表 map 的状态（entry 的 `prev`/`next` 指针被设为 `-1` 后被当作数组下标访问）。属于治标修复——在调用点抑制 AIOOBE 让 tick 存活，每次发生输出一条 WARN 日志便于排查。/ Add `entity-lookup-remove-guard`: catches `ArrayIndexOutOfBoundsException` thrown by `Int2ObjectLinkedOpenHashMap.fixPointers` inside `EntityLookup.remove` during `PersistentEntitySectionManager.stopTracking`, preventing single-entity removal failures from crashing the server tick loop. Root cause is Sable's SubLevel entity management corrupting the EntityLookup internal linked-map state (entry `prev`/`next` pointer set to `-1` and accessed as array index). Fix is symptomatic — suppresses the AIOOBE at the call site so the tick survives, with a WARN log per occurrence for diagnosis.

## [1.7.2] - 2026-07-19

### 新增修复 / New Fixes
- 新增 `udp-invalid-packet-guard`：在 `SableUDPPacketDecoder.decode` 头部静默丢弃 packet ID 越界的 UDP 数据包（如旧版 Minecraft 服务器列表 ping 的 packet ID 254），不让 Sable 抛出 `IOException("Received an invalid packet ID: 254")`。消除服务器列表 ping 探测和 UDP 扫描 Sable UDP 端口时反复刷屏的 `Server UDP channel caught exception` ERROR 日志。/ Add `udp-invalid-packet-guard`: silently drop UDP packets with invalid packet IDs (e.g. legacy Minecraft server list ping packet ID 254) at the head of `SableUDPPacketDecoder.decode` instead of letting Sable throw `IOException("Received an invalid packet ID: 254")`. Stops the recurring `Server UDP channel caught exception` ERROR log spam triggered by server-list-ping probes and UDP scans hitting Sable's UDP port.

## [1.7.1] - 2026-07-18

### Bug 修复 / Bug Fixes
- 修复 `FuckSableMixinConfigPlugin` 在 Mohist/Youer 1.21.1 专用服务端 mixin prepare 阶段崩溃的问题：/ Fix `FuckSableMixinConfigPlugin` crashing on Mohist/Youer 1.21.1 dedicated servers during mixin prepare phase:
  - `ArtifactVersion.compareTo` 通过 `getMethod("compareTo", artifactVersionClass)` 查找失败抛 `NoSuchMethodException`，因为 `Comparable<ArtifactVersion>` 桥接方法参数类型被擦除为 `Object` 而非 `ArtifactVersion`。改为直接使用 `((Comparable) version).compareTo(threshold)` 通过 JVM 多态派发。/ `ArtifactVersion.compareTo` lookup via `getMethod("compareTo", artifactVersionClass)` threw `NoSuchMethodException` because `Comparable<ArtifactVersion>` bridge method has parameter type `Object` (erased), not `ArtifactVersion`. Replaced with direct `((Comparable) version).compareTo(threshold)` call dispatched via JVM polymorphism.
  - fallback 的 `detectByClassSignature` 使用 `Class.forName("dev.ryanhcode.sable.physics.impl.rapier.RapierPhysicsPipeline")` 触发 `ReEntrantTransformerError`，因为在 prepare 阶段加载一个被 mixin 处理的类会重新进入 mixin transformer。重写为使用 `ClassLoader.getResourceAsStream` + ASM `ClassReader` 直接从字节码解析方法描述符，不触发任何类加载。/ Fallback `detectByClassSignature` used `Class.forName("dev.ryanhcode.sable.physics.impl.rapier.RapierPhysicsPipeline")` which triggered `ReEntrantTransformerError` because loading a mixin-processed class during the prepare phase re-enters the mixin transformer. Rewritten to use `ClassLoader.getResourceAsStream` + ASM `ClassReader` to parse method descriptors directly from class bytecode without triggering class loading.

## [1.7.0] - 2026-07-16

`OLKMO/FuckSable-Unofficial` GitHub 仓库的首次发布。本版本汇总了 1.6.11–1.6.14 期间所有未发布的修复，并新增跨版本 Sable 1.x/2.x 支持。

First release on the `OLKMO/FuckSable-Unofficial` GitHub repository. This version rolls up all unreleased fixes from 1.6.11–1.6.14 plus cross-version Sable 1.x/2.x support.

### 重大变更 / Major Changes
- **跨版本 Sable 支持 / Cross-version Sable support**: 重写 `FuckSableMixinConfigPlugin` 版本检测。改用 `ModList.getMods()` 在运行时读取 Sable mod 版本，并添加类签名 fallback 检查 `RapierPhysicsPipeline.addConstraint` 参数类型。修复了 `NoSuchMethodException: ModFileInfo.getModInfos()` bug——该 bug 在所有 Sable 版本上都会静默禁用 V1/V2 自约束修复 mixin。/ Rewritten `FuckSableMixinConfigPlugin` version detection. Now uses `ModList.getMods()` to read the Sable mod version at runtime, with a class-signature fallback that inspects `RapierPhysicsPipeline.addConstraint` parameter types. This fixes the `NoSuchMethodException: ModFileInfo.getModInfos()` bug that silently disabled both V1/V2 constraint self-fix mixins on every Sable version.
- **按版本自适应的约束自修复 / Version-specific constraint self-fix**: 新增 `RapierConstraintSelfFixMixinV1`（Sable 1.x，`ServerSubLevel` 参数）和 `RapierConstraintSelfFixMixinV2`（Sable 2.x，`PhysicsPipelineBody` 参数）。正确的 mixin 由上面的插件自动选择，因此单个 FuckSable 构建现在可同时在 Sable 1.2.x 和 2.0.x 上运行，无需编译时依赖。/ Added `RapierConstraintSelfFixMixinV1` (Sable 1.x, `ServerSubLevel` params) and `RapierConstraintSelfFixMixinV2` (Sable 2.x, `PhysicsPipelineBody` params). The correct mixin is auto-selected by the plugin above, so a single FuckSable build now runs on both Sable 1.2.x and 2.0.x without compile-time dependencies.

### 新增修复 / New Fixes
- `ServerLevelSendBlockUpdateMixin`: 当目标 plot holder 不存在时取消 `sendBlockUpdated` 调用。防止在 Sable 2.0.x 上方块在未加载的 sub-level 中更新时出现 `UnsupportedOperationException: Cannot change blocks in nonexistent plot holder` 崩溃。/ Cancel `sendBlockUpdated` when the target plot holder is missing. Prevents the `UnsupportedOperationException: Cannot change blocks in nonexistent plot holder` crash that occurred on Sable 2.0.x when blocks were updated inside unloaded sub-levels.
- `SubLevelStorageLogSpamMixin`: 将 "Couldn't find sub-level at index N" ERROR 日志限流为同一 chunk+index 每 60 秒输出一次。避免 sub-level 存储条目损坏或缺失时日志刷屏。/ Throttle the "Couldn't find sub-level at index N" ERROR log to once per 60 seconds per chunk+index pair. Stops log flooding when sub-level storage entries are corrupted or missing.
- `FrogportItemExtractLimitMixin`: 当相邻库存槽位数超过 256 时跳过 `ItemHelper.extract`。防止 FrogportBlockEntity 在 `lazyTick` 中扫描超大型漏斗链 / Create 仓库导致服务器卡死数秒。/ Skip `ItemHelper.extract` when an adjacent inventory exceeds 256 slots. Prevents multi-second server freezes caused by FrogportBlockEntity scanning huge hopper chains / Create warehouses during `lazyTick`.
- `CttPostTickTimeoutGuardMixin`: 在 CTT `CreateThreadedTrains.postTick` 中给 `Future.get()` 加 10 秒超时。若异步火车工作线程卡住（例如 Sable 物理自约束死循环），future 会被取消并打 WARN 日志，而不是让主线程挂起触发 Watchdog 崩溃。/ Wrap `Future.get()` inside CTT `CreateThreadedTrains.postTick` with a 10-second timeout. If the async train worker task is stuck (e.g. Sable physics self-constraint loop), the future is cancelled and a warning is logged instead of hanging the main thread and triggering a Watchdog crash.

### 变更 / Changes
- `PlayerPositionGuardMixin`: 世界边界钳制放宽到 ±5（原 +1）。Y 轴钳制改为仅创造模式生效——生存模式玩家正常坠落，创造模式玩家被拉回 `minBuildHeight + 5` 之上。/ World-border clamp relaxed to ±5 (was +1). Y-axis clamp is now creative-only — survival players fall normally, creative players are pulled back above `minBuildHeight + 5`.
- 更新检查器现在查询 `OLKMO/FuckSable-Unofficial` Releases API。/ Update checker now queries the `OLKMO/FuckSable-Unofficial` Releases API.
- 构建产物重命名为 `FuckSable-Unofficial-1.7.0.jar`。/ Built jar is now named `FuckSable-Unofficial-1.7.0.jar`.

## [1.6.14] - 2026-07-08

### Bug Fixes
- Fix `FuckSableMixinConfigPlugin` version detection: use `ModList.getMods()` instead of nonexistent `getModInfos()`, add class signature detection fallback for Sable version (fixes `NoSuchMethodException` that disabled both V1/V2 constraint self-fix mixins)

### New Fixes
- Add `CttPostTickTimeoutGuardMixin`: 10s timeout on `Future.get()` in CTT `postTick` to prevent Watchdog server crash when async train worker is stuck
- Add `RapierConstraintSelfFixMixinV1/V2`: version-specific mixins for Sable 1.x (ServerSubLevel params) and 2.x (PhysicsPipelineBody params) `addConstraint` method, auto-selected by `FuckSableMixinConfigPlugin`

## [1.6.13] - 2026-06-29

### New Fixes
- Add `FrogportItemExtractLimitMixin`: skip `ItemHelper.extract` when adjacent inventory exceeds 256 slots to prevent server freeze from oversized inventories (hopper chains, Create warehouses)

### Changes
- Update `PlayerPositionGuardMixin`: clamp to world border+5 (was +1), creative-only Y-axis clamp (survival mode falls normally, creative mode clamped to minBuildHeight+5)

## [1.6.12] - 2026-06-29

### New Fixes
- Add `SubLevelStorageLogSpamMixin`: throttle "Couldn't find sub-level" ERROR log to once per 60s per chunk+index, preventing log spam when sub-level storage entry is corrupted/missing

## [1.6.11] - 2026-06-29

### New Fixes
- Add `ServerLevelSendBlockUpdateMixin`: cancel `sendBlockUpdated` when plot holder is missing to prevent `UnsupportedOperationException: Cannot change blocks in nonexistent plot holder` crash on Sable 2.0.x

## [1.6.10] - 2026-06-27

### Bug Fixes
- Prevent server crash when `TrackGraph.removeNode` triggers `Train.detachFromTracks` on a train with corrupted state (null `TravellingPoint.edge`): skips `TrainMigration` creation for points with null edge instead of throwing `NullPointerException` in `TrainMigration` constructor (fixes server crash when placing/breaking rails near trains with corrupted carriage state)

## [1.6.9] - 2026-06-25

### Bug Fixes
- Prevent server crash when Create train navigation searches with a null TrackNode (corrupted train state from CTT concurrent issues): `TrackGraph.getConnectionsFrom` returns empty Map instead of null to avoid NullPointerException in `Navigation.search` (fixes server crash when driving trains with corrupted carriage state)

## [1.6.8] - 2026-06-19

### Bug Fixes
- Fix Vista camera chunk loading incompatibility with Sable physics structures: project ViewFinder SubLevel coordinates to world coordinates before force-loading chunks, preventing TPS drop and infinite loading loops
- Fix `SteamVentValueBoxTransformMixin` crash on Aeronautics 1.3.0+: remove `@Shadow direction` field (removed in upstream), use reflection to set direction field for cross-version compatibility

## [1.6.7] - 2026-06-19

### Bug Fixes
- Fix CTT log spam fix mixin crash: correct `Logger.warn` target signature from `(String, Object)` to `(String, Throwable)`
- Fix `RapierPhysicsPipelineMixin` crash: remove unused `poseCache` `@Shadow` field that doesn't exist on some Sable versions
- Fix startup animation character misalignment

## [1.6.6] - 2026-06-18

### Bug Fixes
- Suppress repeated CTT (CreateThreadedTrains) warning logs when train calculation fails — only logs once per error type
- Fix physics structures spamming logs when repeatedly out of world bounds — now only warns once per SubLevel

## [1.6.5] - 2026-06-18

### Bug Fixes
- Fix physics structures spamming logs when repeatedly out of world bounds — now only warns once per SubLevel, silences subsequent clamps

### Changes
- Add Discord community link to README

## [1.6.4] - 2026-06-17

### Bug Fixes
- Fix `ParticleEngine.crack()` method signature mismatch causing client crash (fixes #1)
- Fix `RapierPhysicsPipelineMixin` crash: `sceneId` field removed, `cache` renamed to `poseCache` in Sable 2.0.2+ (fixes #2)
- Update `SteamVentValueBoxTransformMixin` to also cover `fromSide` method for Aeronautics compat (fixes #3)

### Changes
- Change 18 fix entries from `Side.SERVER` to `Side.BOTH` so fixes also work in singleplayer (integrated server)

## [1.6.3] - 2026-06-16

### Bug Fixes
- Fix crash on Sable 2.0.2+: `@Shadow field sceneId was not located in RapierPhysicsPipeline` (field removed in upstream)
- Remove fstemp3/fsban/fslook features (conflicted with core functionality)
- Restore auto-update to config-controlled behavior

## [1.6.0] - 2026-06-14

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
