# -*- coding: utf-8 -*-
"""按最新真实口径重写《项目进度与开发计划.xlsx》（v1.7 · AppID 就位 + M10 收尾）"""
import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side

PATH = '/Users/mezo/WorkBuddy/2026-09-14-09-59-23/docs/项目进度与开发计划.xlsx'

TITLE_FONT = Font(bold=True, size=14, color='1E1B4B')
SUB_FONT = Font(size=9, color='6B7280')
SEC_FONT = Font(bold=True, size=12, color='4F46E5')
HDR_FILL = PatternFill('solid', fgColor='4F46E5')
HDR_FONT = Font(bold=True, size=11, color='FFFFFF')
BODY_FONT = Font(size=11)
DONE_FONT = Font(size=11, color='15803D')
THIN = Side(style='thin', color='D1D5DB')
BORDER = Border(left=THIN, right=THIN, top=THIN, bottom=THIN)


def header(ws, row, titles):
    for i, t in enumerate(titles, start=1):
        c = ws.cell(row=row, column=i, value=t)
        c.fill = HDR_FILL
        c.font = HDR_FONT
        c.alignment = Alignment(horizontal='center', vertical='center', wrap_text=True)
        c.border = BORDER


def body(ws, row, values, center_cols=()):
    for i, v in enumerate(values, start=1):
        c = ws.cell(row=row, column=i, value=v)
        c.font = DONE_FONT if (isinstance(v, str) and v.startswith('✅')) else BODY_FONT
        c.alignment = Alignment(
            horizontal='center' if i in center_cols else 'left',
            vertical='top', wrap_text=True)
        c.border = BORDER


def widths(ws, mapping):
    for k, v in mapping.items():
        ws.column_dimensions[k].width = v


wb = openpyxl.Workbook()

# ============ 1. 功能点及完成进度 ============
ws = wb.active
ws.title = '功能点及完成进度'
ws['A1'] = '宽带业务管理系统 · 功能点及完成进度'
ws['A1'].font = TITLE_FONT
ws.merge_cells('A1:E1')
ws['A2'] = ('生成日期 2026-09-15 ｜ 后端 4 模块+1公共层 / 56 接口 / 小程序 28 页 / PC后台 25 页 '
            '｜ 口径版本 v1.7（M9 双端登录态收口 + M10 师傅端数据绑定与隔离 已完成；微信 AppID 已就位，缺 AppSecret）')
ws['A2'].font = SUB_FONT
ws.merge_cells('A2:E2')

