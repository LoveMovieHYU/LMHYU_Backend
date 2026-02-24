package Recommend.Movie.LikeMovie.Repository;

import Recommend.Movie.LikeMovie.Domain.LikedMovie;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.User.Domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface LikeMovieRepository extends JpaRepository<LikedMovie,Integer> {
    Optional<LikedMovie> findByUserAndMovie(User user, Movie movie);
}
