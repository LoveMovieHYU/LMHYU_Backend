package Recommend.Movie.Service;

import Recommend.Movie.Converter.CompanyConverter;
import Recommend.Movie.Converter.GenreConverter;
import Recommend.Movie.Converter.MoviesConverter;
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
    private final PeopleService peopleService;

    public MovieService(CompanyRepository companyRepository, MovieCompanyRepository movieCompanyRepository, RestTemplate restTemplate, MovieRepository movieRepository, GenreRepository genreRepository, MovieGenreRepository movieGenreRepository, PeopleService peopleService) {
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

    public void fetchAndSaveAllFromDiscover(int startPage, int endPage, boolean includeAdult){
        if (startPage < 1 || endPage < startPage) {
            log.error("Invalid page range: startPage={}, endPage={}", startPage, endPage);
            throw new IllegalArgumentException("페이지 범위가 올바르지 않습니다.");
        }

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
                Optional<Movie> optionalMovie = movieRepository.findByTmdbId((long) summary.getId());
                if (optionalMovie.isPresent()) {
                    continue;
                }
                try {
                    fetchAndSaveMovieDetail(summary.getId());
                    sleepSilently(Duration.ofMillis(150));
                } catch (HttpClientErrorException.NotFound nf) {
                    // 비어있는 ID(404)는 스킵
                } catch (HttpClientErrorException.TooManyRequests tmr) {
                    sleepSilently(Duration.ofSeconds(2));
                    try {
                        fetchAndSaveMovieDetail(summary.id);
                    } catch (Exception e2) {
                        log.error(e2.getMessage(), e2);
                    }
                } catch (Exception e) {
                    log.error("Failed to process summary id={}", summary.getId(), e);
                }
            }

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
            Movie movie = getOrCreateMovieFromDTO(detailDTO);
            movieRepository.saveAndFlush(movie);
            peopleService.fetchAndSaveCreditsByMovieId(movie, true);

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
            if (detailDTO.getProductionCompanies() != null) {
                for (CompanyDTO companyDTO : detailDTO.getProductionCompanies()) {
                    if (companyDTO == null) continue;
                    if (!movieCompanyRepository.existsByMovie_IdAndCompany_Id(movie.getId(), companyDTO.getId())) {
                        Company company = companyRepository.getReferenceById(companyDTO.getId());
                        MovieCompany movieCompany = getMovieCompany(company, movie);
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
