package Recommend.Movie.Config.Batch;

import Recommend.Movie.Tmdb.Dto.CompanyDTO;
import Recommend.Movie.Tmdb.Dto.CreditsPeople;
import Recommend.Movie.Tmdb.Dto.CreditsResponse;
import Recommend.Movie.Tmdb.Dto.GenreDTO;
import Recommend.Movie.Tmdb.Dto.MovieDetailDTO;
import Recommend.Movie.Tmdb.Dto.PeopleDetailDTO;
import Recommend.Movie.Tmdb.Dto.WorkItem;
import Recommend.Movie.Tmdb.Service.PeopleService;
import Recommend.Movie.Tmdb.Service.TmdbService;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Configuration
@Slf4j
@RequiredArgsConstructor
public class TmdbBatch {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TmdbService tmdbService;
    private final PeopleService peopleService;

    @Bean
    public Job tmdbJob(Step tmdbStep){
        return new JobBuilder("tmdbJob", jobRepository)
                .listener(new TmdbJobListener())
                .start(tmdbStep)
                .build();
    }

    @Bean
    public Step tmdbStep(ItemReader<WorkItem> tmdbReader,
                         ItemProcessor<WorkItem, MovieBatchItem> tmdbProcessor,
                         ItemWriter<MovieBatchItem> tmdbWriter){
        return new StepBuilder("tmdbStep", jobRepository)
                .<WorkItem, MovieBatchItem>chunk(50, transactionManager)
                .reader(tmdbReader)
                .processor(tmdbProcessor)
                .writer(tmdbWriter)
                .faultTolerant()
                .retry(org.springframework.web.client.ResourceAccessException.class)
                .retry(java.net.SocketTimeoutException.class)
                .retryLimit(3)
                .backOffPolicy(new FixedBackOffPolicy() {{ setBackOffPeriod(2000L); }})
                // skip 대상을 실제로 건너뛰어도 되는 예외로 한정한다.
                // 개별 영화의 TMDB 4xx 응답(HttpClientErrorException)과
                // 파싱/데이터 오류(IllegalArgumentException)만 건너뛰고,
                // 그 외 SQL 오류·버그 등은 조용히 삼키지 않고 Step 을 실패시킨다.
                .skip(org.springframework.web.client.HttpClientErrorException.class)
                .skip(IllegalArgumentException.class)
                .skipLimit(100)
                .listener(new TmdbSkipListener())
                .build();
    }

    @Bean
    @StepScope
    public TmdbDiscoverItemReader tmdbReader(
            @Value("#{jobParameters['includeAdult']}") String includeAdult,
            @Value("${tmdb.batch.start-year:2000}") int startYear,
            @Value("${tmdb.batch.end-year:0}") int endYear
    ){
        boolean incAdult = Boolean.parseBoolean(includeAdult);

        // endYear 가 0 이면 리더에서 현재 연도를 종료 연도로 사용한다.
        return new TmdbDiscoverItemReader(tmdbService, startYear, endYear, incAdult);
    }

    @Bean
    @StepScope
    public ItemProcessor<WorkItem, MovieBatchItem> tmdbProcessor(){
        return item -> {
            int movieId = item.getMovieId();
            log.debug("[MovieBatch] Fetch movie detail. movieId={}", movieId);

            MovieDetailDTO detailDTO = tmdbService.fetchMovieDetailOnly(movieId);
            if (detailDTO == null) {
                return null;
            }

            if (detailDTO.getPopularity() != null && detailDTO.getPopularity() <= 1.0) {
                log.debug("[MovieBatch] Filtered by popularity. movieId={}, title={}, popularity={}",
                        movieId, detailDTO.getTitle(), detailDTO.getPopularity());
                return null;
            }

            CreditsResponse credits = peopleService.fetchCreditsOnly(detailDTO.getTmdbId());
            return MovieBatchItem.from(detailDTO, toPeopleBatchItems(detailDTO.getTmdbId(), credits));
        };
    }

    @Bean
    public ItemWriter<MovieBatchItem> tmdbWriter(
            @Qualifier("dataDBSource") DataSource dataSource
    ){
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        return items -> writeChunk(jdbcTemplate, items);
    }

