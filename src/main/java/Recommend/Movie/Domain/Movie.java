package Recommend.Movie.Domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true, nullable = false)
    private Long tmdbId;

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
    private double voteAverage;
    // 성인물 여부
    private boolean adult;
    // 원어
    private String originalLanguage;

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews;

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MovieCompany> companies = new ArrayList<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MovieGenre> genres = new ArrayList<>();

    public void addGenre(MovieGenre genre) {
        genres.add(genre);
        genre.setMovie(this);
    }
    public void addCompany(MovieCompany company) {
        companies.add(company);
        company.setMovie(this);
    }

    public void addReview(Review review) {
        reviews.add(review);
        review.setMovie(this);
    }
}