header(ws, 4, ['模块', '功能点', '状态', '完成度', '说明 / 关联接口'])
rows = [
    ('后端 · 小区覆盖 community', '覆盖查询 / 覆盖需求登记 / 后台小区维护', '✅ 已完成', '100%',
     'GET /api/community/check · POST /api/community/demand · GET,POST /api/admin/communities'),
    ('后端 · 派单 dispatch', '容量规则(相邻3-4/非相邻1-2) · 并查集聚类 · 贪心派单 · 超容拦截', '✅ 已完成', '100%',
     'POST /api/dispatch/run · GET /api/dispatch/capacity'),
    ('后端 · 装维 SLA（install）', '时限类(超时) · 速率类(达标/赔付) · 看板 · 规则 · 赔付明细', '✅ 已完成', '100%',
     '/api/sla/evaluate(-batch)/board/rules/compensations'),
    ('后端 · 工单与师傅（install）', '工单池 · 工单详情 · 重置 · 师傅列表 · 容量配置', '✅ 已完成', '100%',
     'GET /api/admin/work-orders(/{id}) · POST reset · GET /api/admin/workers · POST worker-capacity'),
    ('后端 · 套餐与升级（product）', '在售列表 · 详情 · 升档选项 · 一次性补差=月差×合约剩余月数', '✅ 已完成', '100%',
     '/api/package/list · detail · upgrade-options · POST upgrade'),
    ('后端 · 流量监控（product）', '流量条 / 剩余 / 预警 / 7日趋势', '✅ 已完成', '100%', 'GET /api/traffic/usage'),
    ('后端 · 客户订单（product）', '本人订单查询（C端，已鉴权）', '✅ 已完成', '100%', 'GET /api/order/my?customerId='),
    ('后端 · 后台聚合（product）', '客户 / 套餐 / 升级单 / 评价 / 销售 / 财务报表', '✅ 已完成', '100%', '/api/admin/customers · packages · upgrade-orders · reviews · sales/report · finance/report'),
    ('后端 · RBAC 权限（system）', '自研HS256 JWT · 实时查库授权 · 5角色/21权限码/26菜单 · 审计日志', '✅ 已完成', '100%',
     '系统模块 20 接口：auth / users / roles / menus / logs / platform'),
    ('后端 · 微信登录双路径（M9新增）', '配置 appid+secret 走 code2Session→openid 绑定客户；未配置回退演示码 1234', '✅ 已完成', '100%',
     'WechatMiniAppService · POST /api/auth/miniapp-login（permitAll）'),
    ('后端 · 微信 AppID 落地（M10）', '两端 project.config.json + 后端 application.yml 均写入真实 AppID', '✅ 已完成', '100%',
     'AppID wxcc344ad7f4f849d4；AppSecret 仍由环境变量 WECHAT_SECRET 注入（未注入即走演示码回退）'),
    ('后端 · C端鉴权收口（M9）', 'protect-client-api=true，C 端接口强制校验 JWT', '✅ 已完成', '100%',
     'JwtAuthFilter 新增 CUSTOMER 分支（CustomerPrincipal + ROLE_CUSTOMER）'),
    ('后端 · 师傅端登录（M9新增）', '按 worker.phone 查表签发 WORKER 令牌', '✅ 已完成', '100%', 'POST /api/auth/worker-login（permitAll）'),
    ('后端 · WORKER 令牌鉴权（M9）', '师傅令牌可访问工单/SLA 接口，与后台 RBAC 隔离', '✅ 已完成', '100%',
     'WorkerPrincipal + ROLE_WORKER + workorder:view + sla:view'),
    ('后端 · 师傅端数据接口（M10新增）', '本人工单列表/详情 · 首页统计 · 容量看板 · 7天排班 · 完工提交', '✅ 已完成', '100%',
     'WorkerController 6 接口 /api/worker/*；服务端按 token 隔离，忽略客户端 workerId 传参'),
    ('后端 · 数据库接入 MySQL', '25表 · 种子数据 · 启动自动初始化 · 全部接口连库', '✅ 已完成', '100%',
     'schema.sql + data.sql（幂等）；M9 增列 customer.openid / worker.phone'),
    ('后端 · 统一请求/错误处理', '非2xx一律reject（原静默吞错）；401 自动跳登录页', '✅ 已完成', '100%', 'utils/api.js（客户端 + 师傅端）'),
    ('客户端小程序 broadband-miniapp', '20页：首页/服务/套餐(详情,列表,升级)/小区查询/订单(地址,确认,列表,详情)/地址/流量/移机/自助/智慧家庭/评价/我的/设置/登录', '✅ 已完成', '100%',
     '蓝#2563eb；登录走真实 miniapp-login，401 门禁已接'),
    ('安装师傅端小程序 broadband-worker', '8页：首页/工单/工单详情/完工确认/容量/排班/我的/登录', '✅ 已完成', '100%',
     '橙#ea580c；登录改调真实 worker-login（原 mock 已废）'),
    ('PC后台 web-admin', '25页/22路由：仪表盘/套餐/小区/工单/派单/容量/SLA/流量/升级/RBAC/客户/评价/审计/监控', '✅ 已完成', '100%',
     'Vue3+ElementPlus按需+Pinia；按钮级 v-perm；菜单由后端 /api/auth/me 动态渲染'),
    ('外部凭证与环境变量', 'AppID 已配置 / Secret 待注入；JWT 密钥、DB 凭据、C 端鉴权开关均已明确', '🟡 90%', '—',
     'AppID wxcc344ad7f4f849d4（两端共用）；缺 WECHAT_SECRET 时自动回退演示码 1234'),
    ('文档套件', '5份说明书 + 模块文档 + 进度计划表 v1.7 + 设计稿 v2.1', '✅ 已完成', '100%', 'md + docx + xlsx + html 已同步项目空间'),
]
r = 5
for row in rows:
    body(ws, r, list(row), center_cols=(3, 4))
    r += 1

