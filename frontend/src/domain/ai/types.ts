export type AiSceneType =
  | 'GENERAL_CHAT'
  | 'QUESTION_TUTOR'
  | 'QUESTION_LIST_PLAN'
  | 'NOTE_COACH'
  | 'NOTE_DETAIL_QA'
  | 'USER_MEMORY'

export type AiBizType = 'QUESTION' | 'QUESTION_LIST' | 'NOTE' | 'USER_SPACE'

export interface AiContextSnapshot {
  title?: string
  categoryName?: string
  contentPath?: string
  questionContent?: string
  examPoint?: string
  noteDraft?: string
  noteContent?: string
  referenceSolution?: string
  description?: string
  relatedTitles?: string[]
}

export interface CreateAiChatSessionBody {
  sceneType: AiSceneType
  bizType: AiBizType
  bizId?: string
  title?: string
  contextSnapshot?: AiContextSnapshot
  forceRecreate?: boolean
}

export interface CreateAiChatMessageBody {
  sessionId: string
  content: string
}

export interface AiChatSession {
  chatSessionId: string
  title?: string
  sceneType?: string
  bizType?: string
  bizId?: string
  createdAt?: string
  updatedAt?: string
}

export interface AiChatHistoryResponse {
  messages: AiChatMessage[]
}

export type AiMessageRole = 'user' | 'assistant'

export interface AiChatMessage {
  chatMessageId?: string
  id: string
  sessionId?: string
  role: AiMessageRole
  content: string
  createdAt?: string
}
