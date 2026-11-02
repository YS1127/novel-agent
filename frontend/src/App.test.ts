import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.vue'

const api = vi.hoisted(() => ({
  listStories: vi.fn(),
  getStory: vi.fn(),
  createStory: vi.fn(),
  updateStory: vi.fn(),
}))
vi.mock('./api/stories', () => api)

const story = (id: number, title: string) => ({
  id, title, idea: `点子${id}`, styleGuide: '', proposal: '', outline: '', body: '',
  stage: 'IDEA', version: 0, updatedAt: '2026-11-03T00:00:00',
})

describe('作品工作台', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.listStories.mockResolvedValue({ items: [story(1, '甲'), story(2, '乙')], total: 2, page: 1, size: 20 })
    api.getStory.mockImplementation(async (id: number) => story(id, id === 1 ? '甲' : '乙'))
  })

  it('切换作品后显示各自的点子', async () => {
    const wrapper = mount(App)
    await flushPromises()
    await wrapper.get('[data-story-id="1"]').trigger('click')
    await flushPromises()
    expect((wrapper.get('[name="idea"]').element as HTMLTextAreaElement).value).toBe('点子1')
    await wrapper.get('[data-story-id="2"]').trigger('click')
    await flushPromises()
    expect((wrapper.get('[name="idea"]').element as HTMLTextAreaElement).value).toBe('点子2')
  })

  it('新建作品时拒绝空标题并保留输入', async () => {
    const wrapper = mount(App)
    await flushPromises()
    await wrapper.get('[name="newIdea"]').setValue('一个点子')
    await wrapper.get('[data-action="create"]').trigger('click')
    expect(api.createStory).not.toHaveBeenCalled()
    expect((wrapper.get('[name="newIdea"]').element as HTMLTextAreaElement).value).toBe('一个点子')
  })

  it('保存失败时保留编辑内容', async () => {
    api.updateStory.mockRejectedValue(new Error('网络错误'))
    const wrapper = mount(App)
    await flushPromises()
    await wrapper.get('[data-story-id="1"]').trigger('click')
    await flushPromises()
    await wrapper.get('[name="idea"]').setValue('修改后的点子')
    await wrapper.get('[data-action="save"]').trigger('click')
    await flushPromises()
    expect((wrapper.get('[name="idea"]').element as HTMLTextAreaElement).value).toBe('修改后的点子')
    expect(wrapper.text()).toContain('网络错误')
  })
})
