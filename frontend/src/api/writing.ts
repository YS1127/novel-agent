import { isRecord, parseStory, request, type Story } from './stories'

export interface Candidate {
  id: number
  kind: 'DRAFT' | 'REWRITE'
  sourceText: string
  instruction: string
  proposedText: string
  startOffset: number | null
  endOffset: number | null
  baseVersion: number
  status: string
}

function parseCandidate(value: unknown): Candidate {
  if (!isRecord(value) || typeof value.id !== 'number' || !['DRAFT', 'REWRITE'].includes(String(value.kind))
    || typeof value.proposedText !== 'string' || typeof value.baseVersion !== 'number') {
    throw new Error('候选内容数据格式不正确')
  }
  return {
    id: value.id, kind: value.kind as Candidate['kind'], proposedText: value.proposedText,
    sourceText: typeof value.sourceText === 'string' ? value.sourceText : '',
    instruction: typeof value.instruction === 'string' ? value.instruction : '',
    startOffset: typeof value.startOffset === 'number' ? value.startOffset : null,
    endOffset: typeof value.endOffset === 'number' ? value.endOffset : null,
    baseVersion: value.baseVersion, status: typeof value.status === 'string' ? value.status : 'PENDING',
  }
}
export async function listCandidates(storyId: number): Promise<Candidate[]> {
  const value = await request(`/stories/${storyId}/candidates`)
  if (!Array.isArray(value)) throw new Error('候选列表格式不正确')
  return value.map(parseCandidate)
}
export async function generateDraftCandidate(storyId: number, version: number): Promise<Candidate> {
  return parseCandidate(await request(`/stories/${storyId}/draft-candidate`, {
    method: 'POST', body: JSON.stringify({ version }),
  }))
}
export async function adoptDraftCandidate(storyId: number, candidateId: number, version: number): Promise<Story> {
  return parseStory(await request(`/stories/${storyId}/adopt-draft?candidateId=${candidateId}`, {
    method: 'POST', body: JSON.stringify({ version }),
  }))
}
export async function generateRewrite(storyId: number, input: {
  version: number; startOffset: number; endOffset: number; selectedText: string; instruction: string
}): Promise<Candidate> {
  return parseCandidate(await request(`/stories/${storyId}/rewrites`, {
    method: 'POST', body: JSON.stringify(input),
  }))
}
export async function adoptRewrite(storyId: number, candidateId: number, version: number): Promise<Story> {
  return parseStory(await request(`/stories/${storyId}/rewrites/${candidateId}/adopt`, {
    method: 'POST', body: JSON.stringify({ version }),
  }))
}
export async function dismissCandidate(storyId: number, candidateId: number): Promise<void> {
  await request(`/stories/${storyId}/candidates/${candidateId}`, { method: 'DELETE' })
}
