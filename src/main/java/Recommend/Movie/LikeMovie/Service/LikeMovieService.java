package Recommend.Movie.LikeMovie.Service;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.LikeMovie.Converter.LikeMovieConverter;
import Recommend.Movie.LikeMovie.Domain.LikedMovie;
import Recommend.Movie.LikeMovie.Dto.LikeMovieListResponseDTO;
import Recommend.Movie.LikeMovie.Repository.LikeMovieRepository;
import Recommend.Movie.LikeMovie.Dto.MovieReactionRequestDTO;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Repository.MovieRepository;
import Recommend.Movie.User.Domain.User;
import Recommend.Movie.User.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LikeMovieService {

    private final LikeMovieRepository likeMovieRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;

    public LikeMovieService(LikeMovieRepository likeMovieRepository,
                           UserRepository userRepository, MovieRepository movieRepository) {
        this.likeMovieRepository = likeMovieRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
    }

    /**
     * 추천된 영화 반응 저장
     * */
    @Transactional
    public String saveMovieReaction(long tmdbId, MovieReactionRequestDTO requestDTO,
                                    String userId){
        int id = parseUserId(userId);
        User user = getUser(id);
        Movie movie = movieRepository.findByTmdbId(tmdbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "해당 영화가 존재하지 않습니다."));

        // 중복 좋아요 멱등 처리: 이미 좋아요한 영화면 중복 저장하지 않고 조용히 반환한다.
        if (likeMovieRepository.existsByUser_UserIdAndMovie_TmdbId(id, tmdbId)) {
            return "이미 좋아요한 영화입니다.";
        }

        LikedMovie likedMovie = LikeMovieConverter.toEntity(requestDTO, user, movie);
        user.addLikeMovie(likedMovie);
        likeMovieRepository.save(likedMovie);
        return "성공했습니다.";

    }

    /**
     * 최근 좋아요 누른 영화 조회
     * */
    @Transactional(readOnly = true)
    public List<LikeMovieListResponseDTO> getLikeMovieList(String userId){
        int id = parseUserId(userId);
        getUser(id); // 유저 존재 검증 (없으면 USER_NOT_FOUND)

        // ReactionType 은 현재 LIKE 만 정의되어 있어 liked_movie 에는 LIKE 만 저장된다.
        // (DISLIKE 등이 추가되면 여기서 reactionType 필터를 함께 도입해야 한다.)
        return likeMovieRepository.findByUserIdWithMovie(id).stream()
                .map(likedMovie -> LikeMovieConverter.toDTO(likedMovie.getMovie()))
                .collect(Collectors.toList());
    }

    /**
     * 좋아요 삭제
     * */
    @Transactional
    public String deleteLikeMovie(long tmdbId, String userId){
        User user = getUser(parseUserId(userId));
        Movie movie = movieRepository.findByTmdbId(tmdbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "해당 영화가 존재하지 않습니다."));

        LikedMovie likedMovie = likeMovieRepository.findByUserAndMovie(user, movie)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIKE_MOVIE_NOT_FOUND, "좋아요한 영화가 존재하지 않습니다."));

        user.removeLikeMovie(likedMovie);
        likeMovieRepository.delete(likedMovie);
        return "좋아요가 취소되었습니다.";

    }

    private User getUser(int userId) {
        User user = userRepository.findByUserId(userId);
        if(user == null){
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "유저를 찾을 수 없습니다.");
        }
        return user;
    }

    /**
     * Principal 에서 넘어온 userId 문자열을 int 로 변환한다.
     * 파싱 실패는 500(NumberFormatException) 대신 BusinessException 으로 규약에 맞게 변환한다.
     */
    private int parseUserId(String userId) {
        try {
            return Integer.parseInt(userId);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 사용자 식별자입니다.");
        }
    }

}
