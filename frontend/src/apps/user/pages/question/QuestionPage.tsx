import React, { Suspense, useEffect, useMemo, useState } from 'react'
import { EyeOutlined } from '@ant-design/icons'
import { Upload } from '@icon-park/react'
import { Button, Drawer, Space, Tag, message, Modal, Spin } from 'antd'
import { useParams } from 'react-router-dom'
import {
  MarkdownEditor,
  MarkdownRender,
  Panel,
} from '../../../../base/components'
import { useApp } from '../../../../base/hooks'
import AiAssistantPanel from '../../../../domain/ai/components/AiAssistantPanel.tsx'
import { aiService } from '../../../../domain/ai/service/aiService.ts'
import type { AiChatMessage } from '../../../../domain/ai/types.ts'
import { NoteList, NoteQueryParams, useNotes } from '../../../../domain/note'
import { QuestionView, useQuestion } from '../../../../domain/question'

const QuestionPage: React.FC = () => {
  const { questionId } = useParams()
  const questionIdNumber = Number(questionId)
  const { question, userFinishedQuestion, updateUserNoteContent } =
    useQuestion(questionIdNumber)

  const [value, setValue] = useState(question?.userNote.content ?? '')
  const [isEditorVisible, setIsEditorVisible] = useState(false)
  const [isShowPreview, setIsShowPreview] = useState(false)
  const [createBtnLoading, setCreateBtnLoading] = useState(false)
  const [aiDrawerOpen, setAiDrawerOpen] = useState(false)
  const [aiSessionId, setAiSessionId] = useState<string>()
  const [aiMessages, setAiMessages] = useState<AiChatMessage[]>([])
  const [aiSending, setAiSending] = useState(false)
  const [aiLoadingMessages, setAiLoadingMessages] = useState(false)

  useEffect(() => {
    if (question?.userNote?.finished) {
      setValue(question.userNote.content)
    }
  }, [question])

  useEffect(() => {
    setAiSessionId(undefined)
    setAiMessages([])
  }, [question?.userNote?.content])

  useEffect(() => {
    setAiSessionId(undefined)
    setAiMessages([])
    setAiDrawerOpen(false)
  }, [questionIdNumber])

  const [noteQueryParams, setNoteQueryParams] = useState<NoteQueryParams>({
    page: 1,
    pageSize: 10,
    questionId: questionIdNumber,
  })

  const {
    noteList,
    pagination,
    createNoteHandle,
    updateNoteHandle,
    setNoteLikeStatusHandle,
    setNoteCollectStatusHandle,
  } = useNotes(noteQueryParams)

  const app = useApp()

  const aiQuickPrompts = useMemo(
    () => ['请讲解这道题', '请给我一些思路提示', '请帮我梳理这道题的考点'],
    [],
  )

  const createOrUpdateNoteClickHandle = async () => {
    if (!app.isLogin) {
      message.info('请先登录')
      return
    }

    setCreateBtnLoading(true)
    try {
      if (!question?.userNote?.finished) {
        const noteId = await createNoteHandle(questionIdNumber, value)
        setIsEditorVisible(false)
        if (noteId) {
          userFinishedQuestion(noteId, value)
          setAiSessionId(undefined)
          setAiMessages([])
        }
        message.success('笔记已提交')
      } else {
        if (!question?.userNote) {
          return
        }
        await updateNoteHandle(question.userNote.noteId, {
          content: value,
          questionId: questionIdNumber,
        })
        updateUserNoteContent(value)
        setAiSessionId(undefined)
        setAiMessages([])
        message.success('笔记已修改')
        setIsEditorVisible(false)
      }
    } catch (error: any) {
      message.error(error.message)
    } finally {
      setCreateBtnLoading(false)
    }
  }

  const loadAiMessages = async (sessionId: string) => {
    setAiLoadingMessages(true)
    try {
      const response = await aiService.getChatMessages(sessionId)
      setAiSessionId(sessionId)
      setAiMessages(response.data.messages)
    } finally {
      setAiLoadingMessages(false)
    }
  }

  const ensureQuestionAiSession = async () => {
    if (!question) {
      throw new Error('题目详情尚未加载完成')
    }

    if (aiSessionId) {
      return aiSessionId
    }

    const response = await aiService.createChatSession({
      sceneType: 'QUESTION_TUTOR',
      bizType: 'QUESTION',
      bizId: String(question.questionId),
      title: `${question.title} - AI讲解`,
      forceRecreate: true,
      contextSnapshot: {
        title: question.title,
        questionContent: question.title,
        description: `难度：${question.difficulty ?? '未知'}`,
        referenceSolution: question.referenceSolution ?? undefined,
        examPoint: question.examPoint,
        noteContent: question.userNote?.finished
          ? question.userNote.content
          : undefined,
      },
    })
    const nextSessionId = response.data.chatSessionId
    await loadAiMessages(nextSessionId)
    return nextSessionId
  }

  const openAiHandle = async () => {
    if (!app.isLogin) {
      message.info('请先登录')
      return
    }
    setAiDrawerOpen(true)
    try {
      await ensureQuestionAiSession()
    } catch (error: any) {
      message.error(error.message ?? '打开 AI 讲解失败')
    }
  }

  const handleAiSend = async (content: string) => {
    setAiSending(true)
    try {
      const sessionId = await ensureQuestionAiSession()
      await aiService.createChatMessage({
        sessionId,
        content,
      })
      await loadAiMessages(sessionId)
    } catch (error: any) {
      message.error(error.message ?? 'AI 请求失败，请稍后重试')
    } finally {
      setAiSending(false)
    }
  }

  return (
    <>
      <QuestionView
        question={question}
        writeOrEditButtonHandle={() => setIsEditorVisible((prev) => !prev)}
        openAiHandle={() => void openAiHandle()}
      />
      <Drawer
        title="题目 AI 辅导"
        width={560}
        open={aiDrawerOpen}
        onClose={() => setAiDrawerOpen(false)}
        destroyOnClose={false}
      >
        <div className="mb-3">
          <div className="mb-2 text-sm text-gray-500">
            当前回答会优先参考平台解析与用户笔记，不判定对错。
          </div>
          <Space wrap>
            <Tag color="blue">题目讲解</Tag>
            <Tag color="gold">思路提示</Tag>
            <Tag color="purple">考点梳理</Tag>
            <Tag color="green">继续追问</Tag>
          </Space>
        </div>
        <div className="mb-3 flex flex-wrap gap-2">
          {aiQuickPrompts.map((prompt) => (
            <Button
              key={prompt}
              size="small"
              onClick={() => void handleAiSend(prompt)}
            >
              {prompt}
            </Button>
          ))}
        </div>
        <AiAssistantPanel
          title={question ? `${question.title} · AI讲解` : '题目 AI 辅导'}
          messages={aiMessages}
          loading={aiLoadingMessages}
          sending={aiSending}
          onSend={handleAiSend}
          emptyDescription="从当前题目发起讲解或追问，AI 会复用这道题的上下文"
          placeholder="继续追问这道题的思路、考点或你的卡点"
        />
      </Drawer>
      {isEditorVisible ? (
        <div className="mb-4 flex w-full justify-center">
          <div className="w-[900px]">
            <div className="h-[calc(100vh-var(--header-height)-65px)]">
              <Suspense
                fallback={
                  <Spin tip="编辑器加载中" className="mt-12">
                    {''}
                  </Spin>
                }
              >
                <MarkdownEditor
                  value={value}
                  setValue={setValue}
                ></MarkdownEditor>
              </Suspense>
            </div>
            <div className="sticky bottom-0 z-20 flex justify-end gap-2 border-t border-gray-200 bg-white p-4 shadow">
              <Button
                icon={<EyeOutlined />}
                onClick={() => setIsShowPreview(true)}
              >
                预览笔记
              </Button>
              <Button
                type="primary"
                icon={<Upload />}
                loading={createBtnLoading}
                onClick={createOrUpdateNoteClickHandle}
              >
                {question?.userNote?.finished ? '修改笔记' : '提交笔记'}
              </Button>
            </div>
          </div>
        </div>
      ) : null}
      <Modal
        open={isShowPreview}
        onCancel={() => setIsShowPreview(false)}
        footer={null}
        width={1000}
      >
        <MarkdownRender markdown={value} />
      </Modal>
      {question?.referenceSolution ? (
        <div className="mb-4 flex w-full justify-center">
          <div className="w-[900px]">
            <Panel>
              <div className="mb-3 text-lg font-semibold text-neutral-800">
                题目参考解析
              </div>
              <MarkdownRender markdown={question.referenceSolution} />
            </Panel>
          </div>
        </div>
      ) : null}
      <div className="flex w-full justify-center">
        <div className="w-[700px]">
          <Panel>
            <NoteList
              showQuestion={false}
              noteList={noteList}
              pagination={pagination}
              queryParams={noteQueryParams}
              setQueryParams={setNoteQueryParams}
              setNoteLikeStatusHandle={setNoteLikeStatusHandle}
              setNoteCollectStatusHandle={setNoteCollectStatusHandle}
            />
          </Panel>
        </div>
      </div>
    </>
  )
}

export default QuestionPage
