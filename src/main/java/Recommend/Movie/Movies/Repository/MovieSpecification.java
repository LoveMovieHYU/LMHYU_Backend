package Recommend.Movie.Movies.Repository;

import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Domain.MoviePeople;
import Recommend.Movie.Tmdb.Domain.People;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class MovieSpecification {
    public static Specification<Movie> searchByKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);

            Join<Movie, MoviePeople> moviePeopleJoin = root.join("peoples", JoinType.LEFT);
            Join<MoviePeople, People> peopleJoin = moviePeopleJoin.join("people", JoinType.LEFT);

            String searchPattern = "%" + keyword + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(root.get("title"), searchPattern),
                    criteriaBuilder.like(peopleJoin.get("name"), searchPattern)
            );
        };
    }
}
