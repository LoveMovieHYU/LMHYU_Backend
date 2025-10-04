package Recommend.Movie.DTO.UserDTO;

import lombok.Getter;

@Getter
public class UserFindResponseDTO {
    private String name;
    private String email;

    public UserFindResponseDTO(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
