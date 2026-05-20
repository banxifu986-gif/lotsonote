import { ApiList, httpClient } from '../../../request'
import type {
  AiChatHistoryResponse,
  AiChatMessage,
  AiChatSession,
  CreateAiChatMessageBody,
  CreateAiChatSessionBody,
} from '../types.ts'

const aiApi: ApiList = {
  listChatSessions: ['GET', '/api/ai/chat-sessions'],
  createChatSession: ['POST', '/api/ai/chat-sessions'],
  createChatMessage: ['POST', '/api/ai/chat-messages'],
  getChatMessages: ['GET', '/api/ai/chat-sessions/{sessionId}/messages'],
}

export const aiService = {
  listChatSessions: () =>
    httpClient.request<AiChatSession[]>(aiApi.listChatSessions),

  createChatSession: (body: CreateAiChatSessionBody) =>
    httpClient.request<AiChatSession>(aiApi.createChatSession, {
      body,
    }),

  createChatMessage: (body: CreateAiChatMessageBody) =>
    httpClient.request<AiChatMessage>(aiApi.createChatMessage, {
      body,
    }),

  getChatMessages: (sessionId: string) =>
    httpClient.request<AiChatHistoryResponse>(aiApi.getChatMessages, {
      pathParams: [sessionId],
    }),
}
