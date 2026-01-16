package Recommend.Movie.Tmdb.Converter;

import Recommend.Movie.Tmdb.Dto.GenreDTO;
import Recommend.Movie.Tmdb.Domain.Genre;

public class GenreConverter {
    public static Genre toEntity(GenreDTO genreDTO) {
        return Genre.builder()
                .id(genreDTO.getId())
                .name(genreDTO.getName())
                .build();

    }
}
