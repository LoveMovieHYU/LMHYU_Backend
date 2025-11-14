package Recommend.Movie.DTO.TmdbDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class MovieDetailDTO {

    @JsonProperty("id")
    private Long tmdbId;

    private String title;
    private String overview;
    private Integer runtime;
    private Boolean adult;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("backdrop_path")
    private String backdropPath;

    @JsonProperty("release_date")
    private LocalDate releaseDate;

    @JsonProperty("vote_average")
    private Double voteAverage;

    @JsonProperty("vote_count")
    private Integer voteCount;

    @JsonProperty("original_language")
    private String originalLanguage;

    private List<GenreDTO> genres;

    @JsonProperty("production_companies")
    private List<CompanyDTO> productionCompanies;
}