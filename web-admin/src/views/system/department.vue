<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">部门管理</h2>
        <p class="page-sub">按区域划分部门；运营人员仅可见本部门及下级部门的订单与工单（数据权限）。</p>
      </div>
      <div class="head-actions">
        <SourceTag :live="live" />
        <el-button type="primary" @click="openCreate()">新增部门</el-button>
      </div>
    </div>

    <el-table :data="treeData" row-key="id" default-expand-all border stripe
              :tree-props="{ children: 'children' }" style="width: 100%">
      <el-table-column prop="name" label="部门名称" min-width="180">
        <template #default="{ row }">
          <span class="dept-name">{{ row.name }}</span>
          <el-tag v-if="!row.parentId" size="small" type="warning" effect="plain" class="ml-1">区域根</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="region" label="所属区域" width="120" />
      <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
            {{ row.status === 'ENABLED' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openCreate(row)">加子部门</el-button>
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" @closed="resetForm">
      <el-form :model="form" label-width="92px">
        <el-form-item label="上级部门">
          <el-select v-model="form.parentId" placeholder="不选则作为区域根" clearable filterable style="width: 100%">
            <el-option v-for="d in flatList" :key="d.id" :value="d.id" :label="d.name"
                       :disabled="form.id === d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门名称" required>
          <el-input v-model="form.name" placeholder="如：深圳分公司" maxlength="64" />
        </el-form-item>
        <el-form-item label="所属区域">
          <el-input v-model="form.region" placeholder="如：华南 / 华东" maxlength="32" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="ENABLED">启用</el-radio>
            <el-radio label="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SourceTag from '@/components/SourceTag.vue'
import { loadResource } from '@/composables/useResource.js'
import {
  departmentList, departmentTree, createDepartment, updateDepartment, deleteDepartment
} from '@/api/system.js'
import { demoDepartments } from '@/mock/fallback.js'

const treeData = ref([])
const flatList = ref([])
const live = ref(true)
const dialogVisible = ref(false)
const dialogTitle = ref('新增部门')
const saving = ref(false)

const emptyForm = () => ({ id: null, parentId: null, name: '', region: '', sortOrder: 0, status: 'ENABLED' })
const form = reactive(emptyForm())

function toTree(flat) {
  const index = {}
  flat.forEach((d) => (index[d.id] = { ...d, children: [] }))
  const roots = []
  flat.forEach((d) => {
    if (d.parentId && index[d.parentId]) index[d.parentId].children.push(index[d.id])
    else roots.push(index[d.id])
  })
  return roots
}

async function load() {
  const res = await loadResource(
    async () => {
      const [flat, tree] = await Promise.all([departmentList(), departmentTree()])
      flatList.value = flat
      return tree
    },
    () => toTree(demoDepartments)
  )
  live.value = res.live
  treeData.value = res.data
  if (!res.live) flatList.value = demoDepartments
}

function resetForm() {
  Object.assign(form, emptyForm())
}

function openCreate(parent) {
  resetForm()
  if (parent) form.parentId = parent.id
  dialogTitle.value = parent ? `新增子部门（上级：${parent.name}）` : '新增部门'
  dialogVisible.value = true
}

function openEdit(row) {
  resetForm()
  Object.assign(form, { id: row.id, parentId: row.parentId, name: row.name, region: row.region, sortOrder: row.sortOrder, status: row.status })
  dialogTitle.value = `编辑部门（${row.name}）`
  dialogVisible.value = true
}

async function save() {
  if (!form.name || !form.name.trim()) {
    ElMessage.warning('请填写部门名称')
    return
  }
  saving.value = true
  try {
    const payload = {
      parentId: form.parentId || null,
      name: form.name.trim(),
      region: form.region || null,
      sortOrder: form.sortOrder,
      status: form.status
    }
    if (form.id) {
      await updateDepartment(form.id, payload)
      ElMessage.success('已保存')
    } else {
      await createDepartment(payload)
      ElMessage.success('已新增')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除部门「${row.name}」？含子部门的节点需先删除下级。`, '删除确认', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await deleteDepartment(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.page { padding: 18px 20px; }
.page-head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 16px; gap: 12px; }
.page-title { font-size: 19px; font-weight: 600; margin: 0; }
.page-sub { margin: 4px 0 0; color: #6b7280; font-size: 13px; }
.head-actions { display: flex; align-items: center; gap: 10px; }
.dept-name { font-weight: 500; }
.ml-1 { margin-left: 6px; }
</style>
