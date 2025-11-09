-- ================================================
-- 이메일 인증 기능 추가 마이그레이션
-- 버전: V2
-- 작성일: 2025-11-10
-- ================================================

-- Step 1: users 테이블에 이메일 인증 관련 컬럼 추가
ALTER TABLE users
ADD COLUMN email_verified BOOLEAN DEFAULT FALSE,
ADD COLUMN email_verified_at TIMESTAMP NULL;

-- Step 2: 기존 사용자는 모두 인증 완료 처리 (기존 사용자는 신뢰할 수 있다고 가정)
UPDATE users
SET email_verified = TRUE,
    email_verified_at = created_at
WHERE email_verified IS FALSE OR email_verified IS NULL;

-- Step 3: email_verification_tokens 테이블 생성
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    token_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Step 4: 인덱스 생성 (성능 최적화)
CREATE INDEX idx_evt_token ON email_verification_tokens(token);
CREATE INDEX idx_evt_user_id ON email_verification_tokens(user_id);
CREATE INDEX idx_evt_expires_at ON email_verification_tokens(expires_at);

-- Step 5: 인증 완료된 토큰 조회 최적화를 위한 복합 인덱스
CREATE INDEX idx_evt_user_verified ON email_verification_tokens(user_id, verified_at);
