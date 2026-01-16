package Recommend.Movie.User.Converter;

import Recommend.Movie.User.Dto.LoginDTO;
import Recommend.Movie.User.Dto.UpdateDTO;
import Recommend.Movie.User.Domain.User;

public class UserConverter {

    public static User toEntity(LoginDTO dto){
        return User.builder()
                .name(dto.getName())
                .providerId(dto.getProviderId())
                .email(dto.getEmail())
                .socialProviderType(dto.getSocialProviderType())
                .isLock(dto.isLock())
                .isSocial(dto.isSocial())
                .roleType(dto.getRole())
                .ageGroup(dto.getAgeGroup())
                .gender(dto.getGender())
                .build();
    }
    public static User updateUser(UpdateDTO dto){
        return User.builder()
                .providerId(dto.getProviderId())
                .email(dto.getEmail())
                .build();
    }
}
