<script setup>
import { reactive } from 'vue'
import SourceTag from '@/components/SourceTag.vue'

/** 与后端 CapacityPolicy / AdjacencyCluster 常量同源（改常量需重新构建后端） */
const rule = reactive({
  adjacentDistanceM: 800,
  sameStreetAdjacent: true,
  adjCapMin: 3,
  adjCapMax: 4,
  nonAdjCapMin: 1,
  nonAdjCapMax: 2,
  slotsPerDay: 3,
  strategy: 'greedy',
  overCapacity: 'block'
})

const SLOT_ROWS = [
  { slot: 'AM（09:00-12:00）', adj: '3 – 4 单', non: '1 – 2 单' },
  { slot: 'PM（14:00-17:00）', adj: '3 – 4 单', non: '1 – 2 单' },
  { slot: 'EV（19:00-21:00）', adj: '3 – 4 单', non: '1 – 2 单' }
]

const STEPS = [
  { n: 1, t: '按时段分组', d: '工单按 timeSlot（yyyy-MM-dd#AM|PM）分组，分别独立调度' },
  { n: 2, t: '并查集聚类', d: '同街道或经纬度距离 ≤ 800m 的小区并入同一相邻簇' },
  { n: 3, t: '时段容量校验', d: '读取 worker_capacity（缺失则取默认 3–4 / 1–2）' },
  { n: 4, t: '贪心派单', d: '优先把同簇工单分配给同一师傅，减少往返；相邻路线计入相邻额度' },
  { n: 5, t: '超容拦截', d: '容量耗尽不再派单，工单进入 exceptions 并给出改派 / 改期建议' }
]

function save() {
  ElMessage.info(
    '调度规则当前由后端 CapacityPolicy 常量承载（架构决策：不引入配置中心）。修改常量后需重启服务生效。'
  )
}
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">调度规则</h2>
        <p class="page-sub">相邻性判定、时段容量与超容处理策略（规则台账，与后端算法常量对齐）</p>
      </div>
      <SourceTag :live="false" label="规则常量" />
    </div>

    <div class="cols">
      <div>
        <div class="card">
          <div class="card-head">
            <h3>相邻性判定</h3>
            <span class="hint">AdjacencyCluster</span>
          </div>
          <div class="card-body">
            <el-form label-width="150px" label-position="left">
              <el-form-item label="距离阈值">
                <el-input-number v-model="rule.adjacentDistanceM" :min="100" :max="5000" :step="100" />
                <span class="tip">米（经纬度直线距离 ≤ 该值视为相邻）</span>
              </el-form-item>
              <el-form-item label="同街道视为相邻">
                <el-switch v-model="rule.sameStreetAdjacent" />
              </el-form-item>
            </el-form>
          </div>
        </div>

        <div class="card">
          <div class="card-head">
            <h3>时段容量</h3>
            <span class="hint">CapacityPolicy</span>
          </div>
          <div class="card-body">
            <el-form label-width="150px" label-position="left">
              <el-form-item label="相邻小区 / 时段">
                <el-input-number v-model="rule.adjCapMin" :min="1" :max="8" />
                <span class="dash">–</span>
                <el-input-number v-model="rule.adjCapMax" :min="1" :max="8" />
                <span class="tip">单</span>
              </el-form-item>
              <el-form-item label="非相邻 / 时段">
                <el-input-number v-model="rule.nonAdjCapMin" :min="1" :max="6" />
                <span class="dash">–</span>
                <el-input-number v-model="rule.nonAdjCapMax" :min="1" :max="6" />
                <span class="tip">单</span>
              </el-form-item>
              <el-form-item label="每日时段数">
                <el-input-number v-model="rule.slotsPerDay" :min="1" :max="6" />
                <span class="tip">段（AM / PM / EV）</span>
              </el-form-item>
              <el-form-item label="派单策略">
                <el-radio-group v-model="rule.strategy">
                  <el-radio-button label="greedy">贪心（同簇优先）</el-radio-button>
                  <el-radio-button label="nearest">最近优先</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="超容处理">
                <el-radio-group v-model="rule.overCapacity">
                  <el-radio-button label="block">拦截 + 给建议</el-radio-button>
                  <el-radio-button label="next">顺延下一时段</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-form>

            <el-alert
              type="info"
              :closable="false"
              show-icon
              title="规则参数化说明"
              description="按架构决策（单体 Spring Boot，不引入 Apollo 等配置中心），规则常量内置于 CapacityPolicy。以下表单用于规则评审与台账记录，保存不会热更新，需改常量后重新构建。"
              style="margin-top: 8px"
            />
            <el-button type="primary" style="margin-top: 14px" @click="save">保存规则台账</el-button>
          </div>
        </div>
      </div>

      <div>
        <div class="card">
          <div class="card-head"><h3>算法执行链路</h3></div>
          <div class="card-body">
            <ol class="steps">
              <li v-for="s in STEPS" :key="s.n">
                <span class="n">{{ s.n }}</span>
                <div>
                  <b>{{ s.t }}</b>
                  <p>{{ s.d }}</p>
                </div>
              </li>
            </ol>
          </div>
        </div>

        <div class="card">
          <div class="card-head"><h3>时段容量速览</h3></div>
          <el-table :data="SLOT_ROWS" size="small">
            <el-table-column prop="slot" label="时段" />
            <el-table-column prop="adj" label="相邻" width="90" />
            <el-table-column prop="non" label="非相邻" width="90" />
          </el-table>
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
  margin-left: 8px;
  font-size: 12px;
  color: var(--bd-text-mute);
}

.dash {
  margin: 0 8px;
  color: var(--bd-text-mute);
}

.steps {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.steps li {
  display: flex;
  gap: 10px;
}

.steps .n {
  flex: 0 0 auto;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--bd-primary);
  color: #fff;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 2px;
}

.steps b {
  font-size: 13px;
}

.steps p {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--bd-text-sub);
  line-height: 1.65;
}

@media (max-width: 1100px) {
  .cols {
    grid-template-columns: 1fr;
  }
}
</style>
