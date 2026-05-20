import React, { useEffect, useState } from 'react'
import { BellOutlined } from '@ant-design/icons'
import { Badge, Menu, MenuProps } from 'antd'
import { NavLink, useLocation } from 'react-router-dom'
import { ColumnDivider } from '../../../../base/components'
import { useApp } from '@/base/hooks'
import { messageService } from '../../../../domain/message/service/messageService.ts'
import DownloadNoteItem from '../../../../domain/note/components/DownloadNoteItem.tsx'
import { LoginModal, UserAvatarMenu } from '../../../../domain/user'
import {
  AI_CHAT,
  HOME_PAGE,
  MESSAGE_CENTER,
  QUESTION_LIST,
  QUESTION_SET,
} from '../../router/config.ts'
import Logo from '../logo/Logo.tsx'
import SearchInput from '../searchInput/SearchInput.tsx'

type MenuItem = Required<MenuProps>['items'][number]

const items: MenuItem[] = [
  {
    label: <NavLink to={HOME_PAGE}>首页</NavLink>,
    key: 'home',
  },
  {
    label: <NavLink to={QUESTION_SET}>题库</NavLink>,
    key: 'question-set',
  },
  {
    label: <NavLink to={QUESTION_LIST}>题单</NavLink>,
    key: 'question-list',
  },
  {
    label: <NavLink to={AI_CHAT}>AI 助手</NavLink>,
    key: 'ai',
  },
]

const NavBar: React.FC = () => {
  const [selectedMenuItem, setSelectedMenuItem] = useState<string[]>()
  const [unreadCount, setUnreadCount] = useState<number>(0)
  const location = useLocation()
  const app = useApp()

  useEffect(() => {
    if (location.pathname === '/') {
      setSelectedMenuItem(['home'])
      return
    }
    setSelectedMenuItem([location.pathname.split('/')[1]])
  }, [location.pathname])

  useEffect(() => {
    if (!app.isLogin) {
      setUnreadCount(0)
      return
    }

    const fetchUnreadCount = async () => {
      try {
        const response = await messageService.getUnreadCount()
        setUnreadCount(response.data)
      } catch (error) {
        console.error('获取未读消息数量失败:', error)
      }
    }

    void fetchUnreadCount()
    const interval = setInterval(fetchUnreadCount, 5000)

    return () => {
      clearInterval(interval)
    }
  }, [app.isLogin])

  return (
    <nav className="flex justify-between bg-[#ffffff] px-32 dark:bg-[#141414]">
      <div className="flex items-center gap-2">
        <Logo />
        <Menu
          items={items}
          mode="horizontal"
          style={{ lineHeight: 'var(--header-height)' }}
          selectedKeys={selectedMenuItem}
        />
        <ColumnDivider />
        <div>
          <DownloadNoteItem />
        </div>
      </div>
      <div className="flex items-center gap-8">
        <SearchInput />
        <NavLink to={MESSAGE_CENTER}>
          <Badge count={unreadCount} size="small">
            <BellOutlined className="hover:text-gray-600" />
          </Badge>
        </NavLink>
        {app.isLogin ? <UserAvatarMenu /> : <LoginModal />}
      </div>
    </nav>
  )
}

export default NavBar
