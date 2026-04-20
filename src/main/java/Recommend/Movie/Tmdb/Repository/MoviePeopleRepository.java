package Recommend.Movie.Tmdb.Repository;

import Recommend.Movie.Tmdb.Domain.MoviePeople;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MoviePeopleRepository extends JpaRepository<MoviePeople, String> {
    boolean existsByMovie_IdAndPeople_Id(long movieId, int peopleId);
    List<MoviePeople> findByMovie_Id(int movieId);

}
