import type { OrderItem, OrderStatus } from '@/types'

export const orderStatusRank: Record<OrderStatus, number> = {
  IN_PROGRESS: 0,
  PENDING_COMPLETION: 0,
  PENDING_CONFIRM: 1,
  DISPUTE: 1,
  COMPLETED: 2,
  REVIEWED: 2,
  CANCELLED: 3
}

export function compareOrdersByStatus(a: Pick<OrderItem, 'status' | 'createdAt'>, b: Pick<OrderItem, 'status' | 'createdAt'>) {
  const rankDiff = orderStatusRank[a.status] - orderStatusRank[b.status]
  if (rankDiff !== 0) return rankDiff
  return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
}
