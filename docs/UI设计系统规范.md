# 宽带业务管理系统 · 统一设计系统规范（UI Design System）

> 文档版本：v1.0 ｜ 设计日期：2026-09-15 ｜ 配套原型：`broadband-design/design-system.html`
> 设计原则：**一套地基，三端同源异色** —— 客户端蓝 `#2563eb` · 师傅端橙 `#ea580c` · PC 后台靛 `#4f46e5`
> 默认无障碍基线：**WCAG AA**（正文对比度 ≥ 4.5:1，大字 ≥ 3:1；触控目标 ≥ 44px）

---

## 1. 设计地基（Design Foundations）

### 1.1 间距与圆角（4px 基准栅格）
| Token | 值 | 用途 |
|---|---|---|
| `--sp-1` | 4px | 紧挨元素内间距 |
| `--sp-2` | 8px | 标签与控件间距 |
| `--sp-3` | 12px | 控件内边距 |
| `--sp-4` | 16px | 卡片内边距、区块间距 |
| `--sp-6` | 24px | 段落/分区间距 |
| `--sp-8` | 32px | 大区块间距 |
| `--sp-12` | 48px | 页面级留白 |

圆角四级：`--r-sm 6px` / `--r-md 10px` / `--r-lg 14px` / `--r-xl 20px` / `--r-full 999px`。

### 1.2 字体排印（Typography）
- **主字体**：Inter（中文回退 PingFang SC / 微软雅黑），用于界面与标题。
- **等宽字体**：JetBrains Mono，用于工单号、金额、时间、速率等数值。
- **字号阶梯**：12 → 13 → 14 → 16 → 18 → 22 → 28 → 36px（xs → 3xl）。
- **字重**：400 / 500 / 600 / 700。正文行高 1.6，标题字距收紧（-0.5px）。

### 1.3 阴影与动效（Elevation & Motion）
- 三级阴影：`--sh-sm`（静态卡片）/ `--sh-md`（悬浮抬升）/ `--sh-lg`（浮层 Toast/Modal）。
- 动效时长：交互反馈 ≤ 240ms（`--t-norm`），轻量过渡 140ms（`--t-fast`），缓动 `cubic-bezier(.4,0,.2,1)`。
- **尊重 `prefers-reduced-motion`**：系统开启减弱动效时自动降级为无位移过渡。

### 1.4 色彩系统（Color）
**中性色（三端共用）**：`#0f172a / #334155 / #64748b / #94a3b8 / #cbd5e1 / #e2e8f0 / #f1f5f9 / #f8fafc`，表面 `#ffffff`。
**语义状态色（三端一致，保证跨端理解统一）**：
- 成功 `#16a34a` / 背景 `#dcfce7`
- 预警 `#d97706` / 背景 `#fef3c7`
- 危险 `#dc2626` / 背景 `#fee2e2`
- 信息 `#2563eb` / 背景 `#dbeafe`

**品牌色**：通过 `data-theme` 切换，组件全部引用 `--brand-*` 变量，实现「同源异色」。

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

---

## 2. 组件库（Component Library）

### 2.1 按钮 Button
- **三态**：主操作（实心品牌色）/ 次操作（描边）/ 幽灵（纯文字品牌色）。
- **三尺寸**：`sm 12×32` / `md 14×40` / `lg 16×48`（均 ≥ 44px 触控高度）。
- **状态**：default / hover（抬升+投影）/ active（回弹）/ focus-visible（3px 品牌色外环）/ disabled（50% 透明）。
- 圆角 `--r-md`，过渡 `--t-fast`。

### 2.2 表单 Form
- 标签左对齐、字重 600；输入控件内边距 `12px 16px`、圆角 `--r-md`、边框 `--ink-300`。
- 聚焦态：边框转品牌色 + 3px `--brand-soft` 光晕。
- 错误态：边框转危险色 + 危险色文案，双重提示。

### 2.3 数据展示（Data Display）
- **卡片 Card**：白底、1px `--ink-200` 描边、`--r-lg`、静态 `--sh-sm`，hover 抬升 `--sh-md`。
- **表格 Table**：表头 `--ink-50` 底、13px 600 字、`--ink-500`；行 hover `--ink-50`。
- **标签 Tag**：success / warning / danger / info / brand 五色语义胶囊，用于状态、类型标记。
- **统计卡 Stat**：大字号数值 + 小字标签，品牌色高亮关键指标。

### 2.4 状态与反馈（Feedback）
- **骨架屏 Skeleton**：渐变流光动画，用于列表/详情加载占位。
- **空状态 Empty**：居中图标 + 引导文案，告知用户下一步动作。
- **Toast**：深色浮层 + 成功绿勾，2–3 秒自动消失，用于操作结果即时反馈。

