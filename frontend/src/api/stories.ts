export interface Story {
  id: number
  title: string
  idea: string
  styleGuide: string
  proposal: string
  outline: string
  body: string
  stage: string
  version: number
  updatedAt: string
}

export interface StoryPage {
  items: Story[]
  total: number
  page: number
  size: number
}

export type StoryChanges = Pick<Story, 'title' | 'idea' | 'styleGuide' | 'proposal' | 'outline' | 'body' | 'version'>

export async function request(path: string, init?: RequestInit): Promise<unknown> {
  let response: Response
  try {
    response = await fetch(`/api${path}`, {
      ...init,
      headers: { 'Content-Type': 'application/json', ...init?.headers },
    })
  } catch {
    throw new Error('无法连接后端，请确认服务正在运行')
  }
  const data: unknown = await response.json().catch(() => null)
  if (!response.ok) {
    const message = isRecord(data) && typeof data.message === 'string' ? data.message : `请求失败（${response.status}）`
    throw new Error(message)
  }
  return data
}

export function isRecord(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}

export function parseStory(value: unknown): Story {
  if (!isRecord(value) || typeof value.id !== 'number' || value.id <= 0
    || typeof value.title !== 'string' || typeof value.idea !== 'string'
    || typeof value.version !== 'number') {
    throw new Error('后端返回的作品数据格式不正确')
  }
  return {
    id: value.id,
    title: value.title,
    idea: value.idea,
    styleGuide: typeof value.styleGuide === 'string' ? value.styleGuide : '',
    proposal: typeof value.proposal === 'string' ? value.proposal : '',
    outline: typeof value.outline === 'string' ? value.outline : '',
    body: typeof value.body === 'string' ? value.body : '',
    stage: typeof value.stage === 'string' ? value.stage : 'IDEA',
    version: value.version,
    updatedAt: typeof value.updatedAt === 'string' ? value.updatedAt : '',
  }
}

export async function listStories(page = 1): Promise<StoryPage> {
  const value = await request(`/stories?page=${page}&size=20`)
  if (!isRecord(value) || !Array.isArray(value.items) || typeof value.total !== 'number') {
    throw new Error('后端返回的列表格式不正确')
  }
  return {
    items: value.items.map(parseStory), total: value.total,
    page: typeof value.page === 'number' ? value.page : page,
    size: typeof value.size === 'number' ? value.size : 20,
  }
}

export async function getStory(id: number): Promise<Story> {
  return parseStory(await request(`/stories/${id}`))
}

export async function createStory(input: { title: string; idea: string; styleGuide: string }): Promise<Story> {
  return parseStory(await request('/stories', { method: 'POST', body: JSON.stringify(input) }))
}

export async function updateStory(id: number, input: StoryChanges): Promise<Story> {
  return parseStory(await request(`/stories/${id}`, { method: 'PUT', body: JSON.stringify(input) }))
}
