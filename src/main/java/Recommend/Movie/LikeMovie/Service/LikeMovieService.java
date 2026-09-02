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
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

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
        User user = getUser(userId);
        Movie movie = movieRepository.findByTmdbId(tmdbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "해당 영화가 존재하지 않습니다."));


        LikedMovie likedMovie = LikeMovieConverter.toEntity(requestDTO, user, movie);
        user.addLikeMovie(likedMovie);
        likeMovieRepository.save(likedMovie);
        return "성공했습니다.";

    }

    /**
     * 최근 좋아요 누른 영화 조회
     * */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<LikeMovieListResponseDTO> getLikeMovieList(String userId){
        getUser(userId); // 유저 존재 검증 (없으면 USER_NOT_FOUND)

        return likeMovieRepository.findByUserIdWithMovie(Integer.parseInt(userId)).stream()
                .map(likedMovie -> LikeMovieConverter.toDTO(likedMovie.getMovie()))
                .collect(Collectors.toList());
    }

    /**
     * 좋아요 삭제
     * */
    @Transactional
    public String deleteLikeMovie(long tmdbId, String userId){
        User user = getUser(userId);
        Movie movie = movieRepository.findByTmdbId(tmdbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "해당 영화가 존재하지 않습니다."));

        LikedMovie likedMovie = likeMovieRepository.findByUserAndMovie(user, movie)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIKE_MOVIE_NOT_FOUND, "좋아요한 영화가 존재하지 않습니다."));

        user.removeLikeMovie(likedMovie);
        likeMovieRepository.delete(likedMovie);
        return "좋아요가 취소되었습니다.";

    }

    private User getUser(String userId) {
        User user = userRepository.findByUserId(Integer.parseInt(userId));
        if(user == null){
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "유저를 찾을 수 없습니다.");
        }
        return user;
    }

}
