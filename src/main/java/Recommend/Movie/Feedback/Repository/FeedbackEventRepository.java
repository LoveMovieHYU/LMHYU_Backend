package Recommend.Movie.Feedback.Repository;

import Recommend.Movie.Feedback.Domain.FeedbackEvent;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.User.Domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedbackEventRepository extends JpaRepository<FeedbackEvent,Integer> {
    Optional<FeedbackEvent> findByUserAndMovie(User user, Movie movie);
}
