package Recommend.Movie.Feedback.Domain;

import Recommend.Movie.Diary.Domain.EmotionTag;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Domain.RelationType;
import Recommend.Movie.User.Domain.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity(name = "feedback_event")
public class FeedbackEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @Column(name = "relation_type")
    @Enumerated(EnumType.STRING)
    private RelationType relationType;

    @Column(name = "create_at")
    private LocalDateTime createAt;

    @Column(name = "emotion_tag")
    @Enumerated(EnumType.STRING)
    private EmotionTag emotionTag;
}
