package Recommend.Movie.LikeMovie.Dto;

import Recommend.Movie.Movies.Domain.ReactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovieReactionRequestDTO {
    @Schema(description = "반응 타입", example = "LIKE")
    private ReactionType reactionType;
}
