# 宽带业务管理系统 · UI 与交互设计

> 文档版本：v1.0 ｜ 设计日期：2026-09-15
> 配套原型：`broadband-design/design-system.html`（设计系统）、`broadband-design/admin-dashboard.html`（PC 后台运营概览）
> 设计原则：**一套地基，三端同源异色** —— 客户端蓝 `#2563eb` · 师傅端橙 `#ea580c` · PC 后台靛 `#4f46e5`
> 默认无障碍基线：**WCAG AA**（正文对比度 ≥ 4.5:1，大字 ≥ 3:1；触控目标 ≥ 44px）

---

## 设计原则（Design Principles）

- **一套地基，三端同源异色**：所有页面共用同一套间距、字体、圆角、阴影、组件结构与语义色，仅品牌主色与导航形态随端变化，保证用户跨端获得一致的认知与操作习惯。
- **移动优先（Mobile First）**：基础布局适配 320–639px（小程序为主），再向上渐进增强为多列栅格；不引入新变量，间距、圆角、字体、组件全部复用统一设计系统的 `--*` 令牌。
- **单一事实来源（Single Source of Truth）**：规范令牌名是单一事实来源，旧名以别名方式保留，现有页面零改动即可继续运行。
- **状态不只靠颜色**：所有状态必须配合文字/图标，满足 WCAG AA（如「已超时」标签同时有红底＋文字＋⚠）。
- **无障碍优先**：尊重 `prefers-reduced-motion`，键盘可达，焦点环清晰，触控目标 ≥ 44px。

### 三端主题落地（Theme Mapping）

| 端 | data-theme | 主色 | 典型首屏 | 导航范式 |
|---|---|---|---|---|
| 客户端小程序 | `client` | `#2563eb` | 宽带管家（套餐/报装/流量） | 底部 Tab Bar |
| 师傅端小程序 | `worker` | `#ea580c` | 装维助手（今日工单/排班） | 底部 Tab Bar |
| PC 运营后台 | `admin` | `#4f46e5` | 运营概览（受理量/SLA/评价） | 左侧 Sidebar |

所有页面共用同一套间距、字体、圆角、阴影、组件结构与语义色，仅品牌主色与导航形态随端变化。

---

## 主题与色彩体系（Theme & Color）

### 三端主题色

| 端 | 主题色（主色） | 色值 |
|---|---|---|
| 客户端 | 客户端蓝 (client) | `#2563eb` |
| 师傅端 | 师傅端橙 (worker) | `#ea580c` |
| PC 后台 | PC 后台靛 (admin) | `#4f46e5` |

品牌色通过 `data-theme` 切换，组件全部引用 `--brand-*` 变量，实现「同源异色」。

### 品牌色阶梯（随端变化）

| Token | 后台靛 (admin) | 客户端蓝 (client) | 师傅端橙 (worker) |
|---|---|---|---|
| `--brand-50` | #eef2ff | #eff6ff | #fff7ed |
| `--brand-100` | #e0e7ff | #dbeafe | #ffedd5 |
| `--brand-300` | #a5b4fc | #93c5fd | #fdba74 |
| `--brand-500` | #4f46e5 | #2563eb | #ea580c |
| `--brand-600` | #4338ca | #1d4ed8 | #c2410c |
| `--brand-700` | #3730a3 | #1e40af | #9a3412 |
| `--brand-ink` | #ffffff | #ffffff | #ffffff |
| `--brand-soft` | #eef2ff | #eff6ff | #fff7ed |

### 中性色（三端共用）

`#0f172a / #334155 / #64748b / #94a3b8 / #cbd5e1 / #e2e8f0 / #f1f5f9 / #f8fafc`，表面 `#ffffff`。

对应令牌：`--ink-900/700/500/400/300/200/100/50` + `--surface`。

### 语义状态色（三端一致，保证跨端理解统一）

| 语义 | 主色 | 背景 |
|---|---|---|
| 成功 | `#16a34a` | `#dcfce7` |
| 预警 | `#d97706` | `#fef3c7` |
| 危险 | `#dc2626` | `#fee2e2` |
| 信息 | `#2563eb` | `#dbeafe` |

对应令牌：`--c-success/warning/danger/info` + `--c-*-bg`。

### PC 后台运营概览配色映射（令牌引用，不写死色值）

