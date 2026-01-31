package Recommend.Movie.Tmdb.Repository;

import Recommend.Movie.Tmdb.Domain.Movie;
import io.lettuce.core.dynamic.annotation.Param;
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

    List<Movie> findAllByTmdbId(List<Long> tmdbId);

    // 기본 findById는 연관된 데이터를 안 가져옴 -> JOIN FETCH 사용
    @Query("SELECT m FROM Movie m " +
            "LEFT JOIN FETCH m.peoples mp " +
            "LEFT JOIN FETCH mp.people p " +
            "WHERE m.id = :id")
    Optional<Movie> findByIdWithPeople(@Param("id") int id);

    @Query("SELECT m FROM Movie m JOIN m.genres mg WHERE mg.genre.name = :genreName ORDER BY m.voteAverage DESC")
    List<Movie> findTop10ByGenreName(@org.springframework.data.repository.query.Param("genreName") String genreName, Pageable pageable);

    Optional<Movie> findFirstByOrderByVoteAverageDesc();


}
