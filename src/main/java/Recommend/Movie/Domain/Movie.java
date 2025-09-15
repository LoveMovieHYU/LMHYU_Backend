package Recommend.Movie.Domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    // 영화 제목
    private String title;
    // 개요
    @Column(columnDefinition = "TEXT")
    private String overview;
    // 포스터 이미지 경로
    private String posterPath;
    // 상영시간
    private int runtime;
    // 개봉날짜
    private LocalDate releaseDate;
    // 평점
    private int voteAverage;
    // 성인물 여부
    private boolean adult;
    // 원어
    private String originalLanguage;

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews;

    public void addReview(Review review) {
        reviews.add(review);
        review.setMovie(this);
    }
}
