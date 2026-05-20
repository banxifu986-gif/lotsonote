import { Route } from 'react-router-dom'
import { NotFound } from '../../../base/components'
import UserApp from '../UserApp.tsx'
import { AuthGuard } from '../../../domain/user'
import AiPage from '../pages/ai/AiPage.tsx'
import HomePage from '../pages/home/HomePage.tsx'
import MessagePage from '../pages/message/MessagePage.tsx'
import QuestionPage from '../pages/question/QuestionPage.tsx'
import QuestionListPage from '../pages/questionList/QuestionListPage.tsx'
import QuestionSetPage from '../pages/questionSet/QuestionSetPage.tsx'
import UserHomePage from '../pages/userHome/UserHomePage.tsx'
import UserCenterPage from '../pages/userCenter/UserCenterPage.tsx'
import UserCollect from '../pages/userCenter/collect/UserCollect.tsx'
import UserInfo from '../pages/userCenter/info/UserInfo.tsx'
import UserNote from '../pages/userCenter/note/UserNote.tsx'
import {
  AI_CHAT,
  HOME,
  HOME_PAGE,
  MESSAGE_CENTER,
  QUESTION,
  QUESTION_LIST,
  QUESTION_SET,
  USER_CENTER,
  USER_COLLECT,
  USER_HOME,
  USER_INFO,
  USER_NOTE,
} from './config.ts'

export const UserRouteConfig = (
  <Route element={<AuthGuard />}>
    <Route path={HOME} element={<UserApp />}>
      <Route index element={<HomePage />} />
      <Route path={HOME_PAGE} element={<HomePage />} />
      <Route path={QUESTION_SET} element={<QuestionSetPage />} />
      <Route path={`${QUESTION}/:questionId`} element={<QuestionPage />} />
      <Route path={`${USER_HOME}/:userId`} element={<UserHomePage />} />
      <Route path={QUESTION_LIST} element={<QuestionListPage />} />
      <Route path={USER_CENTER} element={<UserCenterPage />}>
        <Route index element={<UserInfo />} />
        <Route path={USER_INFO} element={<UserInfo />} />
        <Route path={USER_COLLECT} element={<UserCollect />} />
        <Route path={USER_NOTE} element={<UserNote />} />
      </Route>
      <Route path={AI_CHAT} element={<AiPage />} />
      <Route path={MESSAGE_CENTER} element={<MessagePage />} />
      <Route path="/*" element={<NotFound />} />
    </Route>
  </Route>
)
