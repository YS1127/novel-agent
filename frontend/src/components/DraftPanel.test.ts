import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, expect, it, vi } from 'vitest'
import DraftPanel from './DraftPanel.vue'

const writing = vi.hoisted(() => ({
  listCandidates: vi.fn(), generateDraftCandidate: vi.fn(), adoptDraftCandidate: vi.fn(),
  generateRewrite: vi.fn(), adoptRewrite: vi.fn(), dismissCandidate: vi.fn(),
}))
vi.mock('../api/writing', () => writing)
const stories = vi.hoisted(() => ({ updateStory: vi.fn() }))
vi.mock('../api/stories', () => stories)

const story = { id: 1, title: '作品', idea: '点子', styleGuide: '', proposal: '方案', outline: '大纲',
  body: '', stage: 'CONFIRMED', version: 2, updatedAt: '' }
const candidate = { id: 8, kind: 'DRAFT', sourceText: '', instruction: '', proposedText: '候选正文',
  startOffset: null, endOffset: null, baseVersion: 2, status: 'PENDING' }

beforeEach(() => {
  vi.clearAllMocks()
  writing.listCandidates.mockResolvedValue([])
  writing.generateDraftCandidate.mockResolvedValue(candidate)
  writing.adoptDraftCandidate.mockResolvedValue({ ...story, body: '候选正文', stage: 'DRAFT', version: 3 })
})

it('生成初稿只显示候选，不修改正文', async () => {
  const wrapper = mount(DraftPanel, { props: { story } })
  await flushPromises()
  await wrapper.get('[data-action="generate-draft"]').trigger('click')
  await flushPromises()
  expect(wrapper.text()).toContain('候选正文')
  expect(wrapper.emitted('updated')).toBeUndefined()
})

it('明确采纳后才通知父组件更新正文', async () => {
  const wrapper = mount(DraftPanel, { props: { story } })
  await flushPromises()
  await wrapper.get('[data-action="generate-draft"]').trigger('click')
  await flushPromises()
  await wrapper.get('[data-action="adopt-candidate"]').trigger('click')
  await flushPromises()
  expect(writing.adoptDraftCandidate).toHaveBeenCalledWith(1, 8, 2)
  expect(wrapper.emitted('updated')?.[0]?.[0]).toMatchObject({ body: '候选正文', version: 3 })
})

it('正文有未保存修改时通知工作台', async () => {
  const wrapper = mount(DraftPanel, { props: { story } })
  await flushPromises()
  await wrapper.get('[name="body"]').setValue('手动写作')
  expect(wrapper.emitted('body-change')?.at(-1)?.[0]).toBe('手动写作')
})
