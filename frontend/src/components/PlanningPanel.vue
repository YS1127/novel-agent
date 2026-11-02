<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import type { Story } from '../api/stories'
import { getStory } from '../api/stories'
import { confirmPlan, generatePlan, generateQuestions, listQuestions, saveAnswers, type StoryQuestion } from '../api/ai'

const props = defineProps<{ story: Story }>()
const emit = defineEmits<{ updated: [story: Story] }>()
const questions = ref<StoryQuestion[]>([])
const busy = ref(false)
const error = ref('')
const notice = ref('')

async function load() {
  try { questions.value = await listQuestions(props.story.id) }
  catch (cause) { error.value = cause instanceof Error ? cause.message : '加载问题失败' }
}
onMounted(load)
watch(() => props.story.id, load)

async function run(action: () => Promise<void>) {
  busy.value = true
  error.value = ''
  notice.value = ''
  try { await action() }
  catch (cause) { error.value = cause instanceof Error ? cause.message : 'AI 操作失败，请重试' }
  finally { busy.value = false }
}
function ask() {
  return run(async () => {
    questions.value = await generateQuestions(props.story.id, props.story.version)
    emit('updated', await getStory(props.story.id))
    notice.value = '关键问题已生成'
  })
}
function save() {
  return run(async () => {
    questions.value = await saveAnswers(props.story.id, props.story.version,
      questions.value.map(item => ({ id: item.id, answer: item.answer })))
    emit('updated', await getStory(props.story.id))
    notice.value = '回答已保存'
  })
}
function plan() {
  return run(async () => {
    emit('updated', await generatePlan(props.story.id, props.story.version))
    notice.value = '故事方案和大纲已生成，请阅读并修改后确认'
  })
}
function confirm() {
  return run(async () => {
    emit('updated', await confirmPlan(props.story.id, props.story.version))
    notice.value = '大纲已确认，可以生成正文'
  })
}
</script>

<template>
  <section class="planning-panel">
    <div class="panel-header"><div><p class="eyebrow">第二步</p><h2>构思故事</h2></div></div>
    <p class="hint">AI 会先提出少量关键问题。你可以跳过不想回答的问题，也可以修改生成的方案和大纲。</p>
    <p v-if="error" class="inline-error" role="alert">{{ error }}</p>
    <p v-if="notice" class="inline-success" role="status">{{ notice }}</p>
    <div v-if="story.stage !== 'CONFIRMED' && story.stage !== 'DRAFT'" class="planning-actions">
      <button data-action="generate-questions" type="button" class="secondary" :disabled="busy" @click="ask">{{ questions.length ? '重新生成问题' : '生成关键问题' }}</button>
      <button type="button" class="secondary" :disabled="busy" @click="plan">生成故事方案与大纲</button>
    </div>
    <div v-if="questions.length" class="questions">
      <div v-for="item in questions" :key="item.id" class="question-item">
        <label :for="`answer-${item.id}`">{{ item.ordinal }}. {{ item.question }}</label>
        <textarea :id="`answer-${item.id}`" v-model="item.answer" rows="2" placeholder="你的回答（可留空）" />
      </div>
      <button type="button" class="secondary" :disabled="busy" @click="save">保存回答</button>
    </div>
    <div v-if="story.proposal && story.outline && story.stage !== 'CONFIRMED' && story.stage !== 'DRAFT'" class="confirm-row">
      <p class="hint">请先保存上方方案与大纲的修改，再确认。</p>
      <button type="button" class="primary" :disabled="busy" @click="confirm">确认方案与大纲</button>
    </div>
    <p v-if="story.stage === 'CONFIRMED' || story.stage === 'DRAFT'" class="inline-success">方案与大纲已确认。</p>
  </section>
</template>
