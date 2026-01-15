package Recommend.Movie.Tmdb.Repository;

import Recommend.Movie.Tmdb.Domain.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, String> {
    Optional<Movie> findByTmdbId(Long tmdbId);
}
