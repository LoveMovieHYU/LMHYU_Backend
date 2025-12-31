package Recommend.Movie.Repository;

import Recommend.Movie.Domain.Movie;
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
    // N+1 문제를 방지하기 위해 peoples와 그 안의 people 정보를 한꺼번에 fetch join 합니다.
    @EntityGraph(attributePaths = {"peoples", "peoples.people"})
    Optional<Movie> findById(int id);

    // 평점 순으로 특정 장르의 영화 상위 10개를 가져오는 쿼리
    @Query("SELECT m FROM Movie m JOIN m.genres mg WHERE mg.genre.name = :genreName ORDER BY m.voteAverage DESC")
    List<Movie> findTop10ByGenreName(@Param("genreName") String genreName, Pageable pageable);

    // 오늘의 추천 영화를 위해 평점이 가장 높은 영화 1건 조회
    Optional<Movie> findFirstByOrderByVoteAverageDesc();

    // 2. 초기 화면: 평점이 높은 순으로 상위 10개 조회 (요즘 인기 작품)
    List<Movie> findTop10ByOrderByVoteAverageDesc();

    // 1. 제목으로만 검색
    @Query(value = "SELECT * FROM movie " +
            "WHERE MATCH(title) AGAINST(CONCAT('+', :query) IN BOOLEAN MODE)", nativeQuery = true)
    List<Movie> findByTitleOnly(@Param("query") String query);

    // 2. 인물(배우/감독) 이름으로만 검색
    @Query(value = "SELECT DISTINCT m.* FROM movie m " +
            "JOIN movie_people mp ON m.id = mp.movie_id " +
            "JOIN people p ON mp.people_id = p.id " +
            "WHERE MATCH(p.name) AGAINST(CONCAT('+', :query) IN BOOLEAN MODE)", nativeQuery = true)
    List<Movie> findByPersonOnly(@Param("query") String query);

    // 3. 장르 이름으로만 검색
    @Query(value = "SELECT DISTINCT m.* FROM movie m " +
            "JOIN movie_genre mg ON m.id = mg.movie_id " +
            "JOIN genre g ON mg.genre_id = g.id " +
            "WHERE MATCH(g.name) AGAINST(CONCAT('+', :query) IN BOOLEAN MODE)", nativeQuery = true)
    List<Movie> findByGenreOnly(@Param("query") String query);
}