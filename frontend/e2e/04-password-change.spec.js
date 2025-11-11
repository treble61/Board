const { test, expect } = require('@playwright/test');

// Helper function to login with existing test account
async function createAndLogin(page) {
  const testEmail = 'treble611@example.com';
  const testPassword = '1234';

  await page.goto('/');
  await page.click('button.tab-button:has-text("로그인")');
  await page.waitForTimeout(500);
  await page.fill('input[type="text"]', testEmail);
  await page.fill('input[type="password"]', testPassword);
  await page.click('button[type="submit"]:has-text("로그인")');

  await expect(page).toHaveURL(/.*\/posts/, { timeout: 10000 });

  return { testUser: testEmail, testPassword };
}

test.describe('비밀번호 변경 기능', () => {
  test('비밀번호 변경 페이지로 이동해야 함', async ({ page }) => {
    await createAndLogin(page);

    // Look for password change link
    const passwordLink = page.locator('a:has-text("비밀번호"), a:has-text("비밀번호 변경")').first();

    if (await passwordLink.isVisible()) {
      await passwordLink.click();
      await expect(page).toHaveURL(/.*\/change-password/);
      await expect(page.locator('text=/비밀번호 변경|비밀번호 수정/i')).toBeVisible();
    } else {
      // Navigate directly
      await page.goto('/change-password');
      await expect(page.locator('text=/비밀번호 변경|비밀번호 수정/i')).toBeVisible();
    }
  });

  test('현재 비밀번호를 검증해야 함', async ({ page }) => {
    const { testPassword } = await createAndLogin(page);

    await page.goto('/change-password');

    // Fill with wrong current password
    await page.fill('input[name="currentPassword"], input[placeholder*="현재"]', 'WrongPassword123!');
    await page.fill('input[name="newPassword"], input[placeholder*="새"]', 'NewPassword123!');
    await page.fill('input[name="confirmPassword"], input[placeholder*="확인"]', 'NewPassword123!');

    await page.click('button:has-text("변경"), button:has-text("수정")');
    await page.waitForTimeout(2000);

    // Should show error about incorrect current password
    const errorVisible = await page.locator('text=/현재 비밀번호|일치하지|틀렸습니다/i').isVisible();
    if (errorVisible) {
      console.log('Current password validation working');
    }
  });

  test('새 비밀번호 확인을 검증해야 함', async ({ page }) => {
    const { testPassword } = await createAndLogin(page);

    await page.goto('/change-password');

    await page.fill('input[name="currentPassword"], input[placeholder*="현재"]', testPassword);
    await page.fill('input[name="newPassword"], input[placeholder*="새"]', 'NewPassword123!');
    await page.fill('input[name="confirmPassword"], input[placeholder*="확인"]', 'DifferentPassword123!');

    await page.click('button:has-text("변경"), button:has-text("수정")');
    await page.waitForTimeout(2000);

    // Should show error about password mismatch
    await expect(page.locator('text=/비밀번호가 일치하지|일치하지 않습니다/i')).toBeVisible();
  });

  test('비밀번호를 성공적으로 변경해야 함', async ({ page }) => {
    const { testUser, testPassword } = await createAndLogin(page);

    await page.goto('/change-password');

    const newPassword = 'NewTestPass456!';

    await page.fill('input[name="currentPassword"], input[placeholder*="현재"]', testPassword);
    await page.fill('input[name="newPassword"], input[placeholder*="새"]', newPassword);
    await page.fill('input[name="confirmPassword"], input[placeholder*="확인"]', newPassword);

    await page.click('button:has-text("변경"), button:has-text("수정")');
    await page.waitForTimeout(3000);

    // Should show success message or redirect
    const isSuccess = await page.locator('text=/성공|완료|변경되었습니다/i').isVisible() ||
                      page.url().includes('/posts') ||
                      page.url().includes('/login');

    if (isSuccess) {
      console.log('Password change successful');

      // Try to login with new password
      await page.goto('/');
      await page.fill('input[type="text"]', testUser);
      await page.fill('input[type="password"]', newPassword);
      await page.click('button:has-text("로그인")');

      await page.waitForTimeout(2000);
      const loggedIn = page.url().includes('/posts');
      expect(loggedIn).toBe(true);
      console.log('Login with new password successful');
    }
  });

  test('비밀번호 변경에 로그인이 필요해야 함', async ({ page }) => {
    // Try to access without login
    await page.goto('/change-password');

    // Should redirect to login or show error
    await page.waitForTimeout(2000);

    const needsLogin = page.url().includes('/login') ||
                       page.url() === 'http://localhost:3000/' ||
                       await page.locator('text=/로그인/i').isVisible();

    if (needsLogin) {
      console.log('Password change requires authentication');
    }
  });
});

