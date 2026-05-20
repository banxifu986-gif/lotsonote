import { message } from 'antd'
import { useDispatch } from 'react-redux'
import { kamanoteUserToken } from '../../../base/constants'
import { login } from '../../../store/appSlice.ts'
import { setUser } from '../../../store/userSlice.ts'
import { userService } from '../service/userService.ts'
import { RegisterBody } from '../types/serviceTypes.ts'
import { resolveAvatarUrl } from '../utils/avatar.ts'

export function useRegister() {
  const dispatch = useDispatch()

  async function registerHandle(registerBody: RegisterBody) {
    try {
      const resp = await userService.registerService(registerBody)
      if (!resp) {
        return
      }

      const { token } = resp
      if (!token) {
        message.error('token is null')
        throw new Error('token is null')
      }

      localStorage.setItem(kamanoteUserToken, token)
      const me = await userService.whoamiService()
      dispatch(
        setUser({
          ...me.data,
          avatarUrl: resolveAvatarUrl(me.data?.avatarUrl || ''),
        }),
      )
      dispatch(login())
    } catch (e: any) {
      throw new Error(e.message)
    }
  }

  return {
    registerHandle,
  }
}
