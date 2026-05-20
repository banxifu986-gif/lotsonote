import { getApiBaseUrl } from '../../../base/utils/apiBaseUrl.ts'

export function resolveAvatarUrl(avatarUrl: string) {
  if (!avatarUrl) {
    return avatarUrl
  }

  const apiBaseUrl = getApiBaseUrl()
  const normalizedBaseUrl = apiBaseUrl.endsWith('/')
    ? apiBaseUrl
    : `${apiBaseUrl}/`

  if (/^https?:\/\//i.test(avatarUrl)) {
    try {
      const currentApiOrigin = new URL(normalizedBaseUrl).origin
      const avatarUrlObject = new URL(avatarUrl)
      if (avatarUrlObject.pathname.startsWith('/images/')) {
        return new URL(
          avatarUrlObject.pathname.slice(1),
          normalizedBaseUrl,
        ).toString()
      }
      if (avatarUrlObject.origin === currentApiOrigin) {
        return avatarUrl
      }
    } catch {
      return avatarUrl
    }

    return avatarUrl
  }

  return new URL(
    avatarUrl.startsWith('/') ? avatarUrl.slice(1) : avatarUrl,
    normalizedBaseUrl,
  ).toString()
}
