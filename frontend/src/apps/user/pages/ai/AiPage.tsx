import React, { useEffect, useState } from 'react'
import { PlusOutlined } from '@ant-design/icons'
import { Button, Empty, List, Spin, Typography, message } from 'antd'
import { Panel } from '../../../../base/components'
import { useApp } from '../../../../base/hooks'
import AiAssistantPanel from '../../../../domain/ai/components/AiAssistantPanel.tsx'
import { aiService } from '../../../../domain/ai/service/aiService.ts'
import type {
  AiChatMessage,
  AiChatSession,
} from '../../../../domain/ai/types.ts'

const AiPage: React.FC = () => {
  const app = useApp()
  const [sessions, setSessions] = useState<AiChatSession[]>([])
  const [activeSessionId, setActiveSessionId] = useState<string>()
  const [messages, setMessages] = useState<AiChatMessage[]>([])
  const [loadingSessions, setLoadingSessions] = useState(false)
  const [loadingMessages, setLoadingMessages] = useState(false)
  const [sending, setSending] = useState(false)

  const clearState = () => {
    setSessions([])
    setActiveSessionId(undefined)
    setMessages([])
  }

  const loadMessages = async (sessionId: string) => {
    setLoadingMessages(true)
    try {
      const response = await aiService.getChatMessages(sessionId)
      setActiveSessionId(sessionId)
      setMessages(response.data.messages)
    } catch (error) {
      message.error(
        error instanceof Error ? error.message : '加载 AI 会话消息失败',
      )
    } finally {
      setLoadingMessages(false)
    }
  }

  const refreshSessions = async (preferredSessionId?: string) => {
    const response = await aiService.listChatSessions()
    const nextSessions = response.data
    setSessions(nextSessions)

    if (!nextSessions.length) {
      setActiveSessionId(undefined)
      setMessages([])
      return
    }

    const nextSessionId =
      preferredSessionId &&
      nextSessions.some((item) => item.chatSessionId === preferredSessionId)
        ? preferredSessionId
        : nextSessions[0].chatSessionId

    if (nextSessionId && nextSessionId !== activeSessionId) {
      await loadMessages(nextSessionId)
    }
  }

  useEffect(() => {
    if (!app.isLogin) {
      clearState()
      return
    }

    const loadInitialData = async () => {
      setLoadingSessions(true)
      try {
        await refreshSessions()
      } catch (error) {
        message.error(
          error instanceof Error ? error.message : '加载 AI 会话列表失败',
        )
      } finally {
        setLoadingSessions(false)
      }
    }

    void loadInitialData()
  }, [app.isLogin])

  const createSession = async () => {
    const response = await aiService.createChatSession({
      sceneType: 'GENERAL_CHAT',
      bizType: 'USER_SPACE',
      title: '新对话',
    })
    const nextSession = response.data
    setSessions((prev) => [nextSession, ...prev])
    setActiveSessionId(nextSession.chatSessionId)
    setMessages([])
    return nextSession.chatSessionId
  }

  const handleCreateSession = async () => {
    if (!app.isLogin) {
      message.info('请先登录后再使用 AI 助手')
      return
    }

    try {
      await createSession()
    } catch (error) {
      message.error(error instanceof Error ? error.message : '创建 AI 会话失败')
    }
  }

  const handleSend = async (content: string) => {
    if (!app.isLogin) {
      message.info('请先登录后再使用 AI 助手')
      return
    }

    setSending(true)
    try {
      const sessionId = activeSessionId ?? (await createSession())
      await aiService.createChatMessage({
        sessionId,
        content,
      })
      await refreshSessions(sessionId)
      await loadMessages(sessionId)
    } catch (error) {
      message.error(
        error instanceof Error ? error.message : 'AI 请求失败，请稍后重试',
      )
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="flex justify-center gap-4">
      <div className="w-[280px]">
        <Panel>
          <div className="mb-3 flex items-center justify-between">
            <Typography.Title level={5} className="!mb-0">
              AI 会话
            </Typography.Title>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => void handleCreateSession()}
            >
              新建
            </Button>
          </div>
          {loadingSessions ? (
            <div className="py-8 text-center">
              <Spin />
            </div>
          ) : sessions.length === 0 ? (
            <Empty description="暂无 AI 会话" />
          ) : (
            <List
              dataSource={sessions}
              renderItem={(item) => (
                <List.Item
                  className={`cursor-pointer rounded px-3 ${
                    activeSessionId === item.chatSessionId
                      ? 'bg-blue-50'
                      : 'hover:bg-gray-50'
                  }`}
                  onClick={() => void loadMessages(item.chatSessionId)}
                >
                  <div className="w-full">
                    <div className="truncate font-medium text-gray-800">
                      {item.title || '新对话'}
                    </div>
                    <div className="mt-1 text-xs text-gray-500">
                      {item.updatedAt
                        ? new Date(item.updatedAt).toLocaleString()
                        : '刚刚创建'}
                    </div>
                  </div>
                </List.Item>
              )}
            />
          )}
        </Panel>
      </div>
      <div className="w-[900px]">
        <Panel>
          {!app.isLogin ? (
            <Empty description="请先登录后使用 AI 助手" />
          ) : (
            <AiAssistantPanel
              title="LotsoNote AI 助手"
              messages={messages}
              loading={loadingMessages}
              sending={sending}
              onSend={handleSend}
              emptyDescription="创建一个新对话，开始向 LotsoNote AI 助手提问"
              placeholder="输入你的学习问题、笔记整理需求或复习思路"
            />
          )}
        </Panel>
      </div>
    </div>
  )
}

export default AiPage
