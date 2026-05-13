# CLAUDE.md

## 작업 원칙

### 1. 코딩 전에 먼저 생각하기

구현 전에:
- 가정이 있으면 명시적으로 밝힌다. 불확실하면 묻는다.
- 해석이 여러 가지라면 선택지를 제시하고, 조용히 하나를 고르지 않는다.
- 더 단순한 방법이 있으면 말한다. 필요하면 반론을 제기한다.
- 이해되지 않는 부분이 있으면 멈추고, 무엇이 불분명한지 짚은 후 묻는다.

### 2. 단순함 우선

요청한 것만 구현한다.

- 요청하지 않은 기능 추가 금지
- 단일 사용 코드에 추상화 금지
- 요청하지 않은 유연성·설정 가능성 추가 금지
- 불가능한 시나리오에 대한 에러 핸들링 금지
- 200줄로 쓴 코드가 50줄로 가능하다면 다시 쓴다

### 3. 외과적 변경

건드려야 할 것만 건드린다.

- 관련 없는 코드·주석·포맷 개선 금지
- 고장나지 않은 것 리팩토링 금지
- 기존 스타일이 다르더라도 맞춘다
- 관련 없는 dead code를 발견하면 언급만 하고 삭제하지 않는다
- 내 변경으로 인해 생긴 미사용 import·변수·함수는 직접 제거한다

변경된 모든 줄은 요청 사항으로 직접 추적 가능해야 한다.

### 4. 목표 기반 실행

성공 기준을 정의하고, 검증될 때까지 반복한다.

- "버그 수정" → 재현 테스트 작성 후 통과시킨다
- "리팩토링" → 전후 테스트가 동일하게 통과해야 한다

여러 단계 작업이면 간략한 계획을 먼저 제시한다:
```
1. [단계] → 검증: [확인 방법]
2. [단계] → 검증: [확인 방법]
```

---

## 기술 스택

| 항목 | 버전 |
|------|------|
| Java | 15 |
| Spring Boot | 2.7.18 |
| Spring Security | 5.7 (Boot 2.7 내장) |
| JJWT | 0.12.3 |
| Lombok | Boot 2.7 관리 버전 |
| 빌드 도구 | Maven |

## 주요 커맨드

```bash
mvn test              # 전체 테스트 실행
mvn spring-boot:run   # 서버 실행 (포트 3000)
mvn compile           # 컴파일만
```

---

## Java 15 제약 사항

- `record` — Lombok `@Data` / `@Value`로 대체
- `Stream.toList()` — `.collect(Collectors.toList())` 사용
- `jakarta.servlet.*` — `javax.servlet.*` 사용 (Spring Boot 2.x)

---

## 코드 컨벤션

### 계층 구조
- **Controller** — HTTP 요청/응답만 담당. 비즈니스 로직 없음
- **Service** — 비즈니스 로직. VO로 입력값 검증 후 처리
- **Repository** — 저장소 접근만 담당
- **VO (Value Object)** — 생성자에서 검증, 실패 시 즉시 `ApiException` throw. DTO·Model에는 적용하지 않음
- **DTO** — `@Data` + 검증 로직 없음. 요청/응답 형태만 정의
- **Model** — `@Data`. DB 엔티티에 해당하는 순수 데이터 객체

### 의존성 주입
생성자 주입만 사용. `@Autowired` 필드 주입 사용 안 함.

### 예외 처리
- 비즈니스 예외는 `ApiException(HttpStatus, message)` 사용
- `GlobalExceptionHandler`에서 일괄 처리
- 예상치 못한 예외는 로그 출력 후 500 반환

### 컬렉션 반환
응답 JSON에 컬렉션을 직접 반환하지 않고 항상 래핑.
```java
return ResponseEntity.ok(Map.of("posts", list));   // O
return ResponseEntity.ok(list);                    // X
```

### 순환 의존 방지
빈 간 순환 의존이 생길 경우 별도 `@Configuration` 클래스로 분리.

---

## 테스트 컨벤션

- `@SpringBootTest` + `@AutoConfigureMockMvc` 통합 테스트
- 각 테스트 클래스는 `@BeforeEach`에서 저장소 `clear()` 호출해 격리
- 테스트 간 공유 상태 없음
- 메서드명: `대상_상황` 형태 (예: `register_duplicateLoginId`)

---

## 패키지 구조

```
src/main/java/com/loopers/server/
├── config/       # Spring 설정 클래스
├── controller/   # REST 컨트롤러
├── service/      # 비즈니스 로직
├── repository/   # 데이터 접근
├── model/        # 도메인 모델
├── dto/          # 요청·응답 DTO
├── vo/           # 값 객체 (입력 검증)
├── security/     # 인증 필터·JWT·UserPrincipal
├── exception/    # ApiException, GlobalExceptionHandler
└── util/         # 순수 유틸리티 (상태 없음, static 메서드)
```
