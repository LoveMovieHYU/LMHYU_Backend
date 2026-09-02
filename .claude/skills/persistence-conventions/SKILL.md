---
name: persistence-conventions
description: 이 프로젝트의 영속성 규약 — JPA 엔티티 작성, 연관관계, 리포지토리, 이중 데이터소스, 트랜잭션, Redis 캐시. 엔티티/Repository 를 만들거나 DB 접근 코드를 짤 때 사용한다. 패키지 배치는 architecture-conventions, Lombok·네이밍·예외는 global-conventions, 배치 잡 구조는 architecture-conventions 로 라우팅한다.
---

# 영속성 컨벤션

## JPA 엔티티

`Domain/` 패키지에 두고, Lombok 조합과 명시적 컬럼 매핑을 지킨다.

```java
@Entity(name = "liked_movie")
@Builder
@NoArgsConstructor
@Getter
@AllArgsConstructor
public class LikedMovie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "reaction_type")
    @Enumerated(EnumType.STRING)
    private ReactionType reactionType;
}
```

규칙:

- **어노테이션 조합**: `@Entity(name="snake_case")` + `@Builder` + `@NoArgsConstructor` + `@Getter` + `@AllArgsConstructor`.
  엔티티에 `@Setter` 는 붙이지 않는다 (상태 변경은 도메인 메서드로 — 아래 참조).
- **테이블/컬럼명은 snake_case** 로 `@Entity(name=...)`·`@Column(name=...)` 에 명시한다. 필드명은 camelCase.
- **ID 생성 전략**은 `GenerationType.IDENTITY` (MySQL auto_increment).
- **enum 필드**는 `@Enumerated(EnumType.STRING)` 로 문자열 저장한다. ORDINAL 금지.

## 연관관계

- 연관관계 fetch 는 **`FetchType.LAZY`** 를 기본으로 한다. `@ManyToOne` 은 기본이 EAGER 이므로 명시적으로 LAZY 로 지정한다.
- 조인 컬럼은 `@JoinColumn(name="snake_case")` 로 명시한다. 참조 컬럼이 PK 가 아니면 `referencedColumnName` 도 지정한다.
- **연관관계는 단방향을 선호한다.** (성능 개선 과정에서 Movie 엔티티가 양방향 → 단방향으로 정리된 이력이 있다.)
  양방향이 꼭 필요할 때만 도입하고, 연관관계 편의 메서드로 양쪽 정합성을 맞춘다
  (예: `User.addLikeMovie(...)` / `removeLikeMovie(...)`).
- 컬렉션을 순회·정렬할 때는 애플리케이션 단에서 `stream().sorted(...)` 등을 쓰되, N+1 가능성을 의식한다.

## 상태 변경

- 엔티티 상태 변경은 무분별한 `@Setter` 대신 **의미 있는 도메인 메서드**로 표현한다
  (예: `user.addLikeMovie(likedMovie)`).

## 리포지토리

- `Repository/` 패키지에 두고 `JpaRepository` 를 상속한 인터페이스로 작성한다.
- 조회 메서드는 Spring Data 파생 쿼리 네이밍을 쓴다 — `findByTmdbId`, `findByUserAndMovie`, `findByUserId`.
- 반환 타입: 단건 조회는 `Optional<T>` 를 선호하고, Service 에서 `.orElseThrow(() -> new BusinessException(...))` 로 처리한다.
  (일부 레거시는 `null` 을 반환하기도 하니, 그 경우 Service 에서 null 체크 후 `BusinessException` 을 던진다.)

## 트랜잭션

- 쓰기(저장/삭제/수정)가 있는 Service 메서드에는 `@Transactional` 을 붙인다.
  (기존 코드는 `jakarta.transaction.Transactional` 을 사용 중이다. 새로 추가할 때 같은 애노테이션을 쓰거나
  세밀한 읽기전용 최적화가 필요하면 `org.springframework.transaction.annotation.Transactional(readOnly=true)` 를 검토한다.)
- 도메인 로직은 **data DB**(기본/`@Primary` 트랜잭션 매니저)를 사용한다.
- **배치 메타테이블은 meta DB**(`batchTransactionManager`)로 분리되어 있다. 도메인 트랜잭션과 섞지 않는다
  (architecture-conventions 참고).

## Redis 캐시

- `Config/RedisConfig` 설정을 사용한다.
- **캐시 무효화**: 캐시 원본 데이터가 바뀌면 관련 캐시를 삭제한다
  (예: 유저 생년월일 수정 시 바이오리듬 추천 캐시 삭제). 캐시 도입 시 무효화 지점을 반드시 함께 만든다.

## 하지 말 것

- 엔티티를 컨트롤러 응답으로 직접 노출하지 않는다 (응답 DTO 로 변환 — global-conventions).
- `@Enumerated` 없이 enum 을 저장하거나 ORDINAL 로 저장하지 않는다.
- `@ManyToOne` 을 LAZY 지정 없이 두지 않는다.
