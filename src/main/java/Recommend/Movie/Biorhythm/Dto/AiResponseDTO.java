package Recommend.Movie.Biorhythm.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AiResponseDTO {
    @JsonProperty("movie_id")
    private Long movieId;
}
