<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { supportFaq, supportTicket } from '@/api/business'
import { loadResource } from '@/composables/useResource'
import { demoFaqs } from '@/mock/fallback'

const rows = ref([])
const live = ref(false)
const loading = ref(true)
const category = ref('')

const CATS = ['安装', '故障', '账单', '套餐']

const filtered = computed(() =>
  rows.value.filter((r) => !category.value || r.category === category.value)
)

const form = reactive({ contact: '', question: '', desc: '' })
const submitting = ref(false)

async function submitTicket() {
  if (!form.contact || !form.question) {
    ElMessage.warning('请填写联系方式与问题标题')
    return
  }
  submitting.value = true
  // 后端 /api/support/ticket 契约：content（必填）、contact、type、customerId
  const payload = {
    content: form.desc ? `${form.question}\n${form.desc}` : form.question,
    contact: form.contact,
    type: 'CONSULT'
  }
  const r = await loadResource(() => supportTicket(payload), null)
  submitting.value = false
  if (r.live) {
    ElMessage.success('工单已提交，客服将尽快联系')
    form.contact = ''
    form.question = ''
    form.desc = ''
  } else {
    ElMessage.warning('提交接口不可达：仅本地演示')
  }
}

async function load() {
  loading.value = true
  const r = await loadResource(() => supportFaq(), demoFaqs)
  rows.value = Array.isArray(r.data) ? r.data : []
  live.value = r.live
  loading.value = false
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">在线客服 / 帮助中心</h2>
        <p class="page-sub">FAQ 知识库与工单提交（数据源 support_faq / support_ticket 表）</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="card">
      <div class="toolbar">
        <el-select v-model="category" placeholder="全部分类" clearable style="width: 150px">
          <el-option v-for="c in CATS" :key="c" :label="c" :value="c" />
        </el-select>
        <div class="spacer"></div>
        <el-button size="small" @click="load">刷新</el-button>
      </div>
      <el-table v-loading="loading" :data="filtered" style="width: 100%">
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="question" label="问题" min-width="180" />
        <el-table-column prop="answer" label="解答" min-width="260" show-overflow-tooltip />
        <template #empty>
          <EmptyState icon="💬" title="暂无知识库条目" desc="沉淀高频问题解答以自助服务客户" />
        </template>
      </el-table>
    </div>

    <div class="card">
      <div class="card-head">
        <h3>提交工单</h3>
      </div>
      <div class="card-body form-wrap">
        <el-form :model="form" label-width="80px">
          <el-form-item label="联系方式">
            <el-input v-model="form.contact" placeholder="手机号 / 微信号" />
          </el-form-item>
          <el-form-item label="问题标题">
            <el-input v-model="form.question" placeholder="一句话描述您的问题" />
          </el-form-item>
          <el-form-item label="问题描述">
            <el-input
              v-model="form.desc"
              type="textarea"
              :rows="3"
              placeholder="补充细节，便于客服处理"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="submitTicket">提交工单</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.form-wrap {
  max-width: 520px;
}
</style>
