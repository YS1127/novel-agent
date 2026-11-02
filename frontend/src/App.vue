<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { createStory, getStory, listStories, updateStory, type Story, type StoryChanges } from './api/stories'
import AiSettings from './components/AiSettings.vue'
import PlanningPanel from './components/PlanningPanel.vue'
import DraftPanel from './components/DraftPanel.vue'
import './assets/main.css'

const stories = ref<Story[]>([])
const active = ref<Story | null>(null)
const draft = ref<Story | null>(null)
const newTitle = ref('')
const newIdea = ref('')
const newStyle = ref('')
const busy = ref(false)
const error = ref('')
const notice = ref('')
const loading = ref(true)
const showSettings = ref(false)
const dirty = computed(() => active.value !== null && draft.value !== null &&
  JSON.stringify(changes(active.value)) !== JSON.stringify(changes(draft.value)))

function changes(story: Story): StoryChanges {
  return {
    title: story.title, idea: story.idea, styleGuide: story.styleGuide,
    proposal: story.proposal, outline: story.outline, body: story.body, version: story.version,
  }
}
function message(cause: unknown): string {
  return cause instanceof Error ? cause.message : '操作失败，请重试'
}
async function refreshList() {
  stories.value = (await listStories()).items
}
async function selectStory(id: number) {
  if (dirty.value && !window.confirm('当前修改尚未保存，确定切换作品吗？')) return
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    const story = await getStory(id)
    active.value = story
    draft.value = { ...story }
  } catch (cause) {
    error.value = message(cause)
  } finally {
    busy.value = false
  }
}
async function create() {
  error.value = ''
  notice.value = ''
  if (!newTitle.value.trim() || !newIdea.value.trim()) {
    error.value = '请填写标题和故事点子'
    return
  }
  if (dirty.value && !window.confirm('当前修改尚未保存，确定新建作品吗？')) return
  busy.value = true
  try {
    const created = await createStory({
      title: newTitle.value.trim(), idea: newIdea.value.trim(), styleGuide: newStyle.value.trim(),
    })
    active.value = created
    draft.value = { ...created }
    newTitle.value = ''
    newIdea.value = ''
    newStyle.value = ''
    notice.value = '作品已创建'
    try {
      await refreshList()
    } catch {
      stories.value = [created, ...stories.value]
      notice.value = '作品已创建；列表刷新失败，稍后可重试'
    }
  } catch (cause) {
    error.value = message(cause)
  } finally {
    busy.value = false
  }
}
async function save() {
  if (!active.value || !draft.value) return
  error.value = ''
  notice.value = ''
  if (!draft.value.title.trim() || !draft.value.idea.trim()) {
    error.value = '标题和故事点子不能为空'
    return
  }
  busy.value = true
  try {
    const saved = await updateStory(active.value.id, changes(draft.value))
    active.value = saved
    draft.value = { ...saved }
    await refreshList()
    notice.value = '已保存'
  } catch (cause) {
    error.value = message(cause)
  } finally {
    busy.value = false
  }
}
function warnBeforeLeave(event: BeforeUnloadEvent) {
  if (dirty.value) event.preventDefault()
}
function applyServerStory(story: Story) {
  active.value = story
  draft.value = { ...story }
  refreshList().catch(cause => { error.value = message(cause) })
}
function updateDraftBody(value: string) {
  if (draft.value) draft.value.body = value
}
onMounted(async () => {
  window.addEventListener('beforeunload', warnBeforeLeave)
  try {
    await refreshList()
  } catch (cause) {
    error.value = message(cause)
  } finally {
    loading.value = false
  }
})
onBeforeUnmount(() => window.removeEventListener('beforeunload', warnBeforeLeave))
</script>

