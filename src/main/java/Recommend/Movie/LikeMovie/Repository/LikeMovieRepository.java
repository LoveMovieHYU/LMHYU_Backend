package Recommend.Movie.LikeMovie.Repository;

import Recommend.Movie.LikeMovie.Domain.LikedMovie;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.User.Domain.User;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface LikeMovieRepository extends JpaRepository<LikedMovie,Integer> {
    Optional<LikedMovie> findByUserAndMovie(User user, Movie movie);

    boolean existsByUser_UserIdAndMovie_TmdbId(int userId, long tmdbId);

    // Movie 를 함께 fetch 하여 좋아요 목록 조회 시 N+1 을 제거한다. (최신순 정렬)
    // ReactionType 은 현재 LIKE 만 존재하여 liked_movie 에는 LIKE 만 저장되므로 reactionType 필터가 불필요하다.
    // (DISLIKE 등이 추가되면 WHERE 절에 reactionType 조건을 함께 추가해야 한다.)
    @Query("SELECT lm FROM liked_movie lm JOIN FETCH lm.movie WHERE lm.user.userId = :userId ORDER BY lm.id DESC")
    List<LikedMovie> findByUserIdWithMovie(@Param("userId") int userId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM liked_movie WHERE user_id = :userId", nativeQuery = true)
    void deleteAllByUserId(@Param("userId") int userId);
}
