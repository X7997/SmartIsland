# Smart Island (灵动岛) 开发交接与卡点攻坚文档 (HANDOFF.md)

> **文档定位**：本文件为当前会话向新会话 / Codex 移交的完整上下文与卡点技术交接文档。任何新会话或开发者在接手本项目时，请先通读本文档以及 [AGENTS.md](file:///q:/DynamicIsland/AGENTS.md)。

---

## 一、项目与任务背景 (Task Overview)

### 1. 项目概述
- **项目名称**：Smart Island (灵动岛)
- **技术栈**：Kotlin + Jetpack Compose + Hilt + Coroutines/Flow + Android AccessibilityService + NotificationListenerService
- **测试环境/真机**：OPPO PKV110 (ColorOS / Android 14, API 34), Device ID: `V8RGFQU8RO65H6WW`

### 2. 核心功能目标
1. **永久锁定系统最高 2400 层级悬浮窗 (`TYPE_ACCESSIBILITY_OVERLAY`)**：
   - 必须超越系统状态栏（`StatusBar`, layer 151000），锁定在最高层级（base layer 631000），确保在折叠态、展开态、健身模式、媒体播放、通知等所有状态下，屏幕顶缘状态栏区域的手势（左右滑切组、上滑取消休息、下滑展开/收起、单击）100% 被灵动岛捕获并响应，绝不被系统状态栏拦截或吞噬。
2. **划掉后台彻底销毁，冷启动自愈唤醒**：
   - 用户从“最近任务”划掉应用时，服务能干净退出；
   - 用户从桌面重新点击图标打开 App 时，无障碍服务与悬浮窗能够自动自愈拉起，恢复顶层灵动岛。
3. **开始训练立即弹岛 (Fitness Companion Mode)**：
   - 用户在 App 内点击“开始训练”时，灵动岛立即弹出并置顶，显示动作名、组数、做组时间/倒计时，支持左右滑动切换组数与上滑取消休息。
4. **严格遵守 [AGENTS.md](file:///q:/DynamicIsland/AGENTS.md) 六大核心铁律**：
   - 铁律 1：主灵动岛永远绝对物理居中（`Center = screenWidth / 2 + settings.xOffset`）；
   - 铁律 2：副灵动岛永远无条件位于主灵动岛左侧；
   - 铁律 3：悬浮窗层级必须为系统最顶层 2400；
   - 铁律 4：滑动手势流绝不允许被中间阻断；
   - 铁律 5：主流媒体播放优先锁定；
   - 铁律 6：划掉后台销毁，冷启动自愈拉起。

---

## 二、已经完成的工作与当前代码状态 (Completed Work)

1. **彻底修复无障碍崩溃黑名单死循环**：
   - 彻底移除了 `SystemServiceRecovery.kt` 中调用 `pm.setComponentEnabledSetting(..., DISABLED, ...)` 的危险代码。该调用此前在 ColorOS / Android 14 上会导致系统 `UserAwareMgr` 强杀进程，并将服务加入 `system_server` 的 `mCrashedServices`，导致服务永久无法被系统绑定。
   - 改为通过发送合法的无障碍事件及服务自愈拉起。

2. **彻底废除 `TYPE_APPLICATION_OVERLAY` 降级逻辑**：
   - 在 `SmartIslandOverlayService.kt` 中，将 `overlayWindowType` 永久锁定为 `WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY`（2032/2400 层级）；
   - 彻底删除了失败时退回普通应用悬浮窗（2038 层级，base layer 121000，位于状态栏 151000 之下）的代码路径，杜绝因降级而导致的被状态栏物理遮盖。

3. **修复 WindowManager 实例的 Token 继承问题**：
   - 将 `SmartIslandOverlayService.windowManager` 由 `onCreate` 预先缓存的普通 `WindowManagerImpl` 改为动态调用 `getSystemService(Context.WINDOW_SERVICE) as WindowManager`，确保直接从 `AccessibilityService` 自身上下文获取带有系统注入的 `mWindowToken` 的窗口管理器。

4. **折叠态触摸热区全面覆盖顶缘**：
   - 在 `IslandOverlayView.kt` 中，为折叠态创建了专属触摸热区容器（`touchHeight = yOffset + height + 36dp`, `touchWidth = miniPillWidth + 96dp`），顶部从 `y = 0`（屏幕物理最顶端）开始覆盖，避免 Compose 内部因安全区或偏移量导致的触控盲区；
   - 在 `SmartIslandOverlayService.setupTouchableRegion()` 中使用反射配置了 `OnComputeInternalInsetsListener`，设置 `top = 0`，保证非岛区域点击穿透至下层，岛体区域完全由灵动岛拦截。

5. **编译与测试构建状态**：
   - `./gradlew testDebugUnitTest`：**BUILD SUCCESSFUL**，100% 单元测试通过；
   - `./gradlew assembleDebug`：**BUILD SUCCESSFUL**，APK 构建成功。

---

## 三、当前核心卡点与现象深度剖析 (Current Blockers & Analysis)

### 1. 核心卡点现象
用户反馈：**“重新划掉后台再打开，依然被状态栏遮盖住，无法在顶层点击或滑动灵动岛”**，且“划掉后台重启弹出”与“顶层滑动切组”容易出现交替失效的回归问题。

### 2. 底层深层原因与技术证据

#### 原因 A：系统无障碍服务异步绑定时延窗口 (Async A11y Binding Race Condition)
- **机制原理**：
  - 当用户划掉后台（进程被杀死）后再次点击桌面图标时，`MainActivity.onCreate()` 立即运行并执行 `lifecycleScope.launch { ... }`。
  - 然而，Android 系统底层 `AccessibilityManagerService` 重新发现、拉起并与 `SmartIslandOverlayService` 建立 Binder 通信（最终触发 `onServiceConnected()`）存在 **200ms ~ 1500ms 的系统异步时延**。
  - 在此时延期间，`SmartIslandOverlayService.isSystemConnected` 为 `false`。
  - 如果代码为了防崩溃而严格限制“只有 `isSystemConnected == true` 才创建窗口”，那么在无障碍绑定尚未完成的这几百毫秒到一两秒内，悬浮窗根本没有被添加到屏幕上；如果此时用户立即去顶部做交互，就会出现“灵动岛没有显示或无法点击”的现象。

#### 原因 B：`startForegroundService` 与 `AccessibilityService` 的混合启动冲突
- **机制原理**：
  - `SmartIslandOverlayService` 声明在 AndroidManifest 中作为带有 `BIND_ACCESSIBILITY_SERVICE` 权限的无障碍服务。
  - 同时，代码在 `MainActivity` 和 `SystemServiceRecovery` 中为了保活与拉起，调用了 `context.startForegroundService(intent)`。
  - 在 Android 14 / ColorOS 上，如果一个 `AccessibilityService` 先以常规 Started Service 方式启动，其内部的 `AccessibilityService.this` 尚未被系统注入 `mConnection` / `mWindowToken`；
  - 此时若直接尝试使用 `TYPE_ACCESSIBILITY_OVERLAY`（2032），系统 WindowManager 会直接抛出：
    `android.view.WindowManager$BadTokenException: Unable to add window -- permission denied for window type 2032`。

#### 原因 C：OEM 系统状态栏手势竞争 (OEM Insets / Gesture Interception)
- **机制原理**：
  - 在 ColorOS 等定制系统中，系统状态栏具有全局下拉手势监听（Notification Shade Swipe Down）。
  - 如果灵动岛的窗口标志（`flags`）或 `OnComputeInternalInsetsListener` 的动态代理在特定 OEM ROM 上被拦截，或者 `PointerEventPass.Initial` 与系统手势发生竞争，当手指从状态栏区域（`0 <= y <= 83px`）向下滑动时，可能被系统 UI 的下拉手势优先抢占。

---

## 四、下一步攻坚方案与计划 (Action Plan for Codex / New Session)

### 1. 攻坚目标 1：彻底理顺无障碍绑定与悬浮窗挂载的时序
- **建议实施路径**：
  1. 在 `SmartIslandOverlayService` 内部，使用 `StateFlow<Boolean>` 维护系统连接状态；
  2. 当 `onServiceConnected()` 回调触发时，立即同步系统 `windowToken`，并立即触发 `startOverlaySession(settings)`；
  3. 在 `MainActivity` 启动时，如果无障碍权限已开启但 `isSystemConnected` 尚未为 true，可通过状态回调/轮询机制（或 `AccessibilityEvent` 触发）确保在服务连接建立的瞬间立即挂载 2400 级悬浮窗；
  4. 绝不在 `isSystemConnected == false` 时强行调用 `windowManager.addView(..., TYPE_ACCESSIBILITY_OVERLAY)` 导致 `BadTokenException`。

### 2. 攻坚目标 2：确保 2400 层级完全压制状态栏手势
- **建议实施路径**：
  1. 确认 WindowManager `LayoutParams` 标志组合：
     - `type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY` (2032/2400);
     - `flags = FLAG_NOT_FOCUSABLE | FLAG_LAYOUT_NO_LIMITS | FLAG_LAYOUT_IN_SCREEN | FLAG_NOT_TOUCH_MODAL | FLAG_HARDWARE_ACCELERATED | FLAG_SHOW_WHEN_LOCKED`;
     - `layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`;
     - 私有标志反射：`PRIVATE_FLAG_LAYER_FOR_SCREEN` (`0x00100000`) 与 `PRIVATE_FLAG_TRUSTED_OVERLAY` (`0x20000000`)；
  2. 审查 `IslandOverlayView.kt` 中的 `pointerInput`：
     - 确保在折叠态下，触摸捕获不仅涵盖主岛，还涵盖副岛（左侧区域）以及顶部状态栏（`y: 0 -> status_bar_height + pill_height`）；
     - 确保滑动阈值（`swipeThreshold`）设置合理（如 `12dp * density`），能灵敏响应短距快速滑动。

### 3. 攻坚目标 3：构建端到端自动化真机验证闭环
- **验证流程**：
  1. 编译安装 APK：`./gradlew assembleDebug` + `adb install -r ...`；
  2. 授予权限：`adb shell settings put secure enabled_accessibility_services ...`；
  3. 执行冷启动 / 划掉后台杀进程测试：`adb shell am force-stop ...` -> `adb shell am start ...`；
  4. 执行窗口层级验证：`adb shell dumpsys window windows` 验证 `SmartIslandOverlayService` 是否为 `TYPE_ACCESSIBILITY_OVERLAY` 且处于最高可见窗口；
  5. 模拟手势测试：`adb shell input swipe 360 40 200 40 100`（测试左滑切组），`adb shell input tap 360 40`（测试点击展开/收起）；
  6. 截屏抓取验证：`adb shell screencap -p /sdcard/screen.png` + `adb pull ...` 并比对 UI 状态。

---

## 五、绝对不可再踩的“血泪巨坑” (Critical Anti-Patterns & Rules)

在新会话编写与修改代码时，**绝对禁止以下行为**：

1. **绝对禁止调用 `pm.setComponentEnabledSetting(..., COMPONENT_ENABLED_STATE_DISABLED, ...)`**：
   - 禁用无障碍组件会导致 Android 14/ColorOS 立即强杀 App 进程，并将服务拉入系统 `mCrashedServices` 黑名单，导致永久无法自愈。
2. **绝对禁止回退为 `TYPE_APPLICATION_OVERLAY` (2038)**：
   - 2038 层级 base layer 为 121000，位于状态栏（151000）之下，任何在状态栏区域的点击与滑动都会被状态栏完全吃掉！必须且只能使用 `TYPE_ACCESSIBILITY_OVERLAY`。
3. **绝对禁止在已添加的 View 上直接通过 `updateViewLayout` 更改 `params.type`**：
   - WindowManager 严禁在 View 挂载后修改窗口类型，否则抛出致命崩溃 `IllegalArgumentException: Window type can not be changed after the window is added`。
4. **绝对禁止在拖拽/滑动循环中因 `change.isConsumed` 直接 `break`**：
   - 中途 break 会导致抬手事件 `changedToUp()` 丢失，切组手势彻底失效。必须使用 `PointerEventPass.Initial` 并完整追踪至抬手。
5. **绝对禁止破坏主灵动岛绝对居中（铁律 1）与副岛永在左侧（铁律 2）**：
   - 无论单岛/双岛/折叠/展开，主岛物理中心必须严格等于 `screenWidth / 2 + settings.xOffset`，副岛必须严格在主岛左侧。

---
*文档生成时间：2026-09-28*
*Single Source of Truth 规约请严格参照 [AGENTS.md](file:///q:/DynamicIsland/AGENTS.md)。*
