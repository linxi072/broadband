<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import { menuTree, createMenu, updateMenu, deleteMenu, exportMenus } from '@/api/system'
import { loadResource } from '@/composables/useResource'
import { demoMenus } from '@/mock/fallback'
import { navConfig } from '@/router/routes'

const menus = ref([])
const live = ref(false)
const loading = ref(true)

const createDialog = ref(false)
const createForm = reactive({ parentId: '', name: '', type: 'MENU', path: '', perm: '', sortOrder: 0 })
const editDialog = ref(false)
const editForm = reactive({ id: '', parentId: '', name: '', type: 'MENU', path: '', perm: '', sortOrder: 0 })
const saving = ref(false)

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

/** 扁平化菜单树，供「父级」下拉选择 */
const flatMenus = computed(() => {
  const out = []
  const walk = (list, depth) => {
    list.forEach((m) => {
      out.push({ id: m.id, name: '　'.repeat(depth) + m.name, type: m.type })
      if (m.children) walk(m.children, depth + 1)
    })
  }
  walk(menus.value, 0)
  return out
})

function openCreate(parentId = '') {
  Object.assign(createForm, { parentId, name: '', type: 'MENU', path: '', perm: '', sortOrder: 0 })
  createDialog.value = true
}
async function saveCreate() {
  if (!createForm.name.trim()) return ElMessage.warning('请填写名称')
  saving.value = true
  const r = await loadResource(() => createMenu({ ...createForm }), null)
  saving.value = false
  if (r.live) {
    ElMessage.success('菜单已新增')
    createDialog.value = false
    load()
  } else {
    ElMessage.warning('新增接口不可达')
  }
}

function openEdit(row) {
  editForm.id = row.id
  editForm.parentId = row.parentId || ''
  editForm.name = row.name
  editForm.type = row.type || 'MENU'
  editForm.path = row.path || ''
  editForm.perm = row.perm || ''
  editForm.sortOrder = row.sortOrder || 0
  editDialog.value = true
}
async function saveEdit() {
  if (!editForm.name.trim()) return ElMessage.warning('请填写名称')
  saving.value = true
  const r = await loadResource(() => updateMenu(editForm.id, { ...editForm }), null)
  saving.value = false
  if (r.live) {
    ElMessage.success('已保存')
    editDialog.value = false
    load()
  } else {
    ElMessage.warning('保存接口不可达')
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除菜单「${row.name}」？若存在子节点将一并删除。`, '删除菜单', { type: 'warning' })
  } catch (e) {
    return
  }
  const r = await loadResource(() => deleteMenu(row.id), null)
  if (r.live) {
    ElMessage.success('已删除')
    load()
  } else {
    ElMessage.warning('删除接口不可达')
  }
}

async function onExport() {
  try {
    await exportMenus()
    ElMessage.success('已导出菜单数据')
  } catch (e) {
    ElMessage.error(e.message || '导出失败')
  }
}

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

    <div class="toolbar">
      <div class="spacer"></div>
      <el-button @click="onExport">导出</el-button>
      <el-button type="primary" @click="openCreate('')">新增菜单</el-button>
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
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openCreate(row.id)">加子项</el-button>
              <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="onDelete(row)">删除</el-button>
            </template>
          </el-table-column>
          <template #empty><EmptyState icon="🧭" title="暂无菜单数据" desc="同步前端导航与后端 sys_menu 权限树" /></template>
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

    <el-dialog v-model="createDialog" title="新增菜单" width="460px">
      <el-form label-width="80px" label-position="left">
        <el-form-item label="父级">
          <el-select v-model="createForm.parentId" clearable placeholder="顶级（无父级）" style="width: 100%">
            <el-option label="顶级（无父级）" value="" />
            <el-option v-for="m in flatMenus" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称"><el-input v-model="createForm.name" /></el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="createForm.type">
            <el-radio value="DIR">目录</el-radio>
            <el-radio value="MENU">菜单</el-radio>
            <el-radio value="BUTTON">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="路径"><el-input v-model="createForm.path" placeholder="如 /customer（目录可空）" /></el-form-item>
        <el-form-item label="权限码"><el-input v-model="createForm.perm" placeholder="如 customer:view" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="createForm.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCreate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialog" title="编辑菜单" width="460px">
      <el-form label-width="80px" label-position="left">
        <el-form-item label="父级">
          <el-select v-model="editForm.parentId" clearable placeholder="顶级（无父级）" style="width: 100%">
            <el-option label="顶级（无父级）" value="" />
            <el-option v-for="m in flatMenus" :key="m.id" :label="m.name" :value="m.id" :disabled="m.id === editForm.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="editForm.type">
            <el-radio value="DIR">目录</el-radio>
            <el-radio value="MENU">菜单</el-radio>
            <el-radio value="BUTTON">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="路径"><el-input v-model="editForm.path" placeholder="如 /customer（目录可空）" /></el-form-item>
        <el-form-item label="权限码"><el-input v-model="editForm.perm" placeholder="如 customer:view" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="editForm.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.toolbar {
  display: flex;
  align-items: center;
  margin: 12px 0;
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
