import { useApp } from '@/base/hooks'
import LoginModal from './LoginModal.tsx'

const ForceLoginModal: React.FC = () => {
  const app = useApp()
  const shouldForceOpen = app.loginModalOpen && !app.isLogin

  if (!shouldForceOpen) {
    return null
  }

  return <LoginModal forceOpen />
}

export default ForceLoginModal