| UI 元素 | 令牌 | 说明 |
|---|---|---|
| 页面背景 | `--ink-50` (#f8fafc) | 浅灰底，衬托白卡 |
| 卡片表面 | `--surface` (#ffffff) | 白底 + `--ink-200` 描边 + `--sh-sm` |
| 侧边栏 | `--surface` | 右侧 `--ink-200` 1px 分隔 |
| 主标题/正文 | `--ink-900` / `--ink-700` | 正文对比度 ≥4.5:1 |
| 次要文字/标签 | `--ink-500` / `--ink-400` | 辅助信息 |
| 品牌主色（按钮/激活导航/关键指标） | `--brand-500/600/700` + `--brand-soft` | 靛 `#4f46e5` 系 |
| 激活导航项 | 底 `--brand-soft` + 字 `--brand-700` | 清晰选中态 |
| KPI 主指标数值 | `--brand-600`（强调） | 其余用 `--ink-900` |
| 环比上升 | `--c-success` (#16a34a) | 配 `--c-success-bg` 胶囊 |
| 环比下降 / 超时 | `--c-danger` (#dc2626) | 双重提示（色＋箭头/文案） |
| 预警 / 待处理 | `--c-warning` (#d97706) | 状态标签 |
| 进行中 / 信息 | `--c-info` (#2563eb) | 状态标签 |
| 焦点环 | `outline: 3px solid var(--brand-300)` | 键盘可见 |

### 阴影与动效（Elevation & Motion）

- 三级阴影：`--sh-sm`（静态卡片）/ `--sh-md`（悬浮抬升）/ `--sh-lg`（浮层 Toast/Modal）。
- 动效时长：交互反馈 ≤ 240ms（`--t-norm`），轻量过渡 140ms（`--t-fast`），缓动 `cubic-bezier(.4,0,.2,1)`。
- **尊重 `prefers-reduced-motion`**：系统开启减弱动效时自动降级为无位移过渡。

---

## 设计令牌（Design Tokens）

### 间距（4px 基准栅格）

| Token | 值 | 用途 |
|---|---|---|
| `--sp-1` | 4px | 紧挨元素内间距 |
| `--sp-2` | 8px | 标签与控件间距 |
| `--sp-3` | 12px | 控件内边距 |
| `--sp-4` | 16px | 卡片内边距、区块间距 |
| `--sp-6` | 24px | 段落/分区间距 |
| `--sp-8` | 32px | 大区块间距 |
| `--sp-12` | 48px | 页面级留白 |

> 单位：Web 用 px、小程序用 rpx。完整阶梯 `--sp-1…--sp-12`（4/8/12/16/20/24/32/40/48）。

### 圆角（四级）

`--r-sm 6px` / `--r-md 10px` / `--r-lg 14px` / `--r-xl 20px` / `--r-full 999px`。

> 小程序端对应 12/20/28/36/999(rpx)。

### 字号（Typography）

- **主字体**：Inter（中文回退 PingFang SC / 微软雅黑），用于界面与标题。
- **等宽字体**：JetBrains Mono，用于工单号、金额、时间、速率等数值。
- **字号阶梯**：12 → 13 → 14 → 16 → 18 → 22 → 28 → 36px（xs → 3xl）。
- **字重**：400 / 500 / 600 / 700。正文行高 1.6，标题字距收紧（-0.5px）。

对应令牌：`--fs-xs…--fs-3xl`（12→36px / 24→56rpx）、`--font-sans` / `--font-mono`。

字体栈：`--font-sans: 'Inter', -apple-system, 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif;`

#### 字号角色映射（PC 后台运营概览）

| 角色 | 令牌 | 值 | 字重 | 说明 |
|---|---|---|---|---|
| 页面 H1 | `--fs-3xl` | 36px | 700 | 字距 -0.5px |
| 区块标题 | `--fs-xl` | 22px | 700 | 各 section 标题 |
| KPI 数值 | `--fs-2xl`~`--fs-3xl` | 28–36px | 700 | sans 粗体，强调冲击力 |
| 正文 | `--fs-base` | 14px | 400 | 行高 1.6 |
| 次要/标签 | `--fs-xs` | 12px | 600 | uppercase + letter-spacing .8px（英文/数字） |
| 工单号·金额·速率·时间 | `--font-mono` (JetBrains Mono) | 12–14px | 500 | 等宽对齐，扫描更易读 |

---

## 组件规范（Component Library）

### 按钮 Button

- **三态**：主操作（实心品牌色）/ 次操作（描边）/ 幽灵（纯文字品牌色）。
- **三尺寸**：`sm 12×32` / `md 14×40` / `lg 16×48`（均 ≥ 44px 触控高度）。
- **状态**：default / hover（抬升+投影，按钮 `translateY(-1px)` + `--sh-md`）/ active（回弹 `translateY(0)`）/ focus-visible（3px 品牌色外环）/ disabled（50% 透明）。
- 圆角 `--r-md`，过渡 `--t-fast`。

### 表单 Form

- 标签左对齐、字重 600；输入控件内边距 `12px 16px`、圆角 `--r-md`、边框 `--ink-300`。
- 聚焦态：边框转品牌色 + 3px `--brand-soft` 光晕。
- 错误态：边框转危险色 + 危险色文案，双重提示。

### 表格 Table

- 表头 `--ink-50` 底、13px 600 字、`--ink-500`；行 hover `--ink-50`（完整表格行 hover 高亮）。
- 表头吸顶；空态有提示。
- 移动端：`@media(max-width:767px)` 将 `thead` 隐藏，每行 `td` 用 `data-label` 伪元素前缀，渲染为卡片；也可用 `aria-label` 保持可读，非空表格有 `<caption>`/`<th scope>`。

### 卡片 Card / 统计卡 Stat

- **卡片 Card**：白底、1px `--ink-200` 描边、`--r-lg`、静态 `--sh-sm`，hover 抬升 `--sh-md`。
- **统计卡 Stat**：大字号数值 + 小字标签，品牌色高亮关键指标（KPI 主指标用 `--brand-600` 强调）。

### 标签 Tag

- success / warning / danger / info / brand 五色语义胶囊，用于状态、类型标记。
- 状态不只靠颜色：标签带文字＋图标；环比带 ↑/↓ 箭头＋数值。

### 进度条 Progress（PC 后台专用）

- SLA 达标率：`0–100%` 动画填充，达标绿/未达标红；`width` 过渡 600ms ease-out（数据加载后填充）。

### 导航 Navigation

- **PC 后台**：左侧 248px 固定侧边栏（品牌区 + 分区导航），主区最大宽度 1180px 居中。
- **小程序**：底部 Tab Bar（首页 / 列表 / 我的），44px 高度，选中态品牌色。
- 公共：导航项 hover `--ink-100`、选中 `--brand-soft` 底 + 品牌色文字。

### 状态与反馈 Feedback

- **骨架屏 Skeleton**：渐变流光动画，用于列表/详情加载占位；首屏流光 1.4s，数据到达即替换。
- **空状态 Empty**：居中图标 + 引导文案，告知用户下一步动作。
- **Toast**：深色浮层 + 成功绿勾，2–3 秒自动消失（运营概览 2.5s），用于操作结果即时反馈。

---

## 页面布局（Page Layout）

### PC 后台框架

- 桌面 248px 固定左栏（品牌区＋分区导航：概览/工单/派单/小区/客户/SLA/系统），主区最大宽度 1180px 居中。
- **≤920px：侧边栏折叠为顶部汉堡抽屉**（点击展开，半透明遮罩，ESC/点击遮罩关闭，焦点陷阱）；关闭后焦点回到触发按钮。

### 信息架构（运营概览 /dashboard）

首屏自上而下分 6 个区块，移动端全部降为单列堆叠，桌面端组合为多列：

| # | 区块 | 内容 | 移动(≤767) | 桌面(≥1024) |
|---|---|---|---|---|
| 1 | 顶部栏 Topbar | 页面标题「运营概览」＋ 日期范围筛选 ＋ 用户菜单 | 标题左、筛选与用户收进右上溢出菜单 | 标题左、筛选/用户右对齐 |
| 2 | 核心指标 KPI | 受理总量 / 安装完成率 / SLA 达标率 / 客户满意度（4 卡，含环比） | 1 列（屏宽≥480 时 2 列） | `auto-fit minmax(220px,1fr)` 自动 2→4 列 |
| 3 | 工单状态看板 | 待派单 / 安装中 / 已完成 / 已超时 四态分布（CSS 柱状/环形） | 单列 4 个迷你卡 | 与右侧 SLA 并列（左 1.4fr / 右 1fr） |
| 4 | SLA 监控 | 时限类 vs 速率类达标率双进度条 ＋ 赔付提醒 | 单列 | 同上右侧 |
| 5 | 近期工单表 | 工单号 / 小区 / 师傅 / 状态 / 时限（语义标签） | 转为卡片列表（每行一张卡） | 完整表格，行 hover 高亮 |
| 6 | 客户评价流 | 最新 3 条评价（星标＋摘要） | 单列 | 2 列或并入右侧栏 |

### 响应式布局骨架

```
移动端 (≤767px)                        桌面端 (≥1024px)
┌──────────────┐                      ┌────────┬──────────────────────┐
│ [☰] 运营概览  │                      │ 侧边栏 │  Topbar (标题+筛选+用户) │
├──────────────┤                      │ 248px  ├──────────────────────┤
│ KPI 卡 (1-2列)│                      │        │  KPI ×4 (auto-fit)      │
│ 工单看板      │      → 增强 →        │        ├──────────┬───────────┤
│ SLA 监控      │                      │        │ 工单看板  │ SLA 监控   │
│ 工单表(卡片)  │                      │        ├──────────┴───────────┤
│ 评价流        │                      │        │  近期工单表(整表)       │
└──────────────┘                      │        ├──────────────────────┤
                                      │        │  客户评价流             │
                                      └────────┴──────────────────────┘
```

### 关键 CSS 策略

- **断点**（沿用设计系统）：`640`（小屏调整）/ `768`（中屏）/ `1024`（后台全功能）/ `1280`（大屏优化）；**≤920 侧边栏转顶栏**。
- KPI 区用 `display:grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: var(--sp-4);` —— **免媒体查询**自适应 1→4 列。
- 工单看板＋SLA 区：`grid-template-columns: 1fr;` 移动；`@media(min-width:1024px){ grid-template-columns: 1.4fr 1fr; }`。
- 表格移动化：`@media(max-width:767px)` 将 `thead` 隐藏，每行 `td` 用 `data-label` 伪元素前缀，渲染为卡片。
- 侧边栏：`@media(max-width:920px){ .side → 固定抽屉 transform: translateX(-100%)，.side.open → 0；遮罩 .scrim 显隐 }`。

---

## 交互规范（Interaction）

### 反馈 Feedback

- **Toast**：深色浮层 + 成功绿勾，2–3 秒（运营概览 2.5s）自动消失，用于操作结果即时反馈（如刷新/导出）。
- **骨架屏**：首屏加载占位流光动画 1.4s，数据到达即替换。
- **进度条**：`width` 过渡 600ms ease-out 填充，达标绿/未达标红。
- **按钮/卡片**：hover `translateY(-1px)` + `--sh-md`；点击 `translateY(0)` 回弹（active 态）。
- **侧边栏抽屉**：`transform: translateX` 240ms；遮罩 `opacity` 240ms。

### 空态 Empty

- 居中图标 + 引导文案，告知用户下一步动作（如近期工单表为空态提示）。

### 加载 Loading

- 首屏采用骨架屏（流光动画）占位；非空表格有 `<caption>`/`<th scope>`。
- 微交互原则：仅动画 `transform` 与 `opacity`（GPU 加速）；单页同屏动画元素 ≤ 3，避免过载。

### 微交互与动效参数

- **原则**：仅动画 `transform` 与 `opacity`（GPU 加速）；单页同屏动画元素 ≤ 3，避免过载。
- **时长**：交互反馈 `--t-fast` 140ms；轻量过渡 `--t-norm` 240ms；缓动 `cubic-bezier(.4,0,.2,1)`。
- **无障碍降级**：`@media (prefers-reduced-motion: reduce)` 时关闭位移/流光，仅保留即时显隐。

### 无障碍检查清单（WCAG 2.2 AA）

- [ ] 正文/品牌色文字对比度 ≥ 4.5:1；大标题 ≥ 3:1（已用靛 `#4f46e5`+白字 / 蓝 `#2563eb` / 橙 `#ea580c` 验证）。
- [ ] 所有交互元素可 Tab 聚焦，`focus-visible` 3px 品牌色外环清晰。
- [ ] 侧边栏抽屉打开时焦点陷阱，关闭后焦点回到触发按钮；ESC 可关。
- [ ] 触控目标 ≥ 44px（按钮、导航项、筛选触发器）。
- [ ] 语义结构：`<aside>/<nav>/<main>/<header>/<section>` + 合理 `h1→h2→h3` 层级，便于读屏。
- [ ] 状态不只靠颜色：标签带文字＋图标；环比带 ↑/↓ 箭头＋数值。
- [ ] 表格移动端用 `aria-label` 或 `data-label` 保持可读；非空表格有 `<caption>`/`<th scope>`。
- [ ] `prefers-reduced-motion` 已降级。
- [ ] 图标按钮带 `aria-label`；装饰图标 `aria-hidden`。

---

## 三端落地令牌映射（Developer Handoff）

设计稿已落实到三端工程，**规范名是单一事实来源**，旧名以别名方式保留，现有页面零改动即可继续运行。

### 单一事实来源（三端共用同名令牌）

| 类别 | 规范令牌（直接用） | 说明 |
|---|---|---|
| 间距 | `--sp-1…--sp-12`（4/8/12/16/20/24/32/40/48） | 4px 基准栅格，单位 web 用 px、小程序用 rpx |
| 圆角 | `--r-sm/md/lg/xl/full` | 6/10/14/20/999(px) 或 12/20/28/36/999(rpx) |
| 字号 | `--fs-xs…--fs-3xl` | 12→36(px) / 24→56(rpx) |
| 字体 | `--font-sans` / `--font-mono` | 界面用 Inter/PingFang；数值用 JetBrains Mono |
| 中性色 | `--ink-900/700/500/400/300/200/100/50` + `--surface` | 三端完全一致 |
| 语义色 | `--c-success/warning/danger/info` + `--c-*-bg` | 状态统一，禁止仅靠颜色 |
| 阴影 | `--sh-sm/md/lg` | |
| 动效 | `--t-fast`(140ms) / `--t-norm`(240ms) | cubic-bezier(.4,0,.2,1) |
| 品牌色 | `--brand-50/100/300/500/600/700/ink/soft` | 端不同值不同（见上表） |

### 各端品牌色取值

| 端点 | `--brand-500`（主色） | 文件 / 位置 |
|---|---|---|
| PC 后台 `admin` | `#4f46e5` | `web-admin/src/styles/index.css` `:root` + `index.html` 的 `data-theme="admin"` |
| 客户端 `client` | `#2563eb` | `broadband-miniapp/app.wxss` `page{}` |
| 师傅端 `worker` | `#ea580c` | `broadband-worker/app.wxss` `page{}` |

> web-admin 额外保留 `[data-theme="client"/"worker"]` 钩子，未来若后台要复用同套令牌切换主题，无需改样式。

### 各端兼容别名（仅供存量页面，新代码请用规范名）

- **web-admin**：`--bd-*`（`--bd-primary`→`var(--brand-500)`、`--bd-bg`→`var(--ink-50)`、`--bd-text`→`var(--ink-900)` 等），以及 Element Plus 的 `--el-color-primary-*`。
- **小程序**：`--c` / `--c-50` / `--c-100` / `--c-200` / `--c-600` / `--c-700` 仍保留，新页面建议改用 `--brand-*` / `--ink-*` / `--sp-*`。

### 迁移建议（渐进式）

1. 新组件 / 新页面：一律引用规范令牌，不再新增 `--bd-*` / `--c-*` 写法。
2. 存量页面：随迭代把 `--bd-primary` / `--c` 等替换为 `--brand-500` 等，替换后视觉无变化（值已对齐）。
3. 状态色：统一用 `--c-success/warning/danger/info` + 对应 `*-bg`，并配合文字/图标，满足 WCAG AA。

### 研发验收点

1. 改 `data-theme` 整页换肤（admin/client/worker 三套已定义）。
2. 320px 下单列可读、触控可达；≥1024px 自动多列。
3. 间距全部取自 `--sp-*`；状态色统一语义 Token。
4. 键盘 Tab 全可达，焦点环清晰；`prefers-reduced-motion` 生效。

---

**UI Designer**：UI Designer（用户界面设计师）
**Design System Date**：2026-09-15
**Implementation**：Ready for developer handoff
**QA Process**：设计评审 + 令牌一致性校验已建立
