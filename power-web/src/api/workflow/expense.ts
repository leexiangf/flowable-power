import type { ApiResult } from '@/types/api'
import type { ProcessInstanceVO } from '@/types/workflow'
import { request, unwrap } from '../request'

export interface ExpenseStartRequest {
  managerUserId: string
  countersignUserIds: string[]
  title?: string
  amount?: string
  reason?: string
}

export function startExpense(data: ExpenseStartRequest) {
  return unwrap(request.post<ApiResult<ProcessInstanceVO>>('/workflow/expense', data))
}
