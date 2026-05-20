import React, { useEffect, useState } from 'react'
import { Empty, Pagination } from 'antd'
import { useSearchParams } from 'react-router-dom'
import { Panel } from '../../../../base/components'
import QuestionListView from '../../../../domain/questionList/components/QuestionListView.tsx'
import {
  convertQuestionListToTreeStruct,
  QuestionListParentNode,
  QuestionListTreeView,
  useQuestionListItem2,
  useQuestionLists,
} from '../../../../domain/questionList'
import { QuestionListItemQueryParams } from '../../../../domain/questionList/types/types.ts'
import TrainingCampListHeader from './components/TrainingCampListHeader.tsx'
import TrainingCampListInfo from './components/TrainingCampListInfo.tsx'

const QuestionListPage: React.FC = () => {
  const { questionLists } = useQuestionLists()
  const treeData = convertQuestionListToTreeStruct(questionLists)
  const [selectedQuestionListId, setSelectedQuestionListId] = useState<
    number | undefined
  >()
  const [queryParams, setQueryParams] = useState<QuestionListItemQueryParams>({
    page: 1,
    pageSize: 10,
    questionListId: 0,
  })

  const [searchParams, setSearchParams] = useSearchParams()
  const questionListId = searchParams.get('questionListId') || ''

  useEffect(() => {
    if (questionListId) {
      setSelectedQuestionListId(Number(questionListId))
    }
  }, [questionListId])

  useEffect(() => {
    if (selectedQuestionListId && selectedQuestionListId > 0) {
      setSearchParams({ questionListId: selectedQuestionListId.toString() })
      setQueryParams((prev) => ({
        ...prev,
        questionListId: selectedQuestionListId,
      }))
      return
    }

    setSearchParams({})
    setQueryParams((prev) => ({
      ...prev,
      questionListId: 0,
    }))
  }, [selectedQuestionListId, setSearchParams])

  const { questionListItems, pagination } = useQuestionListItem2(queryParams)

  return (
    <div className="flex justify-center gap-3">
      <div className="w-[300px]">
        <Panel>
          <QuestionListTreeView
            treeData={treeData}
            selectedQuestionListId={selectedQuestionListId}
            handleQuestionListSelect={setSelectedQuestionListId}
          />
        </Panel>
      </div>
      <div className="w-[950px]">
        <Panel>
          {selectedQuestionListId === QuestionListParentNode.TRAINING_CAMP ? (
            <TrainingCampListInfo />
          ) : null}
          {selectedQuestionListId === undefined ? (
            <Empty description="请选择题单"></Empty>
          ) : null}
          {selectedQuestionListId !== undefined &&
          questionListItems.length > 0 ? (
            <>
              <TrainingCampListHeader />
              <QuestionListView questionList={questionListItems} />
            </>
          ) : null}
          {selectedQuestionListId !== undefined &&
          questionListItems.length > 0 ? (
            <div className="mt-2 flex justify-center">
              <Pagination
                total={pagination?.total}
                onChange={(page, pageSize) => {
                  setQueryParams((prev) => ({
                    ...prev,
                    page,
                    pageSize,
                  }))
                }}
                current={queryParams.page}
                pageSize={queryParams.pageSize}
              />
            </div>
          ) : null}
        </Panel>
      </div>
    </div>
  )
}

export default QuestionListPage
