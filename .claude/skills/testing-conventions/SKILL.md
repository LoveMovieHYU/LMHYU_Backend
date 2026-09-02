---
name: testing-conventions
description: 이 프로젝트의 테스트 규약 — JUnit5 + Mockito + AssertJ 기반 서비스 단위 테스트 작성, 실행 커맨드. 테스트 코드를 만들거나 고칠 때 사용한다. 대상 코드의 구조·규칙은 architecture-conventions·global-conventions·persistence-conventions·api-conventions 로 라우팅한다.
---

# 테스트 컨벤션

## 스택 / 위치

- **JUnit 5**(`org.junit.jupiter`) + **Mockito** + **AssertJ**. `spring-boot-starter-test` 에 포함.
- 테스트는 대상과 **동일한 패키지 경로**에 둔다: `src/test/java/Recommend/Movie/<Feature>/Service/...`.
- 파일명은 `<대상클래스>Test` (예: `LikeMovieServiceTest`).
- 현재 테스트는 **서비스 레이어 비즈니스 로직 단위 테스트** 중심이다.

## 서비스 단위 테스트 골격

스프링 컨텍스트를 띄우지 않고 Mockito 로 의존성을 목킹한다.

```java
@ExtendWith(MockitoExtension.class)
class LikeMovieServiceTest {

    @InjectMocks
    private LikeMovieService likeMovieService;

    @Mock
    private LikeMovieRepository likeMovieRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MovieRepository movieRepository;

    private User testUser;
    private Movie testMovie;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setLikedMovieList(new ArrayList<>());
        testMovie = Movie.builder().tmdbId(100L).title("Test Movie").build();
    }

    @Test
    @DisplayName("존재하지 않는 영화면 MOVIE_NOT_FOUND 예외를 던진다")
    void saveReaction_movieNotFound() {
        when(movieRepository.findByTmdbId(anyLong())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> likeMovieService.saveMovieReaction(100L, requestDTO, "1"));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.MOVIE_NOT_FOUND);
    }
}
```

규칙:

- `@ExtendWith(MockitoExtension.class)` + `@InjectMocks`(대상) + `@Mock`(의존성).
- 공통 픽스처는 `@BeforeEach setUp()` 에서 준비한다. 엔티티는 빌더로 생성한다.
- **테스트 이름은 `@DisplayName` 에 한국어**로 무엇을 검증하는지 서술한다.
- 단언은 **AssertJ `assertThat(...)`** 를 기본으로 쓴다. 예외는 `assertThrows(...)`.
- **예외 검증은 타입뿐 아니라 `ErrorCode` 까지 확인**한다 — `assertThat(ex.getCode()).isEqualTo(ErrorCode.XXX)`.
- 상호작용 검증은 `verify(mock, times(n)).method(...)`, 스텁은 `when(...).thenReturn(...)`.
- 인자 매처는 `any()`, `anyLong()` 등 `org.mockito.ArgumentMatchers` 를 사용한다.

## 무엇을 테스트하나

- Service 의 **분기와 예외 경로**를 우선 커버한다 (정상 경로 + not-found/unauthorized 등 예외 경로).
- 저장/삭제 시 리포지토리 호출 여부(`verify`)와 연관관계 편의 메서드 반영을 확인한다.
- 외부 API(TMDB)·Redis·실제 DB 에 의존하지 않도록 목킹한다. 새 통합 테스트가 필요하면 별도로 분리한다.

## 실행

```bash
./gradlew test          # 전체 테스트
./gradlew test --tests "Recommend.Movie.LikeMovie.Service.LikeMovieServiceTest"   # 단일 클래스
```

- JUnit Platform(`useJUnitPlatform()`)으로 실행된다.
- 코드 변경 후에는 관련 테스트를 돌려 통과를 확인한다.

## 하지 말 것

- 서비스 단위 테스트에서 `@SpringBootTest` 로 전체 컨텍스트를 띄우지 않는다 (필요한 통합 테스트만 예외).
- 예외 테스트에서 타입만 보고 `ErrorCode` 검증을 생략하지 않는다.
- 실제 외부 API/DB/Redis 를 호출하는 단위 테스트를 만들지 않는다.
