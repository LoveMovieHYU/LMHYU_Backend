package Recommend.Movie.Tmdb.Repository;

import Recommend.Movie.Tmdb.Domain.MovieGenre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieGenreRepository extends JpaRepository<MovieGenre, Long> {
    boolean existsByMovie_IdAndGenre_Id(int movieId, int genreId);

    // Fetch join 을 사용하여 N+1 쿼리 문제 해결
    @Query("SELECT mg FROM MovieGenre mg JOIN FETCH mg.genre WHERE mg.movie.id IN :movieIds")
    List<MovieGenre> findByMovie_IdIn(List<Integer> movieIds);

}
