import type { ApiResult } from '@/types/api'
import type { WorkflowEngineInfoVO } from '@/types/workflow'
import { request, unwrap } from '../request'

export function fetchEngineInfo() {
  return unwrap(request.get<ApiResult<WorkflowEngineInfoVO>>('/workflow/engine/info'))
}
