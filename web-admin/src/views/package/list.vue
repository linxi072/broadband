<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import SourceTag from '@/components/SourceTag.vue'
import { adminPackageList, packageDetail } from '@/api/business'
import { loadResource, money } from '@/composables/useResource'
import { demoPackages } from '@/mock/fallback'

const router = useRouter()
const rows = ref([])
const live = ref(false)
const loading = ref(true)
const keyword = ref('')

const preview = ref(false)
const detail = ref(null)
const detailLoading = ref(false)

const filtered = computed(() => {
  const kw = keyword.value.trim()
  return rows.value.filter(
    (r) => !kw || String(r.name).includes(kw) || String(r.category || '').includes(kw)
  )
})

const onShelf = computed(() => filtered.value.filter((r) => r.online || r.status === '上架').length)

async function openPreview(row) {
  preview.value = true
  detail.value = null
  detailLoading.value = true
  const r = await loadResource(() => packageDetail(row.id), null)
  detail.value = r.data || { ...row, images: [], converge: [], params: [] }
  detailLoading.value = false
}

onMounted(async () => {
  const r = await loadResource(() => adminPackageList(), demoPackages)
  const data = r.data
  rows.value = Array.isArray(data) ? data : (data && data.records) || demoPackages
  live.value = r.live
  loading.value = false
})
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">套餐管理</h2>
        <p class="page-sub">融合套餐主数据：主图 / 轮播图 / 动态可选参数 / 资费与合约</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="stat-grid">
      <div class="stat"><div class="label">套餐总数</div><div class="value">{{ filtered.length }}<small> 个</small></div></div>
      <div class="stat is-down"><div class="label">在售中</div><div class="value">{{ onShelf }}<small> 个</small></div></div>
      <div class="stat is-warn"><div class="label">已下架</div><div class="value">{{ filtered.length - onShelf }}<small> 个</small></div></div>
    </div>

    <div class="card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="套餐名称 / 分类" clearable style="width: 240px" />
        <div class="spacer"></div>
        <el-button type="primary" @click="router.push('/package/edit')">新增套餐</el-button>
      </div>

      <el-table v-loading="loading" :data="filtered" style="width: 100%">
        <el-table-column prop="id" label="套餐ID" width="100" />
        <el-table-column prop="name" label="套餐名称" min-width="190" />
        <el-table-column prop="category" label="分类" width="90" />
        <el-table-column label="月费" width="100">
          <template #default="{ row }">
            <b class="fee">{{ money(row.monthlyFee) }}</b>
            <span v-if="row.originalFee" class="origin">{{ money(row.originalFee) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contract" label="合约期" width="100" />
        <el-table-column label="主图/轮播/参数" width="150">
          <template #default="{ row }">
            <span class="mute">
              主图 {{ row.mainImage ? '✓' : '—' }} ｜ 轮播 {{ row.images ?? 0 }} ｜ 参数 {{ row.params ?? 0 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.online || row.status === '上架' ? 'success' : 'info'" size="small" effect="light">
              {{ row.online || row.status === '上架' ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openPreview(row)">预览</el-button>
            <el-button link type="primary" size="small" @click="router.push(`/package/edit/${row.id}`)">
              编辑
            </el-button>
          </template>
        </el-table-column>
        <template #empty>暂无套餐</template>
      </el-table>
    </div>

    <el-drawer v-model="preview" title="套餐预览（真实接口 /api/package/detail）" size="460px">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <h3 class="pname">{{ detail.name }}</h3>
          <p class="pmeta">
            月费 {{ money(detail.monthlyFee) }} ｜ 原价 {{ money(detail.originalFee) }} ｜
            调测费 {{ money(detail.deposit) }} ｜ 设备租 {{ money(detail.deviceRent) }}
          </p>
          <SourceTag :live="live" />

          <h4 class="sec">轮播图（{{ (detail.images || []).length }}）</h4>
          <div class="thumbs">
            <div v-for="(img, i) in detail.images || []" :key="i" class="thumb">
              {{ img.caption || `图${i + 1}` }}
            </div>
            <el-empty v-if="!(detail.images || []).length" description="无轮播图" :image-size="50" />
          </div>

          <h4 class="sec">套餐组成</h4>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item v-for="c in detail.converge || []" :key="c.id" :label="c.label">
              {{ c.desc }}
            </el-descriptions-item>
          </el-descriptions>

          <h4 class="sec">动态可选参数</h4>
          <div v-for="p in detail.params || []" :key="p.key" class="param">
            <div class="param-head">
              <b>{{ p.name }}</b>
              <el-tag size="small" :type="p.type === 'SINGLE' ? 'primary' : 'warning'" effect="light">
                {{ p.type === 'SINGLE' ? '单选' : '多选' }}
              </el-tag>
              <el-tag v-if="p.required" size="small" type="danger" effect="plain">必选</el-tag>
            </div>
            <div class="opts">
              <span v-for="o in p.options || []" :key="o.id" class="opt">
                {{ o.value }}<i v-if="o.extraFee">+{{ o.extraFee }}</i>
              </span>
            </div>
          </div>
        </template>
      </div>
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

.stat-grid {
  margin: 0 0 16px;
}

.fee {
  color: var(--bd-up);
}

.origin {
  margin-left: 6px;
  font-size: 12px;
  color: var(--bd-text-mute);
  text-decoration: line-through;
}

.mute {
  font-size: 12px;
  color: var(--bd-text-sub);
}

.pname {
  margin: 0 0 6px;
  font-size: 16px;
}

.pmeta {
  margin: 0 0 10px;
  font-size: 12px;
  color: var(--bd-text-sub);
}

.sec {
  margin: 20px 0 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--bd-text-sub);
}

.thumbs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.thumb {
  width: 88px;
  height: 54px;
  border-radius: 6px;
  border: 1px dashed var(--bd-primary-border);
  background: var(--bd-primary-light);
  color: #4338ca;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.param {
  margin-bottom: 14px;
}

.param-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.opts {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.opt {
  padding: 3px 10px;
  border: 1px solid var(--bd-border);
  border-radius: 14px;
  font-size: 12px;
  color: var(--bd-text-sub);
}

.opt i {
  font-style: normal;
  color: var(--bd-up);
  margin-left: 4px;
}
</style>
