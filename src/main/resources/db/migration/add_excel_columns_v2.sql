-- posts 테이블에 엑셀 파일 관련 컬럼 추가

ALTER TABLE posts
ADD COLUMN excel_filename VARCHAR(255) DEFAULT NULL;

ALTER TABLE posts
ADD COLUMN excel_stored_filename VARCHAR(255) DEFAULT NULL;

ALTER TABLE posts
ADD COLUMN excel_file_path VARCHAR(500) DEFAULT NULL;

ALTER TABLE posts
ADD COLUMN excel_file_size BIGINT DEFAULT NULL;
