<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="err">
    <div class="code">404</div>
    <h3>页面不存在</h3>
    <p>你访问的地址未在运营后台注册，可能是链接过期或路由已调整。</p>
    <div class="btns">
      <el-button type="primary" @click="router.push('/dashboard')">返回数据看板</el-button>
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
}

.btns {
  margin-top: 22px;
}
</style>
