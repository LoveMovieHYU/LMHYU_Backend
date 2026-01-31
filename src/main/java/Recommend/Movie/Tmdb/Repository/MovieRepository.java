package Recommend.Movie.Tmdb.Repository;

import Recommend.Movie.Tmdb.Domain.Movie;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Integer>, JpaSpecificationExecutor<Movie> {
    Optional<Movie> findByTmdbId(Long tmdbId);

    List<Movie> findAllByTmdbIdIn(List<Long> tmdbIds);

    @Query("""
    SELECT DISTINCT m FROM Movie m
    LEFT JOIN FETCH m.peoples mp
    LEFT JOIN FETCH mp.people p
    WHERE m.tmdbId = :tmdbId
    """)
    Optional<Movie> findByTmdbIdWithPeople(@Param("tmdbId") long tmdbId);

}
