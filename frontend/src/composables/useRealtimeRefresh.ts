import { onBeforeUnmount, onMounted } from 'vue'

import { subscribeRealtime, type RealtimeEvent, type RealtimeEventType } from '@/services/realtime'

export function useRealtimeRefresh(
  eventTypes: RealtimeEventType[],
  refresh: () => void | Promise<void>,
  matches: (event: RealtimeEvent) => boolean = () => true
) {
  let unsubscribe: (() => void) | undefined
  let refreshTimer: number | undefined
  let pendingWhileHidden = false

  function queueRefresh(event?: RealtimeEvent) {
    if (event && (!eventTypes.includes(event.type) || !matches(event))) return
    if (document.hidden) {
      pendingWhileHidden = true
      return
    }
    if (refreshTimer !== undefined) window.clearTimeout(refreshTimer)
    refreshTimer = window.setTimeout(() => {
      refreshTimer = undefined
      void refresh()
    }, 120)
  }

  function handleVisibilityChange() {
    if (!document.hidden && pendingWhileHidden) {
      pendingWhileHidden = false
      queueRefresh()
    }
  }

  function handleWindowFocus() {
    queueRefresh()
  }

  onMounted(() => {
    unsubscribe = subscribeRealtime(queueRefresh)
    document.addEventListener('visibilitychange', handleVisibilityChange)
    window.addEventListener('focus', handleWindowFocus)
  })

  onBeforeUnmount(() => {
    unsubscribe?.()
    document.removeEventListener('visibilitychange', handleVisibilityChange)
    window.removeEventListener('focus', handleWindowFocus)
    if (refreshTimer !== undefined) window.clearTimeout(refreshTimer)
  })
}
