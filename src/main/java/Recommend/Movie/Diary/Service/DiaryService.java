package Recommend.Movie.Diary.Service;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.Diary.Converter.DiaryConverter;
import Recommend.Movie.Diary.Domain.Diary;
import Recommend.Movie.Diary.Dto.DiaryRequestDTO;
import Recommend.Movie.Diary.Dto.DiaryResponseDTO;
import Recommend.Movie.Diary.Repository.DiaryRepository;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Repository.MovieRepository;
import Recommend.Movie.User.Domain.User;
import Recommend.Movie.User.Repository.UserRepository;
import org.springframework.stereotype.Service;


@Service
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;

    public DiaryService(DiaryRepository diaryRepository, UserRepository userRepository,
                        MovieRepository movieRepository) {
        this.diaryRepository = diaryRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
    }

    public DiaryResponseDTO createDiary(DiaryRequestDTO diaryRequestDTO, int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "가입된 유저가 없습니다."));

        Movie movie = movieRepository.findById(diaryRequestDTO.getMovieId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "해당 영화가 존재하지 않습니다."));

        Diary diary = DiaryConverter.toEntity(diaryRequestDTO, user, movie);
        user.addDiary(diary);

        diaryRepository.save(diary);
        return new DiaryResponseDTO("일기 작성이 완료됐습니다.");

    }
}
