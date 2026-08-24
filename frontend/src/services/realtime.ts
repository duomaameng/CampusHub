export type RealtimeEventType =
  | 'TASKS_CHANGED'
  | 'APPLICATIONS_CHANGED'
  | 'ORDERS_CHANGED'
  | 'MESSAGES_CHANGED'
  | 'NOTIFICATIONS_CHANGED'
  | 'ANNOUNCEMENTS_CHANGED'
  | 'ADMIN_CHANGED'
  | 'PROFILE_CHANGED'

export interface RealtimeEvent {
  type: RealtimeEventType
  entityId: number | null
}

type RealtimeListener = (event: RealtimeEvent) => void

const listeners = new Set<RealtimeListener>()
let socket: WebSocket | null = null
let activeToken: string | null = null
let reconnectTimer: number | undefined
let reconnectAttempt = 0

function buildWebSocketUrl() {
  const apiBase = import.meta.env.VITE_API_BASE_URL || '/api'
  const url = new URL(apiBase, window.location.origin)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  url.pathname = `${url.pathname.replace(/\/$/, '')}/ws/updates`
  url.search = ''
  return url.toString()
}

function scheduleReconnect() {
  if (activeToken === null || reconnectTimer !== undefined) return
  const delay = Math.min(1000 * 2 ** reconnectAttempt, 15000)
  reconnectAttempt += 1
  reconnectTimer = window.setTimeout(() => {
    reconnectTimer = undefined
    openSocket()
  }, delay)
}

function openSocket() {
  if (activeToken === null || socket?.readyState === WebSocket.OPEN || socket?.readyState === WebSocket.CONNECTING) return

  const protocols = activeToken ? ['campushub', activeToken] : ['campushub']
  const currentSocket = new WebSocket(buildWebSocketUrl(), protocols)
  socket = currentSocket

  currentSocket.addEventListener('open', () => {
    reconnectAttempt = 0
  })
  currentSocket.addEventListener('message', (message) => {
    try {
      const event = JSON.parse(message.data) as RealtimeEvent
      if (event?.type) listeners.forEach((listener) => listener(event))
    } catch {
      // Ignore malformed server messages and keep the realtime connection alive.
    }
  })
  currentSocket.addEventListener('close', () => {
    if (socket === currentSocket) socket = null
    scheduleReconnect()
  })
}

export function connectRealtime(token: string) {
  if (token === activeToken && socket) return
  disconnectRealtime()
  activeToken = token
  openSocket()
}

export function disconnectRealtime() {
  activeToken = null
  reconnectAttempt = 0
  if (reconnectTimer !== undefined) {
    window.clearTimeout(reconnectTimer)
    reconnectTimer = undefined
  }
  const currentSocket = socket
  socket = null
  currentSocket?.close()
}

export function subscribeRealtime(listener: RealtimeListener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}
