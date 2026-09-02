# CLAUDE.md

TMDB 데이터 기반 **영화 추천 백엔드**(HYMV). Spring Boot 3.5.5 / Java 17.

## 빌드 · 실행 · 테스트

```bash
./gradlew build       # 빌드 (컴파일 + 테스트)
./gradlew test        # 전체 테스트
./gradlew test --tests "Recommend.Movie.LikeMovie.Service.LikeMovieServiceTest"   # 단일 클래스
./gradlew bootRun     # 로컬 실행
```

- Windows 셸에서는 `gradlew.bat` 사용.
- 실행 설정은 `src/main/resources/application.properties` / `application-dev.properties`,
  비밀값은 `env.properties`(gitignore).

## 아키텍처 한눈에

루트 패키지 `Recommend.Movie`. **도메인별 패키지 + 도메인 내부 레이어드** 구조.

```
Recommend.Movie
├── <Feature>/  (Biorhythm, LikeMovie, Movies, Tmdb, User)
│   └── Controller / Service / Repository / Domain / Dto / Converter / Handler
├── Config/     (Security, Redis, DataSource×2, Batch, Exception)
└── Util/       (JWTUtil, BatchGuard)
```

- 의존 방향: `Controller → Service → Repository → Domain` (단방향).
- **이중 데이터소스**: 도메인 data DB / Spring Batch 메타 meta DB 분리.
- 인증: Spring Security + OAuth2 + JWT. `Principal.getName()` = userId.
- 캐시: Redis (원본 변경 시 캐시 무효화 필수).
- 문서화: springdoc Swagger.

## 컨벤션은 `.claude/skills/` 를 따른다

코드를 쓰기 전에 해당 스킬을 로드한다. 규약은 실제 코드에서 관찰된 것이며 신규 코드도 이를 따른다.

| 스킬 | 범위 |
|---|---|
| `architecture-conventions` | 패키지 구조, 의존 방향, 이중 데이터소스·배치·캐시 경계 |
| `global-conventions` | 네이밍, 예외(BusinessException/ErrorCode/GlobalExceptionHandler), Converter, DI, Lombok, DTO, 커밋 |
| `persistence-conventions` | JPA 엔티티/연관관계/리포지토리/트랜잭션/Redis |
| `api-conventions` | 컨트롤러, ResponseEntity, Swagger, Principal 인증 |
| `testing-conventions` | JUnit5 + Mockito + AssertJ 서비스 단위 테스트 |

### 놓치기 쉬운 핵심

- 비즈니스 예외는 `throw new BusinessException(ErrorCode.XXX, "한글 메시지")` 로만. 컨트롤러에서 에러 응답 직접 조립 금지.
- 엔티티는 `@Setter` 없이, enum 은 `@Enumerated(EnumType.STRING)`, `@ManyToOne(fetch=LAZY)`, snake_case 컬럼.
- 엔티티를 응답으로 직접 노출하지 않고 응답 DTO 로 변환(Converter static 유틸).
- 생성자 주입만. 로깅은 `@Slf4j`. 메시지·주석·Swagger 설명은 한국어.

## 에이전트 · 커맨드

- **`spring-developer`** — 기능 구현/수정 (write-owner, 컨벤션 스킬 로드).
- **`code-reviewer`** — 변경분 리뷰 (read-only).
- **`/codereview`** — code-reviewer 로 현재 변경분 리뷰.

## 커밋

형식 `type : 한글 설명` (feat / refactor / Test / docs / fix).
커밋·푸시는 **사용자가 명시적으로 요청할 때만** 한다.
