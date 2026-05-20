import { useDispatch } from 'react-redux'
import { resetUser } from '../../../store/userSlice.ts'
import { logout, setLoginModalOpen } from '../../../store/appSlice.ts'
import { kamanoteUserToken } from '../../../base/constants'

export function useLogout() {
  const dispatch = useDispatch()

  return () => {
    localStorage.removeItem(kamanoteUserToken)
    localStorage.removeItem('currentUser')
    dispatch(resetUser())
    dispatch(logout())
    dispatch(setLoginModalOpen(false))
  }
}
