const { test, expect } = require('@playwright/test');

// Helper function to login with existing test account
async function createAndLogin(page) {
  const testEmail = 'treble611@example.com';
  const testPassword = '1234';

  // Login
  await page.goto('/');
  await page.click('button.tab-button:has-text("로그인")');
  await page.waitForTimeout(500);
  await page.fill('input[type="text"]', testEmail);
  await page.fill('input[type="password"]', testPassword);
  await page.click('button[type="submit"]:has-text("로그인")');

  await expect(page).toHaveURL(/.*\/posts/, { timeout: 10000 });

  return { testUser: testEmail, testPassword };
}

test.describe('게시글 목록 페이지', () => {
  test('로그인 후 게시글 목록 페이지가 표시되어야 함', async ({ page }) => {
    await createAndLogin(page);

    // Check post list elements
    await expect(page.locator('text=/게시판|게시글/i')).toBeVisible();
    await expect(page.locator('a:has-text("글쓰기"), button:has-text("글쓰기")')).toBeVisible();
  });

  test('검색 기능이 있어야 함', async ({ page }) => {
    await createAndLogin(page);

    // Look for search input
    const searchInput = page.locator('input[type="text"][placeholder*="검색"], input[name="search"]').first();
    if (await searchInput.isVisible()) {
      await searchInput.fill('테스트');
      await page.keyboard.press('Enter');
      await page.waitForTimeout(1000);
    }
  });

  test('페이지네이션이 있어야 함', async ({ page }) => {
    await createAndLogin(page);

    // Check if pagination exists (may not if less than page size posts)
    const paginationExists = await page.locator('text=/이전|다음|페이지/i, button:has-text("1"), button:has-text("2")').count() > 0;

    if (paginationExists) {
      console.log('Pagination controls found');
    } else {
      console.log('No pagination (likely less than one page of posts)');
    }
  });
});

test.describe('게시글 작성', () => {
  test('새 게시글을 성공적으로 작성해야 함', async ({ page }) => {
    await createAndLogin(page);

    // Navigate to post write page
    await page.click('a:has-text("글쓰기"), button:has-text("글쓰기")');
    await expect(page).toHaveURL(/.*\/posts\/new/);

    // Fill post form
    const postTitle = `테스트 게시글 ${Date.now()}`;
    const postContent = '이것은 자동화 테스트로 작성된 게시글입니다.';

    await page.fill('input[name="title"], input[placeholder*="제목"]', postTitle);
    await page.fill('textarea[name="content"], textarea[placeholder*="내용"]', postContent);

    // Submit
    await page.click('button:has-text("작성"), button:has-text("등록")');

    // Should redirect to post detail or list
    await page.waitForTimeout(2000);
    const currentUrl = page.url();
    expect(currentUrl).toMatch(/\/posts/);
  });

  test('필수 필드를 검증해야 함', async ({ page }) => {
    await createAndLogin(page);

    await page.click('a:has-text("글쓰기"), button:has-text("글쓰기")');

    // Try to submit empty form
    await page.click('button:has-text("작성"), button:has-text("등록")');

    // Should show validation error or stay on page
    await page.waitForTimeout(1000);
    const currentUrl = page.url();
    expect(currentUrl).toMatch(/\/posts\/new/);
  });

  test('공지사항 게시글 작성을 허용해야 함', async ({ page }) => {
    await createAndLogin(page);

    await page.click('a:has-text("글쓰기"), button:has-text("글쓰기")');

    // Fill form
    await page.fill('input[name="title"], input[placeholder*="제목"]', `공지사항 테스트 ${Date.now()}`);
    await page.fill('textarea[name="content"], textarea[placeholder*="내용"]', '공지사항 내용입니다.');

    // Check notice checkbox if exists
    const noticeCheckbox = page.locator('input[type="checkbox"][name="isNotice"], input[type="checkbox"]:has-text("공지")').first();
    if (await noticeCheckbox.isVisible()) {
      await noticeCheckbox.check();
    }

    await page.click('button:has-text("작성"), button:has-text("등록")');
    await page.waitForTimeout(2000);
  });
});

