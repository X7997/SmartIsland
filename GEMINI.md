# Smart Island (灵动岛) 架构设计不变量与开发约束规约

请参阅 [AGENTS.md](file:///q:/DynamicIsland/AGENTS.md) 了解完整的六大核心铁律、坐标推导及防回退审查流程。
在对本项目进行任何修改时，必须严格遵守 `AGENTS.md` 中定义的六大不可动摇铁律：
1. **主灵动岛必须永远绝对居中**（无论是否存在副岛，物理中心锁定在 `screenWidth / 2 + settings.xOffset`）；
2. **副灵动岛必须无条件位于主灵动岛左侧**（严禁出现在右侧）；
3. **悬浮窗层级必须为系统最顶层**（`TYPE_ACCESSIBILITY_OVERLAY` 2400 层级）；
4. **滑动手势流绝不允许被中间阻断**（`PointerEventPass.Initial`，禁止 `isConsumed break`）；
5. **网易云与主流音频播放优先锁定**（`isMusicActive == true` 优先保障主流音乐会话）；
6. **划掉后台时彻底销毁，冷启动时自愈拉起**。
