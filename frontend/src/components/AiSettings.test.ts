import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, expect, it, vi } from 'vitest'
import AiSettings from './AiSettings.vue'

const api = vi.hoisted(() => ({ getAiSettings: vi.fn(), saveAiSettings: vi.fn() }))
vi.mock('../api/ai', () => api)

beforeEach(() => {
  vi.clearAllMocks()
  api.getAiSettings.mockResolvedValue({ baseUrl: 'https://example.com/v1', model: 'model-a', hasKey: true, canSaveKey: true })
  api.saveAiSettings.mockResolvedValue({ baseUrl: 'https://example.com/v1', model: 'model-a', hasKey: true, canSaveKey: true })
})

it('读取设置时不显示已保存的密钥', async () => {
  const wrapper = mount(AiSettings)
  await flushPromises()
  expect((wrapper.get('[name="apiKey"]').element as HTMLInputElement).value).toBe('')
  expect(wrapper.text()).toContain('已配置密钥')
})

it('保存密钥后清空输入框', async () => {
  const wrapper = mount(AiSettings)
  await flushPromises()
  await wrapper.get('[name="apiKey"]').setValue('sk-new-secret')
  await wrapper.get('form').trigger('submit')
  await flushPromises()
  expect(api.saveAiSettings).toHaveBeenCalledWith({ baseUrl: 'https://example.com/v1', model: 'model-a', apiKey: 'sk-new-secret' })
  expect((wrapper.get('[name="apiKey"]').element as HTMLInputElement).value).toBe('')
})
