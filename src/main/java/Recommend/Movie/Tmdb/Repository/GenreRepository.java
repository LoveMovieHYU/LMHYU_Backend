package Recommend.Movie.Tmdb.Repository;

import Recommend.Movie.Tmdb.Domain.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Integer> {
    Genre getReferenceById(int id);
}
