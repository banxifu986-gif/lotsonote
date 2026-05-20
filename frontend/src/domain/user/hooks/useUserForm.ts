import { useDispatch } from 'react-redux'
import { setUser } from '../../../store/userSlice.ts'
import { userService } from '../service/userService.ts'
import { UserState } from '../types/types.ts'
import { useUser } from './useUser.ts'
import { resolveAvatarUrl } from '../utils/avatar.ts'

export function useUserForm() {
  const dispatch = useDispatch()
  const user = useUser()

  async function updateUserInfo(newUserInfo: Partial<UserState>) {
    try {
      await userService.updateMeService(newUserInfo)
      dispatch(
        setUser({
          ...user,
          ...newUserInfo,
          avatarUrl: resolveAvatarUrl(
            String(newUserInfo.avatarUrl ?? user.avatarUrl ?? ''),
          ),
        }),
      )
    } catch (e: any) {
      throw new Error(e.message)
    }
  }

  async function updateUserAvatar(newAvatarUrl: string) {
    const resolvedAvatarUrl = resolveAvatarUrl(newAvatarUrl)

    try {
      await userService.updateMeService({ avatarUrl: resolvedAvatarUrl })
      dispatch(
        setUser({
          ...user,
          avatarUrl: resolvedAvatarUrl,
        }),
      )
    } catch (e: any) {
      throw new Error(e.message)
    }
  }

  return { updateUserInfo, updateUserAvatar }
}
