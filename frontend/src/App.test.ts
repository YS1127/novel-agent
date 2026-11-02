import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
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
  afterEach(() => vi.restoreAllMocks())
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
    await wrapper.get('form.new-story').trigger('submit')
    expect(api.createStory).not.toHaveBeenCalled()
    expect((wrapper.get('[name="newIdea"]').element as HTMLTextAreaElement).value).toBe('一个点子')
  })

  it('提交有效标题和点子后创建作品', async () => {
    api.createStory.mockResolvedValue(story(3, '新作'))
    const wrapper = mount(App)
    await flushPromises()
    await wrapper.get('[name="newTitle"]').setValue('新作')
    await wrapper.get('[name="newIdea"]').setValue('一个点子')
    await wrapper.get('form.new-story').trigger('submit')
    await flushPromises()
    expect(api.createStory).toHaveBeenCalledWith({ title: '新作', idea: '一个点子', styleGuide: '' })
  })

  it('新建已成功但列表刷新失败时仍显示新作品', async () => {
    api.createStory.mockResolvedValue(story(3, '新作'))
    api.listStories.mockResolvedValueOnce({ items: [], total: 0, page: 1, size: 20 }).mockRejectedValueOnce(new Error('刷新失败'))
    const wrapper = mount(App)
    await flushPromises()
    await wrapper.get('[name="newTitle"]').setValue('新作')
    await wrapper.get('[name="newIdea"]').setValue('一个点子')
    await wrapper.get('form.new-story').trigger('submit')
    await flushPromises()
    expect(wrapper.find('[name="idea"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('新作')
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

  it('正文未保存时阻止切换作品', async () => {
    api.getStory.mockImplementation(async (id: number) => ({ ...story(id, id === 1 ? '甲' : '乙'), stage: 'CONFIRMED' }))
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    const wrapper = mount(App)
    await flushPromises()
    await wrapper.get('[data-story-id="1"]').trigger('click')
    await flushPromises()
    await wrapper.get('[name="body"]').setValue('未保存正文')
    await wrapper.get('[data-story-id="2"]').trigger('click')
    expect(api.getStory).not.toHaveBeenCalledWith(2)
  })
})
