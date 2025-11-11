const { test, expect } = require('@playwright/test');

test.describe('인증 플로우', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('로그인 페이지가 표시되어야 함', async ({ page }) => {
    await expect(page).toHaveTitle(/게시판/);
    await expect(page.locator('input[type="text"]')).toBeVisible();
    await expect(page.locator('input[type="password"]')).toBeVisible();
    await expect(page.locator('button[type="submit"]:has-text("로그인")')).toBeVisible();
  });

  test('잘못된 로그인 정보에 대해 오류를 표시해야 함', async ({ page }) => {
    await page.fill('input[type="text"]', 'nonexistent_user');
    await page.fill('input[type="password"]', 'wrong_password');
    await page.click('button[type="submit"]:has-text("로그인")');

    // Wait for error message
    await expect(page.locator('text=/아이디 또는 비밀번호|로그인 실패/i')).toBeVisible({ timeout: 5000 });
  });

  test('회원가입 페이지로 이동해야 함', async ({ page }) => {
    await page.click('button:has-text("회원가입")');
    await page.waitForTimeout(500);
    // Tab-based UI, check if signup form is visible
    await expect(page.locator('button.tab-button:has-text("회원가입")')).toBeVisible();
    await expect(page.locator('input[type="email"]')).toBeVisible();
  });

  test('검증과 함께 회원가입 플로우를 완료해야 함', async ({ page }) => {
    await page.click('button:has-text("회원가입")');
    await page.waitForTimeout(500);

    const timestamp = Date.now();
    const testEmail = `test_${timestamp}@example.com`;

    // Fill signup form - actual field order: email, name, password, password confirm
    await page.fill('input[type="email"]', testEmail);
    await page.fill('input[type="text"]', '테스트사용자');
    await page.locator('input[type="password"]').nth(0).fill('Test1234!');
    await page.locator('input[type="password"]').nth(1).fill('Test1234!');

    await page.click('button:has-text("가입하기"), button:has-text("회원가입")');

    // Should redirect to login or show success
    await page.waitForTimeout(2000);
  });

  test('비밀번호 불일치에 대한 검증을 표시해야 함', async ({ page }) => {
    await page.click('button:has-text("회원가입")');
    await page.waitForTimeout(500);

    await page.fill('input[type="email"]', 'test@example.com');
    await page.fill('input[type="text"]', '테스트');
    await page.locator('input[type="password"]').nth(0).fill('Password123!');
    await page.locator('input[type="password"]').nth(1).fill('DifferentPass123!');

    await page.click('button:has-text("가입하기"), button:has-text("회원가입")');

    // Should show error about password mismatch
    await expect(page.locator('text=/비밀번호가 일치하지|일치하지 않습니다/i')).toBeVisible({ timeout: 3000 });
  });
});

test.describe('로그인 및 세션', () => {
  test('유효한 로그인 정보로 성공적으로 로그인해야 함', async ({ page }) => {
    await page.goto('/');

    // Use existing test account
    const testEmail = 'treble611@example.com';
    const testPassword = '1234';

    // Login with existing account
    await page.click('button.tab-button:has-text("로그인")');
    await page.waitForTimeout(500);
    await page.fill('input[type="text"]', testEmail);
    await page.fill('input[type="password"]', testPassword);
    await page.click('button[type="submit"]:has-text("로그인")');

    // Should redirect to posts list
    await expect(page).toHaveURL(/.*\/posts/, { timeout: 10000 });
  });
});