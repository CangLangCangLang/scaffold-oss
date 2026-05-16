import type { App, Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

const ALL_PERMISSION = '*:*:*'
const SUPER_ADMIN = 'admin'

function checkPermission(value: unknown): boolean {
  const store = useUserStore()
  const permissions = store.permissions || []
  const required = Array.isArray(value) ? (value as string[]) : []
  if (!required.length) return false
  return required.some((perm) => permissions.includes(ALL_PERMISSION) || permissions.includes(perm))
}

function checkRole(value: unknown): boolean {
  const store = useUserStore()
  const roles = store.roles || []
  const required = Array.isArray(value) ? (value as string[]) : []
  if (!required.length) return false
  return required.some((role) => roles.includes(SUPER_ADMIN) || roles.includes(role))
}

const hasPermi: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    if (!checkPermission(binding.value)) el.parentNode?.removeChild(el)
  }
}

const hasRole: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    if (!checkRole(binding.value)) el.parentNode?.removeChild(el)
  }
}

export function setupPermissionDirectives(app: App): void {
  app.directive('hasPermi', hasPermi)
  app.directive('hasRole', hasRole)
}
