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
import java.util.ArrayList;
import java.util.Collections;
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
     * 특정 연도(year)와 페이지(page)에 해당하는 데이터만 API로 가져와서 반환
     * ItemReader에서 이 메서드를 반복 호출하게 됩니다.
     */
    public List<WorkItem> fetchDiscoverPage(int year, int page, boolean includeAdult) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/discover/movie")
                .queryParam("api_key", apikey)
                .queryParam("language", "ko-KR")
                .queryParam("sort_by", "popularity.desc")
                .queryParam("include_adult", includeAdult)
                .queryParam("include_video", false)
                .queryParam("primary_release_year", year)
                .queryParam("popularity.gte", 1.0)
                .queryParam("page", page)
                .toUriString();

        try {
            ResponseEntity<DiscoverResponse> resp = restTemplate.getForEntity(url, DiscoverResponse.class);
            DiscoverResponse body = resp.getBody();

            if (body == null || body.results == null || body.results.isEmpty()) {
                return Collections.emptyList(); // 데이터 없음
            }


            List<WorkItem> items = new ArrayList<>();
            for (DiscoverMovieSummary summary : body.results) {
                if (summary == null) continue;

                // 이미 DB에 있는지 확인
                if (movieRepository.findByTmdbId((long) summary.getId()).isPresent()) {
                    log.debug("Already Data : {}", summary.getTitle());
                    continue;
                }
                items.add(new WorkItem(summary.getId()));
            }

            sleepSilently(Duration.ofMillis(100));

            return items;

        } catch (HttpClientErrorException e) {
            log.error("TMDB API Error for year={}, page={}: {}", year, page, e.getMessage());
            if (e.getStatusCode().value() == 429) {
                log.warn("Rate limit exceeded. Sleeping for 2 seconds...");
                sleepSilently(Duration.ofSeconds(2));
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Unexpected error for year={}, page={}", year, page, e);
            return Collections.emptyList();
        }
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
        log.debug("[MovieBatch] START fetch & save movie detail. movieId={}", movieId);
        MovieDetailDTO detailDTO = fetchMovieDetailOnly(movieId);
        if (detailDTO == null) return;

        if (detailDTO.getPopularity() != null && detailDTO.getPopularity() <= 1.0) {
            log.debug("필터링 스킵 (popularity <= 1.0) - movieId={}, title={}, popularity={}",
                    movieId, detailDTO.getTitle(), detailDTO.getPopularity());
            return;
        }

        try {
            // Movie 저장 (먼저 저장하여 영속 상태로 만듦)
            Movie movie = getOrCreateMovieFromDTO(detailDTO);
            movie = movieRepository.saveAndFlush(movie);
            peopleService.fetchAndSaveCreditsByMovieId(movie, true);

            if (detailDTO.getGenres() != null) {
                for (GenreDTO genreDTO : detailDTO.getGenres()) {
                    if (genreDTO == null) continue;

                    Genre detachedGenre = getOrSaveGenre(genreDTO);

                    // 현재 트랜잭션의 영속성 컨텍스트로 다시 불러오기
                    Genre managedGenre = genreRepository.getReferenceById(detachedGenre.getId());

                    if (!movieGenreRepository.existsByMovie_IdAndGenre_Id(movie.getId(), managedGenre.getId())) {
                        MovieGenre movieGenre = getMovieGenre(managedGenre, movie);
                        movieGenreRepository.save(movieGenre);
                    }
                }
            }

            if (detailDTO.getProductionCompanies() != null) {
                for (CompanyDTO companyDTO : detailDTO.getProductionCompanies()) {
                    if (companyDTO == null) continue;

                    Company detachedCompany = getOrSaveCompany(companyDTO);

                    Company managedCompany = companyRepository.getReferenceById(detachedCompany.getId());

                    if (!movieCompanyRepository.existsByMovie_IdAndCompany_Id(movie.getId(), managedCompany.getId())) {
                        MovieCompany movieCompany = getMovieCompany(managedCompany, movie);
                        movieCompanyRepository.save(movieCompany);
                    }
                }
            }
            log.debug("[MovieBatch] DONE movieId={}, tmdbId={}, title={}",
                    movieId, detailDTO.getTmdbId(), detailDTO.getTitle());

        } catch (Exception ex) {
            log.error("Saving movie failed...", ex);
            throw ex;
        }
    }
    private Company getOrSaveCompany(CompanyDTO dto) {
        return companyRepository.findById(dto.getId())
                .orElseGet(() -> {
                    TransactionTemplate tt = new TransactionTemplate(transactionManager);
                    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    try {
                        return tt.execute(status -> companyRepository.saveAndFlush(CompanyConverter.toEntity(dto)));
                    } catch (Exception e) {
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
        return movieGenre;
    }

    private static MovieCompany getMovieCompany(Company company, Movie movie){
        MovieCompany movieCompany = new MovieCompany();
        movieCompany.setCompany(company);
        movieCompany.setMovie(movie);
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