package Recommend.Movie.Repository;

import Recommend.Movie.Domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, String> {

}
