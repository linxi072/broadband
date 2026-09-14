<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const user = useUserStore()
</script>

<template>
  <div class="err">
    <div class="code">403</div>
    <h3>没有访问该功能的权限</h3>
    <p>
      当前账号「{{ user.displayName }}」角色为
      <b>{{ user.profile?.roleNames?.join('、') || '—' }}</b>，未获得该菜单授权。
    </p>
    <p class="hint">如确需访问，请联系超级管理员在「权限管理 → 角色管理」中授权对应权限码。</p>
    <div class="btns">
      <el-button type="primary" @click="router.push('/dashboard')">返回数据看板</el-button>
      <el-button @click="router.back()">返回上一页</el-button>
    </div>
  </div>
</template>

<style scoped>
.err {
  max-width: 560px;
  margin: 80px auto;
  padding: 40px 32px;
  background: #fff;
  border: 1px solid var(--bd-border);
  border-radius: 14px;
  text-align: center;
  box-shadow: var(--bd-shadow);
}

.code {
  font-size: 62px;
  font-weight: 700;
  color: var(--bd-primary);
  letter-spacing: -0.03em;
  line-height: 1;
}

h3 {
  margin: 14px 0 10px;
  font-size: 17px;
}

p {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--bd-text-sub);
  line-height: 1.8;
}

.hint {
  color: var(--bd-text-mute);
  font-size: 12px;
}

.btns {
  margin-top: 22px;
  display: flex;
  gap: 10px;
  justify-content: center;
}
</style>
