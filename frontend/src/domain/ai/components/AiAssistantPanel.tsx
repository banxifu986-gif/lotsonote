import React, { useState } from 'react'
import { Button, Empty, Input, List, Spin, message } from 'antd'
import type { AiChatMessage } from '../types.ts'

interface AiAssistantPanelProps {
  title: string
  messages: AiChatMessage[]
  loading?: boolean
  sending?: boolean
  emptyDescription?: string
  placeholder?: string
  onSend: (content: string) => Promise<void>
}

const AiAssistantPanel: React.FC<AiAssistantPanelProps> = ({
  title,
  messages,
  loading = false,
  sending = false,
  emptyDescription = '开始一个新的 AI 对话',
  placeholder = '输入你想问 AI 的内容',
  onSend,
}) => {
  const [input, setInput] = useState('')

  const handleSend = async () => {
    const content = input.trim()
    if (!content) {
      message.info('请输入你想问 AI 的内容')
      return
    }

    await onSend(content)
    setInput('')
  }

  return (
    <div>
      <div className="mb-4 border-b border-gray-100 pb-3">
        <div className="text-lg font-semibold text-neutral-800">{title}</div>
        <div className="mt-1 text-sm text-gray-500">
          由 LotsoNote 后端直接调用 DeepSeek 返回结果
        </div>
      </div>
      {loading ? (
        <div className="flex min-h-[420px] items-center justify-center">
          <Spin />
        </div>
      ) : messages.length === 0 ? (
        <div className="flex min-h-[420px] items-center justify-center">
          <Empty description={emptyDescription} />
        </div>
      ) : (
        <List
          className="mb-4 min-h-[420px]"
          dataSource={messages}
          renderItem={(item) => (
            <List.Item>
              <div className="w-full">
                <div className="mb-1 text-xs text-gray-500">
                  {item.role === 'user' ? '我' : 'AI'}
                </div>
                <div className="whitespace-pre-wrap rounded bg-gray-50 p-3">
                  {item.content}
                </div>
              </div>
            </List.Item>
          )}
        />
      )}
      <Input.TextArea
        value={input}
        rows={4}
        onChange={(event) => setInput(event.target.value)}
        placeholder={placeholder}
      />
      <div className="mt-3 flex justify-end">
        <Button
          type="primary"
          loading={sending}
          onClick={() => void handleSend()}
        >
          发送给 AI
        </Button>
      </div>
    </div>
  )
}

export default AiAssistantPanel
