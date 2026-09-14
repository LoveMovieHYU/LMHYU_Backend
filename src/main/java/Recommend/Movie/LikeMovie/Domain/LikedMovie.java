package Recommend.Movie.LikeMovie.Domain;

import Recommend.Movie.Movies.Domain.ReactionType;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.User.Domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity(name = "liked_movie")
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_liked_movie_user_movie",
        columnNames = {"user_id", "tmdb_id"}))
@Builder
@NoArgsConstructor
@Getter
@AllArgsConstructor
public class LikedMovie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tmdb_id", referencedColumnName = "tmdb_id")
    private Movie movie;

    @Column(name = "reaction_type")
    @Enumerated(EnumType.STRING)
    private ReactionType reactionType;

    @Column(name = "create_at")
    private LocalDateTime createAt;

}
