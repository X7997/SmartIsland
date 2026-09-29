# Smart Island (灵动岛) 架构设计不变量与开发约束规约

> 本文件是本项目开发、重构与 Bug 修复的**最高权威规范（Single Source of Truth）**。
> 任何人在编写、修改或审查代码时，**必须严格遵守以下六大不可动摇的核心铁律（Core Invariants）**，坚决杜绝“修一个功能就落下另一个功能”的回归（Regression）问题！

---

## 一、六大核心铁律（Core Invariants）

### 铁律 1：主灵动岛必须永远绝对居中（Zero Horizontal Shift）
- **物理中心公式**：无论当前是否存在副灵动岛（单岛/双岛/多任务/健身/倒计时），主灵动岛在屏幕上的物理中心点必须永远严格锁定在：
  $$\text{Center}_{\text{main}} = \frac{\text{screenWidth}}{2} + \text{settings.xOffset}$$
- **防回退要求**：
  1. 当副灵动岛（圆形气泡）浮现或关闭时，主灵动岛**绝对不允许发生任何左右跳动或偏移**！
  2. 在全宽模式（`isFullWidth == true`，`MATCH_PARENT`）下，主岛在 Compose 内部的平移必须为 `settings.xOffset.dp`；
  3. 在自适应窗口模式（`isFullWidth == false`）下，窗口中心因包含副岛而向左偏移了 $\frac{\text{compactGap} + \text{circleSize}}{2}$，Compose 内部的主岛平移必须严格向右补偿 $+ \frac{\text{compactGap} + \text{circleSize}}{2}$，两者完全抵消！

---

### 铁律 2：副灵动岛必须无条件位于主灵动岛左侧（Never on the Right）
- **物理中心公式**：副灵动岛必须紧贴在主灵动岛左侧（间隔 `8dp`）：
  $$\text{Center}_{\text{sec}} = \text{Center}_{\text{main}} - \left(\frac{\text{miniPillWidth}}{2} + \text{compactGap} + \frac{\text{circleSize}}{2}\right)$$
- **防回退要求**：
  1. **严禁**添加“若左侧空间不足则放到右侧”的逻辑（彻底废弃旧版的 `isSecondaryOnLeft` 翻转分支）；
  2. 副灵动岛的触控区域（`setupTouchableRegion`）、窗口边界（`updateWindowLayoutParams`）、以及 Compose 视觉渲染（`secondaryOffset`）必须 100% 同步在左侧！

---

### 铁律 3：悬浮窗层级必须为系统最顶层（Z-Order Highest）
- **层级规范**：
  1. 默认窗口类型必须为 `TYPE_ACCESSIBILITY_OVERLAY`（2400 层级），凌驾于状态栏、通知中心和挖孔上方；
  2. 必须设置 `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`；`PRIVATE_FLAG_LAYER_FOR_SCREEN` 可尝试，若设备拒绝（PKV110/ColorOS 曾抛 `BadTokenException`），去掉该私有标志并重试同一 `TYPE_ACCESSIBILITY_OVERLAY`（2032）窗口；此设备例外不推广到所有 Android 设备；
  3. 严禁在初始化时因为短暂未连接而永久回退为普通的 `TYPE_APPLICATION_OVERLAY`（2038 层级，会被状态栏遮挡）；
  4. 应用重新从桌面打开时，若系统无障碍服务断开，必须通过 `SystemServiceRecovery.refreshAccessibilityComponent()` 自动触发系统无障碍重连。

---

### 铁律 4：滑动手势流绝不允许被中间阻断（Gesture Continuity）
- **手势监听规范**：
  1. 必须使用 `PointerEventPass.Initial` 获取未经下游消费的原始触摸事件流；
  2. 手势判定（左滑、右滑、上滑、下滑、长按、点击）必须在手指抬起（`changedToUp()`）时计算起始点与终点的位移向量 $(dx, dy)$；
  3. **严禁**在拖拽循环中因为 `change.isConsumed == true` 而直接 `break` 跳出循环，否则手指抬起事件永远无法执行，手势将彻底瘫痪！

---

### 铁律 5：网易云与主流音频播放优先锁定（Media Priority）
- **音频识别规范**：
  1. 当底层系统检测到音频发声（`AudioManager.isMusicActive == true`）时，媒体控制器挑选逻辑必须优先寻找包含 `com.netease.cloudmusic` 等主流音乐包名；
  2. 严禁仅因国内应用未上报标准的 `STATE_PLAYING` 就误回退给桌面或系统空会话；
  3. 空闲状态下单机主岛，必须能唤醒网易云音乐每日推荐并开始播放。

---

### 铁律 6：划掉后台时彻底销毁，冷启动时自愈拉起（Lifecycle Discipline）
- **生命周期规范**：
  1. 最近任务划除时，若收到 `onTaskRemoved()`，清理悬浮窗、广播及运行时回调并停止前台运行；OEM 直接杀进程时不假设回调必到，以窗口/进程实际退出及重进恢复结果验收（PKV110 记录见 `REQUIREMENTS_LEDGER.md` 的 REQ-002）；
  2. 用户重新点击桌面图标打开 App 时，如果开关原本是开启的，服务必须能够通过无障碍自愈和前台服务重连机制重新浮现灵动岛，不得要求用户卸载重装。

---

## 二、开发修改代码的“防回退核查流程”（Pre-Flight Checklist）

每当要对代码进行任何改动时，必须按以下步骤自检：
1. **修改前（Pre-Check）**：
   - 确认待改动的代码是否涉及：窗口参数（WindowManager）、布局坐标（Offset）、手势检测（pointerInput）、媒体监听（NotificationListener）、无障碍生命周期（OverlayService）。
   - 若涉及，对照上述【六大核心铁律】确认改动方案不会破坏既有公式。
   - 窗口尺寸、顶部热区、折叠/自动隐藏布局或触摸区域改动，还须对照 `REQUIREMENTS_LEDGER.md` 的 REQ-005／REG-002，同时核对折叠态物理窗口、系统可触摸区域、Compose 命中区和岛外点击透传；SOL-018 只作旧问题背景，具体几何以账本当前验收条件和现行代码为准。纯文案改动不触发此专项检查。
2. **修改后（Post-Check）**：
   - 运行 `git diff`，仔细逐行审查改动的 diff：
     - 是否误删了坐标补偿？
     - 是否引入了可能被截断的条件分支？
     - 是否破坏了 `TYPE_ACCESSIBILITY_OVERLAY`？
3. **编译与验证（Build & Test）**：
   - 运行编译命令确保构建成功；
   - 审查产物大小与时间戳，确认改动已生效。
   - 涉及上述触摸边界时，运行相关几何测试并在可用设备上验证岛外点击与岛内顶缘手势；设备不可用则标“真机待验”，不以编译或单测通过代替点击验收。
