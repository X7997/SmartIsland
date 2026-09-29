# Smart Island 需求与回归账本

| 稳定 ID | 类型 | 必须成立的行为/验收条件 | 来源与相关触点 | 验证方法及最近证据 | 状态 |
|---|---|---|---|---|---|
| REQ-001 | 冷启动恢复 | 开关开启且无障碍服务获授权时，系统连接回调后挂载 `TYPE_ACCESSIBILITY_OVERLAY`；未授权时不启动无顶层窗口的前台服务。 | 2026-09-28 交接；`MainActivity`、`SystemServiceRecovery`、`SmartIslandOverlayService`。 | PKV110：临时授权后 `dumpsys window` 见可见 `ty=ACCESSIBILITY_OVERLAY`、`mBaseLayer=631000`。强行停止会使 ColorOS 清除该服务授权，桌面重开不能自行恢复；需用户在系统设置重新授权。 | 授权冷启动已验；强停恢复受系统授权限制 |
| REQ-002 | 最近任务退出 | 划掉任务后移除悬浮窗、注销广播与 torch 回调并停止前台运行；再次打开应用可恢复。Android 系统绑定的无障碍服务是否仍存活，单独核实。 | `AGENTS.md` 铁律 6；服务 `onTaskRemoved`。修改前仅在设置关闭时清理。 | PKV110 真机从最近任务划掉卡片后，窗口消失、进程被 ColorOS 杀死；没有观察到应用 `onTaskRemoved` 回调。2026-09-28 新恢复路径在重新打开后令 Bound=true、Crashed=false、2032 窗口可见。 | 真机恢复已通过；回调执行仍受 OEM 杀进程行为限制 |
| REQ-003 | 顶缘手势 | 获授权且窗口已挂载时，折叠、展开、健身和媒体态的目标手势到达岛内处理器；不能以窗口存在代替手势成功。 | `HANDOFF.md`；`IslandOverlayView`、窗口触摸区域。 | PKV110 已证实 2032 窗口高于状态栏；各状态的实际手势响应未逐项测。 | 部分验证 |
| REG-001 | 既有不变量 | 主岛物理居中、副岛始终居左；窗口只用 2032；拖动循环保留抬手事件；媒体选择优先级保留。 | 项目 `AGENTS.md` 六大铁律，相关布局/手势/服务代码。历史未完整核实。 | 最新 `testDebugUnitTest assembleDebug` 通过；真机已证实窗口仍为 2032。位置、手势和媒体交互未在各状态逐项实测。 | 自动测试通过，真机待验证 |
| REQ-004 | 重新进入时自动恢复 | 应用内总开关持续保存；若用户先前在系统中启用了本服务、最近任务划除后 Secure 名单仍保留但服务失联，重新进入应用自动解除 ColorOS 的失联状态并重新挂载 2032 窗口。系统名单中已移除本服务时视为用户撤销，不擅自重新授权。 | 2026-09-28 用户反馈；`SmartIslandSettingsRepository`、`MainActivity`、`SystemServiceRecovery`。 | DataStore 原本就保存 `Keys.Enabled`。PKV110 实测最近任务划除后 Bound=false/Crashed=true/全局 flag=0；重新打开后 Bound=true/Crashed=false/可见 2032 窗口/flag=1。主动清除系统授权后重新打开，名单保持 null、Bound=false。需一次性通过 ADB 授予 `WRITE_SECURE_SETTINGS`；普通安装无法自动写 Secure 设置。 | 已在此设备真机通过；其他设备与无 ADB 授权路径待验 |
| REQ-005 | 折叠态点击透传 | 单岛、双岛、自动隐藏状态下，岛外近旁及胶囊下方点击应到达下层应用；顶部岛内手势仍可进入处理器，展开态交互保持可用。 | 2026-09-29 用户复发反馈；[[SOL-018_overlay_touch_leak_and_gesture_continuity]]；`SmartIslandOverlayService.setupTouchableRegion`、`collapsedParams`、`updateWindowLayoutParams`、`IslandOverlayView`。 | 原代码左右各 48dp、底部 36dp，反射裁剪失败时仍为全屏宽。现统一为 4dp 边距，裁剪不可用时物理窗口收窄；边界单测通过，112 项单测与 `assembleDebug` 通过。2026-09-29 `adb devices -l` 无设备，真实点击透传待测。 | 代码/单测/构建通过；真机待验 |
| REG-002 | 保留历史触摸边界修复 | 不因恢复 2032 顶层窗口及顶部手势而再次扩大折叠态透明拦截区；下层控件在岛外应可点击。 | SOL-018 记录过下方约 40dp 透明区域吞触摸；2026-09-28 P36 项目卡指出现有几何已偏离旧尺寸；本次涉及窗口/触摸区域/Compose 目标。 | 几何单测检查顶缘 y=0、单岛/双岛左侧区域、岛外横向和下方坐标；`git diff --check` 通过。展开态仍全屏交互，自动隐藏态沿用窄窗口。反射裁剪及实际点击效果待真机验证。 | 静态/自动测试通过；真机待验 |

