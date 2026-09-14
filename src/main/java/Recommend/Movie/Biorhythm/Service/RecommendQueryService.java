package Recommend.Movie.Biorhythm.Service;

import Recommend.Movie.Biorhythm.Converter.RecommendConverter;
import Recommend.Movie.Biorhythm.Dto.AiResponseDTO;
import Recommend.Movie.Biorhythm.Dto.MovieAiRecommendationDTO;
import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Domain.MovieGenre;
import Recommend.Movie.Tmdb.Repository.MovieGenreRepository;
import Recommend.Movie.Tmdb.Repository.MovieRepository;
import Recommend.Movie.User.Domain.User;
import Recommend.Movie.User.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 추천 도메인의 DB 접근(읽기 전용)만 담당하는 서비스.
 * 외부 AI 호출·Redis I/O 와 트랜잭션 경계를 분리하기 위해 별도 빈으로 둔다.
 * (RecommendService 에서 프록시를 통해 호출되어야 트랜잭션이 실제로 적용된다.)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendQueryService {

    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final MovieGenreRepository movieGenreRepository;

    /**
     * 유저 조회 (읽기 전용 트랜잭션).
     * 생년월일이 없으면 예외를 던진다.
     */
    @Transactional(readOnly = true)
    public User getUser(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "유저가 없습니다."));
        if (user.getBirthday() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "생년월일 정보가 필요합니다. 마이페이지에서 설정해주세요.");
        }
        return user;
    }

    /**
     * AI 추천 결과의 TmdbID 목록으로 영화·장르를 조회한 뒤 DTO 로 변환한다 (읽기 전용 트랜잭션).
     * 장르는 영화 PK 목록으로 한 번에 조회해 N+1 을 방지한다.
     */
    @Transactional(readOnly = true)
    public List<MovieAiRecommendationDTO> getMovieAiRecommendationDTOs(AiResponseDTO[] aiResponseArray) {
        List<Long> tmdbIds = Arrays.stream(aiResponseArray)
                .map(AiResponseDTO::getMovieId)
                .collect(Collectors.toList());

        List<Movie> movies = movieRepository.findAllByTmdbIdIn(tmdbIds);

        List<Integer> movieIds = movies.stream()
                .map(Movie::getId)
                .collect(Collectors.toList());

        // 추출한 영화 PK 목록으로 장르 매핑 정보를 한번에 조회 ( N+1 문제 방지 )
        List<MovieGenre> allMovieGenres = movieGenreRepository.findByMovie_IdIn(movieIds);

        Map<Integer, List<MovieGenre>> genreMap = allMovieGenres.stream()
                .collect(Collectors.groupingBy(mg -> mg.getMovie().getId())); // 장르를 영화 ID 기준으로 그룹화

        Map<Long, Movie> movieMap = movies.stream()
                .collect(Collectors.toMap(Movie::getTmdbId, Function.identity())); // tmdbId를 key 로 가지는 Movie Map 생성

        return tmdbIds.stream()
                .map(movieMap::get)
                .filter(Objects::nonNull)
                .map(movie -> {
                    List<MovieGenre> genres = genreMap.getOrDefault(movie.getId(), Collections.emptyList());
                    return RecommendConverter.fromEntity(movie, genres);
                })
                .sorted(Comparator.comparing(MovieAiRecommendationDTO::getPopularity, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }
}