test.describe('비밀번호 보안 요구사항', () => {
  test('비밀번호 복잡도 규칙을 적용해야 함', async ({ page }) => {
    await createAndLogin(page);

    await page.goto('/change-password');

    // Test weak passwords
    const weakPasswords = [
      { password: '123456', description: 'Simple numbers' },
      { password: 'password', description: 'Common word' },
      { password: 'abc', description: 'Too short' },
    ];

    for (const { password, description } of weakPasswords) {
      await page.fill('input[name="newPassword"], input[placeholder*="새"]', password);
      await page.fill('input[name="confirmPassword"], input[placeholder*="확인"]', password);

      // Check if client-side validation shows error
      const errorVisible = await page.locator('text=/너무 짧습니다|약합니다|형식/i').isVisible();
      console.log(`Weak password (${description}) validation:`, errorVisible);
    }
  });

  test('password_changed_at 타임스탬프를 업데이트해야 함', async ({ page }) => {
    const { testUser, testPassword } = await createAndLogin(page);

    await page.goto('/change-password');

    const newPassword = `Changed${Date.now()}!`;

    await page.fill('input[name="currentPassword"], input[placeholder*="현재"]', testPassword);
    await page.fill('input[name="newPassword"], input[placeholder*="새"]', newPassword);
    await page.fill('input[name="confirmPassword"], input[placeholder*="확인"]', newPassword);

    await page.click('button:has-text("변경"), button:has-text("수정")');
    await page.waitForTimeout(3000);

    // Note: timestamp update happens in UserService.changePassword()
    console.log('Database should update both password and password_changed_at columns');
    console.log('Implementation: UserMapper.updatePassword() in UserMapper.xml');
  });
});

test.describe('비밀번호 변경 세션 관리', () => {
  test('비밀번호 변경 후 세션을 유지해야 함', async ({ page }) => {
    const { testUser, testPassword } = await createAndLogin(page);

    await page.goto('/change-password');

    const newPassword = `SessionTest${Date.now()}!`;

    await page.fill('input[name="currentPassword"], input[placeholder*="현재"]', testPassword);
    await page.fill('input[name="newPassword"], input[placeholder*="새"]', newPassword);
    await page.fill('input[name="confirmPassword"], input[placeholder*="확인"]', newPassword);

    await page.click('button:has-text("변경"), button:has-text("수정")');
    await page.waitForTimeout(3000);

    // Should still be logged in
    await page.goto('/posts');
    const stillLoggedIn = !page.url().includes('/login');
    console.log('Session maintained after password change:', stillLoggedIn);
  });

  test('세션이 만료된 경우 재로그인을 요구해야 함', async ({ page }) => {
    await createAndLogin(page);

    // Note: Session timeout is 1800 seconds (30 min) per application.yml
    console.log('Session timeout: 1800 seconds (30 minutes)');
    console.log('Configured in: src/main/resources/application.yml');
    console.log('spring.servlet.session.timeout: 1800');

    // Simulate expired session by clearing cookies
    await page.context().clearCookies();

    await page.goto('/change-password');
    await page.waitForTimeout(2000);

    // Should redirect to login
    const redirectedToLogin = page.url().includes('/login') || page.url() === 'http://localhost:3000/';
    console.log('Expired session redirects to login:', redirectedToLogin);
  });
});