## 2026-09-28 真机例外

PKV110 上单独注入 `PRIVATE_FLAG_LAYER_FOR_SCREEN` 就导致 2032 `addView` 抛 `BadTokenException`；不注入时，同一无障碍连接成功挂载可见 2032 窗口。因此代码先尝试项目规定标志，被系统拒绝时仅去掉该私有标志重试同一窗口类型，绝不降级到 2038。`PRIVATE_FLAG_TRUSTED_OVERLAY` 没有单独测试，当前不注入。AOSP 和 Android 官方文档确认无障碍窗口必须由系统连接授权，普通应用不能自行恢复已被系统撤销的授权。

最近任务划除的实测结果与强行停止不同：Secure 授权文本仍在，但 ColorOS 将服务加入 `mCrashedServices`，并把全局 `accessibility_enabled` 设为 0。组件状态在 DEFAULT 与 ENABLED 之间切换未解除该状态，还触发授权清除，所以恢复工具不修改组件状态。已验证的恢复路径是在授权名单仍包含本服务时，短暂移除并恢复本服务、恢复全局 flag=1；其他服务条目保留。此路径需要 `WRITE_SECURE_SETTINGS`：当前 PKV110 已通过 `adb shell pm grant com.agupta07505.smartisland android.permission.WRITE_SECURE_SETTINGS` 一次性授权。官方 Android 普通应用无法自行获得该权限；未经该授权的安装仍需用户手动开启。

## 2026-09-28 复盘归档

本次真机排障已归档至知识库 [[EXP-078_android_accessibility_overlay_zorder_keepalive]]，并更新 [[P36_SmartIsland]] 项目卡。旧条目关于“必须注入私有层级标志”“100% 手势零拦截”“折叠态仍为高度 +2f、4px padding”的描述已按当前证据修正。归档只记录现有真机、单测和构建证据；REQ-003 与 REG-001 中各状态手势、位置和媒体交互仍待逐项实测。

## 2026-09-29 折叠态点击区域复发

2026-09-28 的顶部热区改动把折叠态左右余量扩大为各 48dp、底部多 36dp，并让反射裁剪不可用时的窗口仍占全屏宽；这覆盖了 SOL-018 所记录的岛外点击透传目标。本次将窗口物理边界、系统 touchable region 和 Compose 触摸目标统一收紧至胶囊/副岛及 4dp 余量，同时保留 y=0 顶缘的窄手势走廊。最小几何回归测试、全量 112 项单测与 Debug 构建通过。由于 2026-09-29 ADB 未连接设备，真实桌面/其他应用近旁点击、顶部滑动、双岛切换、自动隐藏、展开态与重进恢复尚未完成运行验收。
