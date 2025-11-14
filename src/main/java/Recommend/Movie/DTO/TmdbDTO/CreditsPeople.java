package Recommend.Movie.DTO.TmdbDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CreditsPeople {
    private int id;
    private String name;
    private Integer gender;
    @JsonProperty("profile_path")
    private String profilePath;

    private String job;  // crew 용
    private Integer order; // cast 용

}
