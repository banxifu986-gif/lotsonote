import { useCallback } from 'react'
import { useDispatch } from 'react-redux'
import { LoginBody } from '../types/serviceTypes.ts'
import {
  loaded,
  login,
  logout,
  setLoginModalOpen,
} from '../../../store/appSlice.ts'
import { userService } from '../service/userService.ts'
import { message } from 'antd'
import { resetUser, setUser } from '../../../store/userSlice.ts'
import { kamanoteUserToken } from '../../../base/constants'
import { resolveAvatarUrl } from '../utils/avatar.ts'

export function useLogin() {
  const dispatch = useDispatch()

  const handleUserAuth = useCallback(
    async (token: string | undefined, data: any) => {
      if (!token) {
        message.error('token is null')
        throw new Error('token is null')
      }
      const normalizedData = {
        ...data,
        avatarUrl: resolveAvatarUrl(data?.avatarUrl || ''),
      }
      localStorage.setItem(kamanoteUserToken, token)
      dispatch(setUser(normalizedData))
      dispatch(login())
      dispatch(setLoginModalOpen(false))
    },
    [dispatch],
  )

  const clearUserAuth = useCallback(() => {
    localStorage.removeItem(kamanoteUserToken)
    localStorage.removeItem('currentUser')
    dispatch(resetUser())
    dispatch(logout())
  }, [dispatch])

  const loginHandle = useCallback(
    async (loginBody: LoginBody) => {
      const resp = await userService.loginService(loginBody)
      const { token, data } = resp
      await handleUserAuth(token, data)
    },
    [handleUserAuth],
  )

  const whoAmIHandle = useCallback(async () => {
    const token = localStorage.getItem(kamanoteUserToken)
    if (!token) {
      clearUserAuth()
      dispatch(loaded())
      return
    }

    try {
      const resp = await userService.whoamiService()
      const { data, token: nextToken } = resp
      await handleUserAuth(nextToken, data)
    } catch (e: unknown) {
      console.log(e)
      clearUserAuth()
    } finally {
      dispatch(loaded())
    }
  }, [clearUserAuth, dispatch, handleUserAuth])

  return {
    loginHandle,
    whoAmIHandle,
    clearUserAuth,
  }
}
