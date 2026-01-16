package Recommend.Movie.Tmdb.Service;

import Recommend.Movie.Tmdb.Converter.CompanyConverter;
import Recommend.Movie.Tmdb.Converter.GenreConverter;
import Recommend.Movie.Tmdb.Converter.MoviesConverter;
import Recommend.Movie.Tmdb.Domain.*;
import Recommend.Movie.Tmdb.Dto.*;
import Recommend.Movie.Tmdb.Repository.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TmdbService {
    private final CompanyRepository companyRepository;
    private final MovieCompanyRepository movieCompanyRepository;
    private final RestTemplate restTemplate;
    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final PeopleService peopleService;

    public TmdbService(CompanyRepository companyRepository, MovieCompanyRepository movieCompanyRepository, RestTemplate restTemplate, MovieRepository movieRepository, GenreRepository genreRepository, MovieGenreRepository movieGenreRepository, PeopleService peopleService) {
        this.companyRepository = companyRepository;
        this.movieCompanyRepository = movieCompanyRepository;
        this.restTemplate = restTemplate;
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.peopleService = peopleService;
    }

    @Value("${tmdb.api.key}")
    private String apikey;

    @Value("${tmdb.api.base-url}")
    private String baseUrl;


    /**
     * discover API를 여러 페이지 돌면서
     * 아직 DB에 없는 영화들을 WorkItem으로 만들어 리턴
     */
    public List<WorkItem> buildWorkItemsFromDiscover(int startPage, int endPage, boolean includeAdult) {

        if (startPage < 1 || endPage < startPage) {
            log.error("Invalid page range: startPage={}, endPage={}", startPage, endPage);
            throw new IllegalArgumentException("페이지 범위가 올바르지 않습니다.");
        }

        List<WorkItem> workItems = new ArrayList<>();

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
                break;
            }

            for (DiscoverMovieSummary summary : body.results) {
                if (summary == null) continue;

                // 이미 저장된 영화는 건너뛰기
                if (movieRepository.findByTmdbId((long) summary.getId()).isPresent()) {
                    continue;
                }

                workItems.add(new WorkItem(summary.getId()));
            }

            if (body.total_pages != null && page >= body.total_pages) {
                break;
            }
        }

        log.info("TMDB WorkItem count = {}", workItems.size());
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
        if (detailDTO == null) {
            return;
        }

        saveGenres(detailDTO.getGenres());
        saveCompanies(detailDTO.getProductionCompanies());
        try {
            Movie movie = getOrCreateMovieFromDTO(detailDTO);
            movieRepository.saveAndFlush(movie);
            peopleService.fetchAndSaveCreditsByMovieId(movie, true);

            // movie-genre 조인
            if (detailDTO.getGenres() != null) {
                for (GenreDTO genreDTO : detailDTO.getGenres()) {
                    if (genreDTO == null) continue;
                    if (!movieGenreRepository.existsByMovie_IdAndGenre_Id(movie.getId(), genreDTO.getId())) {
                        Genre genre = genreRepository.getReferenceById(genreDTO.getId());
                        MovieGenre movieGenre = getMovieGenre(genre, movie);
                        movieGenreRepository.save(movieGenre);
                    }
                }
            }

            // movie-company 조인
            if (detailDTO.getProductionCompanies() != null) {
                for (CompanyDTO companyDTO : detailDTO.getProductionCompanies()) {
                    if (companyDTO == null) continue;
                    if (!movieCompanyRepository.existsByMovie_IdAndCompany_Id(movie.getId(), companyDTO.getId())) {
                        Company company = companyRepository.getReferenceById(companyDTO.getId());
                        MovieCompany movieCompany = getMovieCompany(company, movie);
                        movieCompanyRepository.save(movieCompany);
                        log.info("Saving movie-company link: movieId={}, companyId={}", movie.getId(), company.getId());
                    }
                }
            }
            log.info("[MovieBatch] DONE movieId={}, tmdbId={}, title={}",
                    movieId, detailDTO.getTmdbId(), detailDTO.getTitle());

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
                genreRepository.save(GenreConverter.toEntity(genreDTO));
            }
        }
    }

    private void saveCompanies(List<CompanyDTO> companyDTOList){
        if(companyDTOList == null) return;
        for(CompanyDTO companyDTO : companyDTOList){
            if(companyDTO == null) continue;
            if(!companyRepository.existsById(companyDTO.getId())){
                companyRepository.save(CompanyConverter.toEntity(companyDTO));
            }
        }
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
