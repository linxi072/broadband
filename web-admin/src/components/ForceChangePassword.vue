<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import { useUserStore } from '@/store/user'
import { useI18n } from '@/i18n'

/**
 * 首登强制改密弹窗（T-02 安全治理 · M15 上线硬性门禁）。
 *
 * 服务端 MustChangePasswordFilter 会对 mustChangePassword=1 的账号拦截除改密外的
 * 全部后台接口（403）。若前端不做提示，用户只会看到各处空白/报错却不知原因，
 * 因此这里给出「不可关闭」的改密入口：改密成功后后端清零标记，弹窗即时消失。
 *
 * 不可关闭（无 X、点遮罩/ESC 无效）是有意为之——否则用户可直接绕过后继续用默认密码。
 */
const user = useUserStore()
const { t } = useI18n()

const formRef = ref(null)
const saving = ref(false)
const form = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

// 演示模式（无后端）不触发：后端门禁不存在，弹窗只会挡住预览
const visible = computed(() => user.mustChangePassword === 1 && !user.demoMode)

const rules = {
  oldPassword: [{ required: true, message: () => t('security.oldRequired'), trigger: 'blur' }],
  newPassword: [
    { required: true, message: () => t('security.newRequired'), trigger: 'blur' },
    { min: 6, message: () => t('security.newMin'), trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: () => t('security.confirmRequired'), trigger: 'blur' },
    {
      validator: (_r, v, cb) =>
        v === form.value.newPassword ? cb() : cb(new Error(t('security.confirmMismatch'))),
      trigger: 'blur'
    }
  ]
}

async function submit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  saving.value = true
  try {
    await user.changePassword({
      oldPassword: form.value.oldPassword,
      newPassword: form.value.newPassword
    })
    ElMessage.success(t('security.success'))
  } catch (e) {
    ElMessage.error(e?.message || t('security.fail'))
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="t('security.forceTitle')"
    width="440px"
    :show-close="false"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <p class="tip">{{ t('security.forceDesc') }}</p>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
      <el-form-item :label="t('security.oldPassword')" prop="oldPassword">
        <el-input
          v-model="form.oldPassword"
          type="password"
          show-password
          :placeholder="t('security.oldPasswordPlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('security.newPassword')" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          show-password
          :placeholder="t('security.newPasswordPlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('security.confirmPassword')" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          :placeholder="t('security.confirmPasswordPlaceholder')"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button type="primary" :loading="saving" @click="submit">
        {{ t('security.submit') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.tip {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--bd-text-sub);
}
</style>
