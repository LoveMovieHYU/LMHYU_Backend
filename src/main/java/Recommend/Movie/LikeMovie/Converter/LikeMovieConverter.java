package Recommend.Movie.LikeMovie.Converter;

import Recommend.Movie.LikeMovie.Domain.LikedMovie;
import Recommend.Movie.LikeMovie.Domain.LikeMovieListResponseDTO;
import Recommend.Movie.LikeMovie.Dto.MovieReactionRequestDTO;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.User.Domain.User;

import java.time.LocalDateTime;

public class LikeMovieConverter {

    public static LikedMovie toEntity(MovieReactionRequestDTO requestDTO, User user, Movie movie){
        return LikedMovie.builder()
                .createAt(LocalDateTime.now())
                .reactionType(requestDTO.getReactionType())
                .movie(movie)
                .user(user)
                .build();
    }

    public static LikeMovieListResponseDTO toDTO(Movie movie){
        return LikeMovieListResponseDTO.builder()
                .posterPath(movie.getPosterPath())
                .tmdbId(movie.getTmdbId())
                .movieTitle(movie.getTitle())
                .build();
    }
}
