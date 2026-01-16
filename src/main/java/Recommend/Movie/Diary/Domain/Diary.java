package Recommend.Movie.Diary.Domain;

import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.User.Domain.User;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diary_id")
    private int diaryId;

    private String content;

    private String title;

    @Column(name = "create_at")
    private LocalDate createAt;

    @Column(name = "emotion_tag")
    @Enumerated(EnumType.STRING)
    private EmotionTag emotionTag;

    @Column(name = "movie_title")
    private String movieTitle;

    @Column(name = "rating")
    private Float rating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = true)
    private Movie movie;

}
