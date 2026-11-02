<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import type { Story } from '../api/stories'
import { adoptDraftCandidate, adoptRewrite, dismissCandidate, generateDraftCandidate,
  generateRewrite, listCandidates, type Candidate } from '../api/writing'

const props = withDefaults(defineProps<{ story: Story; blocked?: boolean }>(), { blocked: false })
const emit = defineEmits<{ updated: [story: Story]; 'body-change': [body: string]; 'save-request': [] }>()
const body = ref(props.story.body)
const instruction = ref('')
const textarea = ref<HTMLTextAreaElement | null>(null)
const candidates = ref<Candidate[]>([])
const busy = ref(false)
const error = ref('')
const notice = ref('')
const dirty = computed(() => body.value !== props.story.body)

watch(() => props.story, value => { body.value = value.body })
watch(body, value => emit('body-change', value))
async function load() {
  try { candidates.value = await listCandidates(props.story.id) }
  catch (cause) { error.value = cause instanceof Error ? cause.message : '加载候选内容失败' }
}
onMounted(load)
watch(() => props.story.id, load)

async function run(action: () => Promise<void>) {
  busy.value = true
  error.value = ''
  notice.value = ''
  try { await action() }
  catch (cause) { error.value = cause instanceof Error ? cause.message : '操作失败，请重试' }
  finally { busy.value = false }
}
function saveBody() {
  emit('save-request')
}
function generateDraft() {
  return run(async () => {
    const candidate = await generateDraftCandidate(props.story.id, props.story.version)
    candidates.value.unshift(candidate)
    notice.value = '初稿已生成，请审阅后决定是否采纳'
  })
}
function rewrite() {
  if (props.blocked || dirty.value) { error.value = '请先保存修改，再选择片段改写'; return }
  const start = textarea.value?.selectionStart ?? 0
  const end = textarea.value?.selectionEnd ?? 0
  const selectedText = body.value.slice(start, end)
  if (!selectedText || !instruction.value.trim()) {
    error.value = '请选中正文片段并填写改写要求'
    return
  }
  return run(async () => {
    const candidate = await generateRewrite(props.story.id, {
      version: props.story.version, startOffset: start, endOffset: end,
      selectedText, instruction: instruction.value.trim(),
    })
    candidates.value.unshift(candidate)
    instruction.value = ''
    notice.value = '改写建议已生成，请审阅后决定是否采纳'
  })
}
function adopt(candidate: Candidate) {
  if (props.blocked || dirty.value) { error.value = '请先保存修改，再采纳候选内容'; return }
  return run(async () => {
    const saved = candidate.kind === 'DRAFT'
      ? await adoptDraftCandidate(props.story.id, candidate.id, props.story.version)
      : await adoptRewrite(props.story.id, candidate.id, props.story.version)
    emit('updated', saved)
    candidates.value = candidates.value.filter(item => item.id !== candidate.id)
    notice.value = '候选内容已采纳'
  })
}
function dismiss(candidate: Candidate) {
  return run(async () => {
    await dismissCandidate(props.story.id, candidate.id)
    candidates.value = candidates.value.filter(item => item.id !== candidate.id)
  })
}
</script>

<template>
  <section class="draft-panel">
    <div class="panel-header"><div><p class="eyebrow">第三步</p><h2>写作与修改</h2></div></div>
    <p class="hint">AI 初稿和改写都先作为候选内容，只有你点击采纳才会进入正文。</p>
    <p v-if="error" class="inline-error" role="alert">{{ error }}</p>
    <p v-if="notice" class="inline-success" role="status">{{ notice }}</p>
    <div class="writing-actions">
      <button data-action="generate-draft" type="button" class="secondary" :disabled="busy || blocked || dirty" @click="generateDraft">生成正文初稿</button>
      <a :href="`/api/stories/${story.id}/export?format=md`" class="text-button">导出 Markdown</a>
      <a :href="`/api/stories/${story.id}/export?format=txt`" class="text-button">导出纯文本</a>
    </div>
    <label for="story-body" class="field-label">正文</label>
    <textarea id="story-body" ref="textarea" v-model="body" name="body" class="body-editor" rows="18" placeholder="在这里手动写作，或生成初稿后采纳" />
    <div class="writing-actions"><span v-if="dirty" class="unsaved">正文有未保存的修改</span><button type="button" class="primary" :disabled="busy || !dirty" @click="saveBody">保存正文</button></div>
    <div v-if="body" class="rewrite-box">
      <label for="rewrite-instruction" class="field-label">选中正文片段，提出改写要求</label>
      <textarea id="rewrite-instruction" v-model="instruction" rows="2" placeholder="例如：让这一段更紧张，但保留人物语气" />
      <button type="button" class="secondary" :disabled="busy || blocked || dirty" @click="rewrite">生成片段改写</button>
    </div>
    <div v-if="candidates.length" class="candidate-list">
      <h3>待审阅的候选内容</h3>
      <article v-for="candidate in candidates" :key="candidate.id" class="candidate">
        <strong>{{ candidate.kind === 'DRAFT' ? '正文初稿' : '片段改写' }}</strong>
        <p v-if="candidate.kind === 'REWRITE'" class="hint">原片段：{{ candidate.sourceText }}</p>
        <pre>{{ candidate.proposedText }}</pre>
        <p v-if="candidate.baseVersion !== story.version" class="inline-error">正文已更新，此候选内容已过期。</p>
        <div class="writing-actions"><button data-action="adopt-candidate" type="button" class="primary" :disabled="busy || blocked || dirty || candidate.baseVersion !== story.version" @click="adopt(candidate)">采纳</button><button type="button" class="text-button" :disabled="busy" @click="dismiss(candidate)">放弃</button></div>
      </article>
    </div>
  </section>
</template>
