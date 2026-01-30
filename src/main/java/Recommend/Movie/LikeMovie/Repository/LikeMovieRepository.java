package Recommend.Movie.LikeMovie.Repository;

import Recommend.Movie.LikeMovie.Domain.LikedMovie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface LikeMovieRepository extends JpaRepository<LikedMovie,Integer> {
}
