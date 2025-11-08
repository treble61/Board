# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot + React board system with session-based authentication and MyBatis for database operations. Backend runs on port 8080, frontend proxies requests through port 3000.

## Technology Stack

**Backend**: Java 8, Spring Boot 2.1.18, MyBatis, MariaDB on port 3307
**Frontend**: React 17, React Router DOM 5, Axios
**Build**: Gradle (backend), npm (frontend)

## Development Commands

### Backend (from project root)
```bash
# Run backend server (port 8080)
./gradlew bootRun         # Linux/Mac
gradlew.bat bootRun       # Windows

# Run tests
./gradlew test

# Clean build
./gradlew clean build
```

### Frontend (from `frontend/` directory)
```bash
# Install dependencies
npm install

# Run development server (port 3000, proxies to :8080)
npm start

# Run tests
npm test

# Production build
npm run build
```

### Database
- **Connection**: MariaDB on `localhost:3307`, database `boards`, user `root`, password `1234`
- **Schema**: Auto-created from `src/main/resources/schema.sql` on startup
- **Config**: `src/main/resources/application.yml`

## Architecture

### Backend Layer Structure (Classic 3-Tier)

```
Controller → Service → Mapper (MyBatis interface) → XML (SQL queries)
                ↓
              Model (domain objects)
              DTO (request/response objects)
```

**Controllers**: REST endpoints under `/api/*`, session management for authentication
**Services**: Business logic, password validation, LIKE pattern escaping for SQL injection prevention
**Mappers**: MyBatis interfaces in `com.example.boards.mapper`, XML files in `src/main/resources/mapper/`
**Models**: Domain objects (User, Post, Comment, FileAttachment) with Lombok
**DTOs**: Request objects (LoginRequest, SignupRequest, ChangePasswordRequest)

### Frontend Structure

```
App.js (Router) → Components (Login, Signup, PostList, PostDetail, PostWrite, PostEdit, ChangePassword)
                → utils/ (XSS sanitization with DOMPurify)
```

**Routing**: React Router DOM v5 with session state management
**API Calls**: Axios with proxy configuration to backend
**XSS Protection**: DOMPurify sanitization for user-generated HTML content

### Key Design Patterns

1. **Session-Based Auth**: Login creates server-side session, logout invalidates it. Current user retrieved via `/api/users/me`.

2. **Password Security**:
   - SHA-256 hashing with `commons-codec` in `PasswordEncoder.java:7`
   - Password change enforcement after 90 days (tracked via `password_changed_at` column)
   - Validation in service layer before DB operations

3. **SQL Injection Prevention**:
   - `PostService.java:20-28` escapes `%`, `_`, `\` in LIKE search patterns
   - MyBatis parameterized queries in XML mappers

4. **File Upload**:
   - Max file size: 10MB, max request: 50MB (configured in `application.yml:9-11`)
   - Excel file validation via `ExcelValidator.java` using Apache POI
   - File attachments linked to posts via `FileAttachmentMapper`

5. **Transaction Management**:
   - View count increment uses `@Transactional` annotation (`PostService.java:40-44`)
   - Ensures atomicity of read + increment operations

## Database Schema

**users**: `user_id` (PK), `password` (SHA-256), `name`, `email`, `password_changed_at`, `created_at`
**posts**: `post_id` (PK), `title`, `content`, `author_id` (FK), `is_notice`, `view_count`, `excel_*` fields, timestamps
**comments**: `comment_id` (PK), `post_id` (FK), `author_id` (FK), `content`, timestamps
**file_attachments**: `attachment_id` (PK), `post_id` (FK), `filename`, `stored_filename`, `file_path`, `file_size`, timestamps

## Common Patterns

### Adding New API Endpoint
1. Create DTO in `dto/` if needed (request/response objects)
2. Add mapper interface method in `mapper/*.java`
3. Write SQL in `src/main/resources/mapper/*.xml`
4. Implement service logic in `service/`
5. Create controller endpoint in `controller/`

### Testing
- Unit tests in `src/test/java/com/example/boards/`
- Service tests mock mapper dependencies
- Controller tests use Spring Boot test annotations
- Frontend tests use React Testing Library

### Configuration
- Database connection: `application.yml`
- Session timeout: 1800 seconds (30 minutes)
- MyBatis mapper locations: `classpath:mapper/**/*.xml`
- Underscore to camelCase mapping enabled
