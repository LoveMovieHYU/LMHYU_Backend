---
name: architecture-conventions
description: 이 프로젝트의 아키텍처·패키지 구조·의존 방향·모듈 경계 규칙. 새 도메인/기능을 어디에 만들지, 레이어 간 호출 방향, 이중 데이터소스·배치 경계를 정할 때 사용한다. 네이밍·예외·DTO 등 전역 코드 규약은 global-conventions, JPA 엔티티·리포지토리·트랜잭션은 persistence-conventions, 컨트롤러·Swagger·응답은 api-conventions, 테스트는 testing-conventions 로 라우팅한다.
---

# 아키텍처 컨벤션

## 패키지 구조 — 도메인별 패키지 + 도메인 내부 레이어드

루트 패키지는 `Recommend.Movie` 다. 그 아래 **기능(도메인) 단위**로 패키지를 나누고,
각 도메인 안을 레이어별 하위 패키지로 다시 나눈다.

```
Recommend.Movie
├── <Feature>/                  ← 도메인 (Biorhythm, LikeMovie, Movies, Tmdb, User …)
│   ├── Controller/             ← REST 진입점 (@RestController)
│   ├── Service/                ← 비즈니스 로직 (@Service)
│   ├── Repository/             ← JPA 리포지토리 (@Repository / interface)
│   ├── Domain/                 ← JPA 엔티티, enum
│   ├── Dto/                    ← 요청/응답 DTO
│   ├── Converter/              ← 엔티티 ↔ DTO 변환 (static 유틸 클래스)
│   └── Handler/                ← 도메인 전용 핸들러 (예: OAuth2 성공 핸들러)
├── Config/                     ← 전역 설정 (Security, Redis, DataSource, Batch, Exception …)
│   ├── Batch/                  ← Spring Batch Job/Step/Reader/Listener
│   └── Exception/              ← BusinessException, ErrorCode, GlobalExceptionHandler
└── Util/                       ← 공통 유틸 (JWTUtil, BatchGuard …)
```

- 새 기능은 **새 도메인 패키지**를 만들고 그 안에 위 레이어 하위 패키지를 둔다.
  모든 레이어가 항상 필요한 건 아니다 — 실제로 쓰는 레이어만 만든다
  (예: 순수 조회 도메인은 `Repository` 없이 다른 도메인 리포지토리를 재사용하기도 함).
- 전역 설정·부트스트랩성 코드는 도메인이 아니라 `Config/` 에 둔다.
- 특정 도메인에 속하지 않는 순수 유틸은 `Util/` 에 둔다.

## 의존 방향

- `Controller → Service → Repository → Domain` 의 단방향 흐름을 지킨다.
  Controller 는 Repository 를 직접 호출하지 않는다 (Service 경유).
- Converter 는 Service/Controller 에서 호출하는 **정적 유틸**이며 스프링 빈이 아니다.
- 도메인 간 참조는 허용된다 (예: `LikeMovie.Service` 가 `Tmdb.Repository`·`User.Repository` 를 사용).
  단, 참조는 **Service 또는 Repository/Domain** 를 향하게 하고, 다른 도메인의 Controller 는 호출하지 않는다.
- `Config`·`Util` 은 어느 도메인에서든 참조 가능한 공용 계층이다.

## 이중 데이터소스 경계

이 프로젝트는 **두 개의 데이터소스**를 쓴다. 새 코드가 어느 쪽에 붙는지 항상 의식한다.

- **data DB** (`spring.datasource-data`, `DataDBConfig`) — 도메인 엔티티(JPA)의 기본 데이터소스.
  일반적인 도메인 CRUD 는 전부 여기에 붙는다. `@Primary` JPA 트랜잭션 매니저를 쓴다.
- **meta DB** (`spring.datasource-meta`, `MetaDBConfig`) — Spring Batch 메타테이블 전용.
  `@BatchDataSource` + `batchTransactionManager`(DataSourceTransactionManager) 로 분리한다.
- 배치 메타 저장소를 도메인 로직에서 직접 건드리지 않는다. 반대로 배치 Step 의 실제 적재는 data DB 로 간다.

## 배치 경계

- 배치 관련 코드는 `Config/Batch/` 에 모은다 (`TmdbBatch`, `TmdbDiscoverItemReader`, `TmdbJobListener`,
  `Runner/TmdbBatchRunner`).
- 재실행 안전성은 `Util/BatchGuard` 로 보장한다 — 같은 `jobName` + `seedVersion` 으로 COMPLETED 이력이
  있으면 재적재를 건너뛴다. 배치를 새로 추가할 때 이 멱등성 패턴을 따른다.
- 배치 상세 규약(청크·커스텀 리더·성능)은 코드가 확장되면 별도 문서로 분리 검토한다.

## 캐시 경계

- 자주 조회되는 데이터는 Redis(`Config/RedisConfig`)로 캐싱한다.
- **캐시 무효화 규칙**: 캐시의 원본이 되는 데이터가 변경되면 관련 Redis 캐시를 함께 삭제한다
  (예: 유저 생년월일 변경 시 바이오리듬 추천 캐시 삭제). 캐시를 새로 도입할 때 무효화 지점을 함께 설계한다.

## 하지 말 것

- 도메인 패키지 밖(루트 `Recommend.Movie` 직하)에 도메인 클래스를 흩뿌리지 않는다 (`MovieApplication` 제외).
- 레이어를 건너뛰지 않는다 (Controller 에서 Repository 직접 호출 금지).
- 배치 meta DB 와 도메인 data DB 를 섞지 않는다.
