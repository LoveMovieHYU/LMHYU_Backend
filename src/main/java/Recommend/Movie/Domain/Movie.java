package Recommend.Movie.Domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
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
    private Set<Review> reviews = new HashSet<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MovieCompany> companies = new HashSet<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MovieGenre> genres = new HashSet<>();

    @OneToMany(mappedBy = "movie", cascade =  CascadeType.ALL, orphanRemoval = true)
    private Set<MoviePeople> peoples = new HashSet<>();

    public void addGenre(MovieGenre genre) {
        if (genres == null) genres = new HashSet<>();
        genres.add(genre);
        genre.setMovie(this);
    }
    public void addCompany(MovieCompany company) {
        if (company == null) companies = new HashSet<>();
        companies.add(company);
        company.setMovie(this);
    }
    public void addPeople(MoviePeople people) {
        if (peoples == null) peoples = new HashSet<>();
        peoples.add(people);
        people.setMovie(this);
    }

    public void addReview(Review review) {
        if (review == null) reviews = new HashSet<>();
        reviews.add(review);
        review.setMovie(this);
    }
}