r += 1
ws.cell(row=r, column=1, value='总体进度统计').font = SEC_FONT
r += 1
header(ws, r, ['维度', '已完成', '规划中', '覆盖率', '备注'])
stat_start = r + 1
stats = [
    ('后端业务模块', '4 + 1公共层', '0', '100%', 'community / install / product / system + common'),
    ('后端 REST 接口', '56（GET36 / POST15 / PUT5）', '0', '100%', 'M9 +worker-login（49→50）；M10 +6 个 /api/worker/*（50→56）'),
    ('数据库表', '25 张 + 种子数据', '0', '100%', 'M9 仅加列未加表'),
    ('安全与鉴权', '5角色 / 26菜单 / 21权限码', '0', '100%', 'M9 扩至 3 类令牌：SYS / CUSTOMER / WORKER'),
    ('客户端小程序页', '20（含登录页）', '0', '100%', '设计稿 20 页全覆盖'),
    ('安装师傅端小程序页', '8（含登录页）', '0', '100%', '设计稿 8 页全覆盖'),
    ('PC 后台页面', '25（22业务 + login + 403 + 404）', '0', '100%', '设计稿 16 模块全覆盖'),
    ('文档说明书', '说明书 v1.7（md/docx）+ 进度计划表 v1.7 + 设计稿 v2.1', '0', '100%', 'md/docx/xlsx/html 四格式'),
]
r = stat_start
for row in stats:
    body(ws, r, list(row), center_cols=(3, 4))
    r += 1
widths(ws, {'A': 26, 'B': 34, 'C': 12, 'D': 12, 'E': 52})
ws.freeze_panes = 'A5'

# ============ 2. 待开发功能 ============
ws2 = wb.create_sheet('待开发功能')
ws2['A1'] = '宽带业务管理系统 · 待开发功能清单（截至 2026-09-15，M10 收尾后）'
ws2['A1'].font = TITLE_FONT
ws2.merge_cells('A1:E1')
ws2['A2'] = '说明：原「微信真实凭证接入」已完成一半——AppID 已写入两端与后端配置，仅缺 AppSecret；优先级为建议值，最终以负责人排期确认为准。'
ws2['A2'].font = SUB_FONT
ws2.merge_cells('A2:E2')
header(ws2, 4, ['#', '待开发功能', '当前状态', '依赖 / 备注', '建议优先级'])
todo = [
    ('1', '微信 AppSecret 注入', 'AppID 已配置（wxcc344ad7f4f849d4），登录仍走演示码 1234 回退',
     '提供 Secret 并导出环境变量 WECHAT_SECRET 后，自动切换 code2Session 真实登录，前端无需改动', 'P0'),
    ('2', '师傅端页面视觉验收（M10b）', '数据绑定已完成，页面截图验收未做',
     '需微信开发者工具导入 broadband-worker 后逐页截图验收（无浏览器环境）', 'P0'),
    ('3', '业务闭环联调', '下单→支付→派单→装机→SLA赔付→评价 未端到端打通',
     '需订单状态机串联；与微信支付解耦可先跑通非支付链路', 'P0'),
    ('4', '微信支付 / 真实支付闭环', '未接入', '需商户号 + 支付回调 + 幂等处理', 'P1'),
    ('5', '监控深化（可观测性）', '未做', 'metrics / 日志聚合 / 告警；性能监控页接真实数据', 'P1'),
    ('6', '配置治理（多环境 profile）', 'yml + 环境变量，未做环境隔离', '架构决策：不引入 Apollo；做 dev/test/prod profile 隔离', 'P1'),
    ('7', 'CI/CD 流水线', '未做', '构建/测试/部署自动化；GitHub 仓库已建（linxi072/broadband）', 'P1'),
    ('8', '压测与性能优化', '未做', '派单并发、DB 连接池、接口响应时长', 'P2'),
    ('9', 'PC 后台扩展模块', '销售/财务/监控等页已建，部分数据深度不足', '对齐设计稿 v2.1 剩余字段与图表', 'P2'),
    ('10', '小程序 AppID 拆分（合规确认）', '客户端与师傅端共用同一 AppID', '提审前确认走「同主体多端」还是为师傅端单开 AppID', 'P1'),
    ('11', '多端适配 / 国际化（可选）', '未规划', '视业务需要', 'P3'),
]
r = 5
for row in todo:
    body(ws2, r, list(row), center_cols=(1, 5))
    r += 1
