package Recommend.Movie.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class MovieDetailDTO {
    private int id;
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
    private String OriginalLanguage;

    private List<GenreDTO> genres;

    @JsonProperty("production_companies")
    private List<CompanyDTO> productionCompanies;
}