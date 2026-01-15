package Recommend.Movie.Movies.Repository;

import Recommend.Movie.Tmdb.Domain.Movie;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieResponseRepository extends JpaRepository<Movie, Integer> {
    @EntityGraph(attributePaths = {"peoples", "peoples.people"})
    Optional<Movie> findById(int id);

    @Query("SELECT m FROM Movie m JOIN m.genres mg WHERE mg.genre.name = :genreName ORDER BY m.voteAverage DESC")
    List<Movie> findTop10ByGenreName(@Param("genreName") String genreName, Pageable pageable);

    Optional<Movie> findFirstByOrderByVoteAverageDesc();

    List<Movie> findTop10ByOrderByVoteAverageDesc();

    @Query(value = "SELECT * FROM movie " +
            "WHERE MATCH(title) AGAINST(CONCAT('+', :query) IN BOOLEAN MODE)", nativeQuery = true)
    List<Movie> findByTitleOnly(@Param("query") String query);

    @Query(value = "SELECT DISTINCT m.* FROM movie m " +
            "JOIN movie_people mp ON m.id = mp.movie_id " +
            "JOIN people p ON mp.people_id = p.id " +
            "WHERE MATCH(p.name) AGAINST(CONCAT('+', :query) IN BOOLEAN MODE)", nativeQuery = true)
    List<Movie> findByPersonOnly(@Param("query") String query);

    @Query(value = "SELECT DISTINCT m.* FROM movie m " +
            "JOIN movie_genre mg ON m.id = mg.movie_id " +
            "JOIN genre g ON mg.genre_id = g.id " +
            "WHERE MATCH(g.name) AGAINST(CONCAT('+', :query) IN BOOLEAN MODE)", nativeQuery = true)
    List<Movie> findByGenreOnly(@Param("query") String query);
}