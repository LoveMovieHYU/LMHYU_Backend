package Recommend.Movie.Domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "movie")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "tmdb_id", unique = true, nullable = false)
    private Long tmdbId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String overview;

    @Column(name = "poster_path")
    private String posterPath;

    private int runtime;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "vote_average")
    private double voteAverage;

    private boolean adult;

    @Column(name = "original_language")
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
