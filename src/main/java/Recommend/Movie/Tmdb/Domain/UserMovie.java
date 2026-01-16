package Recommend.Movie.Tmdb.Domain;

import Recommend.Movie.User.Domain.User;
import jakarta.persistence.*;

@Entity
@Table(name = "user_movie")
public class UserMovie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;

}
