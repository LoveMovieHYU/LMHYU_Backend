package Recommend.Movie.Service;

import Recommend.Movie.DTO.MovieListDTO.MovieDetailResponse;
import Recommend.Movie.Domain.Movie;
import Recommend.Movie.Repository.MovieRepository;
import Recommend.Movie.Repository.MovieResponseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieResponseService {
    private final MovieRepository movieRepository;
    private final MovieResponseRepository movieResponseRepository;

    public MovieDetailResponse getMovieDetail(int id) {
        Movie movie = movieResponseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("영화를 찾을 수 없습니다. ID: " + id));

        return MovieDetailResponse.from(movie);
    }
}
