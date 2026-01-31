Markdown# 🎬 HYMV (Here is Your Movie)
> **AI 기반 개인화 영화 추천 플랫폼** > 사용자의 취향을 분석하여 TMDB 데이터를 기반으로 최적의 영화를 추천해주는 서비스입니다.

<br>

## 👥 팀원 소개 (Team Members)

|     Role     | Name | Responsibilities                                                     |
|:------------:|:---:|:---------------------------------------------------------------------|
|    **PM**    | **조수빈** | 프로젝트 기획 및 일정 관리, 와이어프레임 설계                                           |
|  **Design**  | **최여진** | UI/UX 디자인, 프로토타이핑, 디자인 시스템 구축                                        |
| **Frontend** | **김의재** | 웹 클라이언트 개발, 사용자 인터랙션 구현                                              |
| **Frontend** | **권용진** | 웹 클라이언트 개발, 화면 구조 설계                                                 |
| **Backend**  | **이현우** | 서버 API 개발, **Batch 시스템 구축(TMDB)**, DB 설계 <br/>, CI/CD 파이프라인, Docker 환경 구성 |
|    **AI**    | **최지환** | 추천 알고리즘 모델링, AI 모델 서빙 (FastAPI)                                      |
|  **Cloud**   | **권용진** | 클라우드(Oracle/AWS) 인프라 구축                                              |

<br>

## 🛠 기술 스택 (Tech Stack)

### Backend
![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring Batch](https://img.shields.io/badge/Spring_Batch-5.x-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![JPA](https://img.shields.io/badge/Spring_Data_JPA-Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white)
![QueryDSL](https://img.shields.io/badge/QueryDSL-5.0-007ACC?style=for-the-badge)

### Frontend (Android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)

### AI & Data
![Python](https://img.shields.io/badge/Python-3.9-3776AB?style=for-the-badge&logo=python&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-0.95-009688?style=for-the-badge&logo=fastapi&logoColor=white)
![TensorFlow](https://img.shields.io/badge/TensorFlow-2.12-FF6F00?style=for-the-badge&logo=tensorflow&logoColor=white)

### Database & Cache
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![MariaDB](https://img.shields.io/badge/MariaDB-10.11-003545?style=for-the-badge&logo=mariadb&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7.0-DC382D?style=for-the-badge&logo=redis&logoColor=white)

### Infrastructure
![Docker](https://img.shields.io/badge/Docker-24.0-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Oracle Cloud](https://img.shields.io/badge/Oracle_Cloud-OCI-F80000?style=for-the-badge&logo=oracle&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-1.24-009639?style=for-the-badge&logo=nginx&logoColor=white)

<br>

## 💡 주요 기능 (Key Features)

### 1. 영화 데이터 수집 (Spring Batch)
- **TMDB API**를 활용하여 1990년부터 최신 영화까지 데이터를 수집합니다.
- `Job`, `Step`, `Chunk` 지향 처리를 통해 대용량 데이터를 안정적으로 DB에 적재합니다.
- **Custom ItemReader**를 구현하여 페이징 방식으로 API 부하를 분산하고 메모리 효율을 최적화했습니다.

### 2. 사용자 맞춤 추천 (AI)
- 사용자의 활동 로그와 취향 데이터를 기반으로 AI 모델이 영화를 추천합니다.
- 협업 필터링(Collaborative Filtering) 및 콘텐츠 기반 필터링(Content-based Filtering) 적용.

### 3. 소셜 로그인 (OAuth2)
- Google, Naver 소셜 로그인을 지원하여 간편하게 가입할 수 있습니다.
- JWT(Access/Refresh Token) 기반의 보안 인증 시스템을 구축했습니다.

### 4. 고성능 캐싱
- 자주 조회되는 영화 정보나 추천 리스트는 **Redis**를 통해 캐싱하여 응답 속도를 극대화했습니다.

<br>

## 🏛 시스템 아키텍처 (System Architecture)

![시스템아키텍처.png](%EC%8B%9C%EC%8A%A4%ED%85%9C%EC%95%84%ED%82%A4%ED%85%8D%EC%B2%98.png)