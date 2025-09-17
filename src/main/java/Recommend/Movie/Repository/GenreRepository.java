package Recommend.Movie.Repository;

import Recommend.Movie.Domain.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, String> {
    
}
