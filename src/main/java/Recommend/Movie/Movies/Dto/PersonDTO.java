package Recommend.Movie.Movies.Dto;

import Recommend.Movie.Tmdb.Domain.People;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PersonDTO {
    private int id;
    private String name;
    private String profileImagePath;

    public static PersonDTO from(People person) {
        return PersonDTO.builder()
                .id(person.getId())
                .name(person.getName())
                .profileImagePath(person.getProfileImagePath())
                .build();
    }
}