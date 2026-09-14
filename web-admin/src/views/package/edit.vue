<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SourceTag from '@/components/SourceTag.vue'
import { packageDetail, savePackage } from '@/api/business'
import { loadResource, money } from '@/composables/useResource'

const route = useRoute()
const router = useRouter()

const isEdit = ref(false)
const live = ref(false)
const loading = ref(false)
const saving = ref(false)

const form = reactive({
  id: '',
  name: '',
  category: '融合',
  status: '上架',
  monthlyFee: 129,
  originalFee: 169,
  contract: '24 个月',
  deposit: 100,
  deviceRent: 0,
  penalty: '提前解约按剩余合约 30% 计违约金',
  slaInfo: '当日装 / 当日修，超时慢必赔',
  mainImage: '',
  images: [],
  params: [
    { groupKey: 'bandwidth', name: '带宽', type: 'SINGLE', required: true, options: ['300M', '500M', '1000M', '2000M'] },
    { groupKey: 'contract', name: '合约期', type: 'SINGLE', required: true, options: ['12 个月', '24 个月'] },
    { groupKey: 'addon', name: '增值服务', type: 'MULTI', required: false, options: ['IPTV', '手机卡', 'FTTR 全屋光纤', '看家监控'] }
  ]
})

const paramTpl = {
  bandwidth: ['300M', '500M', '1000M', '2000M'],
  contract: ['12 个月', '24 个月'],
  addon: ['IPTV', '手机卡', 'FTTR 全屋光纤', '看家监控']
}

const newParam = reactive({ name: '', groupKey: 'addon', type: 'MULTI', required: false })

async function load() {
  const id = route.params.id
  if (!id) {
    isEdit.value = false
    return
  }
  isEdit.value = true
  form.id = id
  loading.value = true
  const r = await loadResource(() => packageDetail(id), null)
  live.value = r.live
  const d = r.data
  if (d) {
    form.name = d.name || form.name
    form.category = d.category || form.category
    form.monthlyFee = d.monthlyFee ?? form.monthlyFee
    form.originalFee = d.originalFee ?? form.originalFee
    form.deposit = d.deposit ?? form.deposit
    form.deviceRent = d.deviceRent ?? form.deviceRent
    form.penalty = d.penalty || form.penalty
    form.slaInfo = d.slaInfo || form.slaInfo
    form.images = (d.images || []).map((i) => i.caption || i.url || '图')
    if (d.params && d.params.length) {
      form.params = d.params.map((p) => ({
        groupKey: p.key,
        name: p.name,
        type: p.type,
        required: p.required,
        options: (p.options || []).map((o) => o.value)
      }))
    }
  }
  loading.value = false
}

function addOption(p) {
  const v = window.prompt(`为「${p.name}」新增选项名称`)
  if (v && v.trim()) p.options.push(v.trim())
}

function removeOption(p, i) {
  p.options.splice(i, 1)
}

function addParam() {
  if (!newParam.name.trim()) return ElMessage.warning('请填写参数组名称')
  form.params.push({
    groupKey: newParam.groupKey,
    name: newParam.name.trim(),
    type: newParam.type,
    required: newParam.required,
    options: [...(paramTpl[newParam.groupKey] || [])]
  })
  newParam.name = ''
  ElMessage.success('已添加参数组')
}

function addImage() {
  const v = window.prompt('轮播图名称（示例：1000M 主视觉）')
  if (v && v.trim()) form.images.push(v.trim())
}

function moveImage(i, delta) {
  const j = i + delta
  if (j < 0 || j >= form.images.length) return
  const [it] = form.images.splice(i, 1)
  form.images.splice(j, 0, it)
}

