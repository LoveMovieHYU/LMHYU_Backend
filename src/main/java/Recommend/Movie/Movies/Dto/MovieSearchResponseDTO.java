package Recommend.Movie.Movies.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieSearchResponseDTO {
    private int movieId;
    private String title;
    private String posterUrl;
    private LocalDate releaseDate;
}
