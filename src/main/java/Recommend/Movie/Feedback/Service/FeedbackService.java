package Recommend.Movie.Feedback.Service;

import Recommend.Movie.Config.Exception.MovieNotFoundExceptionHandler;
import Recommend.Movie.Config.Exception.UserNotFoundExceptionHandler;
import Recommend.Movie.Feedback.Converter.FeedBackConverter;
import Recommend.Movie.Feedback.Domain.FeedbackEvent;
import Recommend.Movie.Feedback.Repository.FeedbackEventRepository;
import Recommend.Movie.Movies.Dto.MovieReactionRequestDTO;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Repository.MovieRepository;
import Recommend.Movie.User.Domain.User;
import Recommend.Movie.User.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FeedbackService {

    private final FeedbackEventRepository feedbackEventRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;

    public FeedbackService(FeedbackEventRepository feedbackEventRepository, UserRepository userRepository, MovieRepository movieRepository) {
        this.feedbackEventRepository = feedbackEventRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
    }

    /**
     * 추천된 영화 반응 저장
     * */
    @Transactional
    public String saveMovieReaction(int movieId, MovieReactionRequestDTO requestDTO,
                                    String userId){
        User user = userRepository.findByUserId(Integer.parseInt(userId));
        if(user == null){
            throw new UserNotFoundExceptionHandler("가입된 유저가 없습니다.");
        }
        Optional<Movie> movieOptional = movieRepository.findById(movieId);
        if(movieOptional.isEmpty()){
            throw new MovieNotFoundExceptionHandler("해당 영화는 없습니다.");
        }
        Movie movie = movieOptional.get();

        Optional<FeedbackEvent> existingFeedback = feedbackEventRepository.findByUserAndMovie(user, movie);

        if(existingFeedback.isPresent()){
            FeedbackEvent feedback = existingFeedback.get();
            feedback.updateReaction(requestDTO.getReactionType(), requestDTO.getEmotionTag());
            return "반응이 수정됐습니다.";
        }else{
            FeedbackEvent feedbackEvent = FeedBackConverter.toEntity(requestDTO, user, movie);
            user.addFeedback(feedbackEvent);
            feedbackEventRepository.save(feedbackEvent);
            return "성공했습니다.";
        }
    }
}
