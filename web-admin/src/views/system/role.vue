<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import {
  roleList,
  menuTree,
  roleMenus,
  saveRoleMenus,
  createRole,
  updateRole,
  deleteRole,
  exportRoles,
  importRoles
} from '@/api/system'
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

const createDialog = ref(false)
const createForm = reactive({ code: '', name: '', remark: '' })
const editDialog = ref(false)
const editForm = reactive({ id: '', name: '', remark: '' })

const importDialog = ref(false)
const importing = ref(false)
const importFile = ref(null)

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

function openCreate() {
  Object.assign(createForm, { code: '', name: '', remark: '' })
  createDialog.value = true
}
async function saveCreate() {
  if (!createForm.code.trim() || !createForm.name.trim()) return ElMessage.warning('请填写角色标识与名称')
  saving.value = true
  const r = await loadResource(() => createRole({ ...createForm }), null)
  saving.value = false
  if (r.live) {
    ElMessage.success('角色已创建')
    createDialog.value = false
    load()
  } else {
    ElMessage.warning('新增接口不可达')
  }
}

function openEdit(row) {
  current.value = row
  editForm.id = row.id
  editForm.name = row.name
  editForm.remark = row.remark || ''
  editDialog.value = true
}
async function saveEdit() {
  if (!editForm.name.trim()) return ElMessage.warning('请填写角色名称')
  saving.value = true
  const r = await loadResource(() => updateRole(editForm.id, { name: editForm.name, remark: editForm.remark }), null)
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
    await ElMessageBox.confirm(`确认删除角色「${row.name}（${row.code}）」？将同步清理其权限分配。`, '删除角色', { type: 'warning' })
  } catch (e) {
    return
  }
  const r = await loadResource(() => deleteRole(row.id), null)
  if (r.live) {
    ElMessage.success('已删除')
    load()
  } else {
    ElMessage.warning('删除接口不可达')
  }
}

async function onExport() {
  try {
    await exportRoles()
    ElMessage.success('已导出角色数据')
  } catch (e) {
    ElMessage.error(e.message || '导出失败')
  }
}
function onImportChange(file) {
  importFile.value = file.raw
}
async function onImport() {
  if (!importFile.value) return ElMessage.warning('请先选择 CSV 文件')
  importing.value = true
  try {
    const res = await importRoles(importFile.value)
    ElMessage.success(`导入完成：新增 ${res.created} 条，跳过 ${res.skipped} 条${res.errors?.length ? '，失败 ' + res.errors.length + ' 条' : ''}`)
    importDialog.value = false
    importFile.value = null
    load()
  } catch (e) {
    ElMessage.error(e.message || '导入失败')
  } finally {
    importing.value = false
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
        <div style="display:flex;gap:8px;align-items:center">
          <span class="hint">共 {{ rows.length }} 个角色 · 菜单项 {{ totalMenus }} 个</span>
          <el-button size="small" @click="onExport">导出</el-button>
          <el-button size="small" @click="importDialog = true">导入</el-button>
          <el-button size="small" type="primary" @click="openCreate">新增角色</el-button>
        </div>
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
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openPerm(row)">编辑权限</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><EmptyState icon="🔐" title="暂无角色" desc="配置 RBAC 角色并绑定权限" /></template>
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

    <el-dialog v-model="createDialog" title="新增角色" width="420px">
      <el-form label-width="80px" label-position="left">
        <el-form-item label="角色标识"><el-input v-model="createForm.code" placeholder="如 OPERATOR" /></el-form-item>
        <el-form-item label="角色名称"><el-input v-model="createForm.name" placeholder="如 运营员" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="createForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCreate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialog" title="编辑角色" width="420px">
      <el-form label-width="80px" label-position="left">
        <el-form-item label="角色名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="editForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialog" title="导入角色（CSV）" width="460px">
      <el-alert type="info" :closable="false" show-icon title="CSV 表头：角色标识,角色名称,说明" style="margin-bottom: 12px" />
      <el-upload
        drag
        accept=".csv"
        :auto-upload="false"
        :limit="1"
        :on-change="onImportChange"
        :on-exceed="() => ElMessage.warning('仅支持单个文件')"
      >
        <div class="el-upload__text">将 CSV 文件拖到此处，或<em>点击选择</em></div>
      </el-upload>
      <template #footer>
        <el-button @click="importDialog = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="onImport">开始导入</el-button>
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
</style>
