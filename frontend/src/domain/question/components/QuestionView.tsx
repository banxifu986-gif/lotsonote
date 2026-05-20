import React from 'react'
import { EditOutlined, RobotOutlined } from '@ant-design/icons'
import { Button, Space } from 'antd'
import { DifficultyTag } from '../index.ts'
import { QuestionWithUserNote } from '../types/types.ts'

interface QuestionViewProps {
  question?: QuestionWithUserNote
  writeOrEditButtonHandle: () => void
  openAiHandle?: () => void
}

const QuestionView: React.FC<QuestionViewProps> = ({
  question,
  writeOrEditButtonHandle,
  openAiHandle,
}) => {
  return (
    <div className="-mt-4 mb-3 flex w-full justify-center rounded-md bg-white p-4 shadow-sm">
      <div className="w-[900px]">
        <div className="flex justify-between">
          <h2 className="text-xl font-semibold text-neutral-800">
            {question?.title}
          </h2>
          {question?.viewCount ? (
            <div className="mt-2 text-sm text-gray-800">
              <span className="text-gray-500">浏览量：</span>
              <span className="text-base font-medium">
                {question.viewCount}
              </span>
            </div>
          ) : null}
        </div>
        <div className="flex gap-4 py-4 text-sm text-neutral-600">
          <div>
            <span>难度：</span>
            <DifficultyTag difficulty={question?.difficulty}></DifficultyTag>
          </div>
          <div>
            <span>考点：</span>
            <span>{question?.examPoint}</span>
          </div>
        </div>
        <Space>
          <Button icon={<RobotOutlined />} onClick={openAiHandle}>
            AI 讲解
          </Button>
          <Button
            type="primary"
            icon={<EditOutlined />}
            onClick={writeOrEditButtonHandle}
          >
            {question?.userNote?.finished ? '修改笔记' : '写笔记'}
          </Button>
        </Space>
      </div>
    </div>
  )
}

export default QuestionView
