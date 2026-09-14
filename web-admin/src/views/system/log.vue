<script setup>
import { onMounted, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import { operLogs } from '@/api/system'
import { loadResource, fmtTime } from '@/composables/useResource'

const DEMO = [
  { id: 1, username: 'admin', name: '超级管理员', action: '登录', target: '/api/auth/login', method: 'POST', ip: '127.0.0.1', result: '成功', costMs: 42, createdTime: Date.now() - 3600e3 },
  { id: 2, username: 'admin', name: '超级管理员', action: '执行派单', target: '/api/dispatch/run', method: 'POST', ip: '127.0.0.1', result: '成功', costMs: 312, createdTime: Date.now() - 3400e3 },
  { id: 3, username: 'liuwei', name: '刘伟', action: '提交升档', target: '/api/package/upgrade', method: 'POST', ip: '127.0.0.1', result: '成功', costMs: 88, createdTime: Date.now() - 3200e3 },
  { id: 4, username: 'wangfang', name: '王芳', action: '访问受限模块', target: '/api/system/users', method: 'GET', ip: '127.0.0.1', result: '拒绝(403)', costMs: 6, createdTime: Date.now() - 3000e3 }
]

const rows = ref([])
const live = ref(false)
const loading = ref(true)
const keyword = ref('')

async function load() {
  loading.value = true
  const r = await loadResource(() => operLogs({ size: 200, keyword: keyword.value }), DEMO)
  const data = r.data
  rows.value = Array.isArray(data) ? data : (data && data.records) || DEMO
  live.value = r.live
  loading.value = false
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">操作日志</h2>
        <p class="page-sub">关键操作与访问留痕（含 401/403 越权尝试），数据源 sys_oper_log 表</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="账号 / 操作 / 接口"
          clearable
          style="width: 240px"
          @keyup.enter="load"
        />
        <el-button @click="load">查询</el-button>
        <div class="spacer"></div>
        <el-button size="small" @click="load">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="账号" width="120" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="action" label="操作" min-width="140" />
        <el-table-column prop="method" label="方法" width="90" />
        <el-table-column prop="target" label="接口" min-width="210" />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column label="结果" width="110">
          <template #default="{ row }">
            <el-tag :type="String(row.result).includes('成功') ? 'success' : 'danger'" size="small" effect="light">
              {{ row.result }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="90">
          <template #default="{ row }">{{ row.costMs }} ms</template>
        </el-table-column>
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.createdTime) }}</template>
        </el-table-column>
        <template #empty>暂无操作日志</template>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
</style>
