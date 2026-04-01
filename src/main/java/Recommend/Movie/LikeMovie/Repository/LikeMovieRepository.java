package Recommend.Movie.LikeMovie.Repository;

import Recommend.Movie.LikeMovie.Domain.LikedMovie;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.User.Domain.User;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface LikeMovieRepository extends JpaRepository<LikedMovie,Integer> {
    Optional<LikedMovie> findByUserAndMovie(User user, Movie movie);

    boolean existsByUser_UserIdAndMovie_TmdbId(int userId, long tmdbId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM liked_movie WHERE user_id = :userId", nativeQuery = true)
    void deleteAllByUserId(@Param("userId") int userId);
}
