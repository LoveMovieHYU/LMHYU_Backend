package Recommend.Movie.Tmdb.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PeopleDetailDTO {
    private String biography;
    private String birthday;

    @JsonProperty("profile_path")
    private String profile_path;
}
