import { reactive } from 'vue'

export type ConfirmDialogOptions = {
  title: string
  description: string
  confirmText?: string
  cancelText?: string
}

export function useConfirmDialog() {
  const state = reactive({
    open: false,
    loading: false,
    title: '',
    description: '',
    confirmText: '确认',
    cancelText: '取消'
  })
  let action: (() => Promise<void> | void) | null = null

  function request(options: ConfirmDialogOptions, callback: () => Promise<void> | void) {
    state.title = options.title
    state.description = options.description
    state.confirmText = options.confirmText || '确认'
    state.cancelText = options.cancelText || '取消'
    state.open = true
    action = callback
  }

  function cancel() {
    if (state.loading) return
    state.open = false
    action = null
  }

  async function confirm() {
    if (!action || state.loading) return
    state.loading = true
    try {
      await action()
      state.open = false
      action = null
    } catch {
      // The page-level action keeps responsibility for presenting its error.
    } finally {
      state.loading = false
    }
  }

  return { state, request, cancel, confirm }
}