    private void writeChunk(NamedParameterJdbcTemplate jdbcTemplate, Chunk<? extends MovieBatchItem> chunk) {
        List<? extends MovieBatchItem> movies = chunk.getItems();
        if (movies.isEmpty()) {
            return;
        }

        batchUpdate(jdbcTemplate, MOVIE_UPSERT_SQL, movies.stream()
                .map(this::movieParams)
                .toList());
        batchUpdate(jdbcTemplate, GENRE_UPSERT_SQL, uniqueGenres(movies).stream()
                .map(this::genreParams)
                .toList());
        batchUpdate(jdbcTemplate, COMPANY_UPSERT_SQL, uniqueCompanies(movies).stream()
                .map(this::companyParams)
                .toList());
        batchUpdate(jdbcTemplate, PEOPLE_UPSERT_SQL, uniquePeople(movies).stream()
                .map(this::peopleParams)
                .toList());
        batchUpdate(jdbcTemplate, MOVIE_GENRE_INSERT_SQL, movieGenres(movies).stream()
                .map(this::movieGenreParams)
                .toList());
        batchUpdate(jdbcTemplate, MOVIE_COMPANY_INSERT_SQL, movieCompanies(movies).stream()
                .map(this::movieCompanyParams)
                .toList());
        batchUpdate(jdbcTemplate, MOVIE_PEOPLE_INSERT_SQL, moviePeople(movies).stream()
                .map(this::moviePeopleParams)
                .toList());
    }

