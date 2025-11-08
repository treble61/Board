# 게시판 시스템

Figma 디자인을 기반으로 구현한 Spring Boot + React 게시판 애플리케이션입니다.

## 기술 스택

### 백엔드
- Java 8
- Spring Boot 2.1.18
- MyBatis
- MariaDB
- Gradle

### 프론트엔드
- React 17
- React Router DOM 5
- Axios

## 주요 기능

1. **회원 관리**
   - 회원가입 (아이디, 비밀번호, 이름, 이메일)
   - 로그인 (SHA-256 암호화)
   - 로그아웃

2. **게시판 기능**
   - 게시글 목록 조회 (공지사항 상단 고정)
   - 게시글 상세보기 (조회수 자동 증가)
   - 게시글 작성
   - 게시글 수정 (작성자만 가능)
   - 게시글 삭제 (작성자만 가능)
   - 공지사항 등록

## 프로젝트 구조

```
boards/
├── src/main/
│   ├── java/com/example/boards/
│   │   ├── BoardsApplication.java
│   │   ├── config/
│   │   │   └── DatabaseInitializer.java
│   │   ├── controller/
│   │   │   ├── UserController.java
│   │   │   └── PostController.java
│   │   ├── service/
│   │   │   ├── UserService.java
│   │   │   └── PostService.java
│   │   ├── mapper/
│   │   │   ├── UserMapper.java
│   │   │   └── PostMapper.java
│   │   ├── model/
│   │   │   ├── User.java
│   │   │   └── Post.java
│   │   ├── dto/
│   │   │   ├── LoginRequest.java
│   │   │   └── SignupRequest.java
│   │   └── util/
│   │       └── PasswordEncoder.java
│   └── resources/
│       ├── application.yml
│       ├── schema.sql
│       └── mapper/
│           ├── UserMapper.xml
│           └── PostMapper.xml
└── frontend/
    ├── package.json
    ├── public/
    │   └── index.html
    └── src/
        ├── App.js
        ├── index.js
        ├── index.css
        └── components/
            ├── Login.js
            ├── Signup.js
            ├── PostList.js
            ├── PostDetail.js
            ├── PostWrite.js
            └── PostEdit.js
```

## 설치 및 실행

### 사전 요구사항
- Java 8 이상
- Node.js 12 이상
- MariaDB 10 이상

### 1. 데이터베이스 설정

MariaDB를 설치하고 3307 포트로 실행합니다.

```bash
# MariaDB 접속
mysql -u root -p -P 3307

# 데이터베이스는 자동 생성됩니다 (createDatabaseIfNotExist=true)
```

### 2. 백엔드 실행

```bash
# Gradle 빌드 및 실행
./gradlew bootRun

# 또는 Windows에서
gradlew.bat bootRun
```

백엔드는 http://localhost:8080 에서 실행됩니다.

### 3. 프론트엔드 실행

```bash
# frontend 디렉토리로 이동
cd frontend

# 의존성 설치
npm install

# 개발 서버 실행
npm start
```

프론트엔드는 http://localhost:3000 에서 실행됩니다.

## API 엔드포인트

### 사용자 API
- POST `/api/users/signup` - 회원가입
- POST `/api/users/login` - 로그인
- POST `/api/users/logout` - 로그아웃
- GET `/api/users/me` - 현재 사용자 정보

### 게시글 API
- GET `/api/posts` - 게시글 목록 조회
- GET `/api/posts/{id}` - 게시글 상세 조회
- POST `/api/posts` - 게시글 작성
- PUT `/api/posts/{id}` - 게시글 수정
- DELETE `/api/posts/{id}` - 게시글 삭제

## 데이터베이스 스키마

### users 테이블
- `user_id` VARCHAR(50) PRIMARY KEY
- `password` VARCHAR(64) NOT NULL
- `name` VARCHAR(50) NOT NULL
- `email` VARCHAR(100) NOT NULL
- `created_at` TIMESTAMP

### posts 테이블
- `post_id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `title` VARCHAR(200) NOT NULL
- `content` TEXT NOT NULL
- `author_id` VARCHAR(50) NOT NULL
- `is_notice` BOOLEAN DEFAULT FALSE
- `view_count` INT DEFAULT 0
- `created_at` TIMESTAMP
- `updated_at` TIMESTAMP

## 보안 기능

- 비밀번호는 SHA-256으로 암호화하여 저장
- 세션 기반 인증
- CORS 설정으로 프론트엔드와 백엔드 분리

## 참고사항

- 데이터베이스 테이블은 애플리케이션 실행 시 자동으로 생성됩니다
- 회원가입 시 비밀번호 확인 기능 제공
- 공지사항은 게시글 목록 상단에 고정됩니다
- 게시글 조회 시 조회수가 자동으로 증가합니다
- 작성자만 자신의 게시글을 수정/삭제할 수 있습니다
