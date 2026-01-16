package Recommend.Movie.Tmdb.Repository;

import Recommend.Movie.Tmdb.Domain.MoviePeople;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoviePeopleRepository extends JpaRepository<MoviePeople, String> {
    boolean existsByMovie_IdAndPeople_Id(long movieId, int peopleId);

}
