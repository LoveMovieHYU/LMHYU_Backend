package Recommend.Movie.Diary.Converter;

import Recommend.Movie.Diary.Domain.Diary;
import Recommend.Movie.Diary.Dto.DiaryMonthResponseDTO;
import Recommend.Movie.Diary.Dto.DiaryPreviewResponseDTO;
import Recommend.Movie.Diary.Dto.DiaryRequestDTO;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.User.Domain.User;

import java.time.LocalDate;
import java.util.List;

public class DiaryConverter {

    public static Diary toEntity(DiaryRequestDTO requestDTO, User user, Movie movie){
        return Diary.builder()
                .emotionTag(requestDTO.getEmotionTag())
                .title(requestDTO.getTitle())
                .content(requestDTO.getContent())
                .createAt(LocalDate.now())
                .rating(requestDTO.getRating())
                .user(user)
                .movie(movie)
                .movieTitle(movie.getTitle())
                .build();
    }

    public static DiaryPreviewResponseDTO toDTO(Diary diary, Movie movie){
        return DiaryPreviewResponseDTO.builder()
                .posterPath(movie.getPosterPath())
                .content(diary.getContent())
                .movieTitle(movie.getTitle())
                .createAt(diary.getCreateAt())
                .rating(diary.getRating())
                .build();
    }

    public static DiaryMonthResponseDTO toMonthDTO(Diary diary) {
        return DiaryMonthResponseDTO.builder()
                .diaryId(diary.getDiaryId())
                .createAt(diary.getCreateAt())
                .build();
    }

}