widths(ws2, {'A': 5, 'B': 30, 'C': 36, 'D': 44, 'E': 12})
ws2.freeze_panes = 'A5'

# ============ 3. 里程碑与后续迭代方案 ============
ws3 = wb.create_sheet('里程碑与迭代计划')
ws3['A1'] = '宽带业务管理系统 · 里程碑与后续迭代计划'
ws3['A1'].font = TITLE_FONT
ws3.merge_cells('A1:F1')
ws3['A2'] = 'M1–M10 为已完成里程碑；M10b 起为建议排期（相对周次，需负责人确认后定稿）。代码已全量推送 GitHub（linxi072/broadband）。'
ws3['A2'].font = SUB_FONT
ws3.merge_cells('A2:F2')
header(ws3, 4, ['里程碑', '目标', '关键交付物', '状态', '时间 / 建议周期', '风险 / 注意'])
ms = [
    ('M1 需求与设计稿', '需求梳理 + 高保真设计稿', 'broadband-design/index.html v1.9', '✅ 已完成', '2026-09-14', '—'),
    ('M2 后端核心算法 + 接口', '派单/SLA/补差算法与首批接口', '3 模块 / 13 接口 / 6 个可运行 Demo', '✅ 已完成', '2026-09-14', '—'),
    ('M3 客户端小程序骨架', '客户端页面与请求封装', '20 页（含登录页）+ api 封装', '✅ 已完成', '2026-09-14', '—'),
    ('M4 数据库接入与联调', 'MySQL 持久化 + 真实联调', '17 表 + 幂等初始化 + 端到端验证', '✅ 已完成', '2026-09-14', '—'),
    ('M5 PC 后台落地', 'Vue3 运营后台', 'web-admin 工程 + 22 业务页 + 22 路由', '✅ 已完成', '2026-09-14', '—'),
    ('M6 安全体系', 'RBAC + JWT 鉴权', '5 角色 / 21 权限码 / 鉴权矩阵实测 / 7 条单测', '✅ 已完成', '2026-09-14', '—'),
    ('M7 小程序补页 + 登录态', '客户端 20 页 + 师傅端 8 页', '两个独立小程序工程 + 登录态骨架', '✅ 已完成', '2026-09-14', '—'),
    ('M8 缺陷修复（小程序契约/登录）', '修复 4 个真实缺陷', '404/越权端点、静默吞错、mock 登录；接口 45→49', '✅ 已完成', '2026-09-14', '—'),
    ('M9 双端登录态收口', 'C 端真正鉴权 + 师傅端真实登录', 'CUSTOMER/WORKER 令牌 · protect-client-api=true · miniapp-login 双路径 · worker-login；接口 49→50', '✅ 已完成', '2026-09-14', 'AppSecret 未注入，登录走演示码回退（AppID 已于 M10 补上）'),
    ('M10 师傅端数据绑定与隔离', '本人工单 + 统计/容量/排班/完工 真实数据', 'WorkerController 6 接口 · 服务端按 token 隔离（忽略客户端 workerId）· 4 个 mock 页面改真实数据 · 接口 50→56', '✅ 已完成', '2026-09-14', '13 项验证全绿（含 5 项越权/隔离用例）'),
    ('M10b 微信 AppSecret 接入', '真实 code2Session 登录', '导出 WECHAT_SECRET 环境变量，重启后端；验证 openid 落库与绑定客户', '⬜ 规划中', '随时（拿到 Secret 即刻）', 'Secret 属敏感项，不入库、不入文档'),
    ('M10c 师傅端视觉验收', '页面截图与交互验收', '微信开发者工具导入 broadband-worker 后逐页截图', '⬜ 规划中', '建议 W1', '无浏览器环境，需人工验收'),
    ('M11 业务闭环联调', '下单→派单→装机→SLA→评价 端到端', '订单状态机 · 派单回写 · SLA 赔付 · 评价落库', '⬜ 规划中', '建议 W2–W3', '多状态一致性；支付可后置'),
    ('M12 监控与配置治理', '可观测性 + 多环境配置', 'metrics/日志/告警 · 多环境 profile · 监控页接数', '⬜ 规划中', '建议 W3–W4', '监控组件选型；不引入 Apollo 的边界'),
    ('M13 CI/CD 与上线', '自动化构建部署 + 文档收尾', 'CI 流水线 · 生产 Nginx 部署 · 文档终版 · 复盘', '⬜ 规划中', '建议 W4–W5', '生产密钥管理（jwt.secret 走环境变量）；GitHub 推送待 SSH 恢复'),
]
r = 5
for row in ms:
    body(ws3, r, list(row), center_cols=(4, 5))
    r += 1
