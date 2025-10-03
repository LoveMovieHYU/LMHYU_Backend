package Recommend.Movie.DTO.UserDTO;

import Recommend.Movie.Domain.SocialProviderType;
import Recommend.Movie.Domain.UserRoleType;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@Builder
public class LoginDTO {
    private String name;
    private String providerId;
    private boolean isLock;
    private boolean isSocial;
    private SocialProviderType socialProviderType;
    private String email;
    private UserRoleType role;

}
