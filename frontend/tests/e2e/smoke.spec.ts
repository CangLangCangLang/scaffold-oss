import { expect, test } from '@playwright/test'

test('应用加载并展示登录页', async ({ page }) => {
  await page.goto('/')
  await expect(page).toHaveTitle(/Fullstack Scaffold|脚手架|Scaffold/i)
  // 路由守卫会把未登录用户带到 /login
  await expect(page.locator('input[type="text"], input[placeholder]')).toBeVisible({ timeout: 10_000 })
})
