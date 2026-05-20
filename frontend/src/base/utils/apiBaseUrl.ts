import { kamanoteHost, legacyKamanoteHost } from '../constants.ts'

function normalizeBaseUrl(baseUrl: string) {
  return baseUrl.trim().replace(/\/+$/, '')
}

export function getApiBaseUrl() {
  const configuredHost = localStorage.getItem(kamanoteHost)
  if (configuredHost && configuredHost.trim()) {
    return normalizeBaseUrl(configuredHost)
  }

  const envBaseUrl = import.meta.env.VITE_API_BASE_URL
  if (envBaseUrl && envBaseUrl.trim()) {
    return normalizeBaseUrl(envBaseUrl)
  }

  const legacyHost = localStorage.getItem(legacyKamanoteHost)
  if (legacyHost && legacyHost.trim()) {
    return normalizeBaseUrl(legacyHost)
  }

  return window.location.origin
}

export function setApiBaseUrl(baseUrl: string) {
  const normalizedBaseUrl = normalizeBaseUrl(baseUrl)
  localStorage.setItem(kamanoteHost, normalizedBaseUrl)
  localStorage.removeItem(legacyKamanoteHost)
  return normalizedBaseUrl
}
