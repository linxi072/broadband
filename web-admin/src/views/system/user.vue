<script setup>
import { onMounted, reactive, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import {
  userList,
  createUser,
  updateUserStatus,
  assignRoles,
  resetPassword,
  roleList
} from '@/api/system'
import { loadResource } from '@/composables/useResource'

const rows = ref([])
const roles = ref([])
const live = ref(false)
const loading = ref(true)
const keyword = ref('')

const dialog = ref(false)
const roleDialog = ref(false)
const saving = ref(false)
const form = reactive({ username: '', name: '', password: '123456', dept: '运营中心', roleIds: [] })
const current = ref(null)

const perms = {
  ADMIN: '全部模块',
  OPERATOR: '业务模块 + 系统只读',
  FINANCE: '财务 / 订单 / 客户',
  CS: '订单 / 客户 / 投诉评价',
  SALES: '订单 / 客户 / 销售'
}

async function load() {
  loading.value = true
  const [u, r] = await Promise.all([
    loadResource(() => userList({ keyword: keyword.value }), null),
    loadResource(() => roleList(), null)
  ])
  const data = u.data
  rows.value = Array.isArray(data) ? data : (data && data.records) || []
  live.value = u.live
  roles.value = Array.isArray(r.data) ? r.data : (r.data && r.data.records) || []
  loading.value = false
}

async function onToggle(row) {
  const next = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const r = await loadResource(() => updateUserStatus(row.id, next), null)
  if (r.live) {
    row.status = next
    ElMessage.success(next === 'ENABLED' ? '已启用' : '已禁用')
  } else {
    ElMessage.warning('状态更新接口不可达')
  }
}

async function onReset(row) {
  try {
    await ElMessageBox.confirm(`确认将「${row.name}」密码重置为 123456 ？`, '重置密码', { type: 'warning' })
  } catch (e) {
    return
  }
  const r = await loadResource(() => resetPassword(row.id, '123456'), null)
  if (r.live) ElMessage.success('密码已重置为 123456')
  else ElMessage.warning('重置接口不可达')
}

async function saveUser() {
  if (!form.username.trim() || !form.name.trim()) return ElMessage.warning('请填写账号与姓名')
  saving.value = true
  const r = await loadResource(() => createUser({ ...form }), null)
  saving.value = false
  if (r.live) {
    ElMessage.success('用户已创建')
    dialog.value = false
    Object.assign(form, { username: '', name: '', password: '123456', dept: '运营中心', roleIds: [] })
    load()
  } else {
    ElMessage.warning('新增接口不可达')
  }
}

function openRoles(row) {
  current.value = row
  form.roleIds = [...(row.roleIds || [])]
  roleDialog.value = true
}

async function saveRoles() {
  saving.value = true
  const r = await loadResource(() => assignRoles(current.value.id, form.roleIds), null)
  saving.value = false
  if (r.live) {
    ElMessage.success('角色已分配')
    roleDialog.value = false
    load()
  } else {
    ElMessage.warning('角色分配接口不可达')
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-sub">
          用户 ↔ 角色 ↔ 权限（RBAC）；角色权限与后端 @PreAuthorize 及前端权限码同源
        </p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="账号 / 姓名"
          clearable
          style="width: 220px"
          @keyup.enter="load"
        />
        <el-button @click="load">查询</el-button>
        <div class="spacer"></div>
        <el-button type="primary" @click="dialog = true">新增用户</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column prop="username" label="账号" width="140" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            <el-tag
              v-for="r in row.roleNames || row.roles || []"
              :key="r"
              size="small"
              effect="light"
              style="margin-right: 4px"
            >
              {{ r }}
            </el-tag>
            <span v-if="!(row.roleNames || row.roles || []).length" class="mute">未分配</span>
          </template>
        </el-table-column>
        <el-table-column prop="dept" label="部门" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small" effect="light">
              {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openRoles(row)">分配角色</el-button>
            <el-button link type="primary" size="small" @click="onReset(row)">重置密码</el-button>
            <el-button link :type="row.status === 'ENABLED' ? 'danger' : 'success'" size="small" @click="onToggle(row)">
              {{ row.status === 'ENABLED' ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>暂无用户</template>
      </el-table>
    </div>

    <el-dialog v-model="dialog" title="新增用户" width="440px">
      <el-form label-width="80px" label-position="left">
        <el-form-item label="账号"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="初始密码"><el-input v-model="form.password" /></el-form-item>
        <el-form-item label="部门"><el-input v-model="form.dept" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialog" :title="`分配角色 · ${current?.name || ''}`" width="420px">
      <el-checkbox-group v-model="form.roleIds" class="rolebox">
        <div v-for="r in roles" :key="r.id" class="roleitem">
          <el-checkbox :label="r.id">{{ r.name }}</el-checkbox>
          <span class="mute">{{ r.remark || perms[r.code] || '' }}</span>
        </div>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRoles">保存</el-button>
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

.mute {
  font-size: 12px;
  color: var(--bd-text-mute);
}

.rolebox {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.roleitem {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid var(--bd-border);
  border-radius: 8px;
}
</style>
