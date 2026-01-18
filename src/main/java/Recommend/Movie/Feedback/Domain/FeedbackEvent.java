package Recommend.Movie.Feedback.Domain;

import Recommend.Movie.Diary.Domain.EmotionTag;
import Recommend.Movie.Movies.Domain.ReactionType;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.User.Domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity(name = "feedback_event")
@Builder
@NoArgsConstructor
@Getter
@AllArgsConstructor
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

    @Column(name = "reaction_type")
    @Enumerated(EnumType.STRING)
    private ReactionType reactionType;

    @Column(name = "create_at")
    private LocalDateTime createAt;

    @Column(name = "emotion_tag")
    @Enumerated(EnumType.STRING)
    private EmotionTag emotionTag;

    public void updateReaction(ReactionType newReaction, EmotionTag newEmotion) {
        this.reactionType = newReaction;
        this.emotionTag = newEmotion;
        this.createAt = LocalDateTime.now();
    }
}
