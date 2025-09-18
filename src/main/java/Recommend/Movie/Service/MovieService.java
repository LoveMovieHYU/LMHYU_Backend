package Recommend.Movie.Service;

import Recommend.Movie.Converter.CompanyConverter;
import Recommend.Movie.Converter.GenreConverter;
import Recommend.Movie.Converter.MovieConverter;
import Recommend.Movie.DTO.*;
import Recommend.Movie.Domain.*;
import Recommend.Movie.Repository.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class MovieService {
    private final CompanyRepository companyRepository;
    private final MovieCompanyRepository movieCompanyRepository;
    private final RestTemplate restTemplate;
    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MovieGenreRepository movieGenreRepository;

    public MovieService(CompanyRepository companyRepository, MovieCompanyRepository movieCompanyRepository, RestTemplate restTemplate, MovieRepository movieRepository, GenreRepository genreRepository, MovieGenreRepository movieGenreRepository) {
        this.companyRepository = companyRepository;
        this.movieCompanyRepository = movieCompanyRepository;
        this.restTemplate = restTemplate;
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.movieGenreRepository = movieGenreRepository;
    }

    @Value("${tmdb.api.key}")
    private String apikey;

    @Value("${tmdb.api.base-url}")
    private String baseUrl;

    public void fetchAndSaveAllFromDiscover(int startPage, int endPage, boolean includeAdult){
        if (startPage < 1 || endPage < startPage) {
            log.error("Invalid page range: startPage={}, endPage={}", startPage, endPage);
            throw new IllegalArgumentException("페이지 범위가 올바르지 않습니다.");
        }

        // 1. TMDB API 호출 URL 생성
        for (int page = startPage; page <= endPage; page++) {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/discover/movie")
                    .queryParam("api_key", apikey)
                    .queryParam("language", "ko-KR")
                    .queryParam("sort_by", "popularity.desc")
                    .queryParam("include_adult", includeAdult)
                    .queryParam("include_video", false)
                    .queryParam("page", page)
                    .toUriString();

        ResponseEntity<DiscoverResponse> resp = restTemplate.getForEntity(url, DiscoverResponse.class);
        DiscoverResponse body = resp.getBody();

            if (body == null || body.results == null || body.results.isEmpty()) {
                // 더 이상 결과가 없으면 조기 종료
                break;
            }

            for (DiscoverMovieSummary summary : body.results) {
                if (summary == null) continue;
                Optional<Movie> optionalMovie = movieRepository.findByTmdbId((long) summary.getId());
                if (optionalMovie.isPresent()) {
                    log.info("이미 존재하는 영화 ID {}, 스킵", summary.getId());
                    continue;
                }
                try {
                    fetchAndSaveMovieDetail(summary.getId());
                    // 간단한 rate-limit 완화 (필요시 조절/제거)
                    sleepSilently(Duration.ofMillis(150));
                } catch (HttpClientErrorException.NotFound nf) {
                    // 비어있는 ID(404)는 스킵
                } catch (HttpClientErrorException.TooManyRequests tmr) {
                    // Rate limit → 잠시 대기 후 재시도(한 번)
                    sleepSilently(Duration.ofSeconds(2));
                    try {
                        fetchAndSaveMovieDetail(summary.id);
                    } catch (Exception e2) {
                        // 재시도 실패는 로깅 후 스킵
                    }
                } catch (Exception e) {
                    // 기타 오류는 로깅 후 스킵
                    log.error("Failed to process summary id={}", summary.getId(), e);
                }
            }

            // total_pages 를 넘어가면 조기 종료
            if (body.total_pages != null && page >= body.total_pages) {
                break;
            }
        }
    }

    /**
     * 특정 movieId 에 대한 상세정보를 가져와 DB에 저장합니다.
     * - Movie / Genre / Company 및 조인 관계 저장
     */
    @Transactional
    public void fetchAndSaveMovieDetail(int movieId){
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/movie/" + movieId)
                .queryParam("api_key", apikey)
                .queryParam("language", "ko-KR")
                .toUriString();
        log.info("Fetching movie detail from URL: {}", url);
        MovieDetailDTO detailDTO = null;
        try {
            detailDTO = restTemplate.getForObject(url, MovieDetailDTO.class);
        } catch (HttpClientErrorException.NotFound nf) {
            return;
        }

        if(detailDTO == null){
            return;
        }

        saveGenres(detailDTO.getGenres());

        saveCompanies(detailDTO.getProductionCompanies());

        try {
            Movie movie = getOrCreateMovieFromDTO(detailDTO); // ← 여기서 NPE 많이 남
            movieRepository.save(movie);

            // 4) 조인 관계 저장 (영화-장르)
            if (detailDTO.getGenres() != null) {
                for (GenreDTO genreDTO : detailDTO.getGenres()) {
                    if (genreDTO == null) continue;
                    if (!movieGenreRepository.existsByMovie_IdAndGenre_Id(movie.getId(), genreDTO.getId())) {
                        Genre genre = genreRepository.getReferenceById(genreDTO.getId());
                        MovieGenre movieGenre = getMovieGenre(genre, movie);
                        log.info("Saving MovieGenre: Movie {} - Genre {}", movie.getId(), genre.getId());
                        movieGenreRepository.save(movieGenre);
                    }
                }
            }
            // 5) 조인 관계 저장 (영화-제작사)
            if (detailDTO.getProductionCompanies() != null) {
                for (CompanyDTO companyDTO : detailDTO.getProductionCompanies()) {
                    if (companyDTO == null) continue;
                    if (!movieCompanyRepository.existsByMovie_IdAndCompany_Id(movie.getId(), companyDTO.getId())) {
                        Company company = companyRepository.getReferenceById(companyDTO.getId());
                        MovieCompany movieCompany = getMovieCompany(company, movie);
                        log.info("Saving MovieCompany: Movie {} - Company {}", movie.getTitle(), company.getName());
                        movieCompanyRepository.save(movieCompany);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Saving movie failed. dtoTmdbId={}, title={}, runtime={}, voteAvg={}, release={}",
                    detailDTO.getTmdbId(), detailDTO.getTitle(), detailDTO.getRuntime(),
                    detailDTO.getVoteAverage(), detailDTO.getReleaseDate(), ex);
            throw ex;
        }

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

    private void saveGenres(List<GenreDTO> genreDTOList){
        if(genreDTOList == null) return;
        for(GenreDTO genreDTO : genreDTOList){
            if(genreDTO == null) continue;
            if(!genreRepository.existsById(genreDTO.getId())){
                log.info("Saving new genre: {} - {}", genreDTO.getId(), genreDTO.getName());
                genreRepository.save(GenreConverter.toEntity(genreDTO));
            }
        }
    }

    private void saveCompanies(List<CompanyDTO> companyDTOList){
        if(companyDTOList == null) return;
        for(CompanyDTO companyDTO : companyDTOList){
            if(companyDTO == null) continue;
            if(!companyRepository.existsById(companyDTO.getId())){
                log.info("Saving new company: {} - {}", companyDTO.getId(), companyDTO.getName());
                companyRepository.save(CompanyConverter.toEntity(companyDTO));
            }
        }
    }

    private Movie getOrCreateMovieFromDTO(MovieDetailDTO detailDTO) {
        return movieRepository.findByTmdbId(detailDTO.getTmdbId())
                .map(movie -> MovieConverter.updateFromDTO(movie, detailDTO))
                .orElseGet(() -> MovieConverter.toEntity(detailDTO));

    }

    private void sleepSilently(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException ignored) {}
    }

}