async function submit() {
  if (!form.name.trim()) return ElMessage.warning('请填写套餐名称')
  saving.value = true
  const r = await loadResource(
    () => savePackage({ ...form, online: form.status === '上架' }),
    { ok: false }
  )
  saving.value = false
  if (r.live) {
    ElMessage.success(isEdit.value ? '套餐已更新' : '套餐已创建')
    router.push('/package')
  } else {
    ElMessage.warning('后端写入接口不可达：变更仅保留在本地表单（未落库）')
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">{{ isEdit ? '编辑套餐' : '新增套餐' }}</h2>
        <p class="page-sub">主图 + 轮播图（可排序）+ 动态可选参数（单选 / 多选），供小程序下单时选用</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div v-loading="loading" class="cols">
      <div>
        <div class="card">
          <div class="card-head"><h3>基础信息</h3></div>
          <div class="card-body">
            <el-form label-width="96px" label-position="left">
              <el-form-item label="套餐名称">
                <el-input v-model="form.name" placeholder="如：1000M 融合套餐「美好家」" />
              </el-form-item>
              <el-row :gutter="12">
                <el-col :span="12">
                  <el-form-item label="分类">
                    <el-select v-model="form.category" style="width: 100%">
                      <el-option label="融合" value="融合" />
                      <el-option label="千兆" value="千兆" />
                      <el-option label="单宽" value="单宽" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="状态">
                    <el-radio-group v-model="form.status">
                      <el-radio-button label="上架" />
                      <el-radio-button label="下架" />
                    </el-radio-group>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="12">
                <el-col :span="12">
                  <el-form-item label="月租">
                    <el-input-number v-model="form.monthlyFee" :min="0" :step="10" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="原价">
                    <el-input-number v-model="form.originalFee" :min="0" :step="10" style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="12">
                <el-col :span="12">
                  <el-form-item label="合约期">
                    <el-select v-model="form.contract" style="width: 100%">
                      <el-option label="12 个月" value="12 个月" />
                      <el-option label="24 个月" value="24 个月" />
                      <el-option label="36 个月" value="36 个月" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="调测费">
                    <el-input-number v-model="form.deposit" :min="0" :step="10" style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="设备租赁">
                <el-input-number v-model="form.deviceRent" :min="0" :step="5" style="width: 180px" />
                <span class="tip">元 / 月，0 表示免费租用</span>
              </el-form-item>
              <el-form-item label="违约金">
                <el-input v-model="form.penalty" />
              </el-form-item>
              <el-form-item label="SLA 说明">
                <el-input v-model="form.slaInfo" />
              </el-form-item>
            </el-form>
          </div>
        </div>

        <div class="card">
          <div class="card-head">
            <h3>动态参数 <span class="hint">下单时客户可选项</span></h3>
          </div>
          <div class="card-body">
            <div v-for="p in form.params" :key="p.groupKey + p.name" class="pgroup">
              <div class="phead">
                <b>{{ p.name }}</b>
                <el-tag size="small" :type="p.type === 'SINGLE' ? 'primary' : 'warning'" effect="light">
                  {{ p.type === 'SINGLE' ? '单选' : '多选' }}
                </el-tag>
                <el-tag size="small" :type="p.required ? 'danger' : 'info'" effect="plain">
                  {{ p.required ? '必选' : '可选' }}
                </el-tag>
                <div class="spacer"></div>
                <el-button link type="primary" size="small" @click="addOption(p)">＋ 选项</el-button>
              </div>
              <div class="opts">
                <span v-for="(o, i) in p.options" :key="o + i" class="opt">
                  {{ o }}
                  <i class="del" @click="removeOption(p, i)">×</i>
                </span>
                <span v-if="!p.options.length" class="mute">暂无选项</span>
              </div>
            </div>

            <el-divider />

            <div class="newparam">
              <el-input v-model="newParam.name" placeholder="新参数组名称" style="width: 160px" />
              <el-select v-model="newParam.groupKey" style="width: 130px">
                <el-option label="带宽" value="bandwidth" />
                <el-option label="合约期" value="contract" />
                <el-option label="增值服务" value="addon" />
              </el-select>
              <el-select v-model="newParam.type" style="width: 100px">
                <el-option label="单选" value="SINGLE" />
                <el-option label="多选" value="MULTI" />
              </el-select>
              <el-checkbox v-model="newParam.required">必选</el-checkbox>
              <el-button @click="addParam">添加参数组</el-button>
            </div>
          </div>
        </div>
      </div>

      <div>
        <div class="card">
          <div class="card-head"><h3>主图（封面 · 1 张）</h3></div>
          <div class="card-body">
            <div class="cover" @click="setMainImage">
              <span v-if="form.mainImage">{{ form.mainImage }}</span>
              <span v-else>点击上传主图<br /><small>建议 750×420</small></span>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="card-head">
            <h3>轮播图 <span class="hint">可排序</span></h3>
            <el-button link type="primary" size="small" @click="addImage">＋ 添加</el-button>
          </div>
          <div class="card-body">
            <div v-for="(img, i) in form.images" :key="img + i" class="imgrow">
              <span class="idx">{{ i + 1 }}</span>
              <span class="imgname">{{ img }}</span>
              <el-button link size="small" @click="moveImage(i, -1)">↑</el-button>
              <el-button link size="small" @click="moveImage(i, 1)">↓</el-button>
              <el-button link type="danger" size="small" @click="form.images.splice(i, 1)">删除</el-button>
            </div>
            <el-empty v-if="!form.images.length" description="暂无轮播图" :image-size="60" />
          </div>
        </div>

        <div class="card">
          <div class="card-head"><h3>实时算价预览</h3></div>
          <div class="card-body">
            <p class="calc">
              月费 <b>{{ money(form.monthlyFee) }}</b>
              <span class="mute">（原价 {{ money(form.originalFee) }}）</span>
            </p>
            <p class="calc">合约期 {{ form.contract }} ｜ 调测费 {{ money(form.deposit) }}</p>
            <p class="calc">参数组 {{ form.params.length }} 个 ｜ 轮播图 {{ form.images.length }} 张</p>
            <el-button type="primary" style="width: 100%; margin-top: 8px" :loading="saving" @click="submit">
              保存
            </el-button>
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
  grid-template-columns: 1fr 320px;
  gap: 16px;
  align-items: start;
}

.tip {
  margin-left: 8px;
  font-size: 12px;
  color: var(--bd-text-mute);
}

.pgroup {
  padding: 12px 14px;
  border: 1px solid var(--bd-border);
  border-radius: 8px;
  margin-bottom: 10px;
}

.phead {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.spacer {
  flex: 1;
}

.opts {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.opt {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border: 1px solid var(--bd-primary-border);
  background: var(--bd-primary-light);
  color: #4338ca;
  border-radius: 14px;
  font-size: 12px;
}

.opt .del {
  cursor: pointer;
  font-style: normal;
  color: var(--bd-text-mute);
}

.opt .del:hover {
  color: var(--bd-danger);
}

.newparam {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.cover {
  height: 130px;
  border: 1px dashed var(--bd-primary-border);
  border-radius: 8px;
  background: var(--bd-primary-light);
  color: #4338ca;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-size: 13px;
  cursor: pointer;
}

.cover small {
  color: #6d6ae0;
}

.imgrow {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px dashed var(--bd-border);
  font-size: 13px;
}

.idx {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--bd-primary-light);
  color: #4338ca;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.imgname {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.calc {
  margin: 6px 0;
  font-size: 13px;
}

.mute {
  color: var(--bd-text-mute);
  font-size: 12px;
}

@media (max-width: 1100px) {
  .cols {
    grid-template-columns: 1fr;
  }
}
</style>