    private void batchUpdate(NamedParameterJdbcTemplate jdbcTemplate,
                             String sql,
                             List<MapSqlParameterSource> params) {
        if (!params.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, params.toArray(MapSqlParameterSource[]::new));
        }
    }

    private List<PeopleBatchItem> toPeopleBatchItems(Long movieTmdbId, CreditsResponse credits) {
        if (credits == null) {
            return List.of();
        }

        List<PeopleBatchItem> people = new ArrayList<>();
        if (credits.getCast() != null) {
            credits.getCast().stream()
                    .filter(cp -> cp != null)
                    .sorted(Comparator.comparing(cp -> cp.getOrder() == null ? Integer.MAX_VALUE : cp.getOrder()))
                    .limit(20)
                    .map(cp -> toPeopleBatchItem(movieTmdbId, cp, "ACTOR"))
                    .forEach(people::add);
        }

        if (credits.getCrew() != null) {
            credits.getCrew().stream()
                    .filter(cp -> cp != null)
                    .filter(cp -> "Director".equalsIgnoreCase(cp.getJob()))
                    .map(cp -> toPeopleBatchItem(movieTmdbId, cp, "DIRECTOR"))
                    .forEach(people::add);
        }
        return people;
    }

    private PeopleBatchItem toPeopleBatchItem(Long movieTmdbId, CreditsPeople creditsPeople, String job) {
        // 이미 상세 정보가 적재된 인물이면 외부 상세 조회(HTTP)를 스킵한다.
        // (name/gender 등 기본 정보는 아래에서 채우고, biography/birthDay 는 upsert 시 기존 값이 유지됨)
        PeopleDetailDTO detail = peopleService.isPersonDetailStored(creditsPeople.getId())
                ? null
                : peopleService.fetchPersonDetailOnly(creditsPeople.getId());
        return PeopleBatchItem.builder()
                .movieTmdbId(movieTmdbId)
                .tmdbId((long) creditsPeople.getId())
                .name(creditsPeople.getName())
                .gender(creditsPeople.getGender() == null ? 0 : creditsPeople.getGender())
                .job(job)
                .birthDay(parseBirthDay(detail))
                .biography(detail == null ? null : detail.getBiography())
                .profileImagePath(firstNonBlank(
                        creditsPeople.getProfilePath(),
                        detail == null ? null : detail.getProfile_path()))
                .build();
    }

    private LocalDate parseBirthDay(PeopleDetailDTO detail) {
        if (detail == null || isBlank(detail.getBirthday())) {
            return null;
        }
        try {
            return LocalDate.parse(detail.getBirthday());
        } catch (Exception e) {
            log.debug("Invalid person birthday. birthday={}", detail.getBirthday());
            return null;
        }
    }

    private String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private List<GenreBatchItem> uniqueGenres(List<? extends MovieBatchItem> movies) {
        Map<Integer, GenreBatchItem> genres = new LinkedHashMap<>();
        for (MovieBatchItem movie : movies) {
            for (GenreBatchItem genre : movie.getGenres()) {
                genres.putIfAbsent(genre.getId(), genre);
            }
        }
        return new ArrayList<>(genres.values());
    }

    private List<CompanyBatchItem> uniqueCompanies(List<? extends MovieBatchItem> movies) {
        Map<Integer, CompanyBatchItem> companies = new LinkedHashMap<>();
        for (MovieBatchItem movie : movies) {
            for (CompanyBatchItem company : movie.getCompanies()) {
                companies.putIfAbsent(company.getId(), company);
            }
        }
        return new ArrayList<>(companies.values());
    }

    private List<PeopleBatchItem> uniquePeople(List<? extends MovieBatchItem> movies) {
        Map<Long, PeopleBatchItem> people = new LinkedHashMap<>();
        for (MovieBatchItem movie : movies) {
            for (PeopleBatchItem person : movie.getPeople()) {
                people.putIfAbsent(person.getTmdbId(), person);
            }
        }
        return new ArrayList<>(people.values());
    }

    private List<MovieGenreBatchItem> movieGenres(List<? extends MovieBatchItem> movies) {
        Map<String, MovieGenreBatchItem> movieGenres = new LinkedHashMap<>();
        for (MovieBatchItem movie : movies) {
            for (GenreBatchItem genre : movie.getGenres()) {
                String key = movie.getTmdbId() + ":" + genre.getId();
                movieGenres.putIfAbsent(key, new MovieGenreBatchItem(movie.getTmdbId(), genre.getId()));
            }
        }
        return new ArrayList<>(movieGenres.values());
    }

    private List<MovieCompanyBatchItem> movieCompanies(List<? extends MovieBatchItem> movies) {
        Map<String, MovieCompanyBatchItem> movieCompanies = new LinkedHashMap<>();
        for (MovieBatchItem movie : movies) {
            for (CompanyBatchItem company : movie.getCompanies()) {
                String key = movie.getTmdbId() + ":" + company.getId();
                movieCompanies.putIfAbsent(key, new MovieCompanyBatchItem(movie.getTmdbId(), company.getId()));
            }
        }
        return new ArrayList<>(movieCompanies.values());
    }

    private List<MoviePeopleBatchItem> moviePeople(List<? extends MovieBatchItem> movies) {
        Map<String, MoviePeopleBatchItem> moviePeople = new LinkedHashMap<>();
        for (MovieBatchItem movie : movies) {
            for (PeopleBatchItem person : movie.getPeople()) {
                String key = movie.getTmdbId() + ":" + person.getTmdbId();
                moviePeople.putIfAbsent(key, new MoviePeopleBatchItem(movie.getTmdbId(), person.getTmdbId()));
            }
        }
        return new ArrayList<>(moviePeople.values());
    }

    private MapSqlParameterSource movieParams(MovieBatchItem item) {
        return new MapSqlParameterSource()
                .addValue("tmdbId", item.getTmdbId())
                .addValue("title", item.getTitle())
                .addValue("overview", item.getOverview())
                .addValue("posterPath", item.getPosterPath())
                .addValue("backdropPath", item.getBackdropPath())
                .addValue("runtime", item.getRuntime())
                .addValue("releaseDate", item.getReleaseDate())
                .addValue("voteAverage", item.getVoteAverage())
                .addValue("voteCount", item.getVoteCount())
                .addValue("adult", item.isAdult())
                .addValue("popularity", item.getPopularity())
                .addValue("originalLanguage", item.getOriginalLanguage());
    }

    private MapSqlParameterSource genreParams(GenreBatchItem item) {
        return new MapSqlParameterSource()
                .addValue("id", item.getId())
                .addValue("name", item.getName());
    }

    private MapSqlParameterSource companyParams(CompanyBatchItem item) {
        return new MapSqlParameterSource()
                .addValue("id", item.getId())
                .addValue("name", item.getName())
                .addValue("logoPath", item.getLogoPath());
    }

    private MapSqlParameterSource peopleParams(PeopleBatchItem item) {
        return new MapSqlParameterSource()
                .addValue("tmdbId", item.getTmdbId())
                .addValue("name", item.getName())
                .addValue("gender", item.getGender())
                .addValue("job", item.getJob())
                .addValue("birthDay", item.getBirthDay())
                .addValue("biography", item.getBiography())
                .addValue("profileImagePath", item.getProfileImagePath());
    }

    private MapSqlParameterSource movieGenreParams(MovieGenreBatchItem item) {
        return new MapSqlParameterSource()
                .addValue("movieTmdbId", item.movieTmdbId())
                .addValue("genreId", item.genreId());
    }

    private MapSqlParameterSource movieCompanyParams(MovieCompanyBatchItem item) {
        return new MapSqlParameterSource()
                .addValue("movieTmdbId", item.movieTmdbId())
                .addValue("companyId", item.companyId());
    }

    private MapSqlParameterSource moviePeopleParams(MoviePeopleBatchItem item) {
        return new MapSqlParameterSource()
                .addValue("movieTmdbId", item.movieTmdbId())
                .addValue("peopleTmdbId", item.peopleTmdbId());
    }

    private static final String MOVIE_UPSERT_SQL = """
            INSERT INTO movie (
                tmdb_id, title, overview, poster_path, backdrop_path, runtime,
                release_date, vote_average, vote_count, adult, popularity, original_language
            ) VALUES (
                :tmdbId, :title, :overview, :posterPath, :backdropPath, :runtime,
                :releaseDate, :voteAverage, :voteCount, :adult, :popularity, :originalLanguage
            )
            ON DUPLICATE KEY UPDATE
                title = VALUES(title),
                overview = VALUES(overview),
                poster_path = VALUES(poster_path),
                backdrop_path = VALUES(backdrop_path),
                runtime = VALUES(runtime),
                release_date = VALUES(release_date),
                vote_average = VALUES(vote_average),
                vote_count = VALUES(vote_count),
                adult = VALUES(adult),
                popularity = VALUES(popularity),
                original_language = VALUES(original_language)
            """;

    private static final String GENRE_UPSERT_SQL = """
            INSERT INTO genre (id, name)
            VALUES (:id, :name)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name)
            """;

    private static final String COMPANY_UPSERT_SQL = """
            INSERT INTO company (id, name, logo_path)
            VALUES (:id, :name, :logoPath)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                logo_path = VALUES(logo_path)
            """;

    private static final String PEOPLE_UPSERT_SQL = """
            INSERT INTO people (
                tmdb_id, name, gender, job, birth_day, biography, profile_image_path
            ) VALUES (
                :tmdbId, :name, :gender, :job, :birthDay, :biography, :profileImagePath
            )
            ON DUPLICATE KEY UPDATE
                name = COALESCE(VALUES(name), name),
                gender = VALUES(gender),
                job = COALESCE(job, VALUES(job)),
                birth_day = COALESCE(VALUES(birth_day), birth_day),
                biography = COALESCE(VALUES(biography), biography),
                profile_image_path = COALESCE(VALUES(profile_image_path), profile_image_path)
            """;

    private static final String MOVIE_GENRE_INSERT_SQL = """
            INSERT INTO movie_genre (movie_id, genre_id)
            SELECT m.id, :genreId
            FROM movie m
            WHERE m.tmdb_id = :movieTmdbId
              AND NOT EXISTS (
                  SELECT 1
                  FROM movie_genre mg
                  WHERE mg.movie_id = m.id
                    AND mg.genre_id = :genreId
              )
            """;

    private static final String MOVIE_COMPANY_INSERT_SQL = """
            INSERT INTO movie_company (movie_id, company_id)
            SELECT m.id, :companyId
            FROM movie m
            WHERE m.tmdb_id = :movieTmdbId
              AND NOT EXISTS (
                  SELECT 1
                  FROM movie_company mc
                  WHERE mc.movie_id = m.id
                    AND mc.company_id = :companyId
              )
            """;

    private static final String MOVIE_PEOPLE_INSERT_SQL = """
            INSERT INTO movie_people (movie_id, people_id)
            SELECT m.id, p.id
            FROM movie m
            JOIN people p ON p.tmdb_id = :peopleTmdbId
            WHERE m.tmdb_id = :movieTmdbId
              AND NOT EXISTS (
                  SELECT 1
                  FROM movie_people mp
                  WHERE mp.movie_id = m.id
                    AND mp.people_id = p.id
              )
            """;

    @Getter
    @Builder
    public static class MovieBatchItem {
        private Long tmdbId;
        private String title;
        private String overview;
        private String posterPath;
        private String backdropPath;
        private int runtime;
        private LocalDate releaseDate;
        private double voteAverage;
        private Integer voteCount;
        private boolean adult;
        private Double popularity;
        private String originalLanguage;
        private List<GenreBatchItem> genres;
        private List<CompanyBatchItem> companies;
        private List<PeopleBatchItem> people;

        public static MovieBatchItem from(MovieDetailDTO dto, List<PeopleBatchItem> people) {
            return MovieBatchItem.builder()
                    .tmdbId(dto.getTmdbId())
                    .title(dto.getTitle())
                    .overview(dto.getOverview())
                    .posterPath(dto.getPosterPath())
                    .backdropPath(dto.getBackdropPath())
                    .runtime(dto.getRuntime() == null ? 0 : dto.getRuntime())
                    .releaseDate(dto.getReleaseDate())
                    .voteAverage(dto.getVoteAverage() == null ? 0 : dto.getVoteAverage())
                    .voteCount(dto.getVoteCount())
                    .adult(Boolean.TRUE.equals(dto.getAdult()))
                    .popularity(dto.getPopularity())
                    .originalLanguage(dto.getOriginalLanguage())
                    .genres(toGenreBatchItems(dto.getGenres()))
                    .companies(toCompanyBatchItems(dto.getProductionCompanies()))
                    .people(people == null ? List.of() : people)
                    .build();
        }

        private static List<GenreBatchItem> toGenreBatchItems(List<GenreDTO> genres) {
            if (genres == null) {
                return List.of();
            }
            return genres.stream()
                    .map(genre -> GenreBatchItem.builder()
                            .id(genre.getId())
                            .name(genre.getName())
                            .build())
                    .toList();
        }

        private static List<CompanyBatchItem> toCompanyBatchItems(List<CompanyDTO> companies) {
            if (companies == null) {
                return List.of();
            }
            return companies.stream()
                    .map(company -> CompanyBatchItem.builder()
                            .id(company.getId())
                            .name(company.getName())
                            .logoPath(company.getLogoPath())
                            .build())
                    .toList();
        }
    }

    @Getter
    @Builder
    public static class GenreBatchItem {
        private int id;
        private String name;
    }

    @Getter
    @Builder
    public static class CompanyBatchItem {
        private int id;
        private String name;
        private String logoPath;
    }

    @Getter
    @Builder
    public static class PeopleBatchItem {
        private Long movieTmdbId;
        private Long tmdbId;
        private String name;
        private int gender;
        private String job;
        private LocalDate birthDay;
        private String biography;
        private String profileImagePath;
    }

    private record MovieGenreBatchItem(Long movieTmdbId, int genreId) {
    }

    private record MovieCompanyBatchItem(Long movieTmdbId, int companyId) {
    }

    private record MoviePeopleBatchItem(Long movieTmdbId, Long peopleTmdbId) {
    }

    /**
     * skip 발생을 관측 가능하게 남기는 리스너.
     * 어느 단계에서 어떤 이유로 몇 건이 스킵됐는지 log.warn 으로 기록한다.
     */
    @Slf4j
    static class TmdbSkipListener implements org.springframework.batch.core.SkipListener<WorkItem, MovieBatchItem> {
        @Override
        public void onSkipInRead(Throwable t) {
            log.warn("[MovieBatch] Skip in read. reason={}", t.getMessage(), t);
        }

        @Override
        public void onSkipInProcess(WorkItem item, Throwable t) {
            log.warn("[MovieBatch] Skip in process. movieId={}, reason={}",
                    item == null ? null : item.getMovieId(), t.getMessage(), t);
        }

        @Override
        public void onSkipInWrite(MovieBatchItem item, Throwable t) {
            log.warn("[MovieBatch] Skip in write. tmdbId={}, reason={}",
                    item == null ? null : item.getTmdbId(), t.getMessage(), t);
        }
    }
}
