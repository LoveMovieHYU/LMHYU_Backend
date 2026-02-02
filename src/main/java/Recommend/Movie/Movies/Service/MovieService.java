package Recommend.Movie.Movies.Service;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.Movies.Converter.MovieConverter;
import Recommend.Movie.Movies.Dto.MovieSearchResponseDTO;
import Recommend.Movie.Movies.Repository.MovieSpecification;
import Recommend.Movie.Tmdb.Dto.MovieDetailResponse;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Repository.MovieRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;


    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    /**
     * 영화 검색 ( 영화 이름, 배우(감독) 이름 )
     * */
    public List<MovieSearchResponseDTO> searchMovies(String keyword, int page){

        int pageNum = (page > 0) ? page - 1 : 0;
        Pageable pageable = PageRequest.of(pageNum, 10, Sort.by(Sort.Direction.DESC, "releaseDate"));

        Specification<Movie> spec = MovieSpecification.searchByKeyword(keyword);

        Page<Movie> moviePage = movieRepository.findAll(spec, pageable);

        return moviePage.getContent().stream()
                .map(MovieConverter::toSearchDTO)
                .collect(Collectors.toList());

    }

    /**
     * 영화 상세보기
     * */
    public MovieDetailResponse getMovieDetail(long tmdbId) {
        Movie movie = movieRepository.findByTmdbIdWithPeople(tmdbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND,"영화를 찾을 수 없습니다."));

        return MovieConverter.toDetailDTO(movie);
    }

}
