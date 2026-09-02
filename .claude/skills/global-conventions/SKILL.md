---
name: global-conventions
description: 이 프로젝트의 전역 코드 규약 — 네이밍, 예외 처리(BusinessException/ErrorCode/GlobalExceptionHandler), Converter 패턴, 의존성 주입, Lombok 사용, DTO 규칙, 로깅, 한글 메시지, 커밋 메시지. 코드를 쓰기 전에 거의 항상 참고한다. 패키지 배치·의존 방향은 architecture-conventions, 엔티티/리포지토리는 persistence-conventions, 컨트롤러/Swagger/응답은 api-conventions, 테스트는 testing-conventions 로 라우팅한다.
---

# 전역 컨벤션

## 네이밍

- **클래스 접미사**: `~Controller`, `~Service`, `~Repository`, `~Converter`, `~Config`, `~Handler`.
- **DTO 접미사는 대문자 `DTO`** — `MovieReactionRequestDTO`, `HomeResponseDTO`, `ErrorDTO`.
  (일부 초기 파일에 `~Dto` 도 있으나 신규 코드는 대문자 `DTO` 로 통일한다.)
- 엔티티는 접미사 없는 명사형 클래스명 — `Movie`, `User`, `LikedMovie`, `Genre`.
- enum 은 명사형 — `ReactionType`, `UserRoleType`, `SocialProviderType`, `Gender`.
- 패키지명은 첫 글자 대문자 관습을 따른다 (`Controller`, `Service`, `Domain` …) — 기존 구조를 유지한다.

## 예외 처리 — 반드시 이 패턴을 따른다

비즈니스 예외는 직접 `RuntimeException` 을 던지지 말고 **`BusinessException`** 을 던진다.

```java
throw new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "해당 영화가 존재하지 않습니다.");
```

- **`ErrorCode`** (`Config/Exception/ErrorCode`) 는 `code`(String) + `httpStatus`(HttpStatus) 를 가진 enum.
  새 에러 상황은 여기에 상수를 추가한다. 카테고리 주석(auth / validation / service)에 맞춰 배치한다.
- **`BusinessException(ErrorCode, message)`** — 두 인자 생성자. 메시지는 한글로, 사용자에게 노출 가능한 문구.
- 예외 → HTTP 응답 변환은 **`GlobalExceptionHandler`**(`@RestControllerAdvice`)가 전담한다.
  개별 컨트롤러에서 try/catch 로 응답을 만들지 않는다.
- 응답 본문은 항상 **`ErrorDTO`**(`code`, `message`, `errors: List<ErrorDetail>`) 형태.
- 알 수 없는 예외(fallback)는 메시지를 클라이언트에 노출하지 않고 `"서버 에러가 발생했습니다."` 로 응답하며,
  서버에는 `log.error` 로 남긴다.
- 새 프레임워크 예외(바인딩/검증 등)를 다뤄야 하면 개별 컨트롤러가 아니라 `GlobalExceptionHandler` 에 핸들러를 추가한다.

## Converter 패턴

엔티티 ↔ DTO 변환은 **static 메서드만 가진 유틸 클래스**로 분리한다. 스프링 빈이 아니다.

```java
public class LikeMovieConverter {
    public static LikedMovie toEntity(RequestDTO dto, User user, Movie movie) { ... }
    public static ResponseDTO toDTO(Movie movie) { ... }
}
```

- 변환 메서드는 `toEntity(...)` / `toDTO(...)` 네이밍을 따른다.
- Service 에서 이 static 메서드를 호출해 변환한다. Service 안에 인라인으로 매핑 로직을 흩뿌리지 않는다.

## 의존성 주입

- **생성자 주입만** 사용한다. 필드 `@Autowired` 를 쓰지 않는다.
- 명시적 생성자를 직접 쓰거나(예: `LikeMovieController`), Lombok `@RequiredArgsConstructor` 로 `final` 필드를 주입한다(예: `BatchGuard`). 둘 다 허용되며, 협력 필드는 `private final` 로 둔다.

## Lombok

- 엔티티: `@Getter` + `@Builder` + `@NoArgsConstructor` + `@AllArgsConstructor` 조합(persistence-conventions 참고).
- DTO: `@Getter` (+ 요청 DTO 는 필요 시 `@Setter`), 빌더가 필요하면 `@Builder`.
- 로깅: `@Slf4j` 를 붙이고 `log.info/warn/error` 를 사용한다. `System.out` 을 쓰지 않는다.
- `@Setter` 는 요청 바인딩 DTO 등 꼭 필요한 곳에만 제한적으로 쓴다. 엔티티에 무분별한 `@Setter` 금지.

## DTO 규칙

- 요청 DTO 와 응답 DTO 를 분리한다 (`~RequestDTO`, `~ResponseDTO`).
- DTO 에는 Swagger `@Schema(description=..., example=...)` 를 붙여 문서화한다(api-conventions 참고).
- 엔티티를 컨트롤러 응답으로 그대로 노출하지 말고 응답 DTO 로 변환해 내보낸다.

## 언어

- 사용자 노출 메시지, 코드 주석, Swagger 설명, 커밋 메시지 모두 **한국어**로 쓴다 (기존 코드베이스 관습).

## 커밋 메시지

형식: `type : 한글 설명` (콜론 양쪽 공백). 관찰된 타입:

- `feat` — 새 기능
- `refeact` — 리팩토링 (프로젝트 관습상 오타를 유지하고 있으나, 신규 커밋은 표준 `refactor` 사용을 권장)
- `Test` — 테스트 코드
- `docs` — 문서
- `fix` — 버그 수정

사용자가 명시적으로 요청할 때만 커밋한다.
