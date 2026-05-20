import { createSlice, PayloadAction } from '@reduxjs/toolkit'

type AppState = {
  isLogin: boolean
  isLoaded: boolean
  isAdminApp: boolean
  loginModalOpen: boolean
}

const initialAppState: AppState = {
  isLogin: false,
  isLoaded: false,
  isAdminApp: false,
  loginModalOpen: false,
}

const appSlice = createSlice({
  name: 'app',
  initialState: initialAppState,
  reducers: {
    login: (state) => {
      state.isLogin = true
    },
    logout: (state) => {
      state.isLogin = false
    },
    setLoginModalOpen: (state, action: PayloadAction<boolean>) => {
      state.loginModalOpen = action.payload
    },
    loaded: (state) => {
      state.isLoaded = true
    },
    intoAdminApp: (state) => {
      state.isAdminApp = true
    },
    outAdminApp: (state) => {
      state.isAdminApp = false
    },
  },
})

export const {
  login,
  logout,
  setLoginModalOpen,
  loaded,
  intoAdminApp,
  outAdminApp,
} = appSlice.actions
export default appSlice.reducer