widths(ws3, {'A': 24, 'B': 30, 'C': 46, 'D': 12, 'E': 16, 'F': 40})
ws3.freeze_panes = 'A5'

# ============ 4. 接口与模块清单 ============
ws4 = wb.create_sheet('接口与模块清单')
ws4['A1'] = '后端模块与接口清单（56 接口 · GET36 / POST15 / PUT5）'
ws4['A1'].font = TITLE_FONT
ws4.merge_cells('A1:E1')
ws4['A2'] = '口径 v1.7（M9 + M10 后）：新增 POST /api/auth/worker-login 与 6 个 /api/worker/* 接口；AppID wxcc344ad7f4f849d4 已回填，全部接口连库可用。'
ws4['A2'].font = SUB_FONT
ws4.merge_cells('A2:E2')
header(ws4, 4, ['模块', '职责', '接口数', '方法分布', '代表接口'])
mods = [
    ('community', '小区覆盖查询 / 需求登记 / 后台小区维护', '4', 'GET2 / POST2', '/api/community/check · /api/community/demand · /api/admin/communities(GET,POST)'),
    ('install · dispatch', '派单调度 + 容量计算', '2', 'POST1 / GET1', '/api/dispatch/run · /api/dispatch/capacity'),
    ('install · SLA', '装维时限 / 速率达标 / 赔付', '5', 'POST2 / GET3', '/api/sla/evaluate(-batch) · /board · /rules · /compensations'),
    ('install · 工单与师傅', '工单池 / 师傅 / 容量配置', '5', 'GET3 / POST2', '/api/admin/work-orders(/{id}) · reset · /api/admin/workers · worker-capacity'),
    ('install · 师傅端（M10新增）', '本人工单 / 首页统计 / 容量看板 / 排班 / 完工提交', '6', 'GET5 / POST1', '/api/worker/work-orders(/{id}) · /summary · /capacity · /schedule · /{id}/complete'),
    ('product', '套餐 / 升级 / 流量 / 客户订单 / 后台聚合', '14', 'GET11 / POST2 / PUT1', '/api/package/* · /api/traffic/usage · /api/order/my · /api/admin/*'),
    ('system（RBAC + 平台 + 登录）', '认证 / 用户 / 角色 / 菜单 / 日志 / 平台聚合', '20', 'GET11 / POST5 / PUT4', '/api/auth/login · miniapp-login · worker-login · me · logout · /api/system/* · /api/admin/dashboard/*'),
    ('公共层 common', '主键生成 / 全局异常处理', '—', '—', 'Ids · GlobalExceptionHandler'),
    ('合计', '4 模块 + 1 公共层', '56', 'GET36 / POST15 / PUT5', '全部连库'),
]
r = 5
for row in mods:
    body(ws4, r, list(row), center_cols=(3, 4))
    r += 1
widths(ws4, {'A': 24, 'B': 32, 'C': 9, 'D': 18, 'E': 56})
ws4.freeze_panes = 'A5'

wb.save(PATH)
print('saved', PATH)
