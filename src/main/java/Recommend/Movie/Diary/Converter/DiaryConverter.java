package Recommend.Movie.Diary.Converter;

import Recommend.Movie.Diary.Domain.Diary;
import Recommend.Movie.Diary.Dto.DiaryRequestDTO;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.User.Domain.User;

import java.time.LocalDate;

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
}