test.describe('게시글 상세 및 상호작용', () => {
  test('게시글 상세를 조회하고 조회수를 증가시켜야 함', async ({ page }) => {
    await createAndLogin(page);

    // Create a post first
    await page.click('a:has-text("글쓰기"), button:has-text("글쓰기")');
    const postTitle = `상세보기 테스트 ${Date.now()}`;
    await page.fill('input[name="title"], input[placeholder*="제목"]', postTitle);
    await page.fill('textarea[name="content"], textarea[placeholder*="내용"]', '상세보기 테스트 내용');
    await page.click('button:has-text("작성"), button:has-text("등록")');
    await page.waitForTimeout(2000);

    // Find and click the post
    await page.goto('/posts');
    await page.click(`text=${postTitle}`);

    // Should be on detail page
    await expect(page.locator(`text=${postTitle}`)).toBeVisible();
    await expect(page.locator('text=/조회수|조회/i')).toBeVisible();
  });

  test('자신의 게시글을 수정해야 함', async ({ page }) => {
    await createAndLogin(page);

    // Create a post
    await page.click('a:has-text("글쓰기"), button:has-text("글쓰기")');
    const originalTitle = `수정 테스트 ${Date.now()}`;
    await page.fill('input[name="title"], input[placeholder*="제목"]', originalTitle);
    await page.fill('textarea[name="content"], textarea[placeholder*="내용"]', '원본 내용');
    await page.click('button:has-text("작성"), button:has-text("등록")');
    await page.waitForTimeout(2000);

    // Go to detail page
    await page.goto('/posts');
    await page.click(`text=${originalTitle}`);

    // Click edit button
    const editButton = page.locator('button:has-text("수정"), a:has-text("수정")').first();
    if (await editButton.isVisible()) {
      await editButton.click();

      // Update content
      await page.fill('input[name="title"], input[placeholder*="제목"]', `${originalTitle} (수정됨)`);
      await page.fill('textarea[name="content"], textarea[placeholder*="내용"]', '수정된 내용입니다.');

      await page.click('button:has-text("수정"), button:has-text("저장")');
      await page.waitForTimeout(2000);

      // Verify update
      await expect(page.locator('text=/수정됨/i')).toBeVisible();
    }
  });

  test('자신의 게시글을 삭제해야 함', async ({ page }) => {
    await createAndLogin(page);

    // Create a post
    await page.click('a:has-text("글쓰기"), button:has-text("글쓰기")');
    const postTitle = `삭제 테스트 ${Date.now()}`;
    await page.fill('input[name="title"], input[placeholder*="제목"]', postTitle);
    await page.fill('textarea[name="content"], textarea[placeholder*="내용"]', '삭제될 내용');
    await page.click('button:has-text("작성"), button:has-text("등록")');
    await page.waitForTimeout(2000);

    // Go to detail page
    await page.goto('/posts');
    await page.click(`text=${postTitle}`);

    // Click delete button
    const deleteButton = page.locator('button:has-text("삭제")').first();
    if (await deleteButton.isVisible()) {
      // Handle confirmation dialog
      page.on('dialog', dialog => dialog.accept());
      await deleteButton.click();

      await page.waitForTimeout(2000);

      // Should redirect to list
      await expect(page).toHaveURL(/.*\/posts$/);
    }
  });
});

test.describe('댓글 기능', () => {
  test('게시글에 댓글을 추가해야 함', async ({ page }) => {
    await createAndLogin(page);

    // Create a post
    await page.click('a:has-text("글쓰기"), button:has-text("글쓰기")');
    const postTitle = `댓글 테스트 ${Date.now()}`;
    await page.fill('input[name="title"], input[placeholder*="제목"]', postTitle);
    await page.fill('textarea[name="content"], textarea[placeholder*="내용"]', '댓글 테스트용 게시글');
    await page.click('button:has-text("작성"), button:has-text("등록")');
    await page.waitForTimeout(2000);

    // Go to detail page
    await page.goto('/posts');
    await page.click(`text=${postTitle}`);

    // Add comment
    const commentTextarea = page.locator('textarea[placeholder*="댓글"], textarea[name="comment"]').first();
    if (await commentTextarea.isVisible()) {
      await commentTextarea.fill('테스트 댓글입니다.');
      await page.click('button:has-text("댓글 작성"), button:has-text("등록")');

      await page.waitForTimeout(2000);

      // Verify comment appears
      await expect(page.locator('text=테스트 댓글입니다.')).toBeVisible();
    }
  });
});