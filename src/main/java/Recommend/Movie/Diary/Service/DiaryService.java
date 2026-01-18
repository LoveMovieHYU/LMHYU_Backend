package Recommend.Movie.Diary.Service;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.Diary.Converter.DiaryConverter;
import Recommend.Movie.Diary.Domain.Diary;
import Recommend.Movie.Diary.Dto.*;
import Recommend.Movie.Diary.Repository.DiaryRepository;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Repository.MovieRepository;
import Recommend.Movie.User.Domain.User;
import Recommend.Movie.User.Repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


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

    // 감정일기 작성
    public DiaryResponseDTO createDiary(DiaryRequestDTO diaryRequestDTO, int userId) {
        User user = getUser(userId);

        Movie movie = movieRepository.findById(diaryRequestDTO.getMovieId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "해당 영화가 존재하지 않습니다."));

        Diary diary = DiaryConverter.toEntity(diaryRequestDTO, user, movie);
        user.addDiary(diary);

        diaryRepository.save(diary);
        return new DiaryResponseDTO("일기 작성이 완료됐습니다.");
    }

    // 감정일기 미리보기 조회
    public DiaryPreviewResponseDTO getDiaryPreview(LocalDate date, int userId){
        User user = getUser(userId);
        Diary diary = diaryRepository.findByUserAndCreateAt(user, date);
        Movie movie = diary.getMovie();
        DiaryPreviewResponseDTO responseDTO = DiaryConverter.toDTO(diary, movie);
        return responseDTO;
    }

    // 해당 달 작성된 감정일기 반환
    public List<DiaryMonthResponseDTO> getMontyDiaryList(YearMonth date, int userId){
        User user = getUser(userId);
        LocalDate startDate = date.atDay(1);
        LocalDate endDate = date.atEndOfMonth();
        List<Diary> diaryList = diaryRepository.findAllByUserAndCreateAtBetween(user, startDate, endDate);

        return diaryList.stream()
                .map(DiaryConverter::toMonthDTO)
                .collect(Collectors.toList());
    }
    
    // 일기 상세 조회
    public DiaryDetailResponseDTO getDetailDiary(int diaryId){
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND,"해당 감정일기가 없습니다."));

        return DiaryConverter.toDetailDTO(diary);

    }

    private User getUser(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "가입된 유저가 없습니다."));
        return user;
    }
}
