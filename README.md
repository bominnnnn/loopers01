# LOOPERS Server

Spring Boot 기반 REST API 백엔드 서버

---

## 기술 스택

| 항목 | 내용 |
|------|------|
| 언어 | Java 15 |
| 프레임워크 | Spring Boot 2.7.18 |
| 인증 | JWT (jjwt 0.12.3) |
| 비밀번호 | BCrypt |
| DB | In-Memory (List) |
| 테스트 | JUnit 5 + MockMvc |
| 빌드 | Maven |

---

## 실행 방법

```bash
# 서버 실행
mvn spring-boot:run

# 테스트 실행
mvn test
```

서버 기본 포트: `http://localhost:3000`

---

## 환경변수 설정

`application.yml` 에서 수정

```yaml
jwt:
  secret: 32자 이상의 랜덤 문자열로 교체
  expiration-days: 7

cors:
  allowed-origin: http://localhost:5173  # 프론트엔드 주소
```

---

## 프로젝트 구조

```
src/
  main/
    java/com/loopers/server/
      config/
        SecurityConfig.java         # JWT 필터, CORS, 인증 경로 설정
      controller/
        AuthController.java         # /api/auth/* 엔드포인트
        PostController.java         # /api/posts/* 엔드포인트
        HealthController.java       # /health
      service/
        AuthService.java            # 인증 비즈니스 로직
        PostService.java            # 게시글 비즈니스 로직
      repository/
        InMemoryUserRepository.java # 유저 데이터 저장소
        InMemoryPostRepository.java # 게시글 데이터 저장소
      model/
        User.java                   # 유저 도메인 객체
        Post.java                   # 게시글 도메인 객체
      dto/
        RegisterRequest.java        # 회원가입 요청
        LoginRequest.java           # 로그인 요청
        UpdateProfileRequest.java   # 프로필 수정 요청
        ChangePasswordRequest.java  # 비밀번호 변경 요청
        ValidatePasswordRequest.java# 비밀번호 유효성 검사 요청
        CreatePostRequest.java      # 게시글 작성 요청
        UserResponse.java           # 유저 응답 (비밀번호 제외)
        PostResponse.java           # 게시글 응답 (작성자 정보 포함)
      security/
        JwtUtil.java                # 토큰 생성/파싱
        JwtAuthFilter.java          # 매 요청마다 토큰 검증
        UserPrincipal.java          # 인증된 유저 정보 (id, loginId, role)
      exception/
        ApiException.java           # 커스텀 예외
        GlobalExceptionHandler.java # 전역 에러 처리
    resources/
      application.yml
  test/
    java/com/loopers/server/
      AuthControllerTest.java       # 인증 API 테스트 17개
      PostControllerTest.java       # 게시글 API 테스트 9개
```

---

## API 목록

### 인증 (`/api/auth`)

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| POST | `/api/auth/register` | ❌ | 회원가입 |
| POST | `/api/auth/login` | ❌ | 로그인 |
| GET | `/api/auth/me` | ✅ | 내 정보 조회 |
| PATCH | `/api/auth/me` | ✅ | 내 정보 수정 |
| PATCH | `/api/auth/me/password` | ✅ | 비밀번호 변경 |
| DELETE | `/api/auth/me` | ✅ | 회원 탈퇴 |
| POST | `/api/auth/validate-password` | ❌ | 비밀번호 유효성 검사 |
| GET | `/api/auth/me/posts` | ✅ | 내가 쓴 게시글 (미구현) |

### 게시글 (`/api/posts`)

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| GET | `/api/posts` | ✅ | 게시글 목록 (subject, type 필터 가능) |
| GET | `/api/posts/:id` | ✅ | 게시글 단건 조회 |
| POST | `/api/posts` | ✅ | 게시글 작성 |
| POST | `/api/posts/:id/like` | ✅ | 게시글 추천 |
| GET | `/api/posts/ranking/all` | ✅ | 랭킹 조회 |

---

## 요청/응답 예시

### 회원가입

**Request**
```json
POST /api/auth/register
{
  "loginId": "hong123",
  "name": "홍길동",
  "email": "hong@test.com",
  "password": "Hong1234!",
  "duressPassword": "Destroy9@",
  "referral": "Loopers"
}
```

**Response** `201`
```json
{
  "token": "eyJ...",
  "user": {
    "loginId": "hong123",
    "name": "홍길동",
    "email": "hong@test.com",
    "role": "agent",
    "watchlisted": false
  }
}
```

### 로그인

**Request**
```json
POST /api/auth/login
{
  "loginId": "hong123",
  "password": "Hong1234!"
}
```

**Response** `200`
```json
{
  "token": "eyJ...",
  "user": { ... }
}
```

### 게시글 작성

**Request**
```json
POST /api/posts
Authorization: Bearer eyJ...
{
  "subject": "수학",
  "grade": "고1",
  "publisher": "미래엔",
  "page": "42",
  "type": "오류신고",
  "content": "본문 내용",
  "secretContent": "요원 전용 메모"
}
```

**Response** `201`
```json
{
  "post": {
    "id": 1,
    "subject": "수학",
    "content": "본문 내용",
    "reporterLoginId": "hong123",
    "reporterName": "홍길동",
    "likes": 0,
    "createdAt": "2026-05-12"
  }
}
```

---

## 유저 역할 (role)

| 역할 | 설명 |
|------|------|
| `civilian` | 일반 유저 — 가입 기본값 |
| `agent` | 요원 — 가입 시 referral에 `"Loopers"` 입력 |
| `watchlisted_agent` | 감시 대상 — 게시글 조회 시 가짜 내용 반환 |

---

## 특수 기능

### 과거 열쇠 함정
비밀번호를 변경한 뒤 **이전 비밀번호**로 로그인하면 가짜 토큰과 가짜 유저 정보를 반환합니다.
```json
{
  "token": "가짜 토큰",
  "user": { "loginId": "fake_id", "isFake": true },
  "trap": "old_key"
}
```

### 자폭 비밀번호
가입 시 `duressPassword`를 등록한 경우, 해당 비밀번호로 로그인하면 계정이 즉시 삭제됩니다.
```json
{ "selfDestruct": true }
```

### 비밀번호 규칙
- 8자 이상
- 대문자 포함
- 숫자 포함
- 특수문자 포함

---

## TODO

- `GET /api/auth/me/posts` — 내가 쓴 게시글 목록 구현
- 게시글 페이지네이션
- 좋아요 중복 방지
- 회원 탈퇴 시 게시글 연동 삭제
- 실 DB (PostgreSQL 등) 연동 — `InMemoryUserRepository`, `InMemoryPostRepository` 교체
