package Recommend.Movie.DTO;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor // JSON 역직렬화를 위해 기본 생성자 추가
public class GoogleLoginRequest {
    private String idToken;

    // 초기화가 필요한 경우를 위한 생성자
    public GoogleLoginRequest(String idToken) {
        this.idToken = idToken;
    }
}