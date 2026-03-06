package Recommend.Movie.Tmdb.Repository;

import Recommend.Movie.Tmdb.Domain.Movie;
import org.springframework.data.domain.Page;
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

    @Query(
            value = "SELECT m.* FROM movie m " +
                    "WHERE MATCH(m.title) AGAINST(:keyword IN BOOLEAN MODE) " +
                    "OR MATCH(m.title_no_space) AGAINST(:noSpaceKeyword IN BOOLEAN MODE) " +
                    "OR m.id IN (" +
                    "    SELECT mp.movie_id FROM movie_people mp " +
                    "    JOIN people p ON mp.people_id = p.id " +
                    "    WHERE MATCH(p.name) AGAINST(:keyword IN BOOLEAN MODE)" +
                    ") " +
                    "ORDER BY (MATCH(m.title) AGAINST(:keyword IN BOOLEAN MODE) + MATCH(m.title_no_space) AGAINST(:noSpaceKeyword IN BOOLEAN MODE)) DESC, m.release_date DESC",

            countQuery = "SELECT count(*) FROM movie m " +
                    "WHERE MATCH(m.title) AGAINST(:keyword IN BOOLEAN MODE) " +
                    "OR MATCH(m.title_no_space) AGAINST(:noSpaceKeyword IN BOOLEAN MODE) " +
                    "OR m.id IN (" +
                    "    SELECT mp.movie_id FROM movie_people mp " +
                    "    JOIN people p ON mp.people_id = p.id " +
                    "    WHERE MATCH(p.name) AGAINST(:keyword IN BOOLEAN MODE)" +
                    ")",
            nativeQuery = true
    )
    Page<Movie> searchByKeywordNative(
            @Param("keyword") String keyword,
            @Param("noSpaceKeyword") String noSpaceKeyword,
            Pageable pageable
    );
}
