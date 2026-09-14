<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { adminCommunityList, saveCommunity, checkCommunity } from '@/api/business'
import { loadResource } from '@/composables/useResource'
import { demoCommunities } from '@/mock/fallback'

const route = useRoute()
const router = useRouter()

const isEdit = ref(false)
const loading = ref(false)
const saving = ref(false)

const form = reactive({
  id: '',
  name: '',
  region: '南山区/科技园',
  street: '科技园街道',
  lat: 22.54,
  lng: 113.95,
  carrier: '电信',
  covered: true,
  portTotal: 64,
  portUsed: 0,
  status: 'AVAILABLE'
})

const testResult = ref(null)
const testing = ref(false)

async function onTest() {
  if (!form.name.trim()) return ElMessage.warning('请先填写小区名称')
  testing.value = true
  const r = await loadResource(() => checkCommunity(form.name.trim()), null)
  testResult.value = r.live
    ? r.data
    : { message: '后端不可达，无法校验（保存后由后端接口验真）', portRemaining: 0 }
  testing.value = false
}

async function load() {
  const id = route.params.id
  if (!id) return
  isEdit.value = true
  form.id = id
  loading.value = true
  const r = await loadResource(() => adminCommunityList({ size: 500 }), demoCommunities)
  const list = Array.isArray(r.data) ? r.data : (r.data && r.data.records) || demoCommunities
  const hit = list.find((x) => String(x.id) === String(id))
  if (hit) Object.assign(form, hit)
  loading.value = false
}

async function submit() {
  if (!form.name.trim()) return ElMessage.warning('请填写小区名称')
  saving.value = true
  const r = await loadResource(() => saveCommunity({ ...form }), null)
  saving.value = false
  if (r.live) {
    ElMessage.success(isEdit.value ? '小区覆盖已更新' : '小区覆盖已新增')
    router.push('/community')
  } else {
    ElMessage.warning('写入接口不可达：变更未落库')
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">{{ isEdit ? '编辑小区覆盖' : '新增小区覆盖' }}</h2>
        <p class="page-sub">经纬度与街道用于派单「相邻性聚类」（同街道或距离 ≤ 800m 视为相邻）</p>
      </div>
      <el-button @click="router.push('/community')">返回列表</el-button>
    </div>

    <div v-loading="loading" class="cols">
      <div class="card">
        <div class="card-head"><h3>基础信息</h3></div>
        <div class="card-body">
          <el-form label-width="96px" label-position="left">
            <el-form-item label="小区名称">
              <el-input v-model="form.name" placeholder="如：保利花园" />
            </el-form-item>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="所属区域">
                  <el-input v-model="form.region" placeholder="南山区/科技园" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="街道">
                  <el-input v-model="form.street" placeholder="科技园街道" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="纬度">
                  <el-input-number v-model="form.lat" :precision="5" :step="0.001" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="经度">
                  <el-input-number v-model="form.lng" :precision="5" :step="0.001" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="覆盖运营商">
              <el-select v-model="form.carrier" style="width: 100%">
                <el-option label="电信" value="电信" />
                <el-option label="联通" value="联通" />
                <el-option label="电信·联通" value="电信·联通" />
                <el-option label="移动" value="移动" />
                <el-option label="未覆盖" value="—" />
              </el-select>
            </el-form-item>
            <el-form-item label="是否覆盖">
              <el-switch v-model="form.covered" />
              <span class="tip">关闭表示暂未覆盖，小程序将引导登记需求</span>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <div>
        <div class="card">
          <div class="card-head"><h3>端口容量</h3></div>
          <div class="card-body">
            <el-form label-width="76px" label-position="left">
              <el-form-item label="端口总数">
                <el-input-number v-model="form.portTotal" :min="0" :step="8" style="width: 100%" />
              </el-form-item>
              <el-form-item label="已占用">
                <el-input-number v-model="form.portUsed" :min="0" :max="form.portTotal" :step="1" style="width: 100%" />
              </el-form-item>
            </el-form>
            <p class="calc">
              端口余量：<b>{{ Math.max(0, form.portTotal - form.portUsed) }}</b>
            </p>
            <p class="hint-line">
              余量 0 → 不可安装；余量 ≤ 8 或占用率 ≥ 80% → 紧张；否则可安装
            </p>
          </div>
        </div>

        <div class="card">
          <div class="card-head"><h3>覆盖状态</h3></div>
          <div class="card-body">
            <el-radio-group v-model="form.status">
              <el-radio-button label="AVAILABLE">可安装</el-radio-button>
              <el-radio-button label="TIGHT">紧张</el-radio-button>
              <el-radio-button label="UNAVAILABLE">不可安装</el-radio-button>
            </el-radio-group>

            <el-divider />

            <el-button :loading="testing" style="width: 100%" @click="onTest">调用接口校验</el-button>
            <div v-if="testResult" class="test">
              {{ testResult.message }}（余量 {{ testResult.portRemaining }}）
            </div>
          </div>
        </div>

        <div class="card">
          <div class="card-body">
            <el-button type="primary" style="width: 100%" :loading="saving" @click="submit">保存</el-button>
          </div>
        </div>
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

.cols {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 16px;
  align-items: start;
}

.tip {
  margin-left: 10px;
  font-size: 12px;
  color: var(--bd-text-mute);
}

.calc {
  margin: 6px 0;
  font-size: 13px;
}

.hint-line {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--bd-text-mute);
  line-height: 1.6;
}

.test {
  margin-top: 10px;
  padding: 8px 10px;
  background: var(--bd-primary-light);
  border-radius: 6px;
  font-size: 12px;
  color: #3730a3;
}

@media (max-width: 1100px) {
  .cols {
    grid-template-columns: 1fr;
  }
}
</style>
