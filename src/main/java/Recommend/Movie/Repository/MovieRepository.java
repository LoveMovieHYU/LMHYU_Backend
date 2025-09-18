package Recommend.Movie.Repository;

import Recommend.Movie.Domain.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, String> {
    Movie findById(int id);

}
