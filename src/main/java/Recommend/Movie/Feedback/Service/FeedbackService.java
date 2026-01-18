package Recommend.Movie.Feedback.Service;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.Feedback.Converter.FeedBackConverter;
import Recommend.Movie.Feedback.Domain.FeedbackEvent;
import Recommend.Movie.Feedback.Repository.FeedbackEventRepository;
import Recommend.Movie.Feedback.Dto.MovieReactionRequestDTO;
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

    public FeedbackService(FeedbackEventRepository feedbackEventRepository,
                           UserRepository userRepository, MovieRepository movieRepository) {
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
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "유저를 찾을 수 없습니다.");
        }
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "해당 영화가 존재하지 않습니다."));


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
