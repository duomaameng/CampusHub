import { describe, expect, it } from 'vitest'
import { resolveAssetUrl } from './assets'

describe('resolveAssetUrl', () => {
  it('keeps empty, absolute, blob and data urls unchanged for upload preview regressions', () => {
    expect(resolveAssetUrl('')).toBe('')
    expect(resolveAssetUrl(null)).toBe('')
    expect(resolveAssetUrl('https://example.com/a.png')).toBe('https://example.com/a.png')
    expect(resolveAssetUrl('//cdn.example.com/a.png')).toBe('//cdn.example.com/a.png')
    expect(resolveAssetUrl('blob:http://localhost/image')).toBe('blob:http://localhost/image')
    expect(resolveAssetUrl('data:image/png;base64,abc')).toBe('data:image/png;base64,abc')
  })
})
