<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getAiSettings, saveAiSettings, type AiSettings } from '../api/ai'

const emit = defineEmits<{ close: [] }>()
const settings = ref<AiSettings | null>(null)
const baseUrl = ref('')
const model = ref('')
const apiKey = ref('')
const busy = ref(false)
const error = ref('')
const notice = ref('')

onMounted(async () => {
  try {
    settings.value = await getAiSettings()
    baseUrl.value = settings.value.baseUrl
    model.value = settings.value.model
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '加载 AI 设置失败'
  }
})

async function save() {
  error.value = ''
  notice.value = ''
  if (!baseUrl.value.trim() || !model.value.trim()) {
    error.value = '请填写服务地址和模型名称'
    return
  }
  busy.value = true
  try {
    settings.value = await saveAiSettings({ baseUrl: baseUrl.value.trim(), model: model.value.trim(), apiKey: apiKey.value })
    apiKey.value = ''
    notice.value = 'AI 设置已保存'
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '保存 AI 设置失败'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="settings-panel" aria-label="AI 设置">
    <div class="panel-header"><div><p class="eyebrow">模型连接</p><h2>AI 设置</h2></div><button type="button" class="text-button" @click="emit('close')">关闭</button></div>
    <p class="hint">填写兼容 OpenAI 接口的服务地址和模型名称。密钥保存后不会在页面中显示。</p>
    <p v-if="error" class="inline-error" role="alert">{{ error }}</p>
    <p v-if="notice" class="inline-success" role="status">{{ notice }}</p>
    <form @submit.prevent="save" class="settings-form">
      <label for="ai-base-url">服务地址</label>
      <input id="ai-base-url" v-model="baseUrl" name="baseUrl" placeholder="https://example.com/v1" autocomplete="url" />
      <label for="ai-model">模型名称</label>
      <input id="ai-model" v-model="model" name="model" placeholder="例如：your-model" />
      <label for="ai-key">API Key</label>
      <input id="ai-key" v-model="apiKey" name="apiKey" type="password" autocomplete="new-password" :placeholder="settings?.hasKey ? '已配置密钥；留空则保持不变' : '填写 API Key'" />
      <p v-if="settings?.hasKey" class="hint">已配置密钥</p>
      <p v-if="settings && !settings.canSaveKey" class="inline-error">保存新密钥前，请在 .env 中配置 AI_CONFIG_ENCRYPTION_KEY。</p>
      <button class="primary" data-action="save-settings" type="submit" :disabled="busy">{{ busy ? '保存中…' : '保存设置' }}</button>
    </form>
  </section>
</template>
