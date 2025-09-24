package Recommend.Movie.Login.DTO;

public class AuthResponse {
    private String accessToken;

    public AuthResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    // Getter
    public String getAccessToken() {
        return accessToken;
    }
}
