package Recommend.Movie.Domain;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Genre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    // 장르 이름
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;

}
