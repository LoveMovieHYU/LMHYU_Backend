package Recommend.Movie.User.Dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoogleLoginRequest {
    private String idToken;
    private String accessToken;


    public GoogleLoginRequest(String idToken) {
        this.idToken = idToken;
        this.accessToken = accessToken;
    }
}