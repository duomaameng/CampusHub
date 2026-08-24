import { describe, expect, it } from 'vitest'
import type { OrderItem } from '@/types'
import { compareOrdersByStatus } from './orderStatus'

function order(id: number, status: OrderItem['status'], createdAt: string): Pick<OrderItem, 'id' | 'status' | 'createdAt'> {
  return { id, status, createdAt }
}

describe('compareOrdersByStatus', () => {
  it('keeps active orders before reopened, completed and cancelled orders', () => {
    const records = [
      order(1, 'CANCELLED', '2026-06-01T10:00:00.000Z'),
      order(2, 'COMPLETED', '2026-06-04T10:00:00.000Z'),
      order(3, 'PENDING_CONFIRM', '2026-06-05T10:00:00.000Z'),
      order(4, 'IN_PROGRESS', '2026-06-02T10:00:00.000Z')
    ].sort(compareOrdersByStatus)

    expect(records.map((item) => item.status)).toEqual([
      'IN_PROGRESS',
      'PENDING_CONFIRM',
      'COMPLETED',
      'CANCELLED'
    ])
  })

  it('sorts orders with the same rank by latest creation time first', () => {
    const records = [
      order(1, 'IN_PROGRESS', '2026-06-01T10:00:00.000Z'),
      order(2, 'PENDING_COMPLETION', '2026-06-05T10:00:00.000Z')
    ].sort(compareOrdersByStatus)

    expect(records.map((item) => item.id)).toEqual([2, 1])
  })
})
