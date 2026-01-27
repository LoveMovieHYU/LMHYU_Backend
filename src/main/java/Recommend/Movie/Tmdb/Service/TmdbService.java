package Recommend.Movie.Tmdb.Service;

import Recommend.Movie.Tmdb.Converter.CompanyConverter;
import Recommend.Movie.Tmdb.Converter.GenreConverter;
import Recommend.Movie.Tmdb.Converter.MoviesConverter;
import Recommend.Movie.Tmdb.Domain.*;
import Recommend.Movie.Tmdb.Dto.*;
import Recommend.Movie.Tmdb.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TmdbService {
    private final CompanyRepository companyRepository;
    private final MovieCompanyRepository movieCompanyRepository;
    private final RestTemplate restTemplate;
    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final PeopleService peopleService;

    private final PlatformTransactionManager transactionManager;


    @Value("${tmdb.api.key}")
    private String apikey;

    @Value("${tmdb.api.base-url}")
    private String baseUrl;

    /**
     * discover API를 여러 페이지 돌면서
     * 아직 DB에 없는 영화들을 WorkItem으로 만들어 리턴
     */
    public List<WorkItem> buildWorkItemsFromDiscover(int startPage, int endPage, boolean includeAdult) {

        List<WorkItem> workItems = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();
        int startYear = 1990; // 수집 시작 연도 (조절 가능)

        // 연도별 루프 (1990 ~ 2026)
        for (int year = startYear; year <= currentYear; year++) {
            log.info("Collecting movies for YEAR: {}", year);

            // 각 연도마다 1페이지부터 최대 페이지(또는 지정된 endPage)까지 조회
            for (int page = 1; page <= 500; page++) {

                // 요청 URL 생성 (primary_release_year 파라미터 추가)
                String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/discover/movie")
                        .queryParam("api_key", apikey)
                        .queryParam("language", "ko-KR")
                        .queryParam("sort_by", "popularity.desc")
                        .queryParam("include_adult", includeAdult)
                        .queryParam("include_video", false)
                        .queryParam("primary_release_year", year) // ★ 핵심: 연도별 조회
                        .queryParam("page", page)
                        .toUriString();

                try {
                    ResponseEntity<DiscoverResponse> resp = restTemplate.getForEntity(url, DiscoverResponse.class);
                    DiscoverResponse body = resp.getBody();

                    if (body == null || body.results == null || body.results.isEmpty()) {
                        break; // 데이터 없으면 다음 연도로
                    }

                    for (DiscoverMovieSummary summary : body.results) {
                        if (summary == null) continue;

                        if (movieRepository.findByTmdbId((long) summary.getId()).isPresent()) {
                            continue;
                        }

                        workItems.add(new WorkItem(summary.getId()));
                    }

                    // 총 페이지 수를 넘어가거나, TMDB 최대 제한(500)에 도달하면 중단
                    if (body.total_pages != null && page >= body.total_pages) {
                        break;
                    }

                    // 너무 빠른 요청 방지 (0.1초 대기)
                    sleepSilently(Duration.ofMillis(100));

                } catch (HttpClientErrorException e) {
                    log.error("TMDB API Error for year={}, page={}: {}", year, page, e.getMessage());
                    // 429(Too Many Requests) 에러일 경우 잠시 대기 후 재시도 로직을 넣거나, 건너뛰기
                    if (e.getStatusCode().value() == 429) {
                        log.warn("Rate limit exceeded. Sleeping for 2 seconds...");
                        sleepSilently(Duration.ofSeconds(2));
                        page--; // 해당 페이지 다시 시도
                    } else if (e.getStatusCode().value() == 400 && e.getResponseBodyAsString().contains("Invalid page")) {
                        // 500페이지 초과 에러 시 루프 탈출
                        log.warn("Page limit reached for year {}", year);
                        break;
                    }
                } catch (Exception e) {
                    log.error("Unexpected error for year={}, page={}", year, page, e);
                }
            }
        }

        log.info("Total TMDB WorkItems collected: {}", workItems.size());
        return workItems;
    }
    /**
     * TMDB에서 특정 movieId의 상세 정보를 가져오기만 하는 메서드 (DB 저장 없음)
     */
    public MovieDetailDTO fetchMovieDetailOnly(int movieId) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/movie/" + movieId)
                .queryParam("api_key", apikey)
                .queryParam("language", "ko-KR")
                .toUriString();

        MovieDetailDTO detailDTO = null;
        try {
            detailDTO = restTemplate.getForObject(url, MovieDetailDTO.class);
        } catch (HttpClientErrorException.NotFound nf) {
            // 404면 그냥 스킵
            log.debug("TMDB 404 NotFound movieId={}", movieId);
            return null;
        } catch (HttpClientErrorException.TooManyRequests tmr) {
            // 429면 잠깐 쉬었다가 한 번 더 시도
            log.warn("TMDB 429 TooManyRequests movieId={}, retry after 2s", movieId);
            sleepSilently(Duration.ofSeconds(2));
            try {
                detailDTO = restTemplate.getForObject(url, MovieDetailDTO.class);
            } catch (Exception e2) {
                log.error("Retry after 429 failed. movieId={}", movieId, e2);
                return null;
            }
        }

        if (detailDTO == null) {
            log.warn("MovieDetailDTO is null. movieId={}", movieId);
            return null;
        }

        return detailDTO;
    }


    /**
     * 특정 movieId 에 대한 상세정보를 가져와 DB에 저장합니다.
     * - Movie / Genre / Company 및 조인 관계 저장
     */
    @Transactional
    public void fetchAndSaveMovieDetail(int movieId) {
        log.info("[MovieBatch] START fetch & save movie detail. movieId={}", movieId);
        MovieDetailDTO detailDTO = fetchMovieDetailOnly(movieId);
        if (detailDTO == null) return;

        try {
            // Movie 저장 (먼저 저장하여 영속 상태로 만듦)
            Movie movie = getOrCreateMovieFromDTO(detailDTO);
            movie = movieRepository.saveAndFlush(movie);
            peopleService.fetchAndSaveCreditsByMovieId(movie, true);

            // Genre 처리
            if (detailDTO.getGenres() != null) {
                for (GenreDTO genreDTO : detailDTO.getGenres()) {
                    if (genreDTO == null) continue;

                    // 별도 트랜잭션으로 확실히 저장/조회
                    Genre detachedGenre = getOrSaveGenre(genreDTO);

                    // 현재 트랜잭션의 영속성 컨텍스트로 다시 불러오기
                    // 이미 DB에 있는 것이 확실하므로 getReferenceById 사용 가능
                    Genre managedGenre = genreRepository.getReferenceById(detachedGenre.getId());

                    if (!movieGenreRepository.existsByMovie_IdAndGenre_Id(movie.getId(), managedGenre.getId())) {
                        MovieGenre movieGenre = getMovieGenre(managedGenre, movie);
                        movieGenreRepository.save(movieGenre);
                    }
                }
            }

            // Company 처리
            if (detailDTO.getProductionCompanies() != null) {
                for (CompanyDTO companyDTO : detailDTO.getProductionCompanies()) {
                    if (companyDTO == null) continue;

                    // 별도 트랜잭션으로 확실히 저장/조회
                    Company detachedCompany = getOrSaveCompany(companyDTO);

                    // 현재 트랜잭션으로 다시 불러오기 (영속화)
                    Company managedCompany = companyRepository.getReferenceById(detachedCompany.getId());

                    if (!movieCompanyRepository.existsByMovie_IdAndCompany_Id(movie.getId(), managedCompany.getId())) {
                        MovieCompany movieCompany = getMovieCompany(managedCompany, movie);
                        movieCompanyRepository.save(movieCompany);
                    }
                }
            }
            log.info("[MovieBatch] DONE movieId={}, tmdbId={}, title={}",
                    movieId, detailDTO.getTmdbId(), detailDTO.getTitle());

        } catch (Exception ex) {
            log.error("Saving movie failed...", ex);
            throw ex;
        }
    }

    private Company getOrSaveCompany(CompanyDTO dto) {
        // 먼저 조회 시도
        return companyRepository.findById(dto.getId())
                .orElseGet(() -> {
                    // 없으면 별도 트랜잭션으로 저장 시도
                    TransactionTemplate tt = new TransactionTemplate(transactionManager);
                    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    try {
                        return tt.execute(status -> companyRepository.saveAndFlush(CompanyConverter.toEntity(dto)));
                    } catch (Exception e) {
                        // 동시성 문제로 저장이 실패했다면, 누군가 저장한 것이므로 다시 조회
                        return companyRepository.findById(dto.getId())
                                .orElseThrow(() -> new IllegalStateException("Company save failed and not found: " + dto.getId()));
                    }
                });
    }

    private Genre getOrSaveGenre(GenreDTO dto) {
        return genreRepository.findById(dto.getId())
                .orElseGet(() -> {
                    TransactionTemplate tt = new TransactionTemplate(transactionManager);
                    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    try {
                        return tt.execute(status -> genreRepository.saveAndFlush(GenreConverter.toEntity(dto)));
                    } catch (Exception e) {
                        return genreRepository.findById(dto.getId())
                                .orElseThrow(() -> new IllegalStateException("Genre save failed and not found: " + dto.getId()));
                    }
                });
    }

    private static MovieGenre getMovieGenre(Genre genre, Movie movie) {
        MovieGenre movieGenre = new MovieGenre();
        movieGenre.setGenre(genre);
        movieGenre.setMovie(movie);
        movie.addGenre(movieGenre);
        return movieGenre;
    }

    private static MovieCompany getMovieCompany(Company company, Movie movie){
        MovieCompany movieCompany = new MovieCompany();
        movieCompany.setCompany(company);
        movieCompany.setMovie(movie);
        movie.addCompany(movieCompany);
        return movieCompany;
    }

    private Movie getOrCreateMovieFromDTO(MovieDetailDTO detailDTO) {
        return movieRepository.findByTmdbId(detailDTO.getTmdbId())
                .map(movie -> MoviesConverter.updateFromDTO(movie, detailDTO))
                .orElseGet(() -> MoviesConverter.toEntity(detailDTO));

    }

    private void sleepSilently(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException ignored) {}
    }

}