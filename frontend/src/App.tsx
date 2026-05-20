import './App.css'
import { Routes } from 'react-router-dom'
import { AdminRouteConfig } from './apps/admin/router'
import { UserRouteConfig } from './apps/user/router'
import { ErrorBoundary } from 'react-error-boundary'
import { ErrorFallback } from './base/components'
import { useLogin } from './domain/user'
import { useEffect } from 'react'
import { useApp } from './base/hooks'
import { useDispatch } from 'react-redux'
import { setLoginModalOpen } from './store/appSlice.ts'
import ForceLoginModal from './domain/user/components/ForceLoginModal.tsx'
import './base/styles/github-markdown.css'
import './base/styles/github-markdown-light.css'

function App() {
  const { whoAmIHandle } = useLogin()
  const app = useApp()
  const dispatch = useDispatch()

  useEffect(() => {
    void whoAmIHandle()
  }, [whoAmIHandle])

  useEffect(() => {
    const handleAuthRequired = () => {
      dispatch(setLoginModalOpen(true))
    }

    window.addEventListener('auth:required', handleAuthRequired)
    return () => {
      window.removeEventListener('auth:required', handleAuthRequired)
    }
  }, [dispatch])

  return (
    <ErrorBoundary FallbackComponent={ErrorFallback}>
      <Routes>
        {AdminRouteConfig}
        {UserRouteConfig}
      </Routes>
      {app.isLoaded ? <ForceLoginModal /> : null}
    </ErrorBoundary>
  )
}

export default App
