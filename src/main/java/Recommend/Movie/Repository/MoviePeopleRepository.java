package Recommend.Movie.Repository;

import Recommend.Movie.Domain.MoviePeople;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoviePeopleRepository extends JpaRepository<MoviePeople, String> {
    boolean existsByMovie_IdAndPeople_Id(long movieId, int peopleId);

}
