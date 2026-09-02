package Recommend.Movie.Tmdb.Repository;

import Recommend.Movie.Tmdb.Domain.MoviePeople;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MoviePeopleRepository extends JpaRepository<MoviePeople, String> {
    boolean existsByMovie_IdAndPeople_Id(long movieId, int peopleId);

    // People 을 함께 fetch 하여 상세 조회 시 배우/감독 수만큼 발생하던 N+1 을 제거한다.
    @Query("SELECT mp FROM MoviePeople mp JOIN FETCH mp.people WHERE mp.movie.id = :movieId")
    List<MoviePeople> findByMovie_Id(@Param("movieId") int movieId);

}
