import type { App, Directive } from 'vue'
import { hasPerm } from '@/utils/permission'

const permDirective: Directive<HTMLElement, string> = {
  mounted(el, binding) {
    if (!hasPerm(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  },
}

export function setupPermissionDirective(app: App) {
  app.directive('perm', permDirective)
}
