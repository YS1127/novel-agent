import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, expect, it, vi } from 'vitest'
import PlanningPanel from './PlanningPanel.vue'

const api = vi.hoisted(() => ({ listQuestions: vi.fn(), generateQuestions: vi.fn(), saveAnswers: vi.fn(), generatePlan: vi.fn(), confirmPlan: vi.fn() }))
vi.mock('../api/ai', () => api)
const stories = vi.hoisted(() => ({ getStory: vi.fn() }))
vi.mock('../api/stories', () => stories)

const story = { id: 1, title: '测试', idea: '一个故事', styleGuide: '', proposal: '', outline: '', body: '', stage: 'IDEA', version: 0, updatedAt: '' }

beforeEach(() => {
  vi.clearAllMocks()
  api.listQuestions.mockResolvedValue([])
  api.generateQuestions.mockResolvedValue([{ id: 10, ordinal: 1, question: '主角是谁？', answer: '' }])
  stories.getStory.mockResolvedValue({ ...story, stage: 'QUESTIONS', version: 1 })
})

it('生成关键问题后显示问题并通知父组件更新版本', async () => {
  const wrapper = mount(PlanningPanel, { props: { story } })
  await flushPromises()
  await wrapper.get('[data-action="generate-questions"]').trigger('click')
  await flushPromises()
  expect(wrapper.text()).toContain('主角是谁？')
  expect(wrapper.emitted('updated')?.[0]?.[0]).toMatchObject({ version: 1, stage: 'QUESTIONS' })
})
