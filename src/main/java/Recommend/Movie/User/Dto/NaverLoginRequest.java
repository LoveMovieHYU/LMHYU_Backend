package Recommend.Movie.User.Dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NaverLoginRequest {
    private String accessToken;

    public NaverLoginRequest(String accessToken) {
        this.accessToken = accessToken;
    }
}