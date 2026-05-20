import { useEffect } from 'react'
import { Outlet } from 'react-router-dom'
import { Spin } from 'antd'
import { useDispatch } from 'react-redux'
import { useApp } from '@/base/hooks'
import { useUser } from '../hooks/useUser.ts'
import { Admin } from '../types/types.ts'
import { setLoginModalOpen } from '../../../store/appSlice.ts'

interface AuthGuardProps {
  requireAdmin?: boolean
}

const AuthGuard: React.FC<AuthGuardProps> = ({ requireAdmin = false }) => {
  const app = useApp()
  const user = useUser()
  const dispatch = useDispatch()

  useEffect(() => {
    if (!app.isLoaded) {
      return
    }

    if (!app.isLogin) {
      dispatch(setLoginModalOpen(true))
    }
  }, [app.isLoaded, app.isLogin, dispatch])

  if (!app.isLoaded) {
    return (
      <div className="flex min-h-[240px] items-center justify-center">
        <Spin />
      </div>
    )
  }

  if (!app.isLogin) {
    return null
  }

  if (requireAdmin && user.isAdmin !== Admin.ADMIN) {
    return (
      <div className="flex min-h-[240px] items-center justify-center text-base text-gray-500">
        无管理员权限
      </div>
    )
  }

  return <Outlet />
}

export default AuthGuard
