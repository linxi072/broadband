<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ping } from '@/api/auth'
import { HOME_PATH } from '@/router/routes'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const form = reactive({ username: 'admin', password: 'admin123' })
const loading = ref(false)
const backendOnline = ref(null)

onMounted(async () => {
  try {
    await ping()
    backendOnline.value = true
  } catch (e) {
    // 401 也代表后端在线（只是未登录）；只有「无响应」才视为离线。
    // 注意：ping 返回的 401 已被拦截器忽略（不会触发跳登录），此处仅用于状态展示。
    backendOnline.value = !!(e && e.status === 401)
  }
})

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号与密码')
    return
  }
  loading.value = true
  try {
    await user.login({ username: form.username.trim(), password: form.password })
    if (user.demoMode) ElMessage.warning('后端未连接，已进入本地演示模式')
    else ElMessage.success('登录成功')
    const redirect = route.query.redirect
    router.replace(redirect && redirect !== '/login' ? redirect : HOME_PATH)
  } catch (e) {
    // 全局拦截器已提示账号密码错误
  } finally {
    loading.value = false
  }
}

function fill(role) {
  const map = {
    admin: { username: 'admin', password: 'admin123' },
    operator: { username: 'liuwei', password: 'liuwei123' },
    finance: { username: 'zhaomin', password: 'zhaomin123' },
    cs: { username: 'wangfang', password: 'wangfang123' }
  }
  Object.assign(form, map[role])
}
</script>

<template>
  <div class="login">
    <div class="bg"></div>
    <div class="stage">
      <!-- 品牌侧 -->
      <section class="brand-side">
        <div class="logo">宽</div>
        <h1>宽带业务管理系统</h1>
        <p class="sub">Broadband Operation Platform · 运营后台</p>

        <ul class="feats">
          <li><b>可安装性校验</b><span>查覆盖 → 下单链路实时拦截超卖</span></li>
          <li><b>智能派单调度</b><span>相邻小区聚类 + 时段容量管控</span></li>
          <li><b>装维 SLA 赔付</b><span>慢必赔 / 网速不达标自动赔付</span></li>
          <li><b>套餐灵活升级</b><span>补差按剩余合约天数折算</span></li>
        </ul>

        <div class="ver">v1.2 ｜ 后端 13 接口 · MySQL 17 表 ｜ RBAC 已启用</div>
      </section>

      <!-- 表单侧 -->
      <section class="form-side">
        <h2>运营后台登录</h2>
        <p class="tip">
          后端状态：
          <el-tag v-if="backendOnline === true" type="success" size="small" effect="light">
            在线（:8082）
          </el-tag>
          <el-tag v-else-if="backendOnline === false" type="warning" size="small" effect="light">
            离线 · 可演示登录
          </el-tag>
          <el-tag v-else type="info" size="small" effect="light">检测中…</el-tag>
        </p>

        <el-form :model="form" label-position="top" @submit.prevent="submit">
          <el-form-item label="账号">
            <el-input v-model="form.username" size="large" placeholder="请输入账号" clearable />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              type="password"
              size="large"
              placeholder="请输入密码"
              show-password
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-button
            type="primary"
            size="large"
            style="width: 100%"
            :loading="loading"
            @click="submit"
          >
            登 录
          </el-button>
        </el-form>

        <div class="quick">
          <span>演示账号：</span>
          <el-link type="primary" :underline="false" @click="fill('admin')">超级管理员</el-link>
          <el-link type="primary" :underline="false" @click="fill('operator')">运营专员</el-link>
          <el-link type="primary" :underline="false" @click="fill('finance')">财务</el-link>
          <el-link type="primary" :underline="false" @click="fill('cs')">客服</el-link>
        </div>

        <div class="roles">
          <b>角色权限差异（RBAC）</b>
          <p>超级管理员：全部 14 个模块 ｜ 运营专员：全部业务模块 ｜ 财务：看板 + 订单 + 客户 + 财务 ｜ 客服：看板 + 订单 + 客户 + 投诉评价</p>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.login {
  position: relative;
  height: 100%;
  overflow: auto;
  background: #f5f6fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(900px 420px at 12% 8%, rgba(79, 70, 229, 0.16), transparent 62%),
    radial-gradient(700px 380px at 92% 92%, rgba(37, 99, 235, 0.12), transparent 60%);
}

.stage {
  position: relative;
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  width: 920px;
  max-width: 94vw;
  margin: 40px 0;
  background: #fff;
  border: 1px solid var(--bd-border);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 18px 60px rgba(30, 27, 75, 0.14);
}

/* 品牌侧 */
.brand-side {
  padding: 40px 38px;
  background: linear-gradient(158deg, #312e81 0%, #4f46e5 62%, #6366f1 100%);
  color: #fff;
}

.brand-side .logo {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.28);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 700;
}

.brand-side h1 {
  margin: 20px 0 4px;
  font-size: 22px;
  letter-spacing: 0.01em;
}

.brand-side .sub {
  margin: 0 0 26px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
  letter-spacing: 0.06em;
}

.feats {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.feats li {
  padding-left: 14px;
  border-left: 2px solid rgba(255, 255, 255, 0.3);
}

.feats b {
  display: block;
  font-size: 13.5px;
}

.feats span {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.68);
}

.ver {
  margin-top: 30px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
}

/* 表单侧 */
.form-side {
  padding: 40px 38px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-side h2 {
  margin: 0 0 6px;
  font-size: 19px;
}

.tip {
  margin: 0 0 20px;
  font-size: 12px;
  color: var(--bd-text-sub);
}

.quick {
  margin-top: 16px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: var(--bd-text-sub);
}

.roles {
  margin-top: 18px;
  padding: 12px 14px;
  background: var(--bd-primary-light);
  border: 1px solid var(--bd-primary-border);
  border-radius: 8px;
  font-size: 12px;
  color: #3730a3;
}

.roles b {
  display: block;
  margin-bottom: 4px;
}

.roles p {
  margin: 0;
  line-height: 1.65;
}

@media (max-width: 820px) {
  .stage {
    grid-template-columns: 1fr;
  }
  .brand-side {
    display: none;
  }
}
</style>
