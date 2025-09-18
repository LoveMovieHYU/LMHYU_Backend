package Recommend.Movie.Service;

import Recommend.Movie.Converter.CompanyConverter;
import Recommend.Movie.Converter.GenreConverter;
import Recommend.Movie.Converter.MovieConverter;
import Recommend.Movie.DTO.*;
import Recommend.Movie.Domain.*;
import Recommend.Movie.Repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.List;

@Service
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

                // 이미 저장된 영화라면 스킵(상세 호출 줄이기)
                if (movieRepository.findById(summary.getId()) != null) {
                    continue;
                }

                // 상세 저장 시도
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
    public void fetchAndSaveMovieDetail(int movieId){
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/movie/" + movieId)
                .queryParam("api_key", apikey)
                .queryParam("language", "ko-KR")
                .toUriString();

        MovieDetailDTO detailDTO = null;
        try {
            detailDTO = restTemplate.getForObject(url, MovieDetailDTO.class);
        } catch (HttpClientErrorException.NotFound nf) {
            // 없는 영화
            return;
        }

        if(detailDTO == null){
            return;
        }

        // 1) 장르 저장
        saveGenres(detailDTO.getGenres());

        // 2) 제작사 저장
        saveCompanies(detailDTO.getProductionCompanies());

        // 3) 영화 저장(없으면 생성)
        Movie movie = getOrCreateMovieFromDTO(detailDTO);
        movieRepository.save(movie);

        // 4) 조인 관계 저장 (영화-장르)
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

        // 5) 조인 관계 저장 (영화-제작사)
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
        Movie movie = movieRepository.findById(detailDTO.getId());
        if (movie == null) {
            movie = MovieConverter.toEntity(detailDTO);
        } else {
            MovieConverter.updateFromDTO(movie, detailDTO);
        }
        return movie;

    }

    private void sleepSilently(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException ignored) {}
    }

}
