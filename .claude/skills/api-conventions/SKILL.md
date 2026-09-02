---
name: api-conventions
description: 이 프로젝트의 REST API 규약 — 컨트롤러 작성, 라우팅, ResponseEntity 응답, Swagger(springdoc) 문서화, 인증 처리(Principal), 에러 응답. 컨트롤러/엔드포인트를 만들거나 수정할 때 사용한다. 예외/ErrorCode/DTO 규칙은 global-conventions, Service/엔티티는 각각 architecture-conventions·persistence-conventions 로 라우팅한다.
---

# API 컨벤션

## 컨트롤러

```java
@Tag(name = "좋아하는 영화", description = "추천된 영화 좋아요")
@RestController
@RequestMapping("/api/likes")
public class LikeMovieController {

    private final LikeMovieService likeMovieService;

    public LikeMovieController(LikeMovieService likeMovieService) {
        this.likeMovieService = likeMovieService;
    }
}
```

- `@RestController` + 클래스 레벨 `@RequestMapping("/api/...")`.
- **모든 엔드포인트 경로는 `/api/` 로 시작**한다.
- 생성자 주입(생성자 명시 또는 `@RequiredArgsConstructor`), 협력 객체는 `private final`.
- 컨트롤러는 얇게 유지 — 검증·인증 가드 정도만 하고 비즈니스 로직은 Service 로 위임한다.

## 응답

- 반환 타입은 **`ResponseEntity<T>`**. 성공은 `ResponseEntity.ok(body)`.
- 단순 결과 문자열(예: `"성공했습니다."`)을 Service 가 반환하면 그대로 `ResponseEntity.ok(...)` 로 감싼다.
- 목록은 응답 DTO 리스트(`List<~ResponseDTO>`)로 반환한다. 엔티티를 직접 노출하지 않는다.
- 에러 응답은 컨트롤러에서 만들지 않는다 — `BusinessException` 을 던지면 `GlobalExceptionHandler` 가
  `ErrorDTO` 로 변환한다(global-conventions 참고).

## 인증 / 인가

- 인증 주체는 `java.security.Principal` 파라미터로 받는다.
- **`principal.getName()` 은 userId(문자열)** 다. Service 로 이 값을 넘겨 유저를 조회한다.
- 로그인 필수 엔드포인트는 컨트롤러 진입부에서 가드한다:

  ```java
  if (principal == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
  }
  ```

- 인증/보안 정책 자체는 `Config/SecurityConfig`, `Config/JWTFilter`, `Util/JWTUtil`,
  `User/Handler/OAuth2LoginSuccessHandler` 에서 관리한다. 컨트롤러에서 토큰을 직접 파싱하지 않는다.

## Swagger 문서화 (springdoc)

컨트롤러/엔드포인트/DTO 에 문서 어노테이션을 붙인다. 한국어로 작성한다.

- 클래스: `@Tag(name=..., description=...)`
- 메서드: `@Operation(summary=..., description=...)`
- 응답 코드: `@ApiResponses({ @ApiResponse(responseCode="200", description="..."), ... })`
  — 실제로 발생 가능한 상태코드(200/401/404 등)를 명시한다.
- 파라미터: `@Parameter(description=..., example=...)`
- DTO 필드: `@Schema(description=..., example=...)`

## HTTP 메서드 / 상태코드 매핑

- 조회 `GET`, 생성/저장 `POST`, 삭제 `DELETE`, 수정 `PUT/PATCH`.
- 리소스 식별자는 `@PathVariable`(예: `/{tmdbId}`), 필터/옵션은 `@RequestParam`, 본문은 `@RequestBody`.
- 상태코드는 `ErrorCode` 의 `httpStatus` 를 통해 일관되게 매핑된다 — 새 에러 상황은 `ErrorCode` 에 추가한다.

## 하지 말 것

- 컨트롤러에서 try/catch 로 에러 응답을 직접 조립하지 않는다.
- 엔티티를 응답 본문으로 그대로 내보내지 않는다.
- 경로에 `/api/` 접두사를 빠뜨리지 않는다.
