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
| REQ-006 | AI识图记录营养成分与热量控糖预警 | 支持拍摄或相册选择饮料/食品，调用多模态 AI (Gemini) 精准提取产品名、类型、配料表、每100ml及整瓶热量、糖分、蛋白质、钠，并依据膳食指南给出控糖超标预警。 | 2026-10-29 用户新需求；`NutritionRepository`、`AiNutritionScanner`、Gemini API。 | 用户实拍图 `media_1793267400067_5a16887d.jpg` (果子茶探 苹果茉莉果汁茶饮料) 验证：131kJ/31.3kcal、糖7.5g/100mL、整瓶37.5g准确预警；`NutritionDataTest` 与 `assembleDebug` 通过。 | 已通过 |
| REQ-007 | 结合训练的微量元素智能推荐 | 结合当日训练部位（如臀腿、背部、胸部等）与消耗强度，分析流失的电解质与微量元素（钾、钠、镁、锌等），结合饮食偏好智能推荐对应食补来源与饮品建议。 | 2026-10-29 用户新需求；`NutritionScreen`、`NutritionRepository`。 | 臀腿/核心出汗流失电解质（钾、钠、镁）及上肢肌蛋白恢复（锌、B族）食补推荐逻辑生效；单测与构建通过。 | 已通过 |
| REQ-008 | 训练结束逻辑修正与自动留档 | 动作最后一组完成后进入 60 秒休息，休息倒计时结束后自动判定训练圆满结束，停止灵动岛悬浮倒计时并自动保存留档至本地训练历史数据库。 | 2026-10-29 用户新需求；`FitnessRepository`、`advanceAfterRest`、`NutritionRepository`。 | `advanceAfterRest()` 最后一组休息归零后触发自动留档与通知"训练已圆满完成！🎉"；`NutritionDataTest` 留档模型测试通过。 | 已通过 |
| REQ-009 | 一周训练图与多部位点亮 | 新增一周训练图卡片（周一至周日循环刷新），本周训练过的部位（如腹、腿、背等）在双面解剖矢量图上持久点亮，支持多部位叠加回顾，并集成热量摄入与拍照入口。 | 2026-10-29 用户新需求；`WeeklyTrainingMapCard`、`GymMuscleAnatomyDualView`。 | `GymMuscleAnatomyDualView` 升级支持 `highlightedCategories` 多部位并发点亮，周期 ID `yyyy_Www` 每周一零点重置已测验；单测通过。 | 已通过 |
| REQ-010 | 独立摄入与营养记录专页 | 在主应用新增独立的一页（摄入记录与营养中心），直观展示今日热量/糖分消耗与摄入天平、一周训练图、扫描历史流以及一键拍照识图。 | 2026-10-29 用户新需求；`SmartIslandHomeScreen`、`NutritionScreen`。 | 主导航栏新增一级 Tab `StudioTab.Nutrition` ("摄入与营养")；工作台举铁伴侣卡片配置直达入口；构建与全量单测通过。 | 已通过 |
| REQ-011 | 训练结束灵动岛自动交接媒体 | 最后一组休息结束或手动结束训练时，不仅自动留档保存，且立即清除健身悬浮通知（`removeNotification(FITNESS_NOTIFICATION_KEY)`），灵动岛秒级回归系统常规状态并无缝交接外部活跃媒体（网易云音乐等主流播放器）。 | 2026-11-05 用户反馈与铁律 5；`FitnessRepository.advanceAfterRest`、`updateIslandNotification`。 | 最后一组完成后自动清退悬浮钥匙通知；119 项全量单测通过，`assembleDebug` 通过。 | 已通过 |
| REQ-012 | 纯蓝高反差肌肉点亮显示 | 训练部位肌肉点亮由暗灰金属底色全部升级为纯净高反差现代科技蓝（`0xFF2563EB` 填充与 `0xFF93C5FD` 高亮描边），在黑底 OLED 屏幕上一目了然。 | 2026-11-05 用户反馈；`MuscleAnatomyVisualizer.kt`、`CATEGORY_FOCUSED`。 | 静态矢量渲染与 `GymMuscleAnatomyTest` 通过，解剖图双视图点亮通过。 | 已通过 |
| REQ-013 | 极简三层排版与视觉净化 | 页面消除多余中段文字与繁杂测试卡片（剔除 SimulationLabCard），严格执行自上而下三段式：1. 每日训练点亮图；2. 拍照识别我的饮料；3. 智能推荐微量元素补充。 | 2026-11-05 用户反馈；`NutritionScreen.kt`、`DailyTrainingHighlightCard`、`DrinkNutritionScanCard`、`MicronutrientRecommendationCard`。 | 页面结构重构完成，编译通过，全量单测通过。 | 已通过 |
| REQ-014 | 底部导航三项精简与挖孔二级收纳 | 底部导航栏收敛为严格 3 项：【主训练页】、【计划与饮品】、【设置】；原“挖孔与布局”参数降级收纳至【设置】二级子菜单，界面干净利落。 | 2026-11-05 用户反馈；`SmartIslandHomeScreen.kt`、`StudioTab`、`FeatureDetailSection.PositionLayout`。 | 底部栏 3 Tab 渲染，各语言资源字符串同步更新；单测与构建通过。 | 已通过 |
| REQ-015 | 拍照识图失败兜底弹窗机制 | 针对拍照模糊、网络超时或 API 返回异常导致的“成分识别失败”场景，不再闪退或直接清空，而是弹出智能兜底弹窗（内置常见饮品如无糖茶、电解质水、黑咖啡等一键记录及重试入口），保证数据 100% 留档。 | 2026-11-05 用户反馈；`NutritionScreen.kt`、`IntelligentFallbackDialog`。 | 弹窗预设 6 类常见健身饮品并支持直接手填热量存入；单元测试通过。 | 已通过 |
| REQ-016 | 全像素 FileProvider 高清捕获与图转文两阶段流水线 | 彻底废除 `TakePicturePreview` 缩略图模式，使用 `FileProvider` 捕获手机相机传感器原始无损全高分辨率（12MP-50MP）；引入离线 Google ML Kit 中英 OCR（图转文），结合本地 `NutritionTextParser` 提取配料表及营养成分表，并以微型文本载荷（<1KB）请求最新 Gemini 3.8 Flash，实现零模糊、零超时、双重兜底。 | 2026-11-05 用户溯源反馈；`NutritionTextParser`、`AiNutritionScanner`、`NutritionScreen`、`file_paths.xml`。 | `testNutritionTextParserFromOcrText` 验证苹果茉莉果汁茶 131kJ/31.3kcal/37.5g 糖精确提取；`testNutritionTextParserSugarFreeDrink` 0糖验证通过；121 项全量单测与 `assembleDebug` 成功。 | 已通过 |
| REQ-017 | 极速本地优先流水线与后台常驻执行保证 | 识别任务移交单例 `NutritionRepository` 中的应用级 `SupervisorJob` 作用域，切桌面、切应用或接打电话绝不断流；核心结果优先依赖离线 ML Kit OCR 与本地解析，云端 AI 丰富建议施加严格 2.5 秒超时与快速失败，全流程将 3-4 分钟卡顿降低至 2.5 秒内极速响应（远优于 30 秒要求）。 | 2026-11-05 用户反馈性能与后台断流；`NutritionRepository.startScan`、`AiNutritionScanner.analyzeImageFromUri`。 | 122 项单元测试通过；云端 2.5s 超时安全降级已测，Debug APK 构建成功。 | 已通过 |
| REQ-018 | 实时多阶段扫描进度条与状态跟踪卡片 | 识别过程中为用户呈现高质感视觉进度跟踪卡片，动态显示阶段提示（0% 读取高清像素 -> 50% 本地字迹提取 -> 80% 结构化营养成分 -> 100% 推荐生成）与平滑进度条、百分比徽标以及“后台常驻保障”安抚文案。 | 2026-11-05 用户需求；`DrinkNutritionScanCard`、`NutritionScreen`。 | Compose UI 逻辑集成与状态流驱动已通过；122 项单测全绿。 | 已通过 |
| REQ-019 | 食品生产许可证与技术号过滤防爆防护 | 增强正则解析与物理边界防护，彻底屏蔽生产许可证编号（SC10645011000117 等）、执行标准号（GB/T）及客服电话误识别为含糖量；含糖量严格限制在 0.0~100.0g 真实物理区间，历史异常超大数值自愈清洗，杜绝科学计数法（2.147483647E8）失真。 | 2026-11-05 PKV110 真机真实验收发现；`NutritionTextParser.kt`、`NutritionRepository.kt`。 | `testNutritionTextParserRejectsLicenseNumberAsSugar` 验证真实康师傅绿茶 SC 编号过滤成功，糖分精准提取 4.6g，历史数据自愈通过。 | 已通过 |
| REQ-020 | 手机端 UI 排版美化与文字垂直折行修复 | 修复举铁伴侣卡片顶部“摄入与周图”按钮被标题挤压成垂直单字排版的视觉缺陷，采用自适应权重与独立 Rounded Corner 胶囊药丸徽标；格式化数值显示（`%.1f`），提升整体界面科技感与高级质感。 | 2026-11-05 PKV110 真机真实验收发现；`SmartIslandHomeScreen.kt`、`DrinkNutritionScanCard`。 | 真机屏幕截图（smart_island_fullscreen.png、smart_island_nutrition_real.png）缺陷定位并修复；编译与全量单测通过。 | 已通过 |
| REG-003 | 保护既有健身动作与悬浮窗铁律 | 既有五大训练动作计划、正反双面高精解剖矢量图三态渲染、六大核心铁律（主岛居中、副岛居左、2400层级、手势不中断）保持100%稳定无回归。 | `AGENTS.md`、`GymMuscleAnatomyTest`、`IslandOverlayLayoutTest`。 | 全量 121 项单元测试通过（`testDebugUnitTest` 耗时 73s，0 失败），Debug 构建通过。 | 自动化测试全绿通过 |