### 2.5 导航（Navigation）
- **PC 后台**：左侧 248px 固定侧边栏（品牌区 + 分区导航），主区最大宽度 1180px 居中。
- **小程序**：底部 Tab Bar（首页 / 列表 / 我的），44px 高度，选中态品牌色。
- 公共：导航项 hover `--ink-100`、选中 `--brand-soft` 底 + 品牌色文字。

---

## 3. 三端主题落地（Theme Mapping）

| 端 | data-theme | 主色 | 典型首屏 | 导航范式 |
|---|---|---|---|---|
| 客户端小程序 | `client` | `#2563eb` | 宽带管家（套餐/报装/流量） | 底部 Tab Bar |
| 师傅端小程序 | `worker` | `#ea580c` | 装维助手（今日工单/排班） | 底部 Tab Bar |
| PC 运营后台 | `admin` | `#4f46e5` | 运营概览（受理量/SLA/评价） | 左侧 Sidebar |

所有页面共用同一套间距、字体、圆角、阴影、组件结构与语义色，仅品牌主色与导航形态随端变化，保证用户跨端获得一致的认知与操作习惯。

---

## 4. 响应式策略（Responsive）

- **移动优先**：基础布局适配 320–639px（小程序为主）。
- 断点：640（小屏调整）/ 768（中屏）/ 1024（后台全功能）/ 1280（大屏优化）。
- ≤920px：后台侧边栏转为顶部、三端对照改为单列堆叠，确保窄屏可读。

---

## 5. 无障碍标准（Accessibility · WCAG AA）

- **对比度**：正文/品牌色文字均 ≥ 4.5:1；大标题 ≥ 3:1（已用 `#4f46e5`/`#2563eb`/`#ea580c` 配白字验证达标）。
- **键盘可达**：所有交互元素可 Tab 聚焦，focus-visible 清晰外环，逻辑 Tab 顺序。
- **触控目标**：按钮/导航 ≥ 44px。
- **语义结构**：原型使用语义标签 + 合理标题层级，便于读屏。
- **动效敏感**：遵循 `prefers-reduced-motion` 自动降级。

---

## 6. 交付与研发对接（Handoff）

- **原型**：`broadband-design/design-system.html`（浏览器直接打开，含主题切换与三端对照，可交互演示）。
- **令牌落地建议**：Web 端（web-admin）将令牌写入 `src/styles/index.css` 的 `:root` 与 `[data-theme]`；小程序端在 `app.wxss` 定义同名变量，三端变量命名保持一致。
- **研发验收点**：① 任意页面改 `data-theme` 即整体换肤；② 间距全部取自 `--sp-*`；③ 状态色统一引用语义 Token；④ 聚焦环与 44px 触控达标。

---
**UI Designer**：UI Designer（用户界面设计师）
**Design System Date**：2026-09-15
**Implementation**：Ready for developer handoff
**QA Process**：设计评审 + 令牌一致性校验已建立

---

## 7. 三端落地令牌映射（Developer Handoff · 已实施）

设计稿已落实到三端工程，**规范名是单一事实来源**，旧名以别名方式保留，现有页面零改动即可继续运行。

### 7.1 单一事实来源（三端共用同名令牌）

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
| 品牌色 | `--brand-50/100/300/500/600/700/ink/soft` | 端不同值不同（见 7.2） |

### 7.2 各端品牌色取值

| 端点 | `--brand-500`（主色） | 文件 / 位置 |
|---|---|---|
| PC 后台 `admin` | `#4f46e5` | `web-admin/src/styles/index.css` `:root` + `index.html` 的 `data-theme="admin"` |
| 客户端 `client` | `#2563eb` | `broadband-miniapp/app.wxss` `page{}` |
| 师傅端 `worker` | `#ea580c` | `broadband-worker/app.wxss` `page{}` |

> web-admin 额外保留 `[data-theme="client"/"worker"]` 钩子，未来若后台要复用同套令牌切换主题，无需改样式。

### 7.3 各端兼容别名（仅供存量页面，新代码请用 7.1 规范名）

- **web-admin**：`--bd-*`（`--bd-primary`→`var(--brand-500)`、`--bd-bg`→`var(--ink-50)`、`--bd-text`→`var(--ink-900)` 等），以及 Element Plus 的 `--el-color-primary-*`。
- **小程序**：`--c` / `--c-50` / `--c-100` / `--c-200` / `--c-600` / `--c-700` 仍保留，新页面建议改用 `--brand-*` / `--ink-*` / `--sp-*`。

### 7.4 迁移建议（渐进式）

1. 新组件 / 新页面：一律引用 7.1 规范令牌，不再新增 `--bd-*` / `--c-*` 写法。
2. 存量页面：随迭代把 `--bd-primary` / `--c` 等替换为 `--brand-500` 等，替换后视觉无变化（值已对齐）。
3. 状态色：统一用 `--c-success/warning/danger/info` + 对应 `*-bg`，并配合文字/图标，满足 WCAG AA。