<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand"><span class="brand-mark">✦</span><span>小说工坊</span></div>
      <div class="sidebar-heading"><span>我的作品</span><span class="count">{{ stories.length }}</span></div>
      <p v-if="loading" class="muted">正在加载作品…</p>
      <p v-else-if="stories.length === 0" class="muted">还没有作品，从一个点子开始。</p>
      <nav aria-label="作品列表" class="story-list">
        <button v-for="story in stories" :key="story.id" type="button" class="story-item"
          :class="{ selected: active?.id === story.id }" :data-story-id="story.id"
          :disabled="busy" @click="selectStory(story.id)">
          <span class="story-icon">文</span>
          <span class="story-info"><strong>{{ story.title }}</strong><small>{{ story.idea }}</small></span>
        </button>
      </nav>
      <form class="new-story" @submit.prevent="create">
        <div class="section-label">新建作品</div>
        <label for="new-title">标题</label>
        <input id="new-title" v-model="newTitle" name="newTitle" maxlength="200" placeholder="给故事起个名字" />
        <label for="new-idea">故事点子</label>
        <textarea id="new-idea" v-model="newIdea" name="newIdea" rows="3" placeholder="一句话也可以" />
        <label for="new-style">文风说明 <span class="optional">选填</span></label>
        <textarea id="new-style" v-model="newStyle" name="newStyle" rows="2" placeholder="如：克制、悬疑、第一人称" />
        <button data-action="create" class="primary full" type="submit" :disabled="busy">＋ 新建作品</button>
      </form>
    </aside>

    <main class="workspace">
      <header class="topbar"><span>创作工作台</span><div class="topbar-actions"><span class="topbar-note">从灵感到成稿，始终由你决定</span><button type="button" class="text-button" @click="showSettings = !showSettings">AI 设置</button></div></header>
      <div v-if="error" class="banner error" role="alert">{{ error }}</div>
      <div v-if="notice" class="banner success" role="status">{{ notice }}</div>
      <AiSettings v-if="showSettings" @close="showSettings = false" />
      <section v-if="!draft" class="welcome">
        <div class="welcome-icon">✦</div>
        <p class="eyebrow">新的故事，从这里开始</p>
        <h1>把脑海里的那一点灵光，<br />写成你的故事。</h1>
        <p class="welcome-copy">在左侧创建作品，记录故事点子。我们会陪你一步步构思、写作与修改。</p>
        <div class="steps"><span>01 记录灵感</span><span>02 构思大纲</span><span>03 完成正文</span></div>
      </section>
      <section v-else class="editor">
        <div class="editor-top"><div><p class="eyebrow">当前作品 · {{ draft.stage }}</p><h1>{{ draft.title }}</h1></div>
          <button data-action="save" type="button" class="primary" :disabled="busy || !dirty" @click="save">{{ busy ? '处理中…' : '保存修改' }}</button>
        </div>
        <p v-if="dirty" class="unsaved">有未保存的修改</p>
        <div class="editor-card">
          <label for="story-title">作品标题</label>
          <input id="story-title" v-model="draft.title" name="title" maxlength="200" />
          <label for="story-idea">故事点子</label>
          <textarea id="story-idea" v-model="draft.idea" name="idea" rows="6" />
          <label for="story-style">文风说明</label>
          <textarea id="story-style" v-model="draft.styleGuide" name="styleGuide" rows="3" placeholder="描述你希望呈现的语言和氛围" />
          <template v-if="draft.proposal || draft.outline">
            <label for="story-proposal">故事方案</label>
            <textarea id="story-proposal" v-model="draft.proposal" name="proposal" rows="8" />
            <label for="story-outline">故事大纲</label>
            <textarea id="story-outline" v-model="draft.outline" name="outline" rows="10" />
          </template>
        </div>
        <PlanningPanel v-if="active && !dirty" :story="active" @updated="applyServerStory" />
        <DraftPanel v-if="active && (active.stage === 'CONFIRMED' || active.stage === 'DRAFT')"
          :story="active" :blocked="dirty" @body-change="updateDraftBody"
          @save-request="save" @updated="applyServerStory" />
        <p v-if="dirty" class="hint">请先保存当前修改，再继续构思。</p>
      </section>
    </main>
  </div>
</template>
