<script setup>
import { computed, onMounted, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import { menuTree } from '@/api/system'
import { loadResource } from '@/composables/useResource'
import { demoMenus } from '@/mock/fallback'
import { navConfig } from '@/router/routes'

const menus = ref([])
const live = ref(false)
const loading = ref(true)

/** 前端路由 meta.perm 集合，用于核对「前端菜单 ↔ 后端权限树」是否对齐 */
const frontPerms = computed(() => {
  const out = []
  const walk = (list) => {
    list.forEach((n) => {
      if (n.children) walk(n.children)
      else if (n.perm) out.push({ title: n.title, path: n.path, perm: n.perm })
    })
  }
  walk(navConfig)
  return out
})

function collect(list, acc) {
  list.forEach((m) => {
    if (m.perm) acc.push(m.perm)
    if (m.children) collect(m.children, acc)
  })
  return acc
}

const backendPerms = computed(() => collect(menus.value, []))
const aligned = computed(() => {
  const set = new Set(backendPerms.value)
  return frontPerms.value.filter((f) => !set.has(f.perm))
})

onMounted(async () => {
  const r = await loadResource(() => menuTree(), demoMenus)
  menus.value = Array.isArray(r.data) ? r.data : demoMenus
  live.value = r.live
  loading.value = false
})
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">菜单权限</h2>
        <p class="page-sub">
          后端 sys_menu 权限树（目录 / 菜单 / 按钮）与前端路由权限码对齐核查
        </p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="stat-grid">
      <div class="stat"><div class="label">权限树节点</div><div class="value">{{ backendPerms.length }}<small> 个权限码</small></div></div>
      <div class="stat"><div class="label">前端路由权限码</div><div class="value">{{ frontPerms.length }}<small> 个</small></div></div>
      <div class="stat" :class="aligned.length ? 'is-warn' : 'is-down'">
        <div class="label">未对齐项</div>
        <div class="value">{{ aligned.length }}<small> 个</small></div>
      </div>
    </div>

    <div class="cols">
      <div class="card">
        <div class="card-head"><h3>后端权限树</h3><SourceTag :live="live" /></div>
        <el-table
          v-loading="loading"
          :data="menus"
          row-key="id"
          :tree-props="{ children: 'children' }"
          default-expand-all
          style="width: 100%"
        >
          <el-table-column prop="name" label="名称" min-width="170" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              <el-tag size="small" effect="plain" :type="row.type === 'DIR' ? 'info' : 'primary'">
                {{ row.type === 'DIR' ? '目录' : row.type === 'BUTTON' ? '按钮' : '菜单' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="path" label="路径" min-width="170" />
          <el-table-column prop="perm" label="权限码" min-width="150" />
          <template #empty>暂无菜单数据</template>
        </el-table>
      </div>

      <div class="card">
        <div class="card-head">
          <h3>前端路由权限码</h3>
          <span class="hint">router/routes.js</span>
        </div>
        <el-table :data="frontPerms" size="small" max-height="520">
          <el-table-column prop="title" label="菜单" width="130" />
          <el-table-column prop="path" label="路由" min-width="150" />
          <el-table-column prop="perm" label="权限码" min-width="140" />
        </el-table>
      </div>
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

.stat-grid {
  margin: 0 0 16px;
}

.cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  align-items: start;
}

@media (max-width: 1100px) {
  .cols {
    grid-template-columns: 1fr;
  }
}
</style>
