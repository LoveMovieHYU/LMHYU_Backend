package Recommend.Movie.Repository;

import Recommend.Movie.Domain.MovieGenre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieGenreRepository extends JpaRepository<MovieGenre, Long> {
    boolean existsByMovie_IdAndGenre_Id(int movieId, int genreId);

}
