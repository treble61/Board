-- posts 테이블에 엑셀 파일 관련 컬럼 추가

ALTER TABLE posts
ADD COLUMN excel_filename VARCHAR(255) DEFAULT NULL,
ADD COLUMN excel_stored_filename VARCHAR(255) DEFAULT NULL,
ADD COLUMN excel_file_path VARCHAR(500) DEFAULT NULL,
ADD COLUMN excel_file_size BIGINT DEFAULT NULL;

-- 인덱스 추가 (선택적)
-- CREATE INDEX idx_posts_excel_filename ON posts(excel_filename);