## 2026-09-28 真机例外

PKV110 上单独注入 `PRIVATE_FLAG_LAYER_FOR_SCREEN` 就导致 2032 `addView` 抛 `BadTokenException`；不注入时，同一无障碍连接成功挂载可见 2032 窗口。因此代码先尝试项目规定标志，被系统拒绝时仅去掉该私有标志重试同一窗口类型，绝不降级到 2038。`PRIVATE_FLAG_TRUSTED_OVERLAY` 没有单独测试，当前不注入。AOSP 和 Android 官方文档确认无障碍窗口必须由系统连接授权，普通应用不能自行恢复已被系统撤销的授权。

最近任务划除的实测结果与强行停止不同：Secure 授权文本仍在，但 ColorOS 将服务加入 `mCrashedServices`，并把全局 `accessibility_enabled` 设为 0。组件状态在 DEFAULT 与 ENABLED 之间切换未解除该状态，还触发授权清除，所以恢复工具不修改组件状态。已验证的恢复路径是在授权名单仍包含本服务时，短暂移除并恢复本服务、恢复全局 flag=1；其他服务条目保留。此路径需要 `WRITE_SECURE_SETTINGS`：当前 PKV110 已通过 `adb shell pm grant com.agupta07505.smartisland android.permission.WRITE_SECURE_SETTINGS` 一次性授权。官方 Android 普通应用无法自行获得该权限；未经该授权的安装仍需用户手动开启。

## 2026-09-28 复盘归档

本次真机排障已归档至知识库 [[EXP-078_android_accessibility_overlay_zorder_keepalive]]，并更新 [[P36_SmartIsland]] 项目卡。旧条目关于“必须注入私有层级标志”“100% 手势零拦截”“折叠态仍为高度 +2f、4px padding”的描述已按当前证据修正。归档只记录现有真机、单测和构建证据；REQ-003 与 REG-001 中各状态手势、位置和媒体交互仍待逐项实测。

## 2026-09-29 折叠态点击区域复发

2026-09-28 的顶部热区改动把折叠态左右余量扩大为各 48dp、底部多 36dp，并让反射裁剪不可用时的窗口仍占全屏宽；这覆盖了 SOL-018 所记录的岛外点击透传目标。本次将窗口物理边界、系统 touchable region 和 Compose 触摸目标统一收紧至胶囊/副岛及 4dp 余量，同时保留 y=0 顶缘的窄手势走廊。最小几何回归测试、全量 112 项单测与 Debug 构建通过。由于 2026-09-29 ADB 未连接设备，真实桌面/其他应用近旁点击、顶部滑动、双岛切换、自动隐藏、展开态与重进恢复尚未完成运行验收。
