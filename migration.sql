-- users 테이블에 password_changed_at 컬럼 추가
-- 이 스크립트는 기존 데이터베이스에 password_changed_at 컬럼이 없을 때 실행하세요

-- 컬럼 추가 (이미 존재하면 에러 발생 가능)
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 기존 사용자들의 password_changed_at을 created_at으로 설정
UPDATE users
SET password_changed_at = created_at
WHERE password_changed_at IS NULL;
