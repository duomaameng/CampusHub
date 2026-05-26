const explicitAssetBaseUrl = (import.meta.env.VITE_ASSET_BASE_URL || '').replace(/\/+$/, '')
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '')

function inferAssetBaseUrl() {
  if (explicitAssetBaseUrl) return explicitAssetBaseUrl
  if (/^https?:\/\//i.test(apiBaseUrl)) {
    return apiBaseUrl.replace(/\/api(?:\/.*)?$/i, '')
  }
  return ''
}

const assetBaseUrl = inferAssetBaseUrl()

export function resolveAssetUrl(url?: string | null) {
  const value = url?.trim()
  if (!value) return ''
  if (/^(https?:)?\/\//i.test(value) || /^(blob|data):/i.test(value)) return value
  if (value.startsWith('/')) return `${assetBaseUrl}${value}`
  return assetBaseUrl ? `${assetBaseUrl}/${value}` : value
}
