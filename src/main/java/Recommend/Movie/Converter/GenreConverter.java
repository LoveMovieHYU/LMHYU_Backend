package Recommend.Movie.Converter;

import Recommend.Movie.DTO.GenreDTO;
import Recommend.Movie.Domain.Genre;

public class GenreConverter {
    public static Genre toEntity(GenreDTO genreDTO) {
        return Genre.builder()
                .id(genreDTO.getId())
                .name(genreDTO.getName())
                .build();

    }
}
