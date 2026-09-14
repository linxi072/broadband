<script setup>
import { computed, onMounted, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import { roleList, menuTree, roleMenus, saveRoleMenus } from '@/api/system'
import { loadResource } from '@/composables/useResource'
import { demoMenus } from '@/mock/fallback'

const rows = ref([])
const menus = ref([])
const live = ref(false)
const loading = ref(true)

const drawer = ref(false)
const current = ref(null)
const checked = ref([])
const saving = ref(false)

const treeProps = { label: 'name', children: 'children' }
const totalMenus = computed(() => {
  let n = 0
  const walk = (list) => list.forEach((m) => { n += 1; if (m.children) walk(m.children) })
  walk(menus.value)
  return n
})

async function openPerm(row) {
  current.value = row
  drawer.value = true
  const r = await loadResource(() => roleMenus(row.id), null)
  checked.value = Array.isArray(r.data) ? r.data : (r.data && r.data.menuIds) || []
}

async function savePerm() {
  saving.value = true
  const r = await loadResource(() => saveRoleMenus(current.value.id, checked.value), null)
  saving.value = false
  if (r.live) {
    ElMessage.success('角色权限已保存（生效于下次登录）')
    drawer.value = false
  } else {
    ElMessage.warning('权限保存接口不可达')
  }
}

onMounted(async () => {
  loading.value = true
  const [r, m] = await Promise.all([
    loadResource(() => roleList(), null),
    loadResource(() => menuTree(), demoMenus)
  ])
  const data = r.data
  rows.value = Array.isArray(data) ? data : (data && data.records) || []
  live.value = r.live
  menus.value = Array.isArray(m.data) ? m.data : demoMenus
  loading.value = false
})
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">角色管理</h2>
        <p class="page-sub">角色 ↔ 菜单/权限码授权；与后端 Spring Security 6 的接口注解一一对应</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="card">
      <div class="card-head">
        <h3>角色列表</h3>
        <span class="hint">共 {{ rows.length }} 个角色 · 菜单项 {{ totalMenus }} 个</span>
      </div>
      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="code" label="角色标识" width="130">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="角色名称" width="150" />
        <el-table-column prop="remark" label="说明" min-width="240" show-overflow-tooltip />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openPerm(row)">编辑权限</el-button>
          </template>
        </el-table-column>
        <template #empty>暂无角色</template>
      </el-table>
    </div>

    <el-drawer v-model="drawer" :title="`编辑角色权限 · ${current?.name || ''}`" size="420px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="权限树与前端路由 meta.perm、后端 @PreAuthorize 同源"
        style="margin-bottom: 12px"
      />
      <el-tree
        :data="menus"
        :props="treeProps"
        node-key="id"
        show-checkbox
        default-expand-all
        :default-checked-keys="checked"
        @check="(_, info) => (checked = [...info.checkedKeys])"
      />
      <el-button type="primary" style="width: 100%; margin-top: 16px" :loading="saving" @click="savePerm">
        保存权限
      </el-button>
    </el-drawer>
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
