package Recommend.Movie.Feedback.Converter;

import Recommend.Movie.Feedback.Domain.FeedbackEvent;
import Recommend.Movie.Movies.Dto.MovieReactionRequestDTO;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.User.Domain.User;

import java.time.LocalDateTime;

public class FeedBackConverter {

    public static FeedbackEvent toEntity(MovieReactionRequestDTO requestDTO, User user, Movie movie){
        return FeedbackEvent.builder()
                .createAt(LocalDateTime.now())
                .emotionTag(requestDTO.getEmotionTag())
                .reactionType(requestDTO.getReactionType())
                .movie(movie)
                .user(user)
                .build();
    }
}
