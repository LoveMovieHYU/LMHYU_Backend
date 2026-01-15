package Recommend.Movie.User.Dto;

import Recommend.Movie.User.Domain.SocialProviderType;
import Recommend.Movie.User.Domain.UserRoleType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginDTO {
    private String name;
    private String providerId;
    private boolean isLock;
    private boolean isSocial;
    private SocialProviderType socialProviderType;
    private String email;
    private UserRoleType role;
    private String ageGroup;
    private String gender;

}
