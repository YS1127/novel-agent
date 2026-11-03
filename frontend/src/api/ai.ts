import { isRecord, parseStory, request, type Story } from './stories'

export interface AiSettings {
  baseUrl: string
  model: string
  hasKey: boolean
  canSaveKey: boolean
}
export interface StoryQuestion {
  id: number
  ordinal: number
  question: string
  answer: string
}

function parseSettings(value: unknown): AiSettings {
  if (!isRecord(value) || typeof value.baseUrl !== 'string' || typeof value.model !== 'string'
    || typeof value.hasKey !== 'boolean' || typeof value.canSaveKey !== 'boolean') {
    throw new Error('AI 设置数据格式不正确')
  }
  return { baseUrl: value.baseUrl, model: value.model, hasKey: value.hasKey, canSaveKey: value.canSaveKey }
}
function parseQuestions(value: unknown): StoryQuestion[] {
  if (!Array.isArray(value)) throw new Error('问题数据格式不正确')
  return value.map(item => {
    if (!isRecord(item) || typeof item.id !== 'number' || typeof item.ordinal !== 'number'
      || typeof item.question !== 'string') throw new Error('问题数据格式不正确')
    return { id: item.id, ordinal: item.ordinal, question: item.question,
      answer: typeof item.answer === 'string' ? item.answer : '' }
  })
}

export async function getAiSettings(): Promise<AiSettings> {
  return parseSettings(await request('/ai/settings'))
}
export async function saveAiSettings(input: { baseUrl: string; model: string; apiKey: string }): Promise<AiSettings> {
  return parseSettings(await request('/ai/settings', { method: 'PUT', body: JSON.stringify(input) }))
}
export async function listQuestions(storyId: number): Promise<StoryQuestion[]> {
  return parseQuestions(await request(`/stories/${storyId}/questions`))
}
export async function generateQuestions(storyId: number, version: number): Promise<StoryQuestion[]> {
  return parseQuestions(await request(`/stories/${storyId}/questions`, { method: 'POST', body: JSON.stringify({ version }) }))
}
export async function saveAnswers(storyId: number, version: number, answers: { id: number; answer: string }[]): Promise<StoryQuestion[]> {
  return parseQuestions(await request(`/stories/${storyId}/answers`, { method: 'PUT', body: JSON.stringify({ version, answers }) }))
}
export async function generatePlan(storyId: number, version: number): Promise<Story> {
  return parseStory(await request(`/stories/${storyId}/plan`, { method: 'POST', body: JSON.stringify({ version }) }))
}
export async function confirmPlan(storyId: number, version: number): Promise<Story> {
  return parseStory(await request(`/stories/${storyId}/confirm-plan`, { method: 'POST', body: JSON.stringify({ version }) }))
}
