package Recommend.Movie.Feedback.Service;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.Diary.Converter.DiaryConverter;
import Recommend.Movie.Diary.Domain.Diary;
import Recommend.Movie.Feedback.Converter.FeedBackConverter;
import Recommend.Movie.Feedback.Domain.FeedbackEvent;
import Recommend.Movie.Feedback.Domain.LikeMovieListResponseDTO;
import Recommend.Movie.Feedback.Repository.FeedbackEventRepository;
import Recommend.Movie.Feedback.Dto.MovieReactionRequestDTO;
import Recommend.Movie.Movies.Domain.ReactionType;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Repository.MovieRepository;
import Recommend.Movie.User.Domain.User;
import Recommend.Movie.User.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        User user = getUser(userId);
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

    /**
     * 최근 좋아요 누른 영화 조회
     * 하지만 감정일기를 안적은 !
     * */
    public List<LikeMovieListResponseDTO> getLikeMovieList(String userId){
        User user = getUser(userId);

        // 해당 유저가 작성한 MovieId 를 Set 으로 조히
        Set<Integer> diaryMovieIds = user.getDiaryList().stream()
                .map(diary -> diary.getMovie().getId())
                .collect(Collectors.toSet());

        return user.getFeedbackEvents().stream()
                .filter(event -> event.getReactionType() == ReactionType.LIKE)
                .filter(event -> !diaryMovieIds.contains(event.getMovie().getId()))
                .sorted(Comparator.comparing(FeedbackEvent::getId).reversed())
                .map(event -> FeedBackConverter.toDTO(event.getMovie()))
                .collect(Collectors.toList());
    }

    private User getUser(String userId) {
        User user = userRepository.findByUserId(Integer.parseInt(userId));
        if(user == null){
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "유저를 찾을 수 없습니다.");
        }
        return user;
    }

}
