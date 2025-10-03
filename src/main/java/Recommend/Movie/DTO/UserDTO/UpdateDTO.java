package Recommend.Movie.DTO.UserDTO;

import lombok.Getter;

@Getter
public class UpdateDTO {
    String providerId;
    String email;

    public UpdateDTO(String providerId, String email) {
        this.providerId = providerId;
        this.email = email;
    }
}
