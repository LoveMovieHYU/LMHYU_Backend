package Recommend.Movie.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CompanyDTO {
    private int id;
    private String name;
    @JsonProperty("logo_path")
    private String logoPath;
}
