# 이메일 인증 기능 설계 문서

## 문서 정보
- **작성일**: 2025-11-10
- **버전**: 1.0
- **대상 시스템**: Spring Boot + React 게시판 시스템

---

## 목차
1. [현재 시스템 분석](#1-현재-시스템-분석)
2. [이메일 인증 아키텍처 설계](#2-이메일-인증-아키텍처-설계)
3. [데이터베이스 스키마 설계](#3-데이터베이스-스키마-설계)
4. [API 엔드포인트 설계](#4-api-엔드포인트-설계)
5. [이메일 서비스 설계](#5-이메일-서비스-설계)
6. [보안 고려사항](#6-보안-고려사항)
7. [구현 체크리스트](#7-구현-체크리스트)
8. [마이그레이션 전략](#8-마이그레이션-전략)
9. [테스트 계획](#9-테스트-계획)
10. [모니터링 및 메트릭](#10-모니터링-및-메트릭)

---

## 1. 현재 시스템 분석

### 1.1 현재 회원가입 플로우
```
사용자 → 회원가입 폼 작성 → POST /api/users/signup → 계정 즉시 활성화 → 로그인 가능
```

### 1.2 주요 컴포넌트
- **인증 방식**: 세션 기반 인증 (30분 타임아웃)
- **비밀번호 암호화**: SHA-256 해싱
- **Rate Limiting**: IP 기반 (회원가입 3회/시간)
- **사용자 테이블 구조**:
  - `user_id` (PK)
  - `password` (SHA-256)
  - `name`
  - `email`
  - `created_at`
  - `password_changed_at`

### 1.3 관련 파일
- Controller: `src/main/java/com/example/boards/controller/UserController.java`
- Service: `src/main/java/com/example/boards/service/UserService.java`
- Mapper: `src/main/resources/mapper/UserMapper.xml`
- Schema: `src/main/resources/schema.sql`

---

## 2. 이메일 인증 아키텍처 설계

### 2.1 새로운 회원가입 플로우
```
사용자 회원가입 폼 작성
  ↓
POST /api/users/signup (수정됨)
  ↓
사용자 생성 (email_verified=false)
  ↓
인증 토큰 생성 (UUID + 만료시간)
  ↓
인증 이메일 발송
  ↓
"이메일을 확인하세요" 메시지 반환
  ↓
사용자가 이메일 링크 클릭
  ↓
GET /api/users/verify-email?token=xxx
  ↓
토큰 검증 (존재 여부, 만료 여부, 사용 여부)
  ↓
이메일 인증 완료 표시
  ↓
로그인 가능
```

### 2.2 수정된 로그인 플로우
```
POST /api/users/login (수정됨)
  ↓
자격증명 확인
  ↓
email_verified = true 확인
  ↓
false인 경우 → "이메일 인증을 먼저 완료하세요" 반환
  ↓
true인 경우 → 세션 생성 + 로그인 성공
```

### 2.3 인증 이메일 재발송 플로우
```
사용자가 "인증 이메일 재발송" 요청
  ↓
POST /api/users/resend-verification
  ↓
이메일로 사용자 조회
  ↓
email_verified = false 확인 (이미 인증됨 → 에러)
  ↓
기존 토큰 무효화 (선택사항)
  ↓
새 토큰 생성
  ↓
새 인증 이메일 발송
  ↓
성공 메시지 반환
```

---

## 3. 데이터베이스 스키마 설계

### 3.1 users 테이블 수정

```sql
-- 이메일 인증 관련 컬럼 추가
ALTER TABLE users
ADD COLUMN email_verified BOOLEAN DEFAULT FALSE,
ADD COLUMN email_verified_at TIMESTAMP NULL;
```

**수정된 users 테이블 구조**:
```sql
CREATE TABLE users (
    user_id VARCHAR(50) PRIMARY KEY,
    password VARCHAR(64) NOT NULL,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    email_verified BOOLEAN DEFAULT FALSE,        -- 추가
    email_verified_at TIMESTAMP NULL,            -- 추가
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    password_changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 3.2 email_verification_tokens 테이블 생성

```sql
CREATE TABLE email_verification_tokens (
    token_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at)
);
```

**컬럼 설명**:
- `token_id`: 기본 키 (자동 증가)
- `user_id`: 사용자 ID (외래 키)
- `token`: 인증 토큰 (UUID v4, 유니크)
- `expires_at`: 토큰 만료 시간 (생성 시간 + 24시간)
- `verified_at`: 인증 완료 시간 (NULL = 미사용, NOT NULL = 사용됨)
- `created_at`: 토큰 생성 시간

**설계 근거**:
- 별도 테이블로 분리하여 토큰 관리 용이
- 사용자당 여러 토큰 생성 가능 (재발송 기능)
- 인덱스를 통한 빠른 토큰 조회
- CASCADE DELETE로 사용자 삭제 시 토큰도 자동 삭제

---

## 4. API 엔드포인트 설계

### 4.1 회원가입 엔드포인트 (수정)

**엔드포인트**: `POST /api/users/signup`

**요청 본문** (변경 없음):
```json
{
  "userId": "testuser",
  "password": "password123",
  "passwordConfirm": "password123",
  "name": "홍길동",
  "email": "test@example.com"
}
```

**성공 응답** (200 OK) - 수정됨:
```json
{
  "message": "회원가입이 완료되었습니다. 이메일을 확인하여 인증을 완료해주세요.",
  "email": "test@example.com"
}
```

**에러 응답** (변경 없음):
```json
// 400 Bad Request
{
  "error": "비밀번호가 일치하지 않습니다."
}

// 400 Bad Request
{
  "error": "이미 존재하는 아이디입니다."
}

// 429 Too Many Requests
{
  "error": "너무 많은 회원가입 시도가 있었습니다. 잠시 후 다시 시도해주세요."
}
```

**비즈니스 로직 변경사항**:
1. 사용자 생성 시 `email_verified = false` 설정
2. 인증 토큰 생성 (UUID + 24시간 만료)
3. `email_verification_tokens` 테이블에 토큰 저장
4. 인증 이메일 비동기 발송
5. 수정된 성공 메시지 반환

---

### 4.2 이메일 인증 엔드포인트 (신규)

**엔드포인트**: `GET /api/users/verify-email`

**쿼리 파라미터**:
- `token` (필수): 이메일에서 받은 인증 토큰

**요청 예시**:
```
GET /api/users/verify-email?token=550e8400-e29b-41d4-a716-446655440000
```

**성공 응답** (200 OK):
```json
{
  "message": "이메일 인증이 완료되었습니다. 로그인해주세요.",
  "userId": "testuser"
}
```

**에러 응답**:
```json
// 400 Bad Request - 토큰이 존재하지 않음
{
  "error": "유효하지 않은 인증 토큰입니다."
}

// 410 Gone - 토큰 만료
{
  "error": "인증 토큰이 만료되었습니다. 인증 이메일을 재발송해주세요."
}

// 409 Conflict - 이미 인증됨
{
  "error": "이미 인증된 이메일입니다."
}
```

**비즈니스 로직**:
1. 토큰으로 `email_verification_tokens` 조회
2. 토큰 존재 여부 확인 (없으면 400)
3. `expires_at > CURRENT_TIMESTAMP` 확인 (만료되면 410)
4. `verified_at IS NULL` 확인 (이미 사용되면 409)
5. `users.email_verified = true` 업데이트
6. `users.email_verified_at = CURRENT_TIMESTAMP` 업데이트
7. `email_verification_tokens.verified_at = CURRENT_TIMESTAMP` 업데이트
8. 성공 메시지 반환

---

### 4.3 인증 이메일 재발송 엔드포인트 (신규)

**엔드포인트**: `POST /api/users/resend-verification`

**요청 본문**:
```json
{
  "email": "test@example.com"
}
```

**성공 응답** (200 OK):
```json
{
  "message": "인증 이메일이 재전송되었습니다. 이메일을 확인해주세요."
}
```

**에러 응답**:
```json
// 404 Not Found - 이메일로 사용자를 찾을 수 없음
{
  "error": "등록되지 않은 이메일입니다."
}

// 400 Bad Request - 이미 인증된 사용자
{
  "error": "이미 인증된 이메일입니다."
}

// 429 Too Many Requests - Rate Limiting
{
  "error": "너무 많은 재발송 요청이 있었습니다. 1시간 후 다시 시도해주세요."
}
```

**Rate Limiting**: 이메일당 3회/시간

**비즈니스 로직**:
1. 이메일로 사용자 조회 (없으면 404)
2. `email_verified = false` 확인 (true면 400)
3. Rate Limiting 확인 (초과 시 429)
4. 기존 미사용 토큰 무효화 (선택사항: `expires_at`을 과거로 업데이트)
5. 새 인증 토큰 생성 (UUID + 24시간 만료)
6. 새 인증 이메일 발송
7. 성공 메시지 반환

---

### 4.4 로그인 엔드포인트 (수정)

**엔드포인트**: `POST /api/users/login`

**요청 본문** (변경 없음):
```json
{
  "userId": "testuser",
  "password": "password123"
}
```

**성공 응답** (200 OK) - 변경 없음:
```json
{
  "message": "로그인 성공",
  "userId": "testuser",
  "userName": "홍길동"
}
```

**에러 응답** - 추가됨:
```json
// 403 Forbidden - 이메일 미인증
{
  "error": "이메일 인증이 필요합니다. 이메일을 확인해주세요.",
  "emailVerificationRequired": true
}

// 기존 에러 응답들 (변경 없음)
// 400 Bad Request - 자격증명 불일치
{
  "error": "아이디 또는 비밀번호가 일치하지 않습니다."
}

// 429 Too Many Requests
{
  "error": "너무 많은 로그인 시도가 있었습니다. 15분 후 다시 시도해주세요."
}
```

**비즈니스 로직 변경사항**:
- 기존 비밀번호 검증 후 `email_verified` 확인 단계 추가
- `email_verified = false`인 경우 403 에러 반환

---

## 5. 이메일 서비스 설계

### 5.1 EmailService 인터페이스

**파일 위치**: `src/main/java/com/example/boards/service/EmailService.java`

```java
package com.example.boards.service;

public interface EmailService {
    /**
     * 이메일 인증 메일 발송
     * @param email 수신자 이메일
     * @param userId 사용자 ID
     * @param token 인증 토큰
     */
    void sendVerificationEmail(String email, String userId, String token);

    /**
     * 비밀번호 재설정 메일 발송 (향후 구현)
     * @param email 수신자 이메일
     * @param token 재설정 토큰
     */
    void sendPasswordResetEmail(String email, String token);
}
```

### 5.2 EmailServiceImpl 구현

**파일 위치**: `src/main/java/com/example/boards/service/impl/EmailServiceImpl.java`

**주요 기능**:
- Spring JavaMailSender 사용
- HTML 이메일 템플릿 지원
- 비동기 이메일 발송 (`@Async`)
- 이메일 발송 실패 로깅

**의존성**:
```java
@Service
public class EmailServiceImpl implements EmailService {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.verification.base-url}")
    private String baseUrl;
}
```

### 5.3 이메일 템플릿

**제목**: `[게시판] 이메일 인증을 완료해주세요`

**본문** (HTML):
```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .button {
            display: inline-block;
            padding: 12px 24px;
            background-color: #007bff;
            color: white;
            text-decoration: none;
            border-radius: 4px;
        }
        .footer { margin-top: 30px; font-size: 12px; color: #666; }
    </style>
</head>
<body>
    <div class="container">
        <h2>이메일 인증을 완료해주세요</h2>
        <p>안녕하세요, <strong>{userName}</strong>님</p>
        <p>회원가입을 완료하려면 아래 버튼을 클릭하여 이메일 인증을 완료해주세요.</p>

        <p style="text-align: center; margin: 30px 0;">
            <a href="{verificationUrl}" class="button">이메일 인증하기</a>
        </p>

        <p>또는 아래 링크를 복사하여 브라우저에 붙여넣으세요:</p>
        <p style="word-break: break-all; background-color: #f5f5f5; padding: 10px;">
            {verificationUrl}
        </p>

        <p class="footer">
            이 링크는 24시간 동안 유효합니다.<br>
            본인이 요청하지 않은 경우 이 이메일을 무시하셔도 됩니다.
        </p>
    </div>
</body>
</html>
```

**플레이스홀더**:
- `{userName}`: 사용자 이름
- `{verificationUrl}`: 인증 URL (예: `http://localhost:3000/verify-email?token=xxx`)

### 5.4 인증 URL 형식

```
http://localhost:3000/verify-email?token={token}
```

**프로덕션**:
```
https://yourdomain.com/verify-email?token={token}
```

### 5.5 이메일 제공자 옵션

| 제공자 | 용도 | 설정 난이도 | 비용 |
|--------|------|------------|------|
| **Gmail SMTP** | 개발/테스트 | 쉬움 | 무료 (일일 500통) |
| **AWS SES** | 프로덕션 | 중간 | 사용량 기반 |
| **SendGrid** | 프로덕션 | 쉬움 | 사용량 기반 |
| **Naver SMTP** | 개발/소규모 | 쉬움 | 무료 (일일 제한) |

**개발 환경 권장**: Gmail SMTP (앱 비밀번호 사용)

---

## 6. 보안 고려사항

### 6.1 토큰 보안

#### 6.1.1 토큰 생성
```java
// UUID v4 사용 (128비트 랜덤성)
String token = UUID.randomUUID().toString();
```

**특징**:
- 암호학적으로 안전한 랜덤 생성
- 충돌 확률 극히 낮음 (2^-128)
- 추측 불가능

#### 6.1.2 토큰 만료
```java
// 24시간 후 만료
Timestamp expiresAt = Timestamp.from(
    Instant.now().plus(24, ChronoUnit.HOURS)
);
```

**권장 만료 시간**:
- 이메일 인증: 24시간
- 비밀번호 재설정: 1시간

#### 6.1.3 일회용 토큰
```sql
-- 인증 완료 시 사용 표시
UPDATE email_verification_tokens
SET verified_at = CURRENT_TIMESTAMP
WHERE token = ?;
```

**검증 로직**:
```java
if (tokenEntity.getVerifiedAt() != null) {
    throw new TokenAlreadyUsedException("이미 사용된 토큰입니다.");
}
```

### 6.2 Rate Limiting

| 엔드포인트 | 제한 | 시간 윈도우 | 기준 |
|-----------|------|-----------|------|
| `/api/users/signup` | 3회 | 1시간 | IP |
| `/api/users/resend-verification` | 3회 | 1시간 | 이메일 |
| `/api/users/verify-email` | 10회 | 1시간 | IP |
| `/api/users/login` | 5회 | 15분 | IP |

**구현**:
- 기존 `RateLimiterService` 확장
- Bucket4j 토큰 버킷 알고리즘 사용

### 6.3 이메일 검증

**DTO 검증**:
```java
@Valid
public class SignupRequest {
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @NotBlank(message = "이메일은 필수입니다.")
    private String email;
}
```

**추가 검증 (선택사항)**:
- 일회용 이메일 차단 (Mailinator, Guerrilla Mail 등)
- 도메인 MX 레코드 확인
- 이메일 형식 정규표현식 검증

### 6.4 CSRF 보호

- 기존 Spring Security CSRF 보호 활용
- 쿠키 기반 CSRF 토큰 (SameSite=Strict)

### 6.5 SQL Injection 방지

- MyBatis 파라미터 바인딩 사용 (`#{}`)
- 직접 SQL 문자열 조합 금지

```xml
<!-- 안전한 방식 -->
<select id="findByToken" resultType="EmailVerificationToken">
    SELECT * FROM email_verification_tokens
    WHERE token = #{token}
</select>
```

---

## 7. 구현 체크리스트

### 7.1 백엔드 작업

#### 데이터베이스
- [ ] `users` 테이블에 `email_verified`, `email_verified_at` 컬럼 추가
- [ ] `email_verification_tokens` 테이블 생성 (인덱스 포함)
- [ ] 기존 사용자 마이그레이션 스크립트 작성 (`email_verified=true` 설정)
- [ ] `schema.sql` 업데이트

#### 모델 & DTO
- [ ] `User` 모델에 `emailVerified`, `emailVerifiedAt` 필드 추가
- [ ] `EmailVerificationToken` 모델 생성
- [ ] `ResendVerificationRequest` DTO 생성
- [ ] `SignupRequest`에 `@Email` 검증 추가 (이미 있으면 확인)

#### Mapper
- [ ] `EmailVerificationTokenMapper` 인터페이스 생성
  - [ ] `insertToken(EmailVerificationToken token)`
  - [ ] `findByToken(String token)`
  - [ ] `findByUserId(String userId)`
  - [ ] `updateVerifiedAt(String token)`
  - [ ] `deleteExpiredTokens()`
- [ ] `EmailVerificationTokenMapper.xml` 생성
- [ ] `UserMapper`에 이메일 인증 관련 메서드 추가
  - [ ] `updateEmailVerified(String userId)`

#### Service
- [ ] `EmailService` 인터페이스 생성
- [ ] `EmailServiceImpl` 구현
  - [ ] `sendVerificationEmail()` 구현
  - [ ] HTML 이메일 템플릿 작성
  - [ ] 비동기 처리 (`@Async`) 설정
- [ ] `EmailVerificationService` 생성
  - [ ] `createVerificationToken(String userId)` 구현
  - [ ] `verifyEmail(String token)` 구현
  - [ ] `isTokenValid(EmailVerificationToken token)` 구현
- [ ] `UserService` 수정
  - [ ] `signup()`: 인증 토큰 생성 및 이메일 발송 추가
  - [ ] `login()`: 이메일 인증 확인 로직 추가
  - [ ] `resendVerification(String email)` 메서드 추가

#### Controller
- [ ] `UserController` 수정
  - [ ] `signup()`: 응답 메시지 변경
  - [ ] `login()`: 이메일 미인증 에러 처리 추가
- [ ] `UserController`에 신규 엔드포인트 추가
  - [ ] `verifyEmail(@RequestParam String token)` 구현
  - [ ] `resendVerification(@RequestBody ResendVerificationRequest)` 구현
- [ ] 예외 처리
  - [ ] `EmailNotVerifiedException` 예외 클래스 생성
  - [ ] `TokenExpiredException` 예외 클래스 생성
  - [ ] `TokenAlreadyUsedException` 예외 클래스 생성

#### Rate Limiting
- [ ] `RateLimiterService`에 `allowEmailVerification()` 메서드 추가
- [ ] `RateLimiterService`에 `allowResendVerification()` 메서드 추가
- [ ] 이메일 재발송 Rate Limit 적용

#### 설정
- [ ] `build.gradle`에 Spring Mail 의존성 추가
  ```gradle
  implementation 'org.springframework.boot:spring-boot-starter-mail'
  ```
- [ ] `application.yml`에 이메일 설정 추가
  - [ ] SMTP 서버 정보
  - [ ] 발신자 이메일
  - [ ] 인증 URL 베이스
  - [ ] 토큰 만료 시간
- [ ] 환경변수 설정 (`MAIL_USERNAME`, `MAIL_PASSWORD`)
- [ ] `@EnableAsync` 설정 (비동기 이메일 발송)

---

### 7.2 프론트엔드 작업

#### 컴포넌트
- [ ] `VerifyEmail.jsx` 컴포넌트 생성
  - [ ] URL 파라미터에서 토큰 추출
  - [ ] API 호출 및 결과 처리
  - [ ] 성공/실패/만료 UI 표시
- [ ] `Signup.jsx` 수정
  - [ ] 회원가입 성공 시 "이메일 확인" 메시지 표시
  - [ ] "인증 이메일 재발송" 링크 추가 (선택사항)
- [ ] `Login.jsx` 수정
  - [ ] 이메일 미인증 에러 처리
  - [ ] "인증 이메일 재발송" 버튼 표시
- [ ] `ResendVerification.jsx` 컴포넌트 생성 (선택사항)
  - [ ] 이메일 입력 폼
  - [ ] 재발송 요청 처리

#### 라우팅
- [ ] `App.js`에 `/verify-email` 라우트 추가
- [ ] 라우트 설정:
  ```jsx
  <Route path="/verify-email" component={VerifyEmail} />
  ```

#### API 통합
- [ ] 회원가입 API 호출 업데이트
  - [ ] 새 응답 메시지 처리
- [ ] 이메일 인증 API 함수 추가
  ```javascript
  const verifyEmail = async (token) => {
      return await axios.get(`/api/users/verify-email?token=${token}`);
  };
  ```
- [ ] 인증 이메일 재발송 API 함수 추가
  ```javascript
  const resendVerification = async (email) => {
      return await axios.post('/api/users/resend-verification', { email });
  };
  ```
- [ ] 로그인 API에서 이메일 인증 에러 처리

#### UI/UX
- [ ] 이메일 인증 대기 상태 UI 디자인
- [ ] 인증 성공 페이지 디자인
- [ ] 인증 실패/만료 페이지 디자인
- [ ] 로딩 스피너 추가

---

### 7.3 테스트 작업

#### 단위 테스트
- [ ] `EmailServiceTest`
  - [ ] 이메일 발송 성공 테스트 (Mock SMTP)
  - [ ] 이메일 발송 실패 테스트
- [ ] `EmailVerificationServiceTest`
  - [ ] 토큰 생성 테스트
  - [ ] 토큰 검증 테스트 (유효/만료/사용됨)
  - [ ] 토큰 만료 로직 테스트
- [ ] `UserServiceTest`
  - [ ] 회원가입 시 이메일 발송 테스트
  - [ ] 로그인 시 이메일 인증 확인 테스트
  - [ ] 이메일 재발송 테스트

#### 통합 테스트
- [ ] 회원가입 → 이메일 발송 → 토큰 생성 플로우
- [ ] 이메일 인증 → 사용자 검증 상태 업데이트
- [ ] 미인증 사용자 로그인 차단
- [ ] 토큰 만료 처리
- [ ] 이미 사용된 토큰 처리
- [ ] 이메일 재발송 플로우

#### E2E 테스트
- [ ] 전체 회원가입 → 이메일 인증 → 로그인 플로우
- [ ] 이메일 재발송 플로우
- [ ] 토큰 만료 후 재발송
- [ ] 미인증 상태로 로그인 시도

---

### 7.4 문서화 작업
- [ ] API 문서 업데이트 (Swagger/OpenAPI)
- [ ] README.md에 이메일 설정 가이드 추가
- [ ] 환경변수 설정 가이드 작성
- [ ] 이메일 템플릿 커스터마이징 가이드

---

## 8. 마이그레이션 전략

### 8.1 기존 사용자 처리

**문제**: 기존 사용자는 `email_verified=false` 상태가 됨

**해결 방안 (옵션 1)**: 즉시 인증 완료 처리
```sql
-- 배포 시 실행: 기존 사용자는 모두 인증됨으로 간주
UPDATE users
SET email_verified = TRUE,
    email_verified_at = created_at
WHERE email_verified IS FALSE OR email_verified IS NULL;
```

**해결 방안 (옵션 2)**: 유예 기간 제공 (7일)
```sql
-- 기존 사용자에게 7일 유예 기간 제공
-- 애플리케이션 로직에서 created_at < 배포일 + 7일이면 로그인 허용
-- 7일 후 자동으로 이메일 인증 강제
```

**권장**: 옵션 1 (기존 사용자 즉시 인증 완료 처리)

### 8.2 마이그레이션 스크립트

**파일**: `src/main/resources/db/migration/V2__add_email_verification.sql`

```sql
-- Step 1: users 테이블에 컬럼 추가
ALTER TABLE users
ADD COLUMN email_verified BOOLEAN DEFAULT FALSE,
ADD COLUMN email_verified_at TIMESTAMP NULL;

-- Step 2: 기존 사용자 인증 완료 처리
UPDATE users
SET email_verified = TRUE,
    email_verified_at = created_at
WHERE email_verified IS FALSE OR email_verified IS NULL;

-- Step 3: email_verification_tokens 테이블 생성
CREATE TABLE email_verification_tokens (
    token_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Step 4: 인덱스 생성
CREATE INDEX idx_token ON email_verification_tokens(token);
CREATE INDEX idx_user_id ON email_verification_tokens(user_id);
CREATE INDEX idx_expires_at ON email_verification_tokens(expires_at);
```

### 8.3 롤백 계획

**롤백 스크립트**: `rollback_email_verification.sql`

```sql
-- Step 1: 테이블 삭제
DROP TABLE IF EXISTS email_verification_tokens;

-- Step 2: 컬럼 제거
ALTER TABLE users
DROP COLUMN email_verified,
DROP COLUMN email_verified_at;
```

**롤백 시나리오**:
1. 이메일 발송 문제로 사용자 가입 불가
2. 치명적인 버그 발견
3. 프로덕션 환경에서 예상치 못한 문제 발생

---

## 9. 테스트 계획

### 9.1 단위 테스트

#### EmailServiceTest
```java
@SpringBootTest
class EmailServiceTest {
    @MockBean
    private JavaMailSender mailSender;

    @Test
    void 이메일_발송_성공() {
        // Given: 유효한 이메일 정보
        // When: sendVerificationEmail 호출
        // Then: mailSender.send() 호출 확인
    }

    @Test
    void 이메일_발송_실패_처리() {
        // Given: mailSender가 예외 발생
        // When: sendVerificationEmail 호출
        // Then: 예외가 로깅되고 전파되지 않음
    }
}
```

#### EmailVerificationServiceTest
```java
@SpringBootTest
class EmailVerificationServiceTest {
    @Test
    void 토큰_생성_및_저장() {
        // Given: 유효한 사용자 ID
        // When: createVerificationToken 호출
        // Then: UUID 토큰 생성, DB 저장, 24시간 만료 설정
    }

    @Test
    void 유효한_토큰_인증_성공() {
        // Given: 유효한 토큰 (만료 전, 미사용)
        // When: verifyEmail 호출
        // Then: email_verified=true, verified_at 업데이트
    }

    @Test
    void 만료된_토큰_인증_실패() {
        // Given: 만료된 토큰
        // When: verifyEmail 호출
        // Then: TokenExpiredException 발생
    }

    @Test
    void 이미_사용된_토큰_인증_실패() {
        // Given: verified_at이 NULL이 아닌 토큰
        // When: verifyEmail 호출
        // Then: TokenAlreadyUsedException 발생
    }
}
```

#### UserServiceTest
```java
@SpringBootTest
class UserServiceTest {
    @Test
    void 회원가입_시_인증_이메일_발송() {
        // Given: 유효한 회원가입 요청
        // When: signup 호출
        // Then: 사용자 생성, 토큰 생성, 이메일 발송
    }

    @Test
    void 미인증_사용자_로그인_차단() {
        // Given: email_verified=false인 사용자
        // When: login 호출
        // Then: EmailNotVerifiedException 발생
    }

    @Test
    void 인증된_사용자_로그인_성공() {
        // Given: email_verified=true인 사용자
        // When: login 호출
        // Then: 로그인 성공, 세션 생성
    }
}
```

---

### 9.2 통합 테스트

#### 회원가입 플로우
```java
@SpringBootTest
@AutoConfigureMockMvc
class SignupIntegrationTest {
    @Test
    void 회원가입_완전한_플로우() {
        // 1. 회원가입 요청
        // 2. 사용자 생성 확인 (email_verified=false)
        // 3. 토큰 생성 확인
        // 4. 이메일 발송 확인 (Mock)
    }
}
```

#### 이메일 인증 플로우
```java
@SpringBootTest
@AutoConfigureMockMvc
class EmailVerificationIntegrationTest {
    @Test
    void 이메일_인증_완전한_플로우() {
        // 1. 회원가입
        // 2. 토큰 조회
        // 3. 이메일 인증 요청
        // 4. email_verified=true 확인
        // 5. 로그인 성공 확인
    }

    @Test
    void 토큰_만료_처리() {
        // 1. 회원가입
        // 2. 토큰 만료시간 조작
        // 3. 이메일 인증 요청
        // 4. 410 Gone 응답 확인
    }
}
```

#### 이메일 재발송 플로우
```java
@SpringBootTest
@AutoConfigureMockMvc
class ResendVerificationIntegrationTest {
    @Test
    void 이메일_재발송_성공() {
        // 1. 회원가입
        // 2. 재발송 요청
        // 3. 새 토큰 생성 확인
        // 4. 이메일 재발송 확인
    }

    @Test
    void 이미_인증된_사용자_재발송_실패() {
        // 1. 회원가입 및 인증 완료
        // 2. 재발송 요청
        // 3. 400 Bad Request 확인
    }
}
```

---

### 9.3 E2E 테스트

#### Selenium/Playwright 테스트
```javascript
// 회원가입 → 이메일 인증 → 로그인 전체 플로우
test('Complete signup with email verification flow', async () => {
  // 1. 회원가입 페이지 접속
  // 2. 회원가입 폼 작성 및 제출
  // 3. "이메일 확인" 메시지 확인
  // 4. 이메일에서 토큰 추출 (테스트 환경)
  // 5. 인증 링크 클릭
  // 6. "인증 완료" 메시지 확인
  // 7. 로그인 성공
});

test('Login without email verification blocked', async () => {
  // 1. 회원가입 (인증하지 않음)
  // 2. 로그인 시도
  // 3. "이메일 인증 필요" 메시지 확인
});
```

---

### 9.4 성능 테스트

#### 이메일 발송 성능
```java
@Test
void 동시_이메일_발송_부하_테스트() {
    // 100개 이메일 동시 발송
    // 평균 발송 시간 측정
    // 실패율 확인
}
```

#### 토큰 조회 성능
```java
@Test
void 토큰_조회_성능_테스트() {
    // 10,000개 토큰 생성
    // 무작위 토큰 조회 1,000회
    // 평균 조회 시간 < 10ms 확인
}
```

---

## 10. 모니터링 및 메트릭

### 10.1 추적 메트릭

#### 비즈니스 메트릭
| 메트릭 | 설명 | 목표 |
|-------|------|------|
| **이메일 인증 완료율** | 회원가입 대비 인증 완료 비율 | > 80% |
| **평균 인증 소요 시간** | 회원가입부터 인증까지 시간 | < 1시간 |
| **토큰 만료율** | 만료된 토큰 비율 | < 20% |
| **재발송 요청율** | 사용자당 평균 재발송 횟수 | < 0.5 |

#### 기술 메트릭
| 메트릭 | 설명 | 임계값 |
|-------|------|--------|
| **이메일 발송 성공률** | 발송 성공 / 전체 시도 | > 99% |
| **이메일 발송 지연 시간** | 요청부터 발송까지 시간 | < 5초 |
| **토큰 조회 응답 시간** | DB에서 토큰 조회 시간 | < 50ms |
| **인증 API 응답 시간** | `/verify-email` 응답 시간 | < 200ms |

### 10.2 로깅 전략

#### 로그 레벨
```java
// INFO: 정상 플로우
log.info("Verification email sent: userId={}, email={}", userId, email);
log.info("Email verified successfully: userId={}, token={}", userId, token);

// WARN: 비정상이지만 처리 가능
log.warn("Verification token expired: userId={}, token={}", userId, token);
log.warn("Email sending failed, will retry: userId={}, error={}", userId, e.getMessage());

// ERROR: 치명적 오류
log.error("Failed to send verification email: userId={}", userId, e);
log.error("Database error during email verification: token={}", token, e);
```

#### 로그 포인트
1. **회원가입 시**:
   - 사용자 생성 성공
   - 토큰 생성 성공
   - 이메일 발송 성공/실패

2. **이메일 인증 시**:
   - 토큰 검증 시작
   - 인증 성공
   - 인증 실패 (원인별 로깅)

3. **이메일 재발송 시**:
   - 재발송 요청
   - 새 토큰 생성
   - 이메일 재발송 성공/실패

4. **로그인 시**:
   - 이메일 미인증으로 로그인 차단

### 10.3 알림 설정

#### Critical 알림 (즉시 대응)
- 이메일 발송 성공률 < 95% (5분 지속)
- 이메일 인증 API 에러율 > 5% (1분 지속)
- 토큰 생성 실패 발생

#### Warning 알림 (모니터링 필요)
- 이메일 인증 완료율 < 70% (1시간 지속)
- 평균 이메일 발송 지연 > 10초 (10분 지속)
- 토큰 만료율 > 30% (1일 지속)

### 10.4 대시보드 구성

#### 실시간 모니터링
```
┌─────────────────────────────────────────┐
│ 이메일 인증 현황                           │
├─────────────────────────────────────────┤
│ 오늘 회원가입: 150명                       │
│ 이메일 인증 완료: 120명 (80%)              │
│ 인증 대기: 30명                           │
│ 평균 인증 시간: 45분                       │
├─────────────────────────────────────────┤
│ 이메일 발송 상태                           │
├─────────────────────────────────────────┤
│ 발송 성공: 98.5%                          │
│ 평균 발송 시간: 2.3초                      │
│ 재발송 요청: 12건                          │
└─────────────────────────────────────────┘
```

#### 주간/월간 리포트
- 주간 회원가입 및 인증 완료 추이
- 토큰 만료율 추이
- 이메일 발송 성공률 추이
- 재발송 요청 패턴 분석

---

## 11. 구성 파일 예시

### 11.1 application.yml

```yaml
spring:
  # 이메일 설정
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000
    default-encoding: UTF-8

  # 비동기 처리 설정
  task:
    execution:
      pool:
        core-size: 2
        max-size: 5
        queue-capacity: 100

# 애플리케이션 설정
app:
  mail:
    # 발신자 이메일
    from: noreply@example.com
    # 인증 설정
    verification:
      # 토큰 만료 시간 (시간 단위)
      expiry-hours: 24
      # 프론트엔드 베이스 URL
      base-url: ${APP_BASE_URL:http://localhost:3000}
```

### 11.2 환경 변수 (.env)

```bash
# 이메일 설정
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# 애플리케이션 설정
APP_BASE_URL=http://localhost:3000

# 프로덕션 환경
# MAIL_USERNAME=noreply@yourdomain.com
# MAIL_PASSWORD=production-password
# APP_BASE_URL=https://yourdomain.com
```

### 11.3 Gmail SMTP 설정 가이드

#### 1단계: Google 계정 설정
1. Google 계정 관리 → 보안
2. 2단계 인증 활성화
3. 앱 비밀번호 생성
   - 앱 선택: 메일
   - 기기 선택: 기타 (사용자 지정 이름)
   - 생성된 16자리 비밀번호 복사

#### 2단계: application.yml 설정
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: xxxx-xxxx-xxxx-xxxx  # 앱 비밀번호
```

#### 3단계: 테스트
```bash
# 애플리케이션 실행 후 회원가입 테스트
# Gmail에서 이메일 수신 확인
```

---

## 12. FAQ & 문제 해결

### Q1: 이메일이 발송되지 않습니다.

**확인 사항**:
1. `MAIL_USERNAME`, `MAIL_PASSWORD` 환경변수 설정 확인
2. Gmail 앱 비밀번호 사용 (일반 비밀번호 X)
3. 방화벽에서 SMTP 포트(587) 허용 확인
4. 로그에서 에러 메시지 확인

**해결 방법**:
```bash
# 로그 확인
grep "email" logs/application.log

# SMTP 연결 테스트
telnet smtp.gmail.com 587
```

---

### Q2: 이메일이 스팸함으로 갑니다.

**원인**:
- 발신자 도메인 인증 부족 (SPF, DKIM, DMARC)
- 수신자의 스팸 필터 설정

**해결 방법**:
1. **개발 환경**: Gmail 설정에서 발신자를 안전한 발신자로 추가
2. **프로덕션 환경**:
   - 도메인 인증 설정 (SPF, DKIM)
   - 전문 이메일 서비스 사용 (AWS SES, SendGrid)

---

### Q3: 토큰이 자꾸 만료됩니다.

**원인**:
- 만료 시간이 너무 짧음 (기본 24시간)
- 서버 시간대 설정 오류

**해결 방법**:
```yaml
# application.yml에서 만료 시간 연장
app:
  mail:
    verification:
      expiry-hours: 48  # 24 → 48시간
```

```sql
-- 서버 시간대 확인
SELECT NOW(), @@time_zone;
```

---

### Q4: 기존 사용자가 로그인할 수 없습니다.

**원인**:
- 기존 사용자의 `email_verified=false`

**해결 방법**:
```sql
-- 기존 사용자 모두 인증 완료 처리
UPDATE users
SET email_verified = TRUE,
    email_verified_at = created_at
WHERE email_verified = FALSE;
```

---

### Q5: Rate Limiting이 너무 엄격합니다.

**원인**:
- 개발 환경에서 테스트 시 제한에 자주 걸림

**해결 방법**:
```java
// RateLimiterService.java - 개발 환경용 설정
@Profile("dev")
@Bean
public Bucket createBucket() {
    // 개발 환경에서는 제한 완화
    return Bucket.builder()
        .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
        .build();
}
```

---

## 13. 향후 개선 사항

### 13.1 단계별 확장 계획

#### Phase 2: 고급 기능
- [ ] 이메일 템플릿 커스터마이징 UI (관리자)
- [ ] 다국어 이메일 지원 (한국어/영어)
- [ ] 이메일 발송 이력 조회 API
- [ ] 이메일 인증 통계 대시보드

#### Phase 3: 보안 강화
- [ ] 이메일 OTP (6자리 숫자 코드) 옵션
- [ ] 2단계 인증 (2FA) 통합
- [ ] 비밀번호 재설정 이메일 인증
- [ ] 로그인 알림 이메일

#### Phase 4: 사용자 경험 개선
- [ ] 이메일 인증 없이 일부 기능 사용 허용 (제한된 권한)
- [ ] 소셜 로그인 통합 (Google, Naver, Kakao)
- [ ] Magic Link 로그인 (비밀번호 없이 이메일로 로그인)
- [ ] 이메일 변경 시 재인증 플로우

---

## 14. 참고 자료

### 14.1 공식 문서
- [Spring Mail 공식 문서](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#mail)
- [JavaMailSender API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/mail/javamail/JavaMailSender.html)
- [MyBatis 공식 문서](https://mybatis.org/mybatis-3/)

### 14.2 보안 가이드
- [OWASP Email Security](https://cheatsheetseries.owasp.org/cheatsheets/Email_Security_Cheat_Sheet.html)
- [Token-Based Authentication Best Practices](https://auth0.com/docs/secure/tokens)

### 14.3 이메일 서비스 제공자
- [Gmail SMTP 설정](https://support.google.com/a/answer/176600?hl=ko)
- [AWS SES 문서](https://docs.aws.amazon.com/ses/)
- [SendGrid 문서](https://docs.sendgrid.com/)

---

## 문서 버전 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2025-11-10 | Claude Code | 초기 설계 문서 작성 |

---

**문서 끝**